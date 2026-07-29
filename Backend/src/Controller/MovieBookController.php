<?php
namespace App\Controller;

use App\Repository\MovieBookRepository;
use App\ViewModel\MovieBookViewModel;

/** 影音书籍接口：list / batchUpsert / delete / pull。 */
class MovieBookController {
    public function __construct(private MovieBookRepository $repo) {}

    public function list(): void {
        $data = MovieBookViewModel::listToArray($this->repo->list());
        ApiResponse::json(['code' => 0, 'msg' => 'ok', 'data' => ['media' => $data]]);
    }

    public function batchUpsert(): void {
        $body = json_decode((string)file_get_contents('php://input'), true);
        $rows = $body['media'] ?? $body['records'] ?? [];
        if (!is_array($rows)) ApiResponse::json(['code' => 400, 'msg' => 'invalid body'], 400);
        $count = 0;
        foreach ($rows as $item) {
            if (empty($item['id'])) continue;
            $this->repo->upsert(MovieBookViewModel::fromArray($item));
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
        $dirty = MovieBookViewModel::listToArray($this->repo->findDirty());
        $deleted = $this->repo->findDeletedIds();
        ApiResponse::json(['code' => 0, 'msg' => 'ok', 'data' => [
            'server_time' => time() * 1000,
            'media' => $dirty,
            'deleted_ids' => $deleted
        ]]);
    }
}
