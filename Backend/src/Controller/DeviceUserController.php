<?php
namespace App\Controller;

use App\Exception\ApiException;
use App\Repository\AdminRepository;
use App\Repository\DeviceUserRepository;
use App\ViewModel\DeviceUserViewModel;
use PDO;

/** 后台用户管理 Controller：以设备 ID 聚合的「用户」视图，支持封禁/备注/清除数据。 */
final class DeviceUserController
{
    public function __construct(private ?PDO $pdo = null) {}

    /** 鉴权守卫：未登录直接抛 401 */
    private function requireAuth(): void
    {
        if (empty($_SESSION['admin_logged_in'])) {
            throw new ApiException('未授权，请先登录', 401, 401);
        }
    }

    private function jsonBody(): array
    {
        $raw = file_get_contents('php://input');
        if ($raw === '') {
            return [];
        }
        $decoded = json_decode($raw, true);
        return is_array($decoded) ? $decoded : [];
    }

    /** GET /admin/users?limit=&offset=&q= —— 设备（用户）列表与统计 */
    public function list(): void
    {
        $this->requireAuth();
        $limit = min(200, max(1, (int) ($_GET['limit'] ?? 50)));
        $offset = max(0, (int) ($_GET['offset'] ?? 0));
        $q = isset($_GET['q']) ? trim((string) $_GET['q']) : null;
        if ($q === '') {
            $q = null;
        }
        $repo = new DeviceUserRepository($this->pdo);
        $rows = $repo->listDevices($limit, $offset, $q);
        \Response::json([
            'code'    => 0,
            'message' => 'ok',
            'data'    => [
                'rows'   => DeviceUserViewModel::listToArray($rows),
                'total'  => $repo->countDevices($q),
                'limit'  => $limit,
                'offset' => $offset,
            ],
        ]);
    }

    /** POST /admin/users/set —— {device_id, status?, note?} 设置封禁状态或备注 */
    public function set(): void
    {
        $this->requireAuth();
        $body = $this->jsonBody();
        $deviceId = (string) ($body['device_id'] ?? '');
        if ($deviceId === '') {
            throw new ApiException('缺少 device_id', 400, 400);
        }
        $status = isset($body['status']) ? (string) $body['status'] : null;
        $note = array_key_exists('note', $body) ? (string) $body['note'] : null;
        if ($status === null && $note === null) {
            throw new ApiException('至少需要提供 status 或 note 之一', 400, 400);
        }
        $repo = new DeviceUserRepository($this->pdo);
        $m = $repo->setStatus($deviceId, $status, $note);
        (new AdminRepository($this->pdo))->audit('update', 'device_users', $deviceId, ['status' => $status, 'note' => $note]);
        \Response::json(['code' => 0, 'message' => 'ok', 'data' => DeviceUserViewModel::toArray($m)]);
    }

    /** POST /admin/users/delete —— {device_id} 软删除该设备全部数据 */
    public function delete(): void
    {
        $this->requireAuth();
        $body = $this->jsonBody();
        $deviceId = (string) ($body['device_id'] ?? '');
        if ($deviceId === '') {
            throw new ApiException('缺少 device_id', 400, 400);
        }
        $repo = new DeviceUserRepository($this->pdo);
        $counts = $repo->deleteDeviceData($deviceId);
        (new AdminRepository($this->pdo))->audit('delete', 'device_users', $deviceId, ['data' => $counts], 'soft');
        \Response::json(['code' => 0, 'message' => 'ok', 'data' => ['affected' => $counts]]);
    }
}
