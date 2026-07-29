<?php
namespace App\ViewModel;

use App\Exception\ApiException;
use App\Model\Task;

/**
 * 任务表现层模型（MVVM 的 ViewModel 层）
 * 负责 Model 与 View（API 契约）之间的双向映射：
 *   - fromUploadArray: 把客户端上传的裸数据规范化为 Task（含必填校验、类型转换、默认值）
 *   - toApiArray: 把 Task 投影为下发客户端的 JSON 结构
 */
final class TaskViewModel
{
    public static function fromUploadArray(array $raw, string $deviceId): Task
    {
        if (empty($raw['id'])) {
            throw new ApiException('task.id is required', 400, 400);
        }

        $now = (int) (microtime(true) * 1000);

        return new Task(
            id: (string) $raw['id'],
            title: isset($raw['title']) ? (string) $raw['title'] : '',
            content: $raw['content'] ?? null,
            categoryId: (string) ($raw['categoryId'] ?? $raw['category_id'] ?? ''),
            status: (int) ($raw['status'] ?? 0),
            priority: (int) ($raw['priority'] ?? 2),
            dueDate: self::intOrNull($raw, 'dueDate', 'due_date'),
            reminderTime: self::intOrNull($raw, 'reminderTime', 'reminder_time'),
            taskType: (int) ($raw['taskType'] ?? $raw['task_type'] ?? 0),
            repeatType: (int) ($raw['repeatType'] ?? $raw['repeat_type'] ?? 0),
            repeatDays: $raw['repeatDays'] ?? $raw['repeat_days'] ?? null,
            repeatRule: $raw['repeatRule'] ?? $raw['repeat_rule'] ?? '',
            parentGoalId: (string) ($raw['parentGoalId'] ?? $raw['parent_goal_id'] ?? ''),
            createdAt: (int) ($raw['createdAt'] ?? $raw['created_at'] ?? $now),
            updatedAt: (int) ($raw['updatedAt'] ?? $raw['updated_at'] ?? $now),
            lastModified: $now,
            isDeleted: (int) ($raw['isDeleted'] ?? $raw['is_deleted'] ?? 0),
            deviceId: $deviceId,
            needsSync: 0,
        );
    }

    /** 投影为下发结构（snake_case 与 DB 契约一致，配合客户端 LOWER_CASE_WITH_UNDERSCORES） */
    public static function toApiArray(Task $task): array
    {
        return $task->toArray();
    }

    private static function intOrNull(array $raw, string $camel, string $snake): ?int
    {
        if (isset($raw[$camel])) {
            return (int) $raw[$camel];
        }
        if (isset($raw[$snake])) {
            return (int) $raw[$snake];
        }
        return null;
    }
}
