// 分类实体（categories 表），对应 PHP App\Model\Category
class Category {
  constructor({
    id,
    name,
    color,
    sortOrder,
    isSystem,
    lastModified,
    isDeleted,
    deviceId,
    needsSync,
  }) {
    this.id = id;
    this.name = name;
    this.color = color;
    this.sortOrder = sortOrder;
    this.isSystem = isSystem;
    this.lastModified = lastModified;
    this.isDeleted = isDeleted;
    this.deviceId = deviceId;
    this.needsSync = needsSync;
  }

  toArray() {
    return {
      id: this.id,
      name: this.name,
      color: this.color,
      sort_order: this.sortOrder,
      is_system: this.isSystem,
      last_modified: this.lastModified,
      is_deleted: this.isDeleted,
      device_id: this.deviceId,
      needs_sync: this.needsSync,
    };
  }
}

module.exports = { Category };
