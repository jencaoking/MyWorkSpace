<?php
namespace App\Controller;

use App\Exception\ApiException;
use App\Repository\AdminRepository;
use PDO;

/** 后台管理 Controller：只读的概览与数据浏览端点（MVVM 的绑定层） */
final class AdminController
{
    public function __construct(private ?PDO $pdo = null) {}

    /** 鉴权守卫：未登录直接抛 401（由全局 try/catch 转成 JSON） */
    private function requireAuth(): void
    {
        if (empty($_SESSION['admin_logged_in'])) {
            throw new ApiException('未授权，请先登录', 401, 401);
        }
    }

    /** POST /admin/login —— 校验密码并写入会话 */
    public function login(): void
    {
        $body = $this->jsonBody();
        $password = is_string($body['password'] ?? null) ? $body['password'] : ($_POST['password'] ?? '');

        $cfg = require __DIR__ . '/../../config/admin.php';
        $ok = false;
        if (!empty($cfg['password_hash'])) {
            $ok = password_verify($password, $cfg['password_hash']);
        } elseif (!empty($cfg['password_plain'])) {
            $ok = hash_equals($cfg['password_plain'], $password);
        }
        if (!is_string($password) || $password === '' || !$ok) {
            throw new ApiException('用户名或密码错误', 401, 401);
        }

        session_regenerate_id(true);
        $_SESSION['admin_logged_in'] = true;
        \Response::json(['code' => 0, 'message' => 'ok', 'data' => ['logged_in' => true]]);
    }

    /** POST /admin/logout —— 销毁会话 */
    public function logout(): void
    {
        session_destroy();
        \Response::json(['code' => 0, 'message' => 'ok', 'data' => ['logged_in' => false]]);
    }

    /** 解析请求体 JSON（兼容空体/非 JSON） */
    private function jsonBody(): array
    {
        $raw = file_get_contents('php://input');
        if ($raw === '') {
            return [];
        }
        $decoded = json_decode($raw, true);
        return is_array($decoded) ? $decoded : [];
    }

    /** GET /admin/overview —— 系统状态 + 各模块全局行数 */
    public function overview(): void
    {
        $this->requireAuth();
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

    /** GET /admin/browse?table=&limit=&offset= —— 通用数据浏览（含列类型，供前端编辑表单使用） */
    public function browse(): void
    {
        $this->requireAuth();
        $table = $_GET['table'] ?? '';
        if (!AdminRepository::isAllowed($table)) {
            throw new ApiException('unknown or disallowed table', 404, 404);
        }
        $limit = min(200, max(1, (int) ($_GET['limit'] ?? 50)));
        $offset = max(0, (int) ($_GET['offset'] ?? 0));

        $repo = new AdminRepository($this->pdo);
        $rows = $repo->browse($table, $limit, $offset);
        $types = [];
        foreach ($repo->columnsOf($table) as $col => $meta) {
            $types[$col] = $meta['type'];
        }
        \Response::json([
            'code' => 0,
            'message' => 'ok',
            'data' => [
                'table' => $table,
                'columns' => $rows ? array_keys($rows[0]) : [],
                'types' => $types,
                'rows' => $rows,
                'total' => count($rows),
                'deletable' => AdminRepository::canDelete($table),
            ],
        ]);
    }

    /** POST /admin/update —— 编辑一行：{table, id, fields:{...}} */
    public function update(): void
    {
        $this->requireAuth();
        $body = $this->jsonBody();
        $table = (string) ($body['table'] ?? '');
        $id = (string) ($body['id'] ?? '');
        $fields = $body['fields'] ?? [];

        if (!AdminRepository::isAllowed($table)) {
            throw new ApiException('unknown or disallowed table', 404, 404);
        }
        if ($id === '' || !is_array($fields) || $fields === []) {
            throw new ApiException('缺少 id 或 fields', 400, 400);
        }

        $repo = new AdminRepository($this->pdo);
        $res = $repo->updateRow($table, $id, $fields);
        if ($res['count'] < 1) {
            throw new ApiException('未找到记录或无字段变更', 404, 404);
        }
        $repo->audit('update', $table, $id, $res['applied']);
        \Response::json(['code' => 0, 'message' => 'ok', 'data' => ['affected' => $res['count']]]);
    }

    /** POST /admin/delete —— 删除一行：{table, id}（含 is_deleted 的表走软删除） */
    public function delete(): void
    {
        $this->requireAuth();
        $body = $this->jsonBody();
        $table = (string) ($body['table'] ?? '');
        $id = (string) ($body['id'] ?? '');

        if (!AdminRepository::isAllowed($table)) {
            throw new ApiException('unknown or disallowed table', 404, 404);
        }
        if (!AdminRepository::canDelete($table)) {
            throw new ApiException('该表不允许在后台删除', 403, 403);
        }
        if ($id === '') {
            throw new ApiException('缺少 id', 400, 400);
        }

        $repo = new AdminRepository($this->pdo);
        $res = $repo->deleteRow($table, $id);
        if ($res['count'] < 1) {
            throw new ApiException('未找到记录', 404, 404);
        }
        $repo->audit('delete', $table, $id, null, $res['mode']);
        \Response::json(['code' => 0, 'message' => 'ok', 'data' => ['affected' => $res['count'], 'mode' => $res['mode']]]);
    }

    /** GET /admin/audit?limit=100 —— 操作审计日志（编辑/删除记录） */
    public function audit(): void
    {
        $this->requireAuth();
        $limit = min(500, max(1, (int) ($_GET['limit'] ?? 100)));
        $rows = (new AdminRepository($this->pdo))->recentAudit($limit);
        \Response::json([
            'code' => 0,
            'message' => 'ok',
            'data' => ['rows' => $rows, 'total' => count($rows)],
        ]);
    }
}
