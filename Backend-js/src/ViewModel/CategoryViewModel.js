// 分类视图模型，对应 PHP App\ViewModel\CategoryViewModel
const { ApiException } = require('../../src/Exception/ApiException');
const { Category } = require('../Model/Category');

function toApiArray(c) {
  return {
    id: c.id,
    name: c.name,
    color: c.color,
    sort_order: c.sortOrder,
    is_system: !!c.isSystem,
    last_modified: c.lastModified,
    is_deleted: !!c.isDeleted,
    device_id: c.deviceId,
    needs_sync: !!c.needsSync,
  };
}

function fromUploadArray(raw, deviceId) {
  const id = String(raw.id ?? '');
  if (id === '') throw new ApiException('category id required', 400, 400);
  const name = String(raw.name ?? '');
  if (name === '') throw new ApiException('category name required', 400, 400);
  const now = Date.now();
  return new Category({
    id,
    name,
    color: raw.color !== undefined ? String(raw.color) : null,
    sortOrder: raw.sort_order !== undefined ? parseInt(raw.sort_order, 10) : 0,
    isSystem: raw.is_system !== undefined ? parseInt(raw.is_system, 10) : 0,
    lastModified: raw.last_modified !== undefined ? parseInt(raw.last_modified, 10) : now,
    isDeleted: 0,
    deviceId,
    needsSync: 0,
  });
}

module.exports = { toApiArray, fromUploadArray };
