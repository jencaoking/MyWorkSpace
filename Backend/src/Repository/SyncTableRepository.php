<?php
namespace App\Repository;

use PDO;

/**
 * 通用同步表仓库：为"行级业务表"提供统一的 list / batchUpsert / delete / pull。
 * 所有表都遵循同步字段约定：id(PK) / created_at / last_modified / is_deleted / device_id / needs_sync。
 * 通过构造时传入的列白名单（col => type）做类型清洗，杜绝注入。
 */
class SyncTableRepository
{
    private PDO $pdo;
    private string $table;
    /** @var array<string,string> col => 'string'|'int'|'long'|'float'|'bool' */
    private array $cols;

    public function __construct(PDO $pdo, string $table, array $cols)
    {
        $this->pdo = $pdo;
        $this->table = $table;
        $this->cols = $cols;
    }

    private function sanitize(array $item): array
    {
        $out = ['id' => (string)($item['id'] ?? '')];
        if ($out['id'] === '') {
            $out['id'] = sprintf(
                '%04x%04x-%04x-%04x-%04x-%04x%04x%04x',
                random_int(0, 0xffff), random_int(0, 0xffff),
                random_int(0, 0xffff),
                random_int(0, 0x0fff) | 0x4000,
                random_int(0, 0x3fff) | 0x8000,
                random_int(0, 0xffff), random_int(0, 0xffff), random_int(0, 0xffff)
            );
        }
        $now = (int)(microtime(true) * 1000);
        foreach ($this->cols as $col => $type) {
            if (!array_key_exists($col, $item)) {
                continue;
            }
            $v = $item[$col];
            switch ($type) {
                case 'int':
                    $out[$col] = (int)$v;
                    break;
                case 'long':
                    $out[$col] = (int)$v;
                    break;
                case 'float':
                    $out[$col] = (float)$v;
                    break;
                case 'bool':
                    $out[$col] = $v ? 1 : 0;
                    break;
                default:
                    $out[$col] = is_string($v) ? $v : (string)$v;
            }
        }
        $out['created_at'] = isset($item['created_at']) ? (int)$item['created_at'] : $now;
        $out['last_modified'] = isset($item['last_modified']) ? (int)$item['last_modified'] : $now;
        $out['is_deleted'] = isset($item['is_deleted']) ? ((int)$item['is_deleted'] ? 1 : 0) : 0;
        $out['needs_sync'] = 0; // 服务端落库后即权威
        return $out;
    }

    /** 列表（按创建时间倒序，限制数量防止过量） */
    public function list(string $deviceId, int $limit = 300, int $offset = 0): array
    {
        $limit = min(1000, max(1, $limit));
        $stmt = $this->pdo->prepare(
            "SELECT * FROM {$this->table} WHERE device_id = ? AND is_deleted = 0 ORDER BY created_at DESC LIMIT ? OFFSET ?"
        );
        $stmt->bindValue(1, $deviceId, PDO::PARAM_STR);
        $stmt->bindValue(2, $limit, PDO::PARAM_INT);
        $stmt->bindValue(3, $offset, PDO::PARAM_INT);
        $stmt->execute();
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }

    /** 批量 upsert（INSERT ... ON DUPLICATE KEY UPDATE，按 id 收敛） */
    public function batchUpsert(string $deviceId, array $items): int
    {
        if (empty($items)) {
            return 0;
        }
        $allCols = array_merge(['id', 'created_at', 'last_modified', 'is_deleted'], array_keys($this->cols));
        $syncCols = ['device_id', 'needs_sync'];
        $insertCols = array_merge($allCols, $syncCols);
        $placeholders = '(' . implode(', ', array_fill(0, count($insertCols), '?')) . ')';
        $updateCols = array_map(fn($c) => "$c = VALUES($c)", $insertCols);
        $sql = sprintf(
            'INSERT INTO %s (%s) VALUES %s ON DUPLICATE KEY UPDATE %s',
            $this->table,
            implode(', ', $insertCols),
            implode(', ', array_fill(0, count($items), $placeholders)),
            implode(', ', $updateCols)
        );
        $params = [];
        foreach ($items as $raw) {
            $row = $this->sanitize($raw);
            $row['device_id'] = $deviceId;
            foreach ($insertCols as $c) {
                $params[] = $row[$c] ?? ($c === 'needs_sync' ? 0 : '');
            }
        }
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute($params);
        return count($items);
    }

    /** 软删除 */
    public function delete(string $deviceId, array $ids): int
    {
        $ids = array_filter(array_map('strval', $ids), fn($x) => $x !== '');
        if (empty($ids)) {
            return 0;
        }
        $now = (int)(microtime(true) * 1000);
        $ph = implode(', ', array_fill(0, count($ids), '?'));
        $sql = "UPDATE {$this->table} SET is_deleted = 1, needs_sync = 1, last_modified = ? WHERE device_id = ? AND id IN ($ph)";
        $params = array_merge([$now, $deviceId], $ids);
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute($params);
        return $stmt->rowCount();
    }

    /** 增量拉取：返回未删除行 + 已删除 id（用于客户端本地软删） */
    public function pull(string $deviceId, int $since): array
    {
        $logs = $this->pdo->prepare(
            "SELECT * FROM {$this->table} WHERE device_id = ? AND is_deleted = 0 AND last_modified > ? ORDER BY last_modified ASC"
        );
        $logs->execute([$deviceId, $since]);
        $rows = $logs->fetchAll(PDO::FETCH_ASSOC);

        $del = $this->pdo->prepare(
            "SELECT id FROM {$this->table} WHERE device_id = ? AND is_deleted = 1 AND last_modified > ?"
        );
        $del->execute([$deviceId, $since]);
        $deletedIds = array_map(fn($r) => $r['id'], $del->fetchAll(PDO::FETCH_ASSOC));

        return ['logs' => $rows, 'deleted_ids' => $deletedIds];
    }
}
