// 记账记录仓库（account_records 表），对应 PHP App\Repository\AccountRecordRepository
const { AccountRecord } = require('../Model/AccountRecord');

class AccountRecordRepository {
  constructor(db) {
    this.db = db;
  }

  hydrate(r) {
    return new AccountRecord({
      id: r.id,
      type: r.type,
      category: r.category,
      amount: parseFloat(r.amount),
      currency: r.currency,
      recordDate: parseInt(r.record_date, 10),
      note: r.note,
      lastModified: parseInt(r.last_modified, 10),
      isDeleted: parseInt(r.is_deleted, 10),
      deviceId: r.device_id,
      needsSync: parseInt(r.needs_sync, 10),
    });
  }

  async list() {
    const [rows] = await this.db.query(
      'SELECT * FROM account_records WHERE is_deleted=0 ORDER BY record_date DESC'
    );
    return rows.map((r) => this.hydrate(r));
  }

  async byId(id) {
    const [rows] = await this.db.query('SELECT * FROM account_records WHERE id=?', [id]);
    return rows.length ? this.hydrate(rows[0]) : null;
  }

  async upsert(m) {
    const now = Date.now();
    const sql = `INSERT INTO account_records
      (id,type,category,amount,currency,record_date,note,last_modified,is_deleted,device_id,needs_sync)
      VALUES (?,?,?,?,?,?,?,?,?,?,0)
      ON DUPLICATE KEY UPDATE
        type=VALUES(type),category=VALUES(category),amount=VALUES(amount),currency=VALUES(currency),
        record_date=VALUES(record_date),note=VALUES(note),
        last_modified=VALUES(last_modified),is_deleted=VALUES(is_deleted),device_id=VALUES(device_id),needs_sync=0`;
    await this.db.query(sql, [
      m.id,
      m.type,
      m.category,
      m.amount,
      m.currency,
      m.recordDate,
      m.note,
      m.lastModified ?? now,
      m.isDeleted ?? 0,
      m.deviceId,
    ]);
  }

  async softDelete(id) {
    await this.db.query(
      'UPDATE account_records SET is_deleted=1, last_modified=?, needs_sync=0 WHERE id=?',
      [Date.now(), id]
    );
  }

  async findDirty() {
    const [rows] = await this.db.query(
      'SELECT * FROM account_records WHERE needs_sync=1 AND is_deleted=0'
    );
    return rows.map((r) => this.hydrate(r));
  }

  async findDeletedIds() {
    const [rows] = await this.db.query(
      'SELECT id FROM account_records WHERE is_deleted=1 AND needs_sync=1'
    );
    return rows.map((r) => r.id);
  }

  async clearDirty(ids) {
    if (ids && ids.length) {
      const ph = ids.map(() => '?').join(',');
      await this.db.query(`UPDATE account_records SET needs_sync=0 WHERE id IN (${ph})`, ids);
    }
  }

  async clearDeleted(ids) {
    if (ids && ids.length) {
      const ph = ids.map(() => '?').join(',');
      await this.db.query(`DELETE FROM account_records WHERE id IN (${ph})`, ids);
    }
  }
}

module.exports = { AccountRecordRepository };
