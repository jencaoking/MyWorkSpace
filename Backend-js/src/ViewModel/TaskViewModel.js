// 任务视图模型，对应 PHP App\ViewModel\TaskViewModel
const { ApiException } = require('../../src/Exception/ApiException');
const { Task } = require('../Model/Task');

function toApiArray(t) {
  return {
    id: t.id,
    title: t.title,
    content: t.content,
    category_id: t.categoryId,
    status: t.status,
    priority: t.priority,
    due_date: t.dueDate,
    reminder_time: t.reminderTime,
    repeat_rule: t.repeatRule,
    task_type: t.taskType,
    repeat_type: t.repeatType,
    repeat_days: t.repeatDays,
    parent_goal_id: t.parentGoalId,
    created_at: t.createdAt,
    updated_at: t.updatedAt,
    last_modified: t.lastModified,
    is_deleted: t.isDeleted,
    device_id: t.deviceId,
    needs_sync: t.needsSync,
  };
}

function fromUploadArray(raw, deviceId) {
  const now = Date.now();
  const id = raw.id || '';
  if (!id) throw new ApiException('task id required', 400, 400);

  const status = raw.status !== undefined ? parseInt(raw.status, 10) : Task.STATUS_TODO;
  const priority = raw.priority !== undefined ? parseInt(raw.priority, 10) : 2;
  const taskType = raw.task_type !== undefined ? parseInt(raw.task_type, 10) : Task.TYPE_ONCE;
  const repeatType = raw.repeat_type !== undefined ? parseInt(raw.repeat_type, 10) : Task.REPEAT_NONE;

  const dueDate =
    raw.due_date !== undefined && raw.due_date !== null && raw.due_date !== ''
      ? parseInt(raw.due_date, 10)
      : null;
  const reminderTime =
    raw.reminder_time !== undefined && raw.reminder_time !== null && raw.reminder_time !== ''
      ? parseInt(raw.reminder_time, 10)
      : null;

  return new Task({
    id,
    title: raw.title !== undefined ? String(raw.title) : '',
    content: raw.content ?? null,
    categoryId: raw.category_id !== undefined ? String(raw.category_id) : '',
    status,
    priority,
    dueDate,
    reminderTime,
    repeatRule: raw.repeat_rule ?? null,
    taskType,
    repeatType,
    repeatDays: raw.repeat_days ?? null,
    parentGoalId: raw.parent_goal_id !== undefined ? String(raw.parent_goal_id) : '',
    createdAt: raw.created_at !== undefined ? parseInt(raw.created_at, 10) : now,
    updatedAt: raw.updated_at !== undefined ? parseInt(raw.updated_at, 10) : now,
    lastModified: raw.last_modified !== undefined ? parseInt(raw.last_modified, 10) : now,
    isDeleted: raw.is_deleted !== undefined ? parseInt(raw.is_deleted, 10) : 0,
    deviceId: deviceId || (raw.device_id !== undefined ? String(raw.device_id) : ''),
    needsSync: raw.needs_sync !== undefined ? parseInt(raw.needs_sync, 10) : 0,
  });
}

function listToArray(list) {
  return list.map(toApiArray);
}

module.exports = { toApiArray, fromUploadArray, listToArray };
