<?php
namespace App\Controller;

use App\Repository\SyncTableRepository;
use App\Lib\ApiAuth;

/**
 * 通用同步表控制器：包装 SyncTableRepository，统一处理设备隔离（X-Device-ID）、JSON 信封。
 * 在 routes/api.php 中按表实例化并注册 list/upsert/delete/pull 四路由。
 */
class SyncTableController
{
    private SyncTableRepository $repo;

    public function __construct($pdo, string $table, array $cols)
    {
        $this->repo = new SyncTableRepository($pdo, $table, $cols);
    }

    private function deviceId(): string
    {
        $id = ApiAuth::deviceId();
        if ($id === '') {
            ApiResponse::json(['code' => 401, 'message' => '缺少 X-Device-ID', 'data' => null]);
        }
        return $id;
    }

    public function list(): void
    {
        $device = $this->deviceId();
        $limit = min(1000, max(1, (int)($_GET['limit'] ?? 300)));
        $offset = max(0, (int)($_GET['offset'] ?? 0));
        $rows = $this->repo->list($device, $limit, $offset);
        ApiResponse::json(['code' => 0, 'message' => 'ok', 'data' => ['logs' => $rows]]);
    }

    public function batchUpsert(): void
    {
        $device = $this->deviceId();
        $body = ApiAuth::jsonBody();
        $items = $body['logs'] ?? [];
        if (!is_array($items)) {
            ApiResponse::json(['code' => 1, 'message' => '参数 logs 必须为数组', 'data' => null]);
        }
        $count = $this->repo->batchUpsert($device, $items);
        ApiResponse::json(['code' => 0, 'message' => 'ok', 'data' => ['count' => $count]]);
    }

    public function delete(): void
    {
        $device = $this->deviceId();
        $body = ApiAuth::jsonBody();
        $ids = $body['ids'] ?? [];
        if (!is_array($ids)) {
            ApiResponse::json(['code' => 1, 'message' => '参数 ids 必须为数组', 'data' => null]);
        }
        $count = $this->repo->delete($device, $ids);
        ApiResponse::json(['code' => 0, 'message' => 'ok', 'data' => ['count' => $count]]);
    }

    public function pull(): void
    {
        $device = $this->deviceId();
        $since = (int)($_GET['since'] ?? 0);
        $res = $this->repo->pull($device, $since);
        ApiResponse::json([
            'code' => 0,
            'message' => 'ok',
            'data' => [
                'server_time' => (int)(microtime(true) * 1000),
                'logs' => $res['logs'],
                'deleted_ids' => $res['deleted_ids'],
            ],
        ]);
    }
}
