// 每日未完成作业接口：列表 / 批量 upsert / 删除 / 拉取 / 处置 / 周报 / 归档。
// 对应 PHP App\Controller\DailyPendingController（本模块不按设备隔离）。
const ApiResponse = require('../../lib/ApiResponse');
const { DailyPendingLogRepository } = require('../Repository/DailyPendingLogRepository');
const DailyPendingLogViewModel = require('../ViewModel/DailyPendingLogViewModel');

class DailyPendingController {
  constructor(db, req, res) {
    this.db = db;
    this.req = req;
    this.res = res;
    this.repo = new DailyPendingLogRepository(db);
  }

  ok(data, http = 200) {
    ApiResponse.json(this.res, { code: 0, msg: 'ok', data }, http);
  }

  bad(msg, http = 400) {
    ApiResponse.json(this.res, { code: http, msg, data: {} }, http);
  }

  async list() {
    const date = this.req.query.date ?? null;
    const disposition = this.req.query.disposition ?? null;
    const data = DailyPendingLogViewModel.listToArray(await this.repo.list(date, disposition));
    this.ok({ logs: data });
  }

  async batchUpsert() {
    const body = this.req.body || {};
    const rows = body.logs ?? [];
    if (!Array.isArray(rows)) return this.bad('invalid body');
    let count = 0;
    for (const item of rows) {
      if (!item || !item.id) continue;
      await this.repo.upsert(DailyPendingLogViewModel.fromArray(item));
      count++;
    }
    this.ok({ uploaded: count });
  }

  async delete() {
    const body = this.req.body || {};
    const ids = body.ids ?? [];
    if (!Array.isArray(ids)) return this.bad('invalid body');
    for (const id of ids) await this.repo.softDelete(String(id));
    this.ok({ deleted: ids.length });
  }

  async pull() {
    const dirty = DailyPendingLogViewModel.listToArray(await this.repo.findDirty());
    const deleted = await this.repo.findDeletedIds();
    this.ok({
      server_time: Date.now(),
      logs: dirty,
      deleted_ids: deleted,
    });
  }

  async dispose() {
    const body = this.req.body || {};
    const id = String(body.id ?? '');
    const disposition = String(body.disposition ?? '');
    const newDueDate =
      body.new_due_date !== undefined && body.new_due_date !== null && body.new_due_date !== ''
        ? parseInt(body.new_due_date, 10)
        : null;
    if (id === '' || !['completed', 'rescheduled', 'abandoned'].includes(disposition)) {
      return this.bad('invalid id or disposition');
    }
    if (disposition === 'rescheduled' && newDueDate === null) {
      return this.bad('new_due_date required for reschedule');
    }
    const log = await this.repo.dispose(id, disposition, newDueDate);
    if (log === null) return this.bad('log not found', 404);
    this.ok({ log: DailyPendingLogViewModel.toArray(log) });
  }

  async weekly() {
    this.ok(await this.repo.weeklyStats());
  }

  async archive() {
    const inserted = await this.repo.archiveOverdue();
    this.ok({ archived: inserted });
  }
}

module.exports = { DailyPendingController };
