<?php
namespace App\Repository;

use App\Model\Note;
use PDO;

/** 笔记数据访问层（阶段3，PDO 的唯一出口） */
final class NoteRepository
{
    public function __construct(private PDO $pdo) {}

    /** 批量 upsert：INSERT ... ON DUPLICATE KEY UPDATE，事务包裹，按 id 幂等写入 */
    public function upsertBatch(array $notes): int
    {
        if ($notes === []) {
            return 0;
        }

        $cols = [
            'id', 'title', 'content', 'is_pinned', 'is_favorite',
            'created_at', 'updated_at', 'last_modified', 'is_deleted', 'device_id', 'needs_sync',
        ];

        $placeholders = [];
        $params = [];
        foreach ($notes as $i => $note) {
            $row = $note->toArray();
            $ph = [];
            foreach ($cols as $c) {
                $key = ":{$c}_{$i}";
                $ph[] = $key;
                $params[$key] = $row[$c];
            }
            $placeholders[] = '(' . implode(',', $ph) . ')';
        }

        $update = implode(', ', array_map(static fn(string $c): string => "$c = VALUES($c)", $cols));
        $sql = 'INSERT INTO notes (' . implode(',', $cols) . ') VALUES '
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

        return count($notes);
    }

    /** 笔记列表（置顶优先，可选仅收藏） */
    public function list(string $deviceId, bool $favoriteOnly = false): array
    {
        $sql = 'SELECT * FROM notes WHERE device_id = ? AND is_deleted = 0';
        $params = [$deviceId];
        if ($favoriteOnly) {
            $sql .= ' AND is_favorite = 1';
        }
        $sql .= ' ORDER BY is_pinned DESC, updated_at DESC';

        $stmt = $this->pdo->prepare($sql);
        $stmt->execute($params);

        $out = [];
        foreach ($stmt->fetchAll() as $row) {
            $out[] = Note::fromArray($row);
        }
        return $out;
    }

    /**
     * 全文搜索：优先 MySQL FULLTEXT（ngram 中文分词），
     * 无索引/异常时回退 LIKE 子串匹配。
     */
    public function search(string $deviceId, string $keyword, int $limit = 100): array
    {
        $keyword = trim($keyword);
        if ($keyword === '') {
            return [];
        }

        try {
            $stmt = $this->pdo->prepare(
                'SELECT * FROM notes WHERE device_id = ? AND is_deleted = 0 '
                . 'AND MATCH(title, content) AGAINST (? IN NATURAL LANGUAGE MODE) '
                . 'ORDER BY is_pinned DESC, updated_at DESC LIMIT ?'
            );
            $stmt->execute([$deviceId, $keyword, $limit]);
            $rows = $stmt->fetchAll();
            if ($rows !== []) {
                return array_map([Note::class, 'fromArray'], $rows);
            }
        } catch (\Throwable) {
            // FULLTEXT 索引缺失等情况回退 LIKE
        }

        $like = '%' . $keyword . '%';
        $stmt = $this->pdo->prepare(
            'SELECT * FROM notes WHERE device_id = ? AND is_deleted = 0 '
            . 'AND (title LIKE ? OR content LIKE ?) '
            . 'ORDER BY is_pinned DESC, updated_at DESC LIMIT ?'
        );
        $stmt->execute([$deviceId, $like, $like, $limit]);
        return array_map([Note::class, 'fromArray'], $stmt->fetchAll());
    }

    /** 增量拉取：返回该设备 last_modified > since 的记录（含墓碑） */
    public function pullSince(int $since, string $deviceId, int $limit = 200): array
    {
        $stmt = $this->pdo->prepare(
            'SELECT * FROM notes WHERE device_id = ? AND last_modified > ? ORDER BY last_modified ASC LIMIT ?'
        );
        $stmt->execute([$deviceId, $since, $limit]);
        return array_map([Note::class, 'fromArray'], $stmt->fetchAll());
    }

    /** 批量逻辑删除（软删），返回影响行数 */
    public function deleteBatch(array $ids, string $deviceId): int
    {
        if ($ids === []) {
            return 0;
        }
        $ph = implode(',', array_fill(0, count($ids), '?'));
        $now = (int) (microtime(true) * 1000);
        $sql = "UPDATE notes SET is_deleted = 1, last_modified = ?, needs_sync = 1 " .
            "WHERE device_id = ? AND id IN ($ph)";
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute(array_merge([$now, $deviceId], $ids));
        return $stmt->rowCount();
    }
}
