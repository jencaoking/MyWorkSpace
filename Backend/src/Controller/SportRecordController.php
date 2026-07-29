<?php
namespace App\Controller;

use App\Repository\SportRecordRepository;
use App\ViewModel\SportRecordViewModel;

/** 运动记录接口：list / batchUpsert / delete / pull。 */
class SportRecordController {
    public function __construct(private SportRecordRepository $repo) {}

    public function list(): void {
        $data = SportRecordViewModel::listToArray($this->repo->list());
        ApiResponse::json(['code' => 0, 'msg' => 'ok', 'data' => ['sports' => $data]]);
    }

    public function batchUpsert(): void {
        $body = json_decode((string)file_get_contents('php://input'), true);
        $rows = $body['sports'] ?? $body['records'] ?? [];
        if (!is_array($rows)) ApiResponse::json(['code' => 400, 'msg' => 'invalid body'], 400);
        $count = 0;
        foreach ($rows as $item) {
            if (empty($item['id'])) continue;
            $this->repo->upsert(SportRecordViewModel::fromArray($item));
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
        $dirty = SportRecordViewModel::listToArray($this->repo->findDirty());
        $deleted = $this->repo->findDeletedIds();
        ApiResponse::json(['code' => 0, 'msg' => 'ok', 'data' => [
            'server_time' => time() * 1000,
            'sports' => $dirty,
            'deleted_ids' => $deleted
        ]]);
    }
}
