// 笔记领域模型（notes 表），对应 PHP App\Model\Note。纯值对象。
class Note {
  constructor({
    id,
    title = '',
    content = null,
    isPinned = 0,
    isFavorite = 0,
    createdAt = 0,
    updatedAt = 0,
    lastModified = 0,
    isDeleted = 0,
    deviceId = '',
    needsSync = 1,
  }) {
    this.id = id;
    this.title = title;
    this.content = content;
    this.isPinned = isPinned;
    this.isFavorite = isFavorite;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.lastModified = lastModified;
    this.isDeleted = isDeleted;
    this.deviceId = deviceId;
    this.needsSync = needsSync;
  }

  static fromArray(row) {
    return new Note({
      id: String(row.id),
      title: String(row.title ?? ''),
      content: row.content ?? null,
      isPinned: parseInt(row.is_pinned ?? 0, 10),
      isFavorite: parseInt(row.is_favorite ?? 0, 10),
      createdAt: parseInt(row.created_at ?? 0, 10),
      updatedAt: parseInt(row.updated_at ?? 0, 10),
      lastModified: parseInt(row.last_modified ?? 0, 10),
      isDeleted: parseInt(row.is_deleted ?? 0, 10),
      deviceId: String(row.device_id ?? ''),
      needsSync: parseInt(row.needs_sync ?? 1, 10),
    });
  }

  toArray() {
    return {
      id: this.id,
      title: this.title,
      content: this.content,
      is_pinned: this.isPinned,
      is_favorite: this.isFavorite,
      created_at: this.createdAt,
      updated_at: this.updatedAt,
      last_modified: this.lastModified,
      is_deleted: this.isDeleted,
      device_id: this.deviceId,
      needs_sync: this.needsSync,
    };
  }
}

module.exports = { Note };
