// 每日未完成作业归档仓库（daily_pending_log 表），对应 PHP App\Repository\DailyPendingLogRepository
const { DailyPendingLog, todayStr } = require('../Model/DailyPendingLog');

function dateStrDaysAgo(n) {
  const d = new Date();
  d.setDate(d.getDate() - n);
  const local = new Date(d.getTime() - d.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
}

class DailyPendingLogRepository {
  constructor(db) {
    this.db = db;
  }

  static hydrate(r) {
    return new DailyPendingLog({
      id: r.id,
      taskId: r.task_id,
      taskTitle: r.task_title,
      categoryName: r.category_name,
      priority: parseInt(r.priority, 10),
      originalDueDate: parseInt(r.original_due_date, 10),
      logDate: r.log_date,
      disposition: r.disposition,
      disposedAt: r.disposed_at === null ? null : parseInt(r.disposed_at, 10),
      newDueDate: r.new_due_date === null ? null : parseInt(r.new_due_date, 10),
      createdAt: parseInt(r.created_at, 10),
      lastModified: parseInt(r.last_modified, 10),
      isDeleted: parseInt(r.is_deleted, 10),
      deviceId: r.device_id,
      needsSync: parseInt(r.needs_sync, 10),
    });
  }

  async list(date = null, disposition = null) {
    let sql = 'SELECT * FROM daily_pending_log WHERE is_deleted=0';
    const params = [];
    if (date) {
      sql += ' AND log_date=?';
      params.push(date);
    }
    if (disposition) {
      sql += ' AND disposition=?';
      params.push(disposition);
    }
    sql += ' ORDER BY log_date DESC, priority ASC';
    const [rows] = await this.db.query(sql, params);
    return rows.map(DailyPendingLogRepository.hydrate);
  }

  async upsert(m) {
    const now = Date.now();
    const sql = `INSERT INTO daily_pending_log
      (id,task_id,task_title,category_name,priority,original_due_date,log_date,disposition,
       disposed_at,new_due_date,created_at,last_modified,is_deleted,device_id,needs_sync)
      VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,0)
      ON DUPLICATE KEY UPDATE
        task_title=VALUES(task_title),category_name=VALUES(category_name),priority=VALUES(priority),
        original_due_date=VALUES(original_due_date),disposition=VALUES(disposition),
        disposed_at=VALUES(disposed_at),new_due_date=VALUES(new_due_date),
        last_modified=VALUES(last_modified),is_deleted=VALUES(is_deleted),
        device_id=VALUES(device_id),needs_sync=0`;
    await this.db.query(sql, [
      m.id, m.taskId, m.taskTitle, m.categoryName, m.priority,
      m.originalDueDate, m.logDate, m.disposition,
      m.disposedAt, m.newDueDate, m.createdAt ?? now,
      m.lastModified ?? now, m.isDeleted ?? 0, m.deviceId,
    ]);
  }

  async archiveOverdue() {
    const d = new Date();
    d.setHours(0, 0, 0, 0);
    const todayStart = d.getTime();
    const now = Date.now();
    const sql = `INSERT IGNORE INTO daily_pending_log
      (id, task_id, task_title, category_name, priority, original_due_date, log_date,
       disposition, disposed_at, new_due_date, created_at, last_modified, is_deleted, device_id, needs_sync)
      SELECT
        CONCAT(t.id, '_', DATE_FORMAT(FROM_UNIXTIME(t.due_date/1000), '%Y-%m-%d')),
        t.id, t.title, IFNULL(c.name, ''), t.priority, t.due_date,
        DATE(FROM_UNIXTIME(t.due_date/1000)),
        'pending', NULL, NULL, ?, ?, 0, t.device_id, 1
      FROM tasks t
      LEFT JOIN categories c ON c.id = t.category_id AND c.is_deleted = 0
      WHERE t.is_deleted = 0
        AND t.status = 0
        AND t.task_type != 2
        AND t.due_date IS NOT NULL
        AND t.due_date < ?`;
    const [r] = await this.db.query(sql, [now, now, todayStart]);
    return r.affectedRows;
  }

  async dispose(id, disposition, newDueDate) {
    const [rows] = await this.db.query('SELECT * FROM daily_pending_log WHERE id=? AND is_deleted=0', [id]);
    if (rows.length === 0) return null;
    const now = Date.now();
    await this.db.query(
      'UPDATE daily_pending_log SET disposition=?, disposed_at=?, new_due_date=?, last_modified=?, needs_sync=1 WHERE id=?',
      [disposition, now, newDueDate, now, id]
    );

    const r = rows[0];
    if (disposition === 'completed') {
      await this.db.query(
        'UPDATE tasks SET status=1, updated_at=?, last_modified=?, needs_sync=1 WHERE id=? AND is_deleted=0',
        [now, now, r.task_id]
      );
    } else if (disposition === 'rescheduled' && newDueDate !== null) {
      await this.db.query(
        'UPDATE tasks SET due_date=?, updated_at=?, last_modified=?, needs_sync=1 WHERE id=? AND is_deleted=0',
        [newDueDate, now, now, r.task_id]
      );
    }

    const [rows2] = await this.db.query('SELECT * FROM daily_pending_log WHERE id=?', [id]);
    return rows2.length ? DailyPendingLogRepository.hydrate(rows2[0]) : null;
  }

  async weeklyStats() {
    const start = dateStrDaysAgo(6);
    const [rows] = await this.db.query(
      'SELECT log_date, disposition, COUNT(*) AS cnt FROM daily_pending_log ' +
        'WHERE is_deleted=0 AND log_date >= ? GROUP BY log_date, disposition ORDER BY log_date ASC',
      [start]
    );
    const byDate = {};
    const byDisposition = { pending: 0, completed: 0, rescheduled: 0, abandoned: 0 };
    for (const row of rows) {
      const key = row.log_date;
      byDate[key] = (byDate[key] || 0) + parseInt(row.cnt, 10);
      const dk = row.disposition;
      byDisposition[dk] = (byDisposition[dk] || 0) + parseInt(row.cnt, 10);
    }
    const total = Object.values(byDisposition).reduce((a, b) => a + b, 0);
    const done = byDisposition.completed;
    return {
      start_date: start,
      end_date: todayStr(),
      total,
      by_date: byDate,
      by_disposition: byDisposition,
      makeup_rate: total > 0 ? Math.round((done / total) * 10000) / 10000 : 0,
    };
  }

  async findDirty() {
    const [rows] = await this.db.query(
      'SELECT * FROM daily_pending_log WHERE needs_sync=1 AND is_deleted=0'
    );
    return rows.map(DailyPendingLogRepository.hydrate);
  }

  async findDeletedIds() {
    const [rows] = await this.db.query(
      'SELECT id FROM daily_pending_log WHERE is_deleted=1 AND needs_sync=1'
    );
    return rows.map((r) => r.id);
  }

  async softDelete(id) {
    await this.db.query(
      'UPDATE daily_pending_log SET is_deleted=1, last_modified=?, needs_sync=0 WHERE id=?',
      [Date.now(), id]
    );
  }
}

module.exports = { DailyPendingLogRepository };
