<?php
namespace App\Repository;

use PDO;

/** 后台管理数据访问层：提供跨设备的全局只读统计与数据浏览（MVVM 中 Model 层） */
final class AdminRepository
{
    public function __construct(private PDO $pdo) {}

    /** 允许后台浏览/统计的数据表白名单 */
    private const TABLES = [
        'tasks', 'categories', 'notes', 'sport_records',
        'english_words', 'movie_books', 'health_records',
        'account_records', 'user_settings',
    ];

    /** 不允许被编辑的列（主键 id 必须保持不变） */
    private const READ_ONLY_COLUMNS = ['id'];

    public static function isAllowed(string $table): bool
    {
        return in_array($table, self::TABLES, true);
    }

    /** 各表全局行数（不区分设备） */
    public function tableCounts(): array
    {
        $out = [];
        foreach (self::TABLES as $t) {
            $out[$t] = (int) $this->pdo->query("SELECT COUNT(*) FROM `$t`")->fetchColumn();
        }
        return $out;
    }

    /** 去重设备数（基于 tasks 表的 device_id） */
    public function deviceCount(): int
    {
        return (int) $this->pdo->query("SELECT COUNT(DISTINCT device_id) FROM tasks WHERE device_id <> ''")->fetchColumn();
    }

    /** 通用浏览：白名单表 + 分页，返回原生行（列名由前端人性化展示） */
    public function browse(string $table, int $limit = 50, int $offset = 0): array
    {
        $stmt = $this->pdo->prepare("SELECT * FROM `$table` ORDER BY 1 DESC LIMIT ? OFFSET ?");
        $stmt->execute([$limit, $offset]);
        return $stmt->fetchAll();
    }

    /** 返回表的列元信息：['列名' => ['type' => 'varchar(255)', 'nullable' => true, ...]] */
    public function columnsOf(string $table): array
    {
        $rows = $this->pdo->query("DESCRIBE `$table`")->fetchAll(\PDO::FETCH_ASSOC);
        $map = [];
        foreach ($rows as $r) {
            $map[$r['Field']] = [
                'type'     => strtolower($r['Type'] ?? ''),
                'nullable' => ($r['Null'] ?? 'NO') === 'YES',
            ];
        }
        return $map;
    }

    /**
     * 通用编辑：按主键 id 更新白名单表的一行。
     * 仅更新请求中提供且真实存在的列，自动跳过主键；
     * 若表中存在 last_modified / needs_sync 则会一并刷新，保证下次 App 同步能拉到变更。
     */
    public function updateRow(string $table, string $id, array $fields): int
    {
        $cols = $this->columnsOf($table);
        $set = [];
        $params = [];

        foreach ($fields as $k => $v) {
            if (!is_string($k) || !array_key_exists($k, $cols)) {
                continue;
            }
            if (in_array($k, self::READ_ONLY_COLUMNS, true)) {
                continue;
            }
            $set[] = "`$k` = ?";
            $params[] = $this->cast($cols[$k], $v);
        }

        if (array_key_exists('last_modified', $cols)) {
            $set[] = "`last_modified` = ?";
            $params[] = (int) (microtime(true) * 1000);
        }
        if (array_key_exists('needs_sync', $cols)) {
            $set[] = "`needs_sync` = ?";
            $params[] = 1;
        }

        if ($set === []) {
            throw new \App\Exception\ApiException('没有可更新的有效字段', 400, 400);
        }

        $params[] = $id;
        $sql = "UPDATE `$table` SET " . implode(', ', $set) . " WHERE `id` = ?";
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute($params);
        return $stmt->rowCount();
    }

    /**
     * 通用删除：若表含 is_deleted 列则软删除（置 1，并刷新同步标记），否则物理删除。
     */
    public function deleteRow(string $table, string $id): int
    {
        $cols = $this->columnsOf($table);
        if (array_key_exists('is_deleted', $cols)) {
            $set = ["`is_deleted` = ?"];
            $params = [1];
            if (array_key_exists('last_modified', $cols)) {
                $set[] = "`last_modified` = ?";
                $params[] = (int) (microtime(true) * 1000);
            }
            if (array_key_exists('needs_sync', $cols)) {
                $set[] = "`needs_sync` = ?";
                $params[] = 1;
            }
            $params[] = $id;
            $sql = "UPDATE `$table` SET " . implode(', ', $set) . " WHERE `id` = ?";
        } else {
            $sql = "DELETE FROM `$table` WHERE `id` = ?";
            $params = [$id];
        }
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute($params);
        return $stmt->rowCount();
    }

    /** 依据列类型把前端字符串安全转换为对应标量，空串按可空性转为 null */
    private function cast(array $col, $v)
    {
        if ($v === null) {
            return null;
        }
        $t = $col['type'];
        if (str_contains($t, 'int') || str_contains($t, 'bigint') || str_contains($t, 'smallint') || str_contains($t, 'mediumint')) {
            return (int) $v;
        }
        if (str_contains($t, 'float') || str_contains($t, 'double') || str_contains($t, 'decimal') || str_contains($t, 'numeric')) {
            return (float) $v;
        }
        if ($v === '' && $col['nullable']) {
            return null;
        }
        return (string) $v;
    }
}
