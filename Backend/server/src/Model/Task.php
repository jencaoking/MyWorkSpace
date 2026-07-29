<?php
namespace App\Model;

/** 任务状态（与 DB status 字段、Android Room 实体一致） */
enum TaskStatus: int
{
    case TODO = 0;
    case DONE = 1;
    case DOING = 2;
}

/** 任务优先级 */
enum TaskPriority: int
{
    case HIGH = 1;
    case MEDIUM = 2;
    case LOW = 3;
}

/** 任务类型 */
enum TaskType: int
{
    case ONCE = 0;
    case REPEAT = 1;
    case GOAL = 2;
}

/**
 * 任务领域模型（MVVM 的 Model 层）
 * 纯值对象：仅承载数据，不含持久化与展示逻辑。
 */
final class Task
{
    public function __construct(
        public string $id,
        public string $title = '',
        public ?string $content = null,
        public string $categoryId = '',
        public int $status = 0,
        public int $priority = 2,
        public ?int $dueDate = null,
        public ?int $reminderTime = null,
        public int $taskType = 0,
        public int $repeatType = 0,
        public ?string $repeatDays = null,
        public string $repeatRule = '',
        public string $parentGoalId = '',
        public int $createdAt = 0,
        public int $updatedAt = 0,
        public int $lastModified = 0,
        public int $isDeleted = 0,
        public string $deviceId = '',
        public int $needsSync = 1,
    ) {}

    /** 从 DB 行构造 */
    public static function fromArray(array $row): self
    {
        return new self(
            id: (string) $row['id'],
            title: (string) ($row['title'] ?? ''),
            content: $row['content'] ?? null,
            categoryId: (string) ($row['category_id'] ?? ''),
            status: (int) ($row['status'] ?? 0),
            priority: (int) ($row['priority'] ?? 2),
            dueDate: isset($row['due_date']) ? (int) $row['due_date'] : null,
            reminderTime: isset($row['reminder_time']) ? (int) $row['reminder_time'] : null,
            taskType: (int) ($row['task_type'] ?? 0),
            repeatType: (int) ($row['repeat_type'] ?? 0),
            repeatDays: $row['repeat_days'] ?? null,
            repeatRule: (string) ($row['repeat_rule'] ?? ''),
            parentGoalId: (string) ($row['parent_goal_id'] ?? ''),
            createdAt: (int) ($row['created_at'] ?? 0),
            updatedAt: (int) ($row['updated_at'] ?? 0),
            lastModified: (int) ($row['last_modified'] ?? 0),
            isDeleted: (int) ($row['is_deleted'] ?? 0),
            deviceId: (string) ($row['device_id'] ?? ''),
            needsSync: (int) ($row['needs_sync'] ?? 1),
        );
    }

    /** 转成与 DB 列名一致的关联数组（用于持久化 / 输出） */
    public function toArray(): array
    {
        return [
            'id' => $this->id,
            'title' => $this->title,
            'content' => $this->content,
            'category_id' => $this->categoryId,
            'status' => $this->status,
            'priority' => $this->priority,
            'due_date' => $this->dueDate,
            'reminder_time' => $this->reminderTime,
            'task_type' => $this->taskType,
            'repeat_type' => $this->repeatType,
            'repeat_days' => $this->repeatDays,
            'repeat_rule' => $this->repeatRule,
            'parent_goal_id' => $this->parentGoalId,
            'created_at' => $this->createdAt,
            'updated_at' => $this->updatedAt,
            'last_modified' => $this->lastModified,
            'is_deleted' => $this->isDeleted,
            'device_id' => $this->deviceId,
            'needs_sync' => $this->needsSync,
        ];
    }
}
