// 健康记录模型（health_records 表），对应 PHP App\Model\HealthRecord。value 为数值（体征），文本描述放 note。
class HealthRecord {
  constructor({
    id,
    type,
    value,
    unit,
    recordTime,
    note,
    lastModified,
    isDeleted,
    deviceId,
    needsSync,
  }) {
    this.id = id;
    this.type = type;
    this.value = value;
    this.unit = unit;
    this.recordTime = recordTime;
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
      value: this.value,
      unit: this.unit,
      record_time: this.recordTime,
      note: this.note,
      last_modified: this.lastModified,
      is_deleted: this.isDeleted,
      device_id: this.deviceId,
      needs_sync: this.needsSync,
    };
  }

  static fromUploadArray(a) {
    const now = Date.now();
    return new HealthRecord({
      id: a.id ?? null,
      type: a.type ?? 'visit',
      value: a.value !== undefined && a.value !== '' ? parseFloat(a.value) : null,
      unit: a.unit ?? '',
      recordTime: a.record_time !== undefined ? parseInt(a.record_time, 10) : now,
      note: a.note ?? '',
      lastModified: a.last_modified !== undefined ? parseInt(a.last_modified, 10) : now,
      isDeleted: a.is_deleted !== undefined ? parseInt(a.is_deleted, 10) : 0,
      deviceId: a.device_id ?? null,
      needsSync: a.needs_sync !== undefined ? parseInt(a.needs_sync, 10) : 0,
    });
  }
}

module.exports = { HealthRecord };
