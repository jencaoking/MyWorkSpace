<?php
namespace App\Model;

/** 每日未完成作业归档模型（对应 daily_pending_log 表）。 */
class DailyPendingLog {
    public ?string $id;
    public ?string $taskId;
    public ?string $taskTitle;
    public ?string $categoryName;
    public ?int $priority;
    public ?int $originalDueDate;
    public ?string $logDate;      // YYYY-MM-DD
    public ?string $disposition;  // pending|completed|rescheduled|abandoned
    public ?int $disposedAt;
    public ?int $newDueDate;
    public ?int $createdAt;
    public ?int $lastModified;
    public ?int $isDeleted;
    public ?string $deviceId;
    public ?int $needsSync;

    public static function fromUploadArray(array $a): self {
        $now = (int)(microtime(true) * 1000);
        $m = new self();
        $m->id = $a['id'] ?? null;
        $m->taskId = $a['task_id'] ?? '';
        $m->taskTitle = $a['task_title'] ?? '';
        $m->categoryName = $a['category_name'] ?? '';
        $m->priority = isset($a['priority']) ? (int)$a['priority'] : 2;
        $m->originalDueDate = isset($a['original_due_date']) ? (int)$a['original_due_date'] : 0;
        $m->logDate = $a['log_date'] ?? date('Y-m-d');
        $m->disposition = $a['disposition'] ?? 'pending';
        $m->disposedAt = isset($a['disposed_at']) && $a['disposed_at'] !== '' && $a['disposed_at'] !== null ? (int)$a['disposed_at'] : null;
        $m->newDueDate = isset($a['new_due_date']) && $a['new_due_date'] !== '' && $a['new_due_date'] !== null ? (int)$a['new_due_date'] : null;
        $m->createdAt = isset($a['created_at']) ? (int)$a['created_at'] : $now;
        $m->lastModified = isset($a['last_modified']) ? (int)$a['last_modified'] : $now;
        $m->isDeleted = isset($a['is_deleted']) ? (int)$a['is_deleted'] : 0;
        $m->deviceId = $a['device_id'] ?? null;
        $m->needsSync = isset($a['needs_sync']) ? (int)$a['needs_sync'] : 0;
        return $m;
    }

    public function toApiArray(): array {
        return [
            'id' => $this->id,
            'task_id' => $this->taskId,
            'task_title' => $this->taskTitle,
            'category_name' => $this->categoryName,
            'priority' => $this->priority,
            'original_due_date' => $this->originalDueDate,
            'log_date' => $this->logDate,
            'disposition' => $this->disposition,
            'disposed_at' => $this->disposedAt,
            'new_due_date' => $this->newDueDate,
            'created_at' => $this->createdAt,
            'last_modified' => $this->lastModified,
            'is_deleted' => $this->isDeleted,
            'device_id' => $this->deviceId,
            'needs_sync' => $this->needsSync,
        ];
    }
}
