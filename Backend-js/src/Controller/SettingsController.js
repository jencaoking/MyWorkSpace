// 设置 Controller（user_settings 单行镜像）：读取 / 保存。对应 PHP App\Controller\SettingsController
const ApiResponse = require('../../lib/ApiResponse');
const { ApiException } = require('../../src/Exception/ApiException');
const { SettingsRepository } = require('../Repository/SettingsRepository');

class SettingsController {
  constructor(db, req, res) {
    this.db = db;
    this.req = req;
    this.res = res;
    this.repo = new SettingsRepository(db);
  }

  async get() {
    let row = await this.repo.get();
    if (!row) {
      row = {
        id: 'local',
        theme: 'system',
        module_toggles: null,
        language: 'zh',
        created_at: null,
        updated_at: null,
      };
    }
    ApiResponse.json(this.res, { code: 0, msg: 'ok', data: { settings: row } });
  }

  async save() {
    const raw = this.req.body || {};
    const theme = raw.theme !== undefined ? String(raw.theme) : 'system';
    let moduleToggles = raw.module_toggles ?? null;
    if (!Array.isArray(moduleToggles) && typeof moduleToggles !== 'object' && moduleToggles !== null) {
      throw new ApiException('module_toggles must be an object/array', 400, 400);
    }
    const language = raw.language !== undefined ? String(raw.language) : null;
    const row = await this.repo.upsert(theme, moduleToggles, language);
    ApiResponse.json(this.res, { code: 0, msg: 'ok', data: { settings: row } });
  }
}

module.exports = { SettingsController };
