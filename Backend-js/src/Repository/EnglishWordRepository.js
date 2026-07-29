// 英语单词仓库（english_words 表），对应 PHP App\Repository\EnglishWordRepository
const { EnglishWord } = require('../Model/EnglishWord');

class EnglishWordRepository {
  constructor(db) {
    this.db = db;
  }

  hydrate(r) {
    return new EnglishWord({
      id: r.id,
      word: r.word,
      phonetic: r.phonetic,
      meaning: r.meaning,
      example: r.example,
      familiarity: parseInt(r.familiarity, 10),
      nextReview: parseInt(r.next_review, 10),
      lastModified: parseInt(r.last_modified, 10),
      isDeleted: parseInt(r.is_deleted, 10),
      deviceId: r.device_id,
      needsSync: parseInt(r.needs_sync, 10),
    });
  }

  async list() {
    const [rows] = await this.db.query(
      'SELECT * FROM english_words WHERE is_deleted=0 ORDER BY next_review ASC'
    );
    return rows.map((r) => this.hydrate(r));
  }

  async byId(id) {
    const [rows] = await this.db.query('SELECT * FROM english_words WHERE id=?', [id]);
    return rows.length ? this.hydrate(rows[0]) : null;
  }

  async upsert(m) {
    const now = Date.now();
    const sql = `INSERT INTO english_words
      (id,word,phonetic,meaning,example,familiarity,next_review,last_modified,is_deleted,device_id,needs_sync)
      VALUES (?,?,?,?,?,?,?,?,?,?,0)
      ON DUPLICATE KEY UPDATE
        word=VALUES(word),phonetic=VALUES(phonetic),meaning=VALUES(meaning),example=VALUES(example),
        familiarity=VALUES(familiarity),next_review=VALUES(next_review),last_modified=VALUES(last_modified),
        is_deleted=VALUES(is_deleted),device_id=VALUES(device_id),needs_sync=0`;
    await this.db.query(sql, [
      m.id,
      m.word,
      m.phonetic,
      m.meaning,
      m.example,
      m.familiarity,
      m.nextReview,
      m.lastModified ?? now,
      m.isDeleted ?? 0,
      m.deviceId,
    ]);
  }

  async softDelete(id) {
    await this.db.query(
      'UPDATE english_words SET is_deleted=1, last_modified=?, needs_sync=0 WHERE id=?',
      [Date.now(), id]
    );
  }

  async findDirty() {
    const [rows] = await this.db.query(
      'SELECT * FROM english_words WHERE needs_sync=1 AND is_deleted=0'
    );
    return rows.map((r) => this.hydrate(r));
  }

  async findDeletedIds() {
    const [rows] = await this.db.query(
      'SELECT id FROM english_words WHERE is_deleted=1 AND needs_sync=1'
    );
    return rows.map((r) => r.id);
  }

  async clearDirty(ids) {
    if (ids && ids.length) {
      const ph = ids.map(() => '?').join(',');
      await this.db.query(`UPDATE english_words SET needs_sync=0 WHERE id IN (${ph})`, ids);
    }
  }

  async clearDeleted(ids) {
    if (ids && ids.length) {
      const ph = ids.map(() => '?').join(',');
      await this.db.query(`DELETE FROM english_words WHERE id IN (${ph})`, ids);
    }
  }
}

module.exports = { EnglishWordRepository };
