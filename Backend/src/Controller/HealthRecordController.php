<?php
namespace App\Controller;

use App\Repository\HealthRecordRepository;
use App\ViewModel\HealthRecordViewModel;

/** 健康记录接口：list / batchUpsert / delete / pull。 */
class HealthRecordController {
    public function __construct(private HealthRecordRepository $repo) {}

    public function list(): void {
        $data = HealthRecordViewModel::listToArray($this->repo->list());
        ApiResponse::json(['code' => 0, 'msg' => 'ok', 'data' => ['health' => $data]]);
    }

    public function batchUpsert(): void {
        $body = json_decode((string)file_get_contents('php://input'), true);
        $rows = $body['health'] ?? $body['records'] ?? [];
        if (!is_array($rows)) ApiResponse::json(['code' => 400, 'msg' => 'invalid body'], 400);
        $count = 0;
        foreach ($rows as $item) {
            if (empty($item['id'])) continue;
            $this->repo->upsert(HealthRecordViewModel::fromArray($item));
            $count++;
        }
        ApiResponse::json(['code' => 0, 'msg' => 'ok', 'data' => ['uploaded' => $count]]);
    }

    public function delete(): void {
        $body = json_decode((string)file_get_contents('php://input'), true);
        $ids = $body['ids'] ?? [];
        if (!is_array($ids)) ApiResponse::json(['code' => 400, 'msg' => 'invalid body'], 400);
        foreach ($ids as $id) $this->repo->softDelete((string)$id);
        ApiResponse::json(['code' => 0, 'msg' => 'ok', 'data' => ['deleted' => count($ids)]]);
    }

    public function pull(): void {
        $dirty = HealthRecordViewModel::listToArray($this->repo->findDirty());
        $deleted = $this->repo->findDeletedIds();
        ApiResponse::json(['code' => 0, 'msg' => 'ok', 'data' => [
            'server_time' => time() * 1000,
            'health' => $dirty,
            'deleted_ids' => $deleted
        ]]);
    }
}
