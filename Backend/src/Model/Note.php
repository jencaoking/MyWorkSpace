<?php
namespace App\Model;

/**
 * 笔记领域模型（MVVM 的 Model 层，阶段3）
 * 纯值对象：仅承载数据，不含持久化与展示逻辑。
 */
final class Note
{
    public function __construct(
        public string $id,
        public string $title = '',
        public ?string $content = null,
        public int $isPinned = 0,
        public int $isFavorite = 0,
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
            isPinned: (int) ($row['is_pinned'] ?? 0),
            isFavorite: (int) ($row['is_favorite'] ?? 0),
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
            'is_pinned' => $this->isPinned,
            'is_favorite' => $this->isFavorite,
            'created_at' => $this->createdAt,
            'updated_at' => $this->updatedAt,
            'last_modified' => $this->lastModified,
            'is_deleted' => $this->isDeleted,
            'device_id' => $this->deviceId,
            'needs_sync' => $this->needsSync,
        ];
    }
}
