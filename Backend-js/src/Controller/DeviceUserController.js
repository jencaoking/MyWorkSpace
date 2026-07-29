// 后台用户管理 Controller：以设备 ID 聚合的「用户」视图，支持封禁/备注/清除数据。
// 对应 PHP App\Controller\DeviceUserController
const Response = require('../../lib/Response');
const { ApiException } = require('../../src/Exception/ApiException');
const { DeviceUserRepository } = require('../Repository/DeviceUserRepository');
const { AdminRepository } = require('../Repository/AdminRepository');
const DeviceUserViewModel = require('../ViewModel/DeviceUserViewModel');

class DeviceUserController {
  constructor(db, req, res) {
    this.db = db;
    this.req = req;
    this.res = res;
    this.repo = new DeviceUserRepository(db);
    this.adminRepo = new AdminRepository(db);
  }

  requireAuth() {
    if (!this.req.session || !this.req.session.admin_logged_in) {
      throw new ApiException('未授权，请先登录', 401, 401);
    }
  }

  async list() {
    this.requireAuth();
    const limit = Math.min(200, Math.max(1, parseInt(this.req.query.limit ?? 50, 10)));
    const offset = Math.max(0, parseInt(this.req.query.offset ?? 0, 10));
    let q = this.req.query.q != null ? String(this.req.query.q).trim() : null;
    if (q === '') q = null;
    const rows = await this.repo.listDevices(limit, offset, q);
    Response.json(this.res, {
      code: 0,
      message: 'ok',
      data: {
        rows: DeviceUserViewModel.listToArray(rows),
        total: await this.repo.countDevices(q),
        limit,
        offset,
      },
    });
  }

  async set() {
    this.requireAuth();
    const body = this.req.body || {};
    const deviceId = String(body.device_id ?? '');
    if (deviceId === '') throw new ApiException('缺少 device_id', 400, 400);
    const status = body.status !== undefined ? String(body.status) : null;
    const note = Object.prototype.hasOwnProperty.call(body, 'note') ? String(body.note) : null;
    if (status === null && note === null) {
      throw new ApiException('至少需要提供 status 或 note 之一', 400, 400);
    }
    const m = await this.repo.setStatus(deviceId, status, note);
    await this.adminRepo.audit('update', 'device_users', deviceId, { status, note }, null, this.req);
    Response.json(this.res, { code: 0, message: 'ok', data: DeviceUserViewModel.toArray(m) });
  }

  async delete() {
    this.requireAuth();
    const body = this.req.body || {};
    const deviceId = String(body.device_id ?? '');
    if (deviceId === '') throw new ApiException('缺少 device_id', 400, 400);
    const counts = await this.repo.deleteDeviceData(deviceId);
    await this.adminRepo.audit('delete', 'device_users', deviceId, { data: counts }, 'soft', this.req);
    Response.json(this.res, { code: 0, message: 'ok', data: { affected: counts } });
  }
}

module.exports = { DeviceUserController };
