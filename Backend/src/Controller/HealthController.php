<?php
namespace App\Controller;

use PDO;

/** 健康检查 Controller：探活数据库连通性（MVVM 中绑定 View 与 Model 的 HTTP 处理） */
final class HealthController
{
    public function __construct(private ?PDO $pdo) {}

    public function index(): void
    {
        $dbOk = true;
        $dbError = '';
        if ($this->pdo === null) {
            $dbOk = false;
            $dbError = 'database not configured or unreachable';
        } else {
            try {
                $this->pdo->query('SELECT 1');
            } catch (\Throwable $e) {
                $dbOk = false;
                $dbError = $e->getMessage();
            }
        }

        \Response::json([
            'code' => 0,
            'message' => 'ok',
            'data' => [
                'server_time' => (int) (microtime(true) * 1000),
                'app_version' => '1.0',
                'php_version' => PHP_VERSION,
                'db_connected' => $dbOk,
                'db_error' => $dbError,
            ],
        ]);
    }
}
