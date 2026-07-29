<?php
namespace App\Controller;

use App\Repository\EnglishWordRepository;
use App\ViewModel\EnglishWordViewModel;

/** 英语单词接口：list / batchUpsert / delete / pull。 */
class EnglishWordController {
    public function __construct(private EnglishWordRepository $repo) {}

    public function list(): void {
        $data = EnglishWordViewModel::listToArray($this->repo->list());
        ApiResponse::json(['code' => 0, 'msg' => 'ok', 'data' => ['words' => $data]]);
    }

    public function batchUpsert(): void {
        $body = json_decode((string)file_get_contents('php://input'), true);
        $rows = $body['words'] ?? $body['records'] ?? [];
        if (!is_array($rows)) ApiResponse::json(['code' => 400, 'msg' => 'invalid body'], 400);
        $count = 0;
        foreach ($rows as $item) {
            if (empty($item['id'])) continue;
            $this->repo->upsert(EnglishWordViewModel::fromArray($item));
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
        $dirty = EnglishWordViewModel::listToArray($this->repo->findDirty());
        $deleted = $this->repo->findDeletedIds();
        ApiResponse::json(['code' => 0, 'msg' => 'ok', 'data' => [
            'server_time' => time() * 1000,
            'words' => $dirty,
            'deleted_ids' => $deleted
        ]]);
    }
}
