// 运动记录仓库（sport_records 表），对应 PHP App\Repository\SportRecordRepository
const { SportRecord } = require('../Model/SportRecord');

class SportRecordRepository {
  constructor(db) {
    this.db = db;
  }

  hydrate(r) {
    return new SportRecord({
      id: r.id,
      type: r.type,
      durationMin: parseInt(r.duration_min, 10),
      distanceKm: r.distance_km === null ? null : parseFloat(r.distance_km),
      calories: r.calories === null ? null : parseInt(r.calories, 10),
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
      'SELECT * FROM sport_records WHERE is_deleted=0 ORDER BY record_date DESC'
    );
    return rows.map((r) => this.hydrate(r));
  }

  async byId(id) {
    const [rows] = await this.db.query('SELECT * FROM sport_records WHERE id=?', [id]);
    return rows.length ? this.hydrate(rows[0]) : null;
  }

  async upsert(m) {
    const now = Date.now();
    const sql = `INSERT INTO sport_records
      (id,type,duration_min,distance_km,calories,record_date,note,last_modified,is_deleted,device_id,needs_sync)
      VALUES (?,?,?,?,?,?,?,?,?,?,0)
      ON DUPLICATE KEY UPDATE
        type=VALUES(type),duration_min=VALUES(duration_min),distance_km=VALUES(distance_km),
        calories=VALUES(calories),record_date=VALUES(record_date),note=VALUES(note),
        last_modified=VALUES(last_modified),is_deleted=VALUES(is_deleted),device_id=VALUES(device_id),needs_sync=0`;
    await this.db.query(sql, [
      m.id,
      m.type,
      m.durationMin,
      m.distanceKm,
      m.calories,
      m.recordDate,
      m.note,
      m.lastModified ?? now,
      m.isDeleted ?? 0,
      m.deviceId,
    ]);
  }

  async softDelete(id) {
    await this.db.query(
      'UPDATE sport_records SET is_deleted=1, last_modified=?, needs_sync=0 WHERE id=?',
      [Date.now(), id]
    );
  }

  async findDirty() {
    const [rows] = await this.db.query(
      'SELECT * FROM sport_records WHERE needs_sync=1 AND is_deleted=0'
    );
    return rows.map((r) => this.hydrate(r));
  }

  async findDeletedIds() {
    const [rows] = await this.db.query(
      'SELECT id FROM sport_records WHERE is_deleted=1 AND needs_sync=1'
    );
    return rows.map((r) => r.id);
  }

  async clearDirty(ids) {
    if (ids && ids.length) {
      const ph = ids.map(() => '?').join(',');
      await this.db.query(`UPDATE sport_records SET needs_sync=0 WHERE id IN (${ph})`, ids);
    }
  }

  async clearDeleted(ids) {
    if (ids && ids.length) {
      const ph = ids.map(() => '?').join(',');
      await this.db.query(`DELETE FROM sport_records WHERE id IN (${ph})`, ids);
    }
  }
}

module.exports = { SportRecordRepository };
