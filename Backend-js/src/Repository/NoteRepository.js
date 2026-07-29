// 笔记数据访问层（notes 表），对应 PHP App\Repository\NoteRepository
const { Note } = require('../Model/Note');

class NoteRepository {
  constructor(db) {
    this.db = db;
  }

  async upsertBatch(notes) {
    if (!notes || notes.length === 0) return 0;
    const cols = [
      'id', 'title', 'content', 'is_pinned', 'is_favorite',
      'created_at', 'updated_at', 'last_modified', 'is_deleted', 'device_id', 'needs_sync',
    ];
    const placeholders = [];
    const params = [];
    for (const n of notes) {
      const row = n.toArray();
      placeholders.push('(' + cols.map(() => '?').join(',') + ')');
      for (const c of cols) params.push(row[c]);
    }
    const update = cols.map((c) => `${c} = VALUES(${c})`).join(', ');
    const sql =
      `INSERT INTO notes (${cols.join(',')}) VALUES ${placeholders.join(',')} ` +
      `ON DUPLICATE KEY UPDATE ${update}`;

    const conn = await this.db.getConnection();
    try {
      await conn.beginTransaction();
      await conn.query(sql, params);
      await conn.commit();
    } catch (e) {
      await conn.rollback();
      throw e;
    } finally {
      conn.release();
    }
    return notes.length;
  }

  async list(deviceId, favoriteOnly = false) {
    let sql = 'SELECT * FROM notes WHERE device_id = ? AND is_deleted = 0';
    const params = [deviceId];
    if (favoriteOnly) sql += ' AND is_favorite = 1';
    sql += ' ORDER BY is_pinned DESC, updated_at DESC';
    const [rows] = await this.db.query(sql, params);
    return rows.map(Note.fromArray);
  }

  async search(deviceId, keyword, limit = 100) {
    keyword = (keyword || '').trim();
    if (keyword === '') return [];
    try {
      const [rows] = await this.db.query(
        'SELECT * FROM notes WHERE device_id = ? AND is_deleted = 0 ' +
          'AND MATCH(title, content) AGAINST (? IN NATURAL LANGUAGE MODE) ' +
          'ORDER BY is_pinned DESC, updated_at DESC LIMIT ?',
        [deviceId, keyword, limit]
      );
      if (rows.length) return rows.map(Note.fromArray);
    } catch (_) {
      // FULLTEXT 索引缺失时回退 LIKE
    }
    const like = '%' + keyword + '%';
    const [rows] = await this.db.query(
      'SELECT * FROM notes WHERE device_id = ? AND is_deleted = 0 ' +
        'AND (title LIKE ? OR content LIKE ?) ' +
        'ORDER BY is_pinned DESC, updated_at DESC LIMIT ?',
      [deviceId, like, like, limit]
    );
    return rows.map(Note.fromArray);
  }

  async pullSince(since, deviceId, limit = 200) {
    const [rows] = await this.db.query(
      'SELECT * FROM notes WHERE device_id = ? AND last_modified > ? ORDER BY last_modified ASC LIMIT ?',
      [deviceId, since, limit]
    );
    return rows.map(Note.fromArray);
  }

  async deleteBatch(ids, deviceId) {
    if (!ids || ids.length === 0) return 0;
    const ph = ids.map(() => '?').join(',');
    const now = Date.now();
    const [r] = await this.db.query(
      `UPDATE notes SET is_deleted = 1, last_modified = ?, needs_sync = 1 WHERE device_id = ? AND id IN (${ph})`,
      [now, deviceId, ...ids]
    );
    return r.affectedRows;
  }
}

module.exports = { NoteRepository };
