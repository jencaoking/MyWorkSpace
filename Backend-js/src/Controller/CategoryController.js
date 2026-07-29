// 分类 Controller：列表 / 批量 upsert / 删除 / 增量拉取，对应 PHP App\Controller\CategoryController
const Response = require('../../lib/Response');
const { ApiException } = require('../../src/Exception/ApiException');
const { CategoryRepository } = require('../Repository/CategoryRepository');
const CategoryViewModel = require('../ViewModel/CategoryViewModel');

class CategoryController {
  constructor(db, req, res) {
    this.db = db;
    this.req = req;
    this.res = res;
    this.repo = new CategoryRepository(db);
  }

  deviceId() {
    const id = (this.req.headers['x-device-id'] || '').toString().trim();
    if (!id) throw new ApiException('X-Device-ID header required', 400, 400);
    return id;
  }

  ok(data, http = 200) {
    Response.json(this.res, { code: 0, msg: 'ok', data }, http);
  }

  bad(msg) {
    Response.json(this.res, { code: 400, msg, data: {} }, 400);
  }

  async list() {
    const data = (await this.repo.list()).map(CategoryViewModel.toApiArray);
    this.ok({ categories: data });
  }

  async batchUpsert() {
    const deviceId = this.deviceId();
    const body = this.req.body || {};
    const rows = body.categories ?? [];
    if (!Array.isArray(rows)) return this.bad('invalid body');
    const cats = [];
    for (const raw of rows) {
      cats.push(CategoryViewModel.fromUploadArray(raw, deviceId));
    }
    const count = await this.repo.upsertBatch(cats);
    this.ok({ uploaded: count });
  }

  async delete() {
    const body = this.req.body || {};
    const ids = body.ids;
    if (!Array.isArray(ids)) return this.bad('invalid body');
    const deleted = await this.repo.deleteBatch(ids);
    this.ok({ deleted });
  }

  async pull() {
    const dirty = (await this.repo.findDirty()).map(CategoryViewModel.toApiArray);
    const deleted = await this.repo.findDeletedIds();
    this.ok({
      server_time: Date.now(),
      dirty,
      deleted_ids: deleted,
    });
  }
}

module.exports = { CategoryController };
