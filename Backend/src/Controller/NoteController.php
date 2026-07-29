<?php
namespace App\Controller;

use App\Exception\ApiException;
use App\Repository\NoteRepository;
use App\ViewModel\NoteViewModel;
use PDO;

/** 笔记 Controller（阶段3）：列表 / 批量 upsert / 删除 / 全文搜索 / 增量拉取 */
final class NoteController
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

    /** GET /api/notes —— 笔记列表（可选 favorite=1 仅收藏） */
    public function list(): void
    {
        $deviceId = $this->deviceId();
        $favoriteOnly = ($_GET['favorite'] ?? '') === '1';
        $notes = (new NoteRepository($this->pdo))->list($deviceId, $favoriteOnly);

        \Response::json([
            'code' => 0,
            'message' => 'ok',
            'data' => [
                'notes' => array_map([NoteViewModel::class, 'toApiArray'], $notes),
            ],
        ]);
    }

    /** POST /api/notes —— 批量 upsert */
    public function batchUpsert(): void
    {
        $deviceId = $this->deviceId();
        $rawNotes = $this->decodeBody()['notes'] ?? [];
        if (!is_array($rawNotes)) {
            throw new ApiException('notes must be an array', 400, 400);
        }

        $notes = [];
        foreach ($rawNotes as $raw) {
            $notes[] = NoteViewModel::fromUploadArray($raw, $deviceId);
        }

        $accepted = (new NoteRepository($this->pdo))->upsertBatch($notes);

        \Response::json([
            'code' => 0,
            'message' => 'received',
            'data' => [
                'accepted' => $accepted,
                'synced_at' => (int) (microtime(true) * 1000),
            ],
        ]);
    }

    /** POST /api/notes/delete —— 批量删除（{ ids: [...] }） */
    public function delete(): void
    {
        $deviceId = $this->deviceId();
        $ids = $this->decodeBody()['ids'] ?? [];
        if (!is_array($ids)) {
            throw new ApiException('ids must be an array', 400, 400);
        }

        $deleted = (new NoteRepository($this->pdo))->deleteBatch($ids, $deviceId);

        \Response::json([
            'code' => 0,
            'message' => 'ok',
            'data' => ['deleted' => $deleted],
        ]);
    }

    /** GET /api/notes/search?q=keyword —— 全文搜索（FULLTEXT ngram + LIKE 兜底） */
    public function search(): void
    {
        $deviceId = $this->deviceId();
        $keyword = (string) ($_GET['q'] ?? '');
        $notes = (new NoteRepository($this->pdo))->search($deviceId, $keyword);

        \Response::json([
            'code' => 0,
            'message' => 'ok',
            'data' => [
                'keyword' => $keyword,
                'notes' => array_map([NoteViewModel::class, 'toApiArray'], $notes),
            ],
        ]);
    }

    /** GET /api/notes/pull?since= —— 增量拉取（供阶段5 同步复用） */
    public function pull(): void
    {
        $deviceId = $this->deviceId();
        $since = (int) ($_GET['since'] ?? 0);
        $notes = (new NoteRepository($this->pdo))->pullSince($since, $deviceId);

        \Response::json([
            'code' => 0,
            'message' => 'ok',
            'data' => [
                'server_time' => (int) (microtime(true) * 1000),
                'notes' => array_map([NoteViewModel::class, 'toApiArray'], $notes),
            ],
        ]);
    }
}
