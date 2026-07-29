<?php
namespace App\Controller;

use App\Exception\ApiException;
use App\Repository\TaskRepository;
use App\ViewModel\TaskViewModel;
use PDO;

/** 任务 Controller：同步 + 阶段2 任务接口（MVVM 中串联 Model / ViewModel / View 的绑定角色） */
final class TaskController
{
    public function __construct(private PDO $pdo) {}

    private function deviceId(): string
    {
        $deviceId = $_SERVER['HTTP_X_DEVICE_ID'] ?? '';
        if ($deviceId === '') {
            throw new ApiException('X-Device-ID header required', 400, 400);
        }
        return $deviceId;
    }

    private function decodeBody(): array
    {
        return json_decode((string) file_get_contents('php://input'), true) ?? [];
    }

    /** POST /sync/upload —— 接收客户端批量 upsert */
    public function upload(): void
    {
        $deviceId = $this->deviceId();
        $rawTasks = $this->decodeBody()['tasks'] ?? [];
        if (!is_array($rawTasks)) {
            throw new ApiException('tasks must be an array', 400, 400);
        }

        $tasks = [];
        foreach ($rawTasks as $raw) {
            $tasks[] = TaskViewModel::fromUploadArray($raw, $deviceId);
        }

        $accepted = (new TaskRepository($this->pdo))->upsertBatch($tasks);

        \Response::json([
            'code' => 0,
            'message' => 'received',
            'data' => [
                'accepted' => $accepted,
                'synced_at' => (int) (microtime(true) * 1000),
            ],
        ]);
    }

    /** GET /sync/pull —— 增量返回 since 之后的变更 */
    public function pull(): void
    {
        $deviceId = $this->deviceId();
        $since = (int) ($_GET['since'] ?? 0);
        $tasks = (new TaskRepository($this->pdo))->pullSince($since, $deviceId);

        \Response::json([
            'code' => 0,
            'message' => 'ok',
            'data' => [
                'server_time' => (int) (microtime(true) * 1000),
                'tasks' => array_map([TaskViewModel::class, 'toApiArray'], $tasks),
            ],
        ]);
    }

    /** GET /api/tasks —— 任务列表（可选 category_id / status 过滤） */
    public function list(): void
    {
        $deviceId = $this->deviceId();
        $categoryId = $_GET['category_id'] ?? '';
        $status = isset($_GET['status']) && $_GET['status'] !== '' ? (int) $_GET['status'] : null;
        $tasks = (new TaskRepository($this->pdo))->list($deviceId, $categoryId, $status);

        \Response::json([
            'code' => 0,
            'message' => 'ok',
            'data' => [
                'tasks' => array_map([TaskViewModel::class, 'toApiArray'], $tasks),
            ],
        ]);
    }

    /** POST /api/tasks —— 批量 upsert（与 /sync/upload 等价，语义化的任务接口） */
    public function batchUpsert(): void
    {
        $this->upload();
    }

    /** POST /api/tasks/delete —— 批量删除（{ ids: [...] }） */
    public function delete(): void
    {
        $deviceId = $this->deviceId();
        $ids = $this->decodeBody()['ids'] ?? [];
        if (!is_array($ids)) {
            throw new ApiException('ids must be an array', 400, 400);
        }

        $deleted = (new TaskRepository($this->pdo))->deleteBatch($ids, $deviceId);

        \Response::json([
            'code' => 0,
            'message' => 'ok',
            'data' => ['deleted' => $deleted],
        ]);
    }

    /** GET /api/tasks/stats —— 统计概览 */
    public function stats(): void
    {
        $deviceId = $this->deviceId();
        $stats = (new TaskRepository($this->pdo))->stats($deviceId);

        \Response::json([
            'code' => 0,
            'message' => 'ok',
            'data' => $stats,
        ]);
    }
}
