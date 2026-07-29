<?php
namespace App\Controller;

use App\Repository\DailyPendingLogRepository;
use App\ViewModel\DailyPendingLogViewModel;
use PDO;

/**
 * 每日未完成作业接口：
 *   GET  /api/daily-pending           列表（?date=YYYY-MM-DD&disposition=pending）
 *   POST /api/daily-pending           批量 upsert（客户端同步上传）
 *   POST /api/daily-pending/delete    软删除
 *   GET  /api/daily-pending/pull      增量拉取（服务端脏数据）
 *   POST /api/daily-pending/dispose   处置：补做/改期/放弃（联动 tasks 表）
 *   GET  /api/daily-pending/weekly    本周回顾统计
 *   POST /api/daily-pending/archive   手动触发服务端归档（Cron 亦调用同一逻辑）
 */
class DailyPendingController {
    private DailyPendingLogRepository $repo;

    public function __construct(private PDO $pdo) {
        $this->repo = new DailyPendingLogRepository($pdo);
    }

    private function decodeBody(): array {
        $body = json_decode((string)file_get_contents('php://input'), true);
        return is_array($body) ? $body : [];
    }

    public function list(): void {
        $date = isset($_GET['date']) ? (string)$_GET['date'] : null;
        $disposition = isset($_GET['disposition']) ? (string)$_GET['disposition'] : null;
        $data = DailyPendingLogViewModel::listToArray($this->repo->list($date, $disposition));
        \ApiResponse::json(['code' => 0, 'msg' => 'ok', 'data' => ['logs' => $data]]);
    }

    public function batchUpsert(): void {
        $body = $this->decodeBody();
        $rows = $body['logs'] ?? [];
        if (!is_array($rows)) \ApiResponse::json(['code' => 400, 'msg' => 'invalid body'], 400);
        $count = 0;
        foreach ($rows as $item) {
            if (empty($item['id'])) continue;
            $this->repo->upsert(DailyPendingLogViewModel::fromArray($item));
            $count++;
        }
        \ApiResponse::json(['code' => 0, 'msg' => 'ok', 'data' => ['uploaded' => $count]]);
    }

    public function delete(): void {
        $body = $this->decodeBody();
        $ids = $body['ids'] ?? [];
        if (!is_array($ids)) \ApiResponse::json(['code' => 400, 'msg' => 'invalid body'], 400);
        foreach ($ids as $id) $this->repo->softDelete((string)$id);
        \ApiResponse::json(['code' => 0, 'msg' => 'ok', 'data' => ['deleted' => count($ids)]]);
    }

    public function pull(): void {
        $dirty = DailyPendingLogViewModel::listToArray($this->repo->findDirty());
        $deleted = $this->repo->findDeletedIds();
        \ApiResponse::json(['code' => 0, 'msg' => 'ok', 'data' => [
            'server_time' => (int)(microtime(true) * 1000),
            'logs' => $dirty,
            'deleted_ids' => $deleted
        ]]);
    }

    public function dispose(): void {
        $body = $this->decodeBody();
        $id = (string)($body['id'] ?? '');
        $disposition = (string)($body['disposition'] ?? '');
        $newDueDate = isset($body['new_due_date']) && $body['new_due_date'] !== null && $body['new_due_date'] !== ''
            ? (int)$body['new_due_date'] : null;
        if ($id === '' || !in_array($disposition, ['completed', 'rescheduled', 'abandoned'], true)) {
            \ApiResponse::json(['code' => 400, 'msg' => 'invalid id or disposition'], 400);
        }
        if ($disposition === 'rescheduled' && $newDueDate === null) {
            \ApiResponse::json(['code' => 400, 'msg' => 'new_due_date required for reschedule'], 400);
        }
        $log = $this->repo->dispose($id, $disposition, $newDueDate);
        if ($log === null) \ApiResponse::json(['code' => 404, 'msg' => 'log not found'], 404);
        \ApiResponse::json(['code' => 0, 'msg' => 'ok', 'data' => ['log' => DailyPendingLogViewModel::toArray($log)]]);
    }

    public function weekly(): void {
        \ApiResponse::json(['code' => 0, 'msg' => 'ok', 'data' => $this->repo->weeklyStats()]);
    }

    public function archive(): void {
        $inserted = $this->repo->archiveOverdue();
        \ApiResponse::json(['code' => 0, 'msg' => 'ok', 'data' => ['archived' => $inserted]]);
    }
}
