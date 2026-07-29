<?php
namespace App\Repository;

use App\Model\DailyPendingLog;
use PDO;

/** 每日未完成作业归档仓库。 */
class DailyPendingLogRepository {
    public function __construct(private PDO $pdo) {}

    /** 列表：按日期（可选）过滤，仅未删除 */
    public function list(?string $date = null, ?string $disposition = null): array {
        $sql = "SELECT * FROM daily_pending_log WHERE is_deleted=0";
        $params = [];
        if ($date !== null && $date !== '') { $sql .= " AND log_date=?"; $params[] = $date; }
        if ($disposition !== null && $disposition !== '') { $sql .= " AND disposition=?"; $params[] = $disposition; }
        $sql .= " ORDER BY log_date DESC, priority ASC";
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute($params);
        return array_map(static fn($r) => self::hydrate($r), $stmt->fetchAll(PDO::FETCH_ASSOC));
    }

    public function upsert(DailyPendingLog $m): void {
        $now = (int)(microtime(true) * 1000);
        $sql = "INSERT INTO daily_pending_log
            (id,task_id,task_title,category_name,priority,original_due_date,log_date,disposition,
             disposed_at,new_due_date,created_at,last_modified,is_deleted,device_id,needs_sync)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,0)
            ON DUPLICATE KEY UPDATE
            task_title=VALUES(task_title),category_name=VALUES(category_name),priority=VALUES(priority),
            original_due_date=VALUES(original_due_date),disposition=VALUES(disposition),
            disposed_at=VALUES(disposed_at),new_due_date=VALUES(new_due_date),
            last_modified=VALUES(last_modified),is_deleted=VALUES(is_deleted),
            device_id=VALUES(device_id),needs_sync=0";
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([
            $m->id, $m->taskId, $m->taskTitle, $m->categoryName, $m->priority,
            $m->originalDueDate, $m->logDate, $m->disposition,
            $m->disposedAt, $m->newDueDate, $m->createdAt ?? $now,
            $m->lastModified ?? $now, $m->isDeleted ?? 0, $m->deviceId
        ]);
    }

    /**
     * 服务端每日归档：把截止日期已过、仍未完成的普通任务写入归档表（幂等）。
     * 归档昨日 23:59:59 前到期、status=0、非长期目标(task_type!=2)、未删除的任务。
     * 返回新增归档条数。
     */
    public function archiveOverdue(): int {
        $todayStart = strtotime(date('Y-m-d')) * 1000;
        $now = (int)(microtime(true) * 1000);
        // INSERT IGNORE + 确定性主键（taskId_logDate）保证幂等；log_date 取任务原截止日
        $sql = "INSERT IGNORE INTO daily_pending_log
            (id, task_id, task_title, category_name, priority, original_due_date, log_date,
             disposition, disposed_at, new_due_date, created_at, last_modified, is_deleted, device_id, needs_sync)
            SELECT
                CONCAT(t.id, '_', DATE_FORMAT(FROM_UNIXTIME(t.due_date/1000), '%Y-%m-%d')),
                t.id, t.title, IFNULL(c.name, ''), t.priority, t.due_date,
                DATE(FROM_UNIXTIME(t.due_date/1000)),
                'pending', NULL, NULL, ?, ?, 0, t.device_id, 1
            FROM tasks t
            LEFT JOIN categories c ON c.id = t.category_id AND c.is_deleted = 0
            WHERE t.is_deleted = 0
              AND t.status = 0
              AND t.task_type != 2
              AND t.due_date IS NOT NULL
              AND t.due_date < ?";
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([$now, $now, $todayStart]);
        return $stmt->rowCount();
    }

    /** 处置：补做完成 / 改期 / 放弃。同步联动 tasks 表。 */
    public function dispose(string $id, string $disposition, ?int $newDueDate): ?DailyPendingLog {
        $stmt = $this->pdo->prepare("SELECT * FROM daily_pending_log WHERE id=? AND is_deleted=0");
        $stmt->execute([$id]);
        $r = $stmt->fetch(PDO::FETCH_ASSOC);
        if (!$r) return null;
        $now = (int)(microtime(true) * 1000);

        $this->pdo->prepare(
            "UPDATE daily_pending_log SET disposition=?, disposed_at=?, new_due_date=?, last_modified=?, needs_sync=1 WHERE id=?"
        )->execute([$disposition, $now, $newDueDate, $now, $id]);

        // 联动原任务：completed -> 置完成；rescheduled -> 更新截止时间
        if ($disposition === 'completed') {
            $this->pdo->prepare("UPDATE tasks SET status=1, updated_at=?, last_modified=?, needs_sync=1 WHERE id=? AND is_deleted=0")
                ->execute([$now, $now, $r['task_id']]);
        } elseif ($disposition === 'rescheduled' && $newDueDate !== null) {
            $this->pdo->prepare("UPDATE tasks SET due_date=?, updated_at=?, last_modified=?, needs_sync=1 WHERE id=? AND is_deleted=0")
                ->execute([$newDueDate, $now, $now, $r['task_id']]);
        }

        $stmt = $this->pdo->prepare("SELECT * FROM daily_pending_log WHERE id=?");
        $stmt->execute([$id]);
        $r2 = $stmt->fetch(PDO::FETCH_ASSOC);
        return $r2 ? self::hydrate($r2) : null;
    }

    /** 本周回顾统计：near 7 天每日数量 + 处置分布 */
    public function weeklyStats(): array {
        $start = date('Y-m-d', strtotime('-6 days'));
        $stmt = $this->pdo->prepare(
            "SELECT log_date, disposition, COUNT(*) AS cnt
             FROM daily_pending_log
             WHERE is_deleted=0 AND log_date >= ?
             GROUP BY log_date, disposition
             ORDER BY log_date ASC"
        );
        $stmt->execute([$start]);
        $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
        $byDate = [];
        $byDisposition = ['pending' => 0, 'completed' => 0, 'rescheduled' => 0, 'abandoned' => 0];
        foreach ($rows as $row) {
            $d = $row['log_date'];
            $byDate[$d] = ($byDate[$d] ?? 0) + (int)$row['cnt'];
            $key = $row['disposition'];
            $byDisposition[$key] = ($byDisposition[$key] ?? 0) + (int)$row['cnt'];
        }
        $total = array_sum($byDisposition);
        $done = $byDisposition['completed'];
        return [
            'start_date' => $start,
            'end_date' => date('Y-m-d'),
            'total' => $total,
            'by_date' => $byDate,
            'by_disposition' => $byDisposition,
            'makeup_rate' => $total > 0 ? round($done / $total, 4) : 0,
        ];
    }

    public function findDirty(): array {
        $stmt = $this->pdo->query("SELECT * FROM daily_pending_log WHERE needs_sync=1 AND is_deleted=0");
        return array_map(static fn($r) => self::hydrate($r), $stmt->fetchAll(PDO::FETCH_ASSOC));
    }

    public function softDelete(string $id): void {
        $this->pdo->prepare("UPDATE daily_pending_log SET is_deleted=1, last_modified=?, needs_sync=0 WHERE id=?")
            ->execute([(int)(microtime(true) * 1000), $id]);
    }

    public function findDeletedIds(): array {
        $stmt = $this->pdo->query("SELECT id FROM daily_pending_log WHERE is_deleted=1 AND needs_sync=1");
        return array_map(static fn($r) => $r['id'], $stmt->fetchAll(PDO::FETCH_ASSOC));
    }

    private static function hydrate(array $r): DailyPendingLog {
        $m = new DailyPendingLog();
        $m->id = $r['id'];
        $m->taskId = $r['task_id'];
        $m->taskTitle = $r['task_title'];
        $m->categoryName = $r['category_name'];
        $m->priority = (int)$r['priority'];
        $m->originalDueDate = (int)$r['original_due_date'];
        $m->logDate = $r['log_date'];
        $m->disposition = $r['disposition'];
        $m->disposedAt = $r['disposed_at'] === null ? null : (int)$r['disposed_at'];
        $m->newDueDate = $r['new_due_date'] === null ? null : (int)$r['new_due_date'];
        $m->createdAt = (int)$r['created_at'];
        $m->lastModified = (int)$r['last_modified'];
        $m->isDeleted = (int)$r['is_deleted'];
        $m->deviceId = $r['device_id'];
        $m->needsSync = (int)$r['needs_sync'];
        return $m;
    }
}
