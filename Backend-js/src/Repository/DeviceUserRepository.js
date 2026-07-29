// 设备用户仓库（后台用户管理），对应 PHP App\Repository\DeviceUserRepository
const { ApiException } = require('../../src/Exception/ApiException');
const { DeviceUser } = require('../Model/DeviceUser');

const TABLE = 'device_users';
const SOURCES = {
  tasks: ['created_at', 'updated_at'],
  categories: ['last_modified'],
  notes: ['created_at', 'updated_at'],
  sport_records: ['record_date'],
  english_words: ['last_modified'],
  movie_books: ['last_modified'],
  health_records: ['record_time'],
  account_records: ['record_date'],
};

class DeviceUserRepository {
  constructor(db) {
    this.db = db;
  }

  async ensureTable() {
    await this.db.query(
      `CREATE TABLE IF NOT EXISTS \`${TABLE}\` (
        \`device_id\`  VARCHAR(64) NOT NULL,
        \`status\`     VARCHAR(16) NOT NULL DEFAULT 'active',
        \`note\`       VARCHAR(255) DEFAULT '',
        \`created_at\` BIGINT NOT NULL,
        \`updated_at\` BIGINT NOT NULL,
        PRIMARY KEY (\`device_id\`),
        KEY \`idx_status\` (\`status\`)
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`
    );
  }

  async listDevices(limit, offset, q) {
    await this.ensureTable();
    const union = [];
    for (const [t, cols] of Object.entries(SOURCES)) {
      const firsts = cols.map((c) => `COALESCE(\`${c}\`,9223372036854775807)`);
      const lasts = cols.map((c) => `COALESCE(\`${c}\`,0)`);
      const firstExpr = firsts.length > 1 ? `LEAST(${firsts.join(',')})` : firsts[0];
      const lastExpr = lasts.length > 1 ? `GREATEST(${lasts.join(',')})` : lasts[0];
      union.push(
        `SELECT \`device_id\`, COUNT(*) AS cnt, ${firstExpr} AS first_at, ${lastExpr} AS last_at ` +
          `FROM \`${t}\` WHERE \`device_id\`<>\'\' AND \`is_deleted\`=0 GROUP BY \`device_id\``
      );
    }
    const sub = union.join('\n UNION ALL\n');
    const where = q ? 'WHERE d.device_id LIKE ?' : '';
    const sql =
      `SELECT d.device_id, COALESCE(u.status,'active') AS status, COALESCE(u.note,'') AS note, ` +
      `SUM(d.cnt) AS total_records, MIN(d.first_at) AS first_seen, MAX(d.last_at) AS last_seen ` +
      `FROM (${sub}) d LEFT JOIN \`${TABLE}\` u ON u.device_id = d.device_id ${where} ` +
      `GROUP BY d.device_id, u.status, u.note ORDER BY last_seen DESC LIMIT ? OFFSET ?`;
    const params = [];
    if (where) params.push('%' + q + '%');
    params.push(limit, offset);
    const [rows] = await this.db.query(sql, params);
    return rows.map((r) => {
      const m = new DeviceUser();
      m.deviceId = r.device_id;
      m.status = r.status;
      m.note = r.note;
      m.totalRecords = parseInt(r.total_records, 10);
      m.firstSeen = parseInt(r.first_seen, 10);
      m.lastSeen = parseInt(r.last_seen, 10);
      return m;
    });
  }

  async countDevices(q) {
    const parts = [];
    for (const t of Object.keys(SOURCES)) {
      parts.push(`SELECT DISTINCT \`device_id\` FROM \`${t}\` WHERE \`device_id\`<>\'\' AND \`is_deleted\`=0`);
    }
    const sub = parts.join('\n UNION \n');
    const where = q ? 'WHERE device_id LIKE ?' : '';
    const [rows] = await this.db.query(`SELECT COUNT(*) AS c FROM (${sub}) x ${where}`, q ? ['%' + q + '%'] : []);
    return parseInt(rows[0].c, 10);
  }

  async setStatus(deviceId, status, note) {
    await this.ensureTable();
    if (status !== null && !['active', 'banned'].includes(status)) {
      throw new ApiException('invalid status', 400, 400);
    }
    const now = Date.now();
    const existingNote = await this.getNote(deviceId);
    if (existingNote === null) {
      await this.db.query(
        `INSERT INTO \`${TABLE}\` (device_id, status, note, created_at, updated_at) VALUES (?,?,?,?,?)`,
        [deviceId, status ?? 'active', note ?? '', now, now]
      );
    } else {
      const finalNote = note === null ? existingNote : note;
      const finalStatus = status ?? 'active';
      await this.db.query(
        `UPDATE \`${TABLE}\` SET status=?, note=?, updated_at=? WHERE device_id=?`,
        [finalStatus, finalNote, now, deviceId]
      );
    }
    const m = new DeviceUser();
    m.deviceId = deviceId;
    m.status = status ?? 'active';
    m.note = note === null ? existingNote ?? '' : note;
    return m;
  }

  async getNote(deviceId) {
    const [rows] = await this.db.query(`SELECT note FROM \`${TABLE}\` WHERE device_id=?`, [deviceId]);
    return rows.length ? String(rows[0].note) : null;
  }

  async deleteDeviceData(deviceId) {
    const now = Date.now();
    const tables = [
      'tasks', 'task_checkins', 'categories', 'notes',
      'sport_records', 'english_words', 'movie_books', 'health_records', 'account_records',
    ];
    const counts = {};
    for (const t of tables) {
      const [r] = await this.db.query(
        `UPDATE \`${t}\` SET is_deleted=1, last_modified=?, needs_sync=1 WHERE device_id=? AND is_deleted=0`,
        [now, deviceId]
      );
      counts[t] = r.affectedRows;
    }
    return counts;
  }
}

module.exports = { DeviceUserRepository };
