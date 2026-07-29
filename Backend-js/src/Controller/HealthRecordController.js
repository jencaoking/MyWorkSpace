// 健康记录接口：list / batchUpsert / delete / pull，对应 PHP App\Controller\HealthRecordController
const ApiResponse = require('../../lib/ApiResponse');
const { HealthRecordRepository } = require('../Repository/HealthRecordRepository');
const HealthRecordViewModel = require('../ViewModel/HealthRecordViewModel');

class HealthRecordController {
  constructor(db, req, res) {
    this.db = db;
    this.req = req;
    this.res = res;
    this.repo = new HealthRecordRepository(db);
  }

  ok(data, http = 200) {
    ApiResponse.json(this.res, { code: 0, msg: 'ok', data }, http);
  }

  bad(msg) {
    ApiResponse.json(this.res, { code: 400, msg, data: {} }, 400);
  }

  async list() {
    const data = HealthRecordViewModel.listToArray(await this.repo.list());
    this.ok({ health: data });
  }

  async batchUpsert() {
    const body = this.req.body || {};
    const rows = body.health ?? body.records ?? [];
    if (!Array.isArray(rows)) return this.bad('invalid body');
    let count = 0;
    for (const item of rows) {
      if (!item || !item.id) continue;
      await this.repo.upsert(HealthRecordViewModel.fromArray(item));
      count++;
    }
    this.ok({ uploaded: count });
  }

  async delete() {
    const body = this.req.body || {};
    const ids = body.ids;
    if (!Array.isArray(ids)) return this.bad('invalid body');
    for (const id of ids) await this.repo.softDelete(String(id));
    this.ok({ deleted: ids.length });
  }

  async pull() {
    const dirty = HealthRecordViewModel.listToArray(await this.repo.findDirty());
    const deleted = await this.repo.findDeletedIds();
    this.ok({
      server_time: Date.now(),
      health: dirty,
      deleted_ids: deleted,
    });
  }
}

module.exports = { HealthRecordController };
