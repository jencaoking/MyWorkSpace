<?php
namespace App\Model;

/** 分类实体（categories 表） */
final class Category
{
    public function __construct(
        public string $id,
        public string $name,
        public ?string $color,
        public int $sortOrder,
        public int $isSystem,
        public int $lastModified,
        public int $isDeleted,
        public string $deviceId,
        public int $needsSync,
    ) {}

    public function toArray(): array
    {
        return [
            'id' => $this->id,
            'name' => $this->name,
            'color' => $this->color,
            'sort_order' => $this->sortOrder,
            'is_system' => $this->isSystem,
            'last_modified' => $this->lastModified,
            'is_deleted' => $this->isDeleted,
            'device_id' => $this->deviceId,
            'needs_sync' => $this->needsSync,
        ];
    }
}
