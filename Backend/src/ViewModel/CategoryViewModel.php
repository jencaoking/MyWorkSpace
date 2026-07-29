<?php
namespace App\ViewModel;

use App\Exception\ApiException;
use App\Model\Category;

/** 分类视图模型：负责上传体校验与对外 API 数组转换 */
final class CategoryViewModel
{
    public static function toApiArray(Category $c): array
    {
        return [
            'id' => $c->id,
            'name' => $c->name,
            'color' => $c->color,
            'sort_order' => $c->sortOrder,
            'is_system' => (bool) $c->isSystem,
            'last_modified' => $c->lastModified,
            'is_deleted' => (bool) $c->isDeleted,
            'device_id' => $c->deviceId,
            'needs_sync' => (bool) $c->needsSync,
        ];
    }

    public static function fromUploadArray(array $raw, string $deviceId): Category
    {
        $id = (string) ($raw['id'] ?? '');
        if ($id === '') {
            throw new ApiException('category id required', 400, 400);
        }
        $name = (string) ($raw['name'] ?? '');
        if ($name === '') {
            throw new ApiException('category name required', 400, 400);
        }
        $now = (int) (microtime(true) * 1000);
        return new Category(
            $id,
            $name,
            isset($raw['color']) ? (string) $raw['color'] : null,
            isset($raw['sort_order']) ? (int) $raw['sort_order'] : 0,
            isset($raw['is_system']) ? (int) $raw['is_system'] : 0,
            isset($raw['last_modified']) ? (int) $raw['last_modified'] : $now,
            0,
            $deviceId,
            0,
        );
    }
}
