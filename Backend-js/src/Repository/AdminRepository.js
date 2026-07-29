// 后台管理数据访问层：跨设备全局只读统计与通用数据浏览/编辑/删除。
// 对应 PHP App\Repository\AdminRepository
const { ApiException } = require('../Exception/ApiException');

const TABLES = [
  'tasks', 'categories', 'notes', 'sport_records',
  'english_words', 'movie_books', 'health_records',
  'account_records', 'user_settings',
];
const READ_ONLY_COLUMNS = ['id'];
const NO_DELETE_TABLES = ['user_settings'];
const AUDIT_TABLE = 'admin_audit_log';

class AdminRepository {
  constructor(db) {
    this.db = db;
  }

  static isAllowed(table) {
    return TABLES.includes(table);
  }

  static canDelete(table) {
    return !NO_DELETE_TABLES.includes(table);
  }

  static isDeleteProtected(table) {
    return NO_DELETE_TABLES.includes(table);
  }

  async tableCounts() {
    const out = {};
    for (const t of TABLES) {
      const [rows] = await this.db.query(`SELECT COUNT(*) AS c FROM \`${t}\``);
      out[t] = parseInt(rows[0].c, 10);
    }
    return out;
  }

  async deviceCount() {
    const [rows] = await this.db.query(
      "SELECT COUNT(DISTINCT device_id) AS c FROM tasks WHERE device_id <> ''"
    );
    return parseInt(rows[0].c, 10);
  }

  orderCol(table) {
    const cols = this.syncColumnsOf(table);
    for (const pref of ['created_at', 'updated_at', 'last_modified', 'id']) {
      if (Object.prototype.hasOwnProperty.call(cols, pref)) return pref;
    }
    return 'id';
  }

  async browse(table, limit = 50, offset = 0) {
    const order = this.orderCol(table);
    const [rows] = await this.db.query(
      `SELECT * FROM \`${table}\` ORDER BY \`${order}\` DESC LIMIT ? OFFSET ?`,
      [limit, offset]
    );
    return rows;
  }

  async countRows(table) {
    const [rows] = await this.db.query(`SELECT COUNT(*) AS c FROM \`${table}\``);
    return parseInt(rows[0].c, 10);
  }

  syncColumnsOf(table) {
    // 同步版本（供 orderCol 使用）
    return null;
  }

  async columnsOf(table) {
    const [rows] = await this.db.query(`DESCRIBE \`${table}\``);
    const map = {};
    for (const r of rows) {
      map[r.Field] = {
        type: (r.Type || '').toLowerCase(),
        nullable: (r.Null || 'NO') === 'YES',
      };
    }
    return map;
  }

  async updateRow(table, id, fields) {
    const cols = await this.columnsOf(table);
    const set = [];
    const params = [];
    const applied = {};
    for (const [k, v] of Object.entries(fields || {})) {
      if (typeof k !== 'string' || !Object.prototype.hasOwnProperty.call(cols, k)) continue;
      if (READ_ONLY_COLUMNS.includes(k)) continue;
      const cast = this.cast(cols[k], v);
      set.push(`\`${k}\` = ?`);
      params.push(cast);
      applied[k] = cast;
    }
    if (Object.prototype.hasOwnProperty.call(cols, 'last_modified')) {
      set.push('`last_modified` = ?');
      params.push(Date.now());
    }
    if (Object.prototype.hasOwnProperty.call(cols, 'needs_sync')) {
      set.push('`needs_sync` = ?');
      params.push(1);
    }
    if (set.length === 0) {
      throw new ApiException('没有可更新的有效字段', 400, 400);
    }
    params.push(id);
    const [r] = await this.db.query(
      `UPDATE \`${table}\` SET ${set.join(', ')} WHERE \`id\` = ?`,
      params
    );
    return { count: r.affectedRows, applied };
  }

  async deleteRow(table, id) {
    const cols = await this.columnsOf(table);
    let mode;
    let sql;
    let params;
    if (Object.prototype.hasOwnProperty.call(cols, 'is_deleted')) {
      const set = ['`is_deleted` = ?'];
      params = [1];
      if (Object.prototype.hasOwnProperty.call(cols, 'last_modified')) {
        set.push('`last_modified` = ?');
        params.push(Date.now());
      }
      if (Object.prototype.hasOwnProperty.call(cols, 'needs_sync')) {
        set.push('`needs_sync` = ?');
        params.push(1);
      }
      params.push(id);
      sql = `UPDATE \`${table}\` SET ${set.join(', ')} WHERE \`id\` = ?`;
      mode = 'soft';
    } else {
      sql = 'DELETE FROM `' + table + '` WHERE `id` = ?';
      params = [id];
      mode = 'hard';
    }
    const [r] = await this.db.query(sql, params);
    return { count: r.affectedRows, mode };
  }

  cast(col, v) {
    if (v === null) return null;
    const t = col.type;
    if (t.includes('int') || t.includes('bigint') || t.includes('smallint') || t.includes('mediumint')) {
      return parseInt(v, 10);
    }
    if (t.includes('float') || t.includes('double') || t.includes('decimal') || t.includes('numeric')) {
      return parseFloat(v);
    }
    if (v === '' && col.nullable) return null;
    return String(v);
  }

  async ensureAuditTable() {
    await this.db.query(
      `CREATE TABLE IF NOT EXISTS \`${AUDIT_TABLE}\` (
        \`id\` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
        \`actor\` VARCHAR(64) NOT NULL DEFAULT 'admin',
        \`action\` VARCHAR(16) NOT NULL,
        \`table_name\` VARCHAR(64) NOT NULL,
        \`row_id\` CHAR(36) NOT NULL,
        \`change_mode\` VARCHAR(8) NULL,
        \`changes\` TEXT NULL,
        \`ip\` VARCHAR(45) NULL,
        \`user_agent\` TEXT NULL,
        \`created_at\` BIGINT NOT NULL,
        PRIMARY KEY (\`id\`),
        KEY \`idx_table_row\` (\`table_name\`, \`row_id\`),
        KEY \`idx_created\` (\`created_at\`)
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`
    );
  }

  async audit(action, table, rowId, changes = null, mode = null, req = null) {
    await this.ensureAuditTable();
    let ip = '';
    let ua = '';
    if (req) {
      const fwd = req.headers['x-forwarded-for'];
      ip = fwd ? String(fwd).split(',')[0].trim() : (req.ip || '');
      ua = req.headers['user-agent'] || '';
    }
    await this.db.query(
      `INSERT INTO \`${AUDIT_TABLE}\`
       (actor, action, table_name, row_id, change_mode, changes, ip, user_agent, created_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        'admin',
        action,
        table,
        rowId,
        mode,
        changes === null ? null : JSON.stringify(changes),
        ip,
        ua,
        Date.now(),
      ]
    );
  }

  async recentAudit(limit) {
    await this.ensureAuditTable();
    const [rows] = await this.db.query(
      `SELECT * FROM \`${AUDIT_TABLE}\` ORDER BY \`created_at\` DESC LIMIT ?`,
      [limit]
    );
    return rows;
  }
}

module.exports = { AdminRepository };
