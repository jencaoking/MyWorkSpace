<?php
namespace App\Controller;

use App\Exception\ApiException;
use App\Repository\AdminRepository;
use PDO;

/** 后台管理 Controller：只读的概览与数据浏览端点（MVVM 的绑定层） */
final class AdminController
{
    public function __construct(private PDO $pdo) {}

    /** GET /admin/overview —— 系统状态 + 各模块全局行数 */
    public function overview(): void
    {
        $dbOk = true;
        $dbError = '';
        try {
            $this->pdo->query('SELECT 1');
        } catch (\Throwable $e) {
            $dbOk = false;
            $dbError = $e->getMessage();
        }

        $repo = new AdminRepository($this->pdo);
        \Response::json([
            'code' => 0,
            'message' => 'ok',
            'data' => [
                'server_time' => (int) (microtime(true) * 1000),
                'php_version' => PHP_VERSION,
                'db_connected' => $dbOk,
                'db_error' => $dbError,
                'device_count' => $repo->deviceCount(),
                'tables' => $repo->tableCounts(),
            ],
        ]);
    }

    /** GET /admin/browse?table=&limit=&offset= —— 通用数据浏览 */
    public function browse(): void
    {
        $table = $_GET['table'] ?? '';
        if (!AdminRepository::isAllowed($table)) {
            throw new ApiException('unknown or disallowed table', 404, 404);
        }
        $limit = min(200, max(1, (int) ($_GET['limit'] ?? 50)));
        $offset = max(0, (int) ($_GET['offset'] ?? 0));

        $rows = (new AdminRepository($this->pdo))->browse($table, $limit, $offset);
        \Response::json([
            'code' => 0,
            'message' => 'ok',
            'data' => [
                'table' => $table,
                'columns' => $rows ? array_keys($rows[0]) : [],
                'rows' => $rows,
                'total' => count($rows),
            ],
        ]);
    }
}
