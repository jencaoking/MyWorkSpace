<?php
namespace App\Controller;

use App\Exception\ApiException;
use App\Repository\CategoryRepository;
use App\ViewModel\CategoryViewModel;
use PDO;

/** 分类 Controller：列表 / 批量 upsert / 删除 / 增量拉取 */
final class CategoryController
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

    /** GET /api/categories */
    public function list(): void
    {
        $data = array_map(
            [CategoryViewModel::class, 'toApiArray'],
            (new CategoryRepository($this->pdo))->list()
        );
        \ApiResponse::json(['code' => 0, 'msg' => 'ok', 'data' => ['categories' => $data]]);
    }

    /** POST /api/categories —— 批量 upsert */
    public function batchUpsert(): void
    {
        $deviceId = $this->deviceId();
        $rows = $this->decodeBody()['categories'] ?? [];
        if (!is_array($rows)) {
            \ApiResponse::json(['code' => 400, 'msg' => 'invalid body'], 400);
        }
        $cats = [];
        foreach ($rows as $raw) {
            $cats[] = CategoryViewModel::fromUploadArray($raw, $deviceId);
        }
        $count = (new CategoryRepository($this->pdo))->upsertBatch($cats);
        \ApiResponse::json(['code' => 0, 'msg' => 'ok', 'data' => ['uploaded' => $count]]);
    }

    /** POST /api/categories/delete —— 批量软删除（{ ids: [...] }） */
    public function delete(): void
    {
        $ids = $this->decodeBody()['ids'] ?? [];
        if (!is_array($ids)) {
            \ApiResponse::json(['code' => 400, 'msg' => 'invalid body'], 400);
        }
        $deleted = (new CategoryRepository($this->pdo))->deleteBatch($ids);
        \ApiResponse::json(['code' => 0, 'msg' => 'ok', 'data' => ['deleted' => $deleted]]);
    }

    /** GET /api/categories/pull?since= —— 增量拉取 */
    public function pull(): void
    {
        $repo = new CategoryRepository($this->pdo);
        \ApiResponse::json([
            'code' => 0,
            'msg' => 'ok',
            'data' => [
                'server_time' => (int) (microtime(true) * 1000),
                'dirty' => array_map([CategoryViewModel::class, 'toApiArray'], $repo->findDirty()),
                'deleted_ids' => $repo->findDeletedIds(),
            ],
        ]);
    }
}
