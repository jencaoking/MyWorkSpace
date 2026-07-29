// 设备用户（后台用户管理视角）：以客户端生成的 device_id 作为唯一用户标识。
// 对应 PHP App\Model\DeviceUser。totalRecords/firstSeen/lastSeen 为运行期聚合字段。
class DeviceUser {
  constructor() {
    this.deviceId = null;
    this.status = 'active';
    this.note = '';
    this.createdAt = 0;
    this.updatedAt = 0;
    this.totalRecords = 0;
    this.firstSeen = 0;
    this.lastSeen = 0;
  }

  toApiArray() {
    return {
      device_id: this.deviceId,
      status: this.status,
      note: this.note,
      total_records: this.totalRecords,
      first_seen: this.firstSeen,
      last_seen: this.lastSeen,
    };
  }

  static fromArray(a) {
    const m = new DeviceUser();
    m.deviceId = a.device_id ?? null;
    m.status = a.status ?? 'active';
    m.note = a.note ?? '';
    return m;
  }
}

module.exports = { DeviceUser };
