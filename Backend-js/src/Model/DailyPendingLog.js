// 每日未完成作业归档模型（daily_pending_log 表），对应 PHP App\Model\DailyPendingLog
function todayStr() {
  const d = new Date();
  const local = new Date(d.getTime() - d.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
}

class DailyPendingLog {
  constructor({
    id,
    taskId,
    taskTitle,
    categoryName,
    priority,
    originalDueDate,
    logDate,
    disposition,
    disposedAt,
    newDueDate,
    createdAt,
    lastModified,
    isDeleted,
    deviceId,
    needsSync,
  }) {
    this.id = id;
    this.taskId = taskId;
    this.taskTitle = taskTitle;
    this.categoryName = categoryName;
    this.priority = priority;
    this.originalDueDate = originalDueDate;
    this.logDate = logDate;
    this.disposition = disposition;
    this.disposedAt = disposedAt;
    this.newDueDate = newDueDate;
    this.createdAt = createdAt;
    this.lastModified = lastModified;
    this.isDeleted = isDeleted;
    this.deviceId = deviceId;
    this.needsSync = needsSync;
  }

  toApiArray() {
    return {
      id: this.id,
      task_id: this.taskId,
      task_title: this.taskTitle,
      category_name: this.categoryName,
      priority: this.priority,
      original_due_date: this.originalDueDate,
      log_date: this.logDate,
      disposition: this.disposition,
      disposed_at: this.disposedAt,
      new_due_date: this.newDueDate,
      created_at: this.createdAt,
      last_modified: this.lastModified,
      is_deleted: this.isDeleted,
      device_id: this.deviceId,
      needs_sync: this.needsSync,
    };
  }

  static fromUploadArray(a) {
    const now = Date.now();
    const numOrNull = (v) => (v !== undefined && v !== '' && v !== null ? parseInt(v, 10) : null);
    return new DailyPendingLog({
      id: a.id ?? null,
      taskId: a.task_id ?? '',
      taskTitle: a.task_title ?? '',
      categoryName: a.category_name ?? '',
      priority: a.priority !== undefined ? parseInt(a.priority, 10) : 2,
      originalDueDate: a.original_due_date !== undefined ? parseInt(a.original_due_date, 10) : 0,
      logDate: a.log_date ?? todayStr(),
      disposition: a.disposition ?? 'pending',
      disposedAt: numOrNull(a.disposed_at),
      newDueDate: numOrNull(a.new_due_date),
      createdAt: a.created_at !== undefined ? parseInt(a.created_at, 10) : now,
      lastModified: a.last_modified !== undefined ? parseInt(a.last_modified, 10) : now,
      isDeleted: a.is_deleted !== undefined ? parseInt(a.is_deleted, 10) : 0,
      deviceId: a.device_id ?? null,
      needsSync: a.needs_sync !== undefined ? parseInt(a.needs_sync, 10) : 0,
    });
  }
}

module.exports = { DailyPendingLog, todayStr };
