// 任务实体（tasks 表），对应 PHP App\Model\Task
class Task {
  static STATUS_TODO = 0;
  static STATUS_DONE = 1;
  static STATUS_DOING = 2;

  static TYPE_ONCE = 0;
  static TYPE_CYCLE = 1;
  static TYPE_GOAL = 2;

  static REPEAT_NONE = 0;
  static REPEAT_DAILY = 1;
  static REPEAT_WEEKLY = 2;
  static REPEAT_MONTHLY = 3;

  constructor({
    id,
    title,
    content,
    categoryId,
    status,
    priority,
    dueDate,
    reminderTime,
    repeatRule,
    taskType,
    repeatType,
    repeatDays,
    parentGoalId,
    createdAt,
    updatedAt,
    lastModified,
    isDeleted,
    deviceId,
    needsSync,
  }) {
    this.id = id;
    this.title = title;
    this.content = content;
    this.categoryId = categoryId;
    this.status = status;
    this.priority = priority;
    this.dueDate = dueDate;
    this.reminderTime = reminderTime;
    this.repeatRule = repeatRule;
    this.taskType = taskType;
    this.repeatType = repeatType;
    this.repeatDays = repeatDays;
    this.parentGoalId = parentGoalId;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.lastModified = lastModified;
    this.isDeleted = isDeleted;
    this.deviceId = deviceId;
    this.needsSync = needsSync;
  }

  toArray() {
    return {
      id: this.id,
      title: this.title,
      content: this.content,
      category_id: this.categoryId,
      status: this.status,
      priority: this.priority,
      due_date: this.dueDate,
      reminder_time: this.reminderTime,
      repeat_rule: this.repeatRule,
      task_type: this.taskType,
      repeat_type: this.repeatType,
      repeat_days: this.repeatDays,
      parent_goal_id: this.parentGoalId,
      created_at: this.createdAt,
      updated_at: this.updatedAt,
      last_modified: this.lastModified,
      is_deleted: this.isDeleted,
      device_id: this.deviceId,
      needs_sync: this.needsSync,
    };
  }
}

module.exports = { Task };
