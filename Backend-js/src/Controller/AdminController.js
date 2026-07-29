// 后台管理 Controller：登录/登出 + 只读概览与通用数据浏览/编辑/删除 + 审计 + API 密钥管理。
// 对应 PHP App\Controller\AdminController
const Response = require('../../lib/Response');
const { ApiException } = require('../../src/Exception/ApiException');
const { AdminRepository } = require('../Repository/AdminRepository');
const { ConfigRepository } = require('../Repository/ConfigRepository');
const adminConfig = require('../../config/admin');

class AdminController {
  constructor(db, req, res) {
    this.db = db;
    this.req = req;
    this.res = res;
    this.adminRepo = new AdminRepository(db);
  }

  requireAuth() {
    if (!this.req.session || !this.req.session.admin_logged_in) {
      throw new ApiException('未授权，请先登录', 401, 401);
    }
  }

  async login() {
    const body = this.jsonBody();
    const password = typeof body.password === 'string' ? body.password : '';
    const ok = await adminConfig.verify(password);
    if (!ok) {
      throw new ApiException('用户名或密码错误', 401, 401);
    }
    this.req.session.admin_logged_in = true;
    Response.json(this.res, { code: 0, message: 'ok', data: { logged_in: true } });
  }

  logout() {
    this.req.session.destroy(() => {
      Response.json(this.res, { code: 0, message: 'ok', data: { logged_in: false } });
    });
  }

  jsonBody() {
    const body = this.req.body;
    return body && typeof body === 'object' ? body : {};
  }

  async overview() {
    this.requireAuth();
    let dbOk = true;
    let dbError = '';
    try {
      await this.db.query('SELECT 1');
    } catch (e) {
      dbOk = false;
      dbError = e.message;
    }
    Response.json(this.res, {
      code: 0,
      message: 'ok',
      data: {
        server_time: Date.now(),
        php_version: process.version,
        db_connected: dbOk,
        db_error: dbError,
        device_count: await this.adminRepo.deviceCount(),
        tables: await this.adminRepo.tableCounts(),
      },
    });
  }

  async browse() {
    this.requireAuth();
    const table = String(this.req.query.table ?? '');
    if (!AdminRepository.isAllowed(table)) {
      throw new ApiException('unknown or disallowed table', 404, 404);
    }
    const limit = Math.min(200, Math.max(1, parseInt(this.req.query.limit ?? 50, 10)));
    const offset = Math.max(0, parseInt(this.req.query.offset ?? 0, 10));
    const rows = await this.adminRepo.browse(table, limit, offset);
    const types = {};
    const cols = await this.adminRepo.columnsOf(table);
    for (const col of Object.keys(cols)) types[col] = cols[col].type;
    Response.json(this.res, {
      code: 0,
      message: 'ok',
      data: {
        table,
        columns: rows.length ? Object.keys(rows[0]) : [],
        types,
        rows,
        total: await this.adminRepo.countRows(table),
        limit,
        offset,
        deletable: AdminRepository.canDelete(table),
      },
    });
  }

  async update() {
    this.requireAuth();
    const body = this.jsonBody();
    const table = String(body.table ?? '');
    const id = String(body.id ?? '');
    const fields = body.fields ?? {};
    if (!AdminRepository.isAllowed(table)) {
      throw new ApiException('unknown or disallowed table', 404, 404);
    }
    if (id === '' || typeof fields !== 'object' || Array.isArray(fields) || Object.keys(fields).length === 0) {
      throw new ApiException('缺少 id 或 fields', 400, 400);
    }
    const res = await this.adminRepo.updateRow(table, id, fields);
    if (res.count < 1) {
      throw new ApiException('未找到记录或无字段变更', 404, 404);
    }
    await this.adminRepo.audit('update', table, id, res.applied, null, this.req);
    Response.json(this.res, { code: 0, message: 'ok', data: { affected: res.count } });
  }

  async delete() {
    this.requireAuth();
    const body = this.jsonBody();
    const table = String(body.table ?? '');
    const id = String(body.id ?? '');
    if (!AdminRepository.isAllowed(table)) {
      throw new ApiException('unknown or disallowed table', 404, 404);
    }
    if (!AdminRepository.canDelete(table)) {
      throw new ApiException('该表不允许在后台删除', 403, 403);
    }
    if (id === '') {
      throw new ApiException('缺少 id', 400, 400);
    }
    const res = await this.adminRepo.deleteRow(table, id);
    if (res.count < 1) {
      throw new ApiException('未找到记录', 404, 404);
    }
    await this.adminRepo.audit('delete', table, id, null, res.mode, this.req);
    Response.json(this.res, {
      code: 0,
      message: 'ok',
      data: { affected: res.count, mode: res.mode },
    });
  }

  async audit() {
    this.requireAuth();
    const limit = Math.min(500, Math.max(1, parseInt(this.req.query.limit ?? 100, 10)));
    const rows = await this.adminRepo.recentAudit(limit);
    Response.json(this.res, {
      code: 0,
      message: 'ok',
      data: { rows, total: rows.length },
    });
  }

  async apiKeys() {
    this.requireAuth();
    const repo = new ConfigRepository(this.db);
    const keys = ['youdao_app_key', 'youdao_app_secret', 'tmdb_key', 'qweather_key', 'qweather_token', 'qweather_host'];
    const out = {};
    for (const k of keys) {
      const v = await repo.get(k, '');
      out[k] = v !== '' ? '******' + String(v).slice(-4) : '';
    }
    Response.json(this.res, { code: 0, message: 'ok', data: { keys: out } });
  }

  async saveApiKeys() {
    this.requireAuth();
    const body = this.jsonBody();
    const repo = new ConfigRepository(this.db);
    for (const k of ['youdao_app_key', 'youdao_app_secret', 'tmdb_key', 'qweather_key', 'qweather_token', 'qweather_host']) {
      if (Object.prototype.hasOwnProperty.call(body, k) && typeof body[k] === 'string' && body[k] !== '') {
        await repo.set(k, body[k].trim());
      }
    }
    await this.adminRepo.audit('update', 'app_config', 'apikeys', { youdao: true }, null, this.req);
    Response.json(this.res, { code: 0, message: 'saved', data: null });
  }
}

module.exports = { AdminController };
