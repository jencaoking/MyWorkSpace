// 任务仓库（tasks 表），对应 PHP App\Repository\TaskRepository
const { Task } = require('../Model/Task');

class TaskRepository {
  constructor(db) {
    this.db = db;
  }

  hydrate(r) {
    return new Task({
      id: String(r.id),
      title: String(r.title),
      content: r.content,
      categoryId: String(r.category_id),
      status: parseInt(r.status, 10),
      priority: parseInt(r.priority, 10),
      dueDate: r.due_date === null ? null : parseInt(r.due_date, 10),
      reminderTime: r.reminder_time === null ? null : parseInt(r.reminder_time, 10),
      repeatRule: r.repeat_rule,
      taskType: parseInt(r.task_type, 10),
      repeatType: parseInt(r.repeat_type, 10),
      repeatDays: r.repeat_days,
      parentGoalId: String(r.parent_goal_id),
      createdAt: parseInt(r.created_at, 10),
      updatedAt: parseInt(r.updated_at, 10),
      lastModified: parseInt(r.last_modified, 10),
      isDeleted: parseInt(r.is_deleted, 10),
      deviceId: String(r.device_id),
      needsSync: parseInt(r.needs_sync, 10),
    });
  }

  async findById(id, deviceId) {
    const [rows] = await this.db.query(
      'SELECT * FROM tasks WHERE id=? AND device_id=?',
      [id, deviceId]
    );
    return rows.length ? this.hydrate(rows[0]) : null;
  }

  async listAll(deviceId, limit, offset) {
    const [rows] = await this.db.query(
      'SELECT * FROM tasks WHERE device_id=? AND is_deleted=0 ORDER BY created_at DESC LIMIT ? OFFSET ?',
      [deviceId, limit, offset]
    );
    return rows.map((r) => this.hydrate(r));
  }

  async list(deviceId, status, limit, offset) {
    const [rows] = await this.db.query(
      'SELECT * FROM tasks WHERE device_id=? AND status=? AND is_deleted=0 ORDER BY created_at DESC LIMIT ? OFFSET ?',
      [deviceId, status, limit, offset]
    );
    return rows.map((r) => this.hydrate(r));
  }

  async countAll(deviceId) {
    const [rows] = await this.db.query(
      'SELECT COUNT(*) AS c FROM tasks WHERE device_id=? AND is_deleted=0',
      [deviceId]
    );
    return rows[0].c;
  }

  async countByStatus(deviceId, status) {
    const [rows] = await this.db.query(
      'SELECT COUNT(*) AS c FROM tasks WHERE device_id=? AND status=? AND is_deleted=0',
      [deviceId, status]
    );
    return rows[0].c;
  }

  async pull(deviceId, since) {
    const [rows] = await this.db.query(
      'SELECT * FROM tasks WHERE device_id=? AND last_modified > ? AND is_deleted=0 ORDER BY last_modified ASC',
      [deviceId, since]
    );
    return rows.map((r) => this.hydrate(r));
  }

  async findDeletedIds(deviceId, since) {
    const [rows] = await this.db.query(
      'SELECT id FROM tasks WHERE device_id=? AND last_modified > ? AND is_deleted=1',
      [deviceId, since]
    );
    return rows.map((r) => r.id);
  }

  async upsertBatch(tasks) {
    const conn = await this.db.getConnection();
    try {
      await conn.beginTransaction();
      const sql = `INSERT INTO tasks
        (id,title,content,category_id,status,priority,due_date,reminder_time,repeat_rule,task_type,repeat_type,repeat_days,parent_goal_id,created_at,updated_at,last_modified,is_deleted,device_id,needs_sync)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0)
        ON DUPLICATE KEY UPDATE
          title=VALUES(title),content=VALUES(content),category_id=VALUES(category_id),
          status=VALUES(status),priority=VALUES(priority),due_date=VALUES(due_date),
          reminder_time=VALUES(reminder_time),repeat_rule=VALUES(repeat_rule),
          task_type=VALUES(task_type),repeat_type=VALUES(repeat_type),repeat_days=VALUES(repeat_days),
          parent_goal_id=VALUES(parent_goal_id),created_at=VALUES(created_at),updated_at=VALUES(updated_at),
          last_modified=VALUES(last_modified),is_deleted=VALUES(is_deleted),device_id=VALUES(device_id),needs_sync=0`;
      for (const t of tasks) {
        const a = t.toArray();
        await conn.query(sql, [
          a.id, a.title, a.content, a.category_id, a.status, a.priority,
          a.due_date, a.reminder_time, a.repeat_rule, a.task_type, a.repeat_type,
          a.repeat_days, a.parent_goal_id, a.created_at, a.updated_at,
          a.last_modified, a.is_deleted, a.device_id,
        ]);
      }
      await conn.commit();
    } catch (e) {
      await conn.rollback();
      throw e;
    } finally {
      conn.release();
    }
  }

  async deleteBatch(ids, deviceId) {
    const conn = await this.db.getConnection();
    try {
      await conn.beginTransaction();
      const now = Date.now();
      const sql =
        'UPDATE tasks SET is_deleted=1, last_modified=?, needs_sync=0 WHERE id=? AND device_id=?';
      for (const id of ids) {
        await conn.query(sql, [now, id, deviceId]);
      }
      await conn.commit();
    } catch (e) {
      await conn.rollback();
      throw e;
    } finally {
      conn.release();
    }
  }
}

module.exports = { TaskRepository };
