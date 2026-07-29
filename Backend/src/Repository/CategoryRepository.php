<?php
namespace App\Repository;

use App\Model\Category;
use PDO;

/** 分类仓储：分类为共享引用数据，列表返回全部未删除项（跨设备可见） */
final class CategoryRepository
{
    public function __construct(private PDO $pdo) {}

    /** 列表：仅未删除，按 sort_order、id 排序 */
    public function list(): array
    {
        $stmt = $this->pdo->query(
            'SELECT * FROM categories WHERE is_deleted = 0 ORDER BY sort_order ASC, id ASC'
        );
        return array_map([self::class, 'hydrate'], $stmt->fetchAll());
    }

    public function byId(string $id): ?Category
    {
        $stmt = $this->pdo->prepare('SELECT * FROM categories WHERE id = ?');
        $stmt->execute([$id]);
        $row = $stmt->fetch();
        return $row ? self::hydrate($row) : null;
    }

    /** 批量 upsert：写入即标记 needs_sync=0（已同步到服务端），并恢复软删除 */
    public function upsertBatch(array $cats): int
    {
        $sql = 'INSERT INTO categories (id, name, color, sort_order, is_system, last_modified, is_deleted, device_id, needs_sync)
                VALUES (?, ?, ?, ?, ?, ?, 0, ?, 0)
                ON DUPLICATE KEY UPDATE
                  name = VALUES(name),
                  color = VALUES(color),
                  sort_order = VALUES(sort_order),
                  is_system = VALUES(is_system),
                  last_modified = VALUES(last_modified),
                  device_id = VALUES(device_id),
                  needs_sync = 0,
                  is_deleted = 0';
        $stmt = $this->pdo->prepare($sql);
        $n = 0;
        foreach ($cats as $c) {
            $stmt->execute([
                $c->id, $c->name, $c->color, $c->sortOrder, $c->isSystem,
                $c->lastModified, $c->deviceId,
            ]);
            $n++;
        }
        return $n;
    }

    /** 批量软删除：标记 is_deleted=1 并置 needs_sync=1 以便广播 */
    public function deleteBatch(array $ids): int
    {
        if (empty($ids)) {
            return 0;
        }
        $placeholders = implode(',', array_fill(0, count($ids), '?'));
        $sql = "UPDATE categories SET is_deleted = 1, needs_sync = 1, last_modified = ? WHERE id IN ($placeholders)";
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute(array_merge([(int) (microtime(true) * 1000)], $ids));
        return $stmt->rowCount();
    }

    /** 增量拉取：needs_sync=1 且未删除 */
    public function findDirty(): array
    {
        $stmt = $this->pdo->query('SELECT * FROM categories WHERE needs_sync = 1 AND is_deleted = 0');
        return array_map([self::class, 'hydrate'], $stmt->fetchAll());
    }

    /** 已删除 id（供客户端本地清理） */
    public function findDeletedIds(): array
    {
        $stmt = $this->pdo->query('SELECT id FROM categories WHERE is_deleted = 1 AND needs_sync = 1');
        return array_map(static fn($r) => $r['id'], $stmt->fetchAll());
    }

    private static function hydrate(array $row): Category
    {
        return new Category(
            (string) $row['id'],
            (string) $row['name'],
            $row['color'] ?? null,
            (int) $row['sort_order'],
            (int) $row['is_system'],
            (int) $row['last_modified'],
            (int) $row['is_deleted'],
            (string) $row['device_id'],
            (int) $row['needs_sync'],
        );
    }
}
