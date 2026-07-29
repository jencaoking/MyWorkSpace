<?php
namespace App\ViewModel;

use App\Exception\ApiException;
use App\Model\Note;

/**
 * 笔记表现层模型（MVVM 的 ViewModel 层，阶段3）
 * 负责 Model 与 View（API 契约）之间的双向映射。
 */
final class NoteViewModel
{
    public static function fromUploadArray(array $raw, string $deviceId): Note
    {
        if (empty($raw['id'])) {
            throw new ApiException('note.id is required', 400, 400);
        }

        $now = (int) (microtime(true) * 1000);

        return new Note(
            id: (string) $raw['id'],
            title: isset($raw['title']) ? (string) $raw['title'] : '',
            content: $raw['content'] ?? null,
            isPinned: (int) ($raw['isPinned'] ?? $raw['is_pinned'] ?? 0),
            isFavorite: (int) ($raw['isFavorite'] ?? $raw['is_favorite'] ?? 0),
            createdAt: (int) ($raw['createdAt'] ?? $raw['created_at'] ?? $now),
            updatedAt: (int) ($raw['updatedAt'] ?? $raw['updated_at'] ?? $now),
            lastModified: $now,
            isDeleted: (int) ($raw['isDeleted'] ?? $raw['is_deleted'] ?? 0),
            deviceId: $deviceId,
            needsSync: 0,
        );
    }

    /** 投影为下发结构（snake_case 与 DB 契约一致） */
    public static function toApiArray(Note $note): array
    {
        return $note->toArray();
    }
}
