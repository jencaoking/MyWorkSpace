<?php
namespace App\Repository;

use App\Model\Task;
use PDO;

/** 任务数据访问层（MVVM 中 Model 层的持久化实现，PDO 的唯一出口） */
final class TaskRepository
{
    public function __construct(private PDO $pdo) {}

    /** 批量 upsert：INSERT ... ON DUPLICATE KEY UPDATE，事务包裹，按 id 幂等写入 */
    public function upsertBatch(array $tasks): int
    {
        if ($tasks === []) {
            return 0;
        }

        $cols = [
            'id', 'title', 'content', 'category_id', 'status', 'priority',
            'due_date', 'reminder_time', 'task_type', 'repeat_type',
            'repeat_days', 'repeat_rule', 'parent_goal_id',
            'created_at', 'updated_at', 'last_modified', 'is_deleted', 'device_id', 'needs_sync',
        ];

        $placeholders = [];
        $params = [];
        foreach ($tasks as $i => $task) {
            $row = $task->toArray();
            $ph = [];
            foreach ($cols as $c) {
                $key = ":{$c}_{$i}";
                $ph[] = $key;
                $params[$key] = $row[$c];
            }
            $placeholders[] = '(' . implode(',', $ph) . ')';
        }

        $update = implode(', ', array_map(static fn(string $c): string => "$c = VALUES($c)", $cols));
        $sql = 'INSERT INTO tasks (' . implode(',', $cols) . ') VALUES '
            . implode(',', $placeholders)
            . ' ON DUPLICATE KEY UPDATE ' . $update;

        $this->pdo->beginTransaction();
        try {
            $this->pdo->prepare($sql)->execute($params);
            $this->pdo->commit();
        } catch (\Throwable $e) {
            $this->pdo->rollBack();
            throw $e;
        }

        return count($tasks);
    }

    /** 增量拉取：返回该设备 last_modified > since 的记录（含已删除，供客户端墓碑同步） */
    public function pullSince(int $since, string $deviceId, int $limit = 200): array
    {
        $stmt = $this->pdo->prepare(
            'SELECT * FROM tasks WHERE device_id = ? AND last_modified > ? ORDER BY last_modified ASC LIMIT ?'
        );
        $stmt->execute([$deviceId, $since, $limit]);

        $out = [];
        foreach ($stmt->fetchAll() as $row) {
            $out[] = Task::fromArray($row);
        }
        return $out;
    }

    /** 任务列表（按分类 / 状态过滤） */
    public function list(string $deviceId, string $categoryId = '', ?int $status = null): array
    {
        $sql = 'SELECT * FROM tasks WHERE device_id = ? AND is_deleted = 0';
        $params = [$deviceId];
        if ($categoryId !== '') {
            $sql .= ' AND category_id = ?';
            $params[] = $categoryId;
        }
        if ($status !== null) {
            $sql .= ' AND status = ?';
            $params[] = $status;
        }
        $sql .= ' ORDER BY created_at DESC';

        $stmt = $this->pdo->prepare($sql);
        $stmt->execute($params);

        $out = [];
        foreach ($stmt->fetchAll() as $row) {
            $out[] = Task::fromArray($row);
        }
        return $out;
    }

    /** 批量逻辑删除（软删），返回影响行数 */
    public function deleteBatch(array $ids, string $deviceId): int
    {
        if ($ids === []) {
            return 0;
        }
        $ph = implode(',', array_fill(0, count($ids), '?'));
        $now = (int) (microtime(true) * 1000);
        $sql = "UPDATE tasks SET is_deleted = 1, last_modified = ?, needs_sync = 1 " .
            "WHERE device_id = ? AND id IN ($ph)";
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute(array_merge([$now, $deviceId], $ids));
        return $stmt->rowCount();
    }

    /** 统计概览：总数 / 完成 / 进行中(待办) / 循环任务数 / 按分类计数 */
    public function stats(string $deviceId): array
    {
        $total = $this->scalar("SELECT COUNT(*) FROM tasks WHERE device_id = ? AND is_deleted = 0", [$deviceId]);
        $done = $this->scalar("SELECT COUNT(*) FROM tasks WHERE device_id = ? AND is_deleted = 0 AND status = 1", [$deviceId]);
        $active = $this->scalar("SELECT COUNT(*) FROM tasks WHERE device_id = ? AND is_deleted = 0 AND status != 1", [$deviceId]);
        $repeat = $this->scalar("SELECT COUNT(*) FROM tasks WHERE device_id = ? AND is_deleted = 0 AND task_type = 1", [$deviceId]);

        $stmt = $this->pdo->prepare('SELECT category_id, COUNT(*) AS cnt FROM tasks WHERE device_id = ? AND is_deleted = 0 GROUP BY category_id');
        $stmt->execute([$deviceId]);
        $byCategory = [];
        foreach ($stmt->fetchAll() as $r) {
            $byCategory[$r['category_id']] = (int) $r['cnt'];
        }

        return [
            'total' => $total,
            'done' => $done,
            'active' => $active,
            'repeat' => $repeat,
            'by_category' => $byCategory,
        ];
    }

    private function scalar(string $sql, array $params): int
    {
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute($params);
        return (int) $stmt->fetchColumn();
    }
}
