// 记账记录模型（account_records 表），对应 PHP App\Model\AccountRecord
class AccountRecord {
  constructor({
    id,
    type,
    category,
    amount,
    currency,
    recordDate,
    note,
    lastModified,
    isDeleted,
    deviceId,
    needsSync,
  }) {
    this.id = id;
    this.type = type;
    this.category = category;
    this.amount = amount;
    this.currency = currency;
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
      category: this.category,
      amount: this.amount,
      currency: this.currency,
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
    return new AccountRecord({
      id: a.id ?? null,
      type: a.type ?? 'expense',
      category: a.category ?? '',
      amount: a.amount !== undefined && a.amount !== '' ? parseFloat(a.amount) : 0.0,
      currency: a.currency ?? 'CNY',
      recordDate: a.record_date !== undefined ? parseInt(a.record_date, 10) : now,
      note: a.note ?? '',
      lastModified: a.last_modified !== undefined ? parseInt(a.last_modified, 10) : now,
      isDeleted: a.is_deleted !== undefined ? parseInt(a.is_deleted, 10) : 0,
      deviceId: a.device_id ?? null,
      needsSync: a.needs_sync !== undefined ? parseInt(a.needs_sync, 10) : 0,
    });
  }
}

module.exports = { AccountRecord };
