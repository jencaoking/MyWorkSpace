// 运动记录模型（sport_records 表），对应 PHP App\Model\SportRecord
class SportRecord {
  constructor({
    id,
    type,
    durationMin,
    distanceKm,
    calories,
    recordDate,
    note,
    lastModified,
    isDeleted,
    deviceId,
    needsSync,
  }) {
    this.id = id;
    this.type = type;
    this.durationMin = durationMin;
    this.distanceKm = distanceKm;
    this.calories = calories;
    this.recordDate = recordDate;
    this.note = note;
    this.lastModified = lastModified;
    this.isDeleted = isDeleted;
    this.deviceId = deviceId;
    this.needsSync = needsSync;
  }

  toApiArray() {
    return {
      id: this.id,
      type: this.type,
      duration_min: this.durationMin,
      distance_km: this.distanceKm,
      calories: this.calories,
      record_date: this.recordDate,
      note: this.note,
      last_modified: this.lastModified,
      is_deleted: this.isDeleted,
      device_id: this.deviceId,
      needs_sync: this.needsSync,
    };
  }

  static fromUploadArray(a) {
    const now = Date.now();
    return new SportRecord({
      id: a.id ?? null,
      type: a.type ?? '',
      durationMin: a.duration_min !== undefined ? parseInt(a.duration_min, 10) : 0,
      distanceKm:
        a.distance_km !== undefined && a.distance_km !== '' ? parseFloat(a.distance_km) : null,
      calories: a.calories !== undefined && a.calories !== '' ? parseInt(a.calories, 10) : null,
      recordDate: a.record_date !== undefined ? parseInt(a.record_date, 10) : now,
      note: a.note ?? '',
      lastModified: a.last_modified !== undefined ? parseInt(a.last_modified, 10) : now,
      isDeleted: a.is_deleted !== undefined ? parseInt(a.is_deleted, 10) : 0,
      deviceId: a.device_id ?? null,
      needsSync: a.needs_sync !== undefined ? parseInt(a.needs_sync, 10) : 0,
    });
  }
}

module.exports = { SportRecord };
