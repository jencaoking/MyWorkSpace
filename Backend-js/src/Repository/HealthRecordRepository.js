// 健康记录仓库（health_records 表），对应 PHP App\Repository\HealthRecordRepository
const { HealthRecord } = require('../Model/HealthRecord');

class HealthRecordRepository {
  constructor(db) {
    this.db = db;
  }

  static hydrate(r) {
    return new HealthRecord({
      id: r.id,
      type: r.type,
      value: r.value === null ? null : parseFloat(r.value),
      unit: r.unit,
      recordTime: parseInt(r.record_time, 10),
      note: r.note,
      lastModified: parseInt(r.last_modified, 10),
      isDeleted: parseInt(r.is_deleted, 10),
      deviceId: r.device_id,
      needsSync: parseInt(r.needs_sync, 10),
    });
  }

  async list() {
    const [rows] = await this.db.query(
      'SELECT * FROM health_records WHERE is_deleted=0 ORDER BY record_time DESC'
    );
    return rows.map(HealthRecordRepository.hydrate);
  }

  async byId(id) {
    const [rows] = await this.db.query('SELECT * FROM health_records WHERE id=?', [id]);
    return rows.length ? HealthRecordRepository.hydrate(rows[0]) : null;
  }

  async upsert(m) {
    const now = Date.now();
    const sql = `INSERT INTO health_records
      (id,type,value,unit,record_time,note,last_modified,is_deleted,device_id,needs_sync)
      VALUES (?,?,?,?,?,?,?,?,?,0)
      ON DUPLICATE KEY UPDATE
        type=VALUES(type),value=VALUES(value),unit=VALUES(unit),record_time=VALUES(record_time),
        note=VALUES(note),last_modified=VALUES(last_modified),is_deleted=VALUES(is_deleted),
        device_id=VALUES(device_id),needs_sync=0`;
    await this.db.query(sql, [
      m.id,
      m.type,
      m.value,
      m.unit,
      m.recordTime,
      m.note,
      m.lastModified ?? now,
      m.isDeleted ?? 0,
      m.deviceId,
    ]);
  }

  async softDelete(id) {
    await this.db.query(
      'UPDATE health_records SET is_deleted=1, last_modified=?, needs_sync=0 WHERE id=?',
      [Date.now(), id]
    );
  }

  async findDirty() {
    const [rows] = await this.db.query(
      'SELECT * FROM health_records WHERE needs_sync=1 AND is_deleted=0'
    );
    return rows.map(HealthRecordRepository.hydrate);
  }

  async findDeletedIds() {
    const [rows] = await this.db.query(
      'SELECT id FROM health_records WHERE is_deleted=1 AND needs_sync=1'
    );
    return rows.map((r) => r.id);
  }

  async clearDirty(ids) {
    if (ids && ids.length) {
      const ph = ids.map(() => '?').join(',');
      await this.db.query(`UPDATE health_records SET needs_sync=0 WHERE id IN (${ph})`, ids);
    }
  }

  async clearDeleted(ids) {
    if (ids && ids.length) {
      const ph = ids.map(() => '?').join(',');
      await this.db.query(`DELETE FROM health_records WHERE id IN (${ph})`, ids);
    }
  }
}

module.exports = { HealthRecordRepository };
