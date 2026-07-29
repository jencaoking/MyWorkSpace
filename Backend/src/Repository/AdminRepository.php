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

    /** 不允许在后台删除的表（多为系统级配置，删错影响全局） */
    private const NO_DELETE_TABLES = ['user_settings'];

    /** 审计日志表名 */
    private const AUDIT_TABLE = 'admin_audit_log';

    public static function isAllowed(string $table): bool
    {
        return in_array($table, self::TABLES, true);
    }

    /** 该表是否允许在后台删除（前端据此隐藏删除按钮，服务端据此二次拦截） */
    public static function canDelete(string $table): bool
    {
        return !in_array($table, self::NO_DELETE_TABLES, true);
    }

    /** 该表是否受删除保护（供前端提示） */
    public static function isDeleteProtected(string $table): bool
    {
        return in_array($table, self::NO_DELETE_TABLES, true);
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
        $applied = [];

        foreach ($fields as $k => $v) {
            if (!is_string($k) || !array_key_exists($k, $cols)) {
                continue;
            }
            if (in_array($k, self::READ_ONLY_COLUMNS, true)) {
                continue;
            }
            $cast = $this->cast($cols[$k], $v);
            $set[] = "`$k` = ?";
            $params[] = $cast;
            $applied[$k] = $cast;
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
        return ['count' => $stmt->rowCount(), 'applied' => $applied];
    }

    /**
     * 通用删除：若表含 is_deleted 列则软删除（置 1，并刷新同步标记），否则物理删除。
     * 返回 ['count' => 受影响行数, 'mode' => 'soft'|'hard'] 供审计使用。
     */
    public function deleteRow(string $table, string $id): array
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
            $mode = 'soft';
        } else {
            $sql = "DELETE FROM `$table` WHERE `id` = ?";
            $params = [$id];
            $mode = 'hard';
        }
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute($params);
        return ['count' => $stmt->rowCount(), 'mode' => $mode];
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

    /** 确保审计日志表存在（幂等，首次写入时建表） */
    private function ensureAuditTable(): void
    {
        $this->pdo->exec(
            "CREATE TABLE IF NOT EXISTS `" . self::AUDIT_TABLE . "` (
                `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                `actor` VARCHAR(64) NOT NULL DEFAULT 'admin',
                `action` VARCHAR(16) NOT NULL COMMENT 'update|delete',
                `table_name` VARCHAR(64) NOT NULL,
                `row_id` CHAR(36) NOT NULL,
                `change_mode` VARCHAR(8) NULL COMMENT 'soft|hard',
                `changes` TEXT NULL COMMENT 'JSON: 实际变更的字段',
                `ip` VARCHAR(45) NULL,
                `user_agent` TEXT NULL,
                `created_at` BIGINT NOT NULL,
                PRIMARY KEY (`id`),
                KEY `idx_table_row` (`table_name`, `row_id`),
                KEY `idx_created` (`created_at`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        );
    }

    /** 写入一条操作审计记录（编辑 / 删除） */
    public function audit(string $action, string $table, string $rowId, ?array $changes = null, ?string $mode = null): void
    {
        $this->ensureAuditTable();
        $stmt = $this->pdo->prepare(
            "INSERT INTO `" . self::AUDIT_TABLE . "`
             (actor, action, table_name, row_id, change_mode, changes, ip, user_agent, created_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
        );
        $stmt->execute([
            'admin',
            $action,
            $table,
            $rowId,
            $mode,
            $changes === null ? null : json_encode($changes, JSON_UNESCAPED_UNICODE),
            $this->clientIp(),
            $_SERVER['HTTP_USER_AGENT'] ?? '',
            (int) (microtime(true) * 1000),
        ]);
    }

    /** 最近的审计记录（按时间倒序） */
    public function recentAudit(int $limit): array
    {
        $this->ensureAuditTable();
        $stmt = $this->pdo->prepare("SELECT * FROM `" . self::AUDIT_TABLE . "` ORDER BY `created_at` DESC LIMIT ?");
        $stmt->execute([$limit]);
        return $stmt->fetchAll();
    }

    /** 取客户端真实 IP（兼容反向代理） */
    private function clientIp(): string
    {
        $fwd = $_SERVER['HTTP_X_FORWARDED_FOR'] ?? '';
        if ($fwd !== '') {
            return trim(explode(',', $fwd)[0]);
        }
        return $_SERVER['REMOTE_ADDR'] ?? '';
    }
}
