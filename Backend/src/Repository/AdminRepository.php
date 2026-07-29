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
}
