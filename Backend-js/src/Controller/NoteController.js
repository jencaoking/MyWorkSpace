// 笔记 Controller：列表 / 批量 upsert / 删除 / 全文搜索 / 增量拉取，对应 PHP App\Controller\NoteController
const Response = require('../../lib/Response');
const { ApiException } = require('../../src/Exception/ApiException');
const { NoteRepository } = require('../Repository/NoteRepository');
const NoteViewModel = require('../ViewModel/NoteViewModel');

class NoteController {
  constructor(db, req, res) {
    this.db = db;
    this.req = req;
    this.res = res;
    this.repo = new NoteRepository(db);
  }

  deviceId() {
    const id = (this.req.headers['x-device-id'] || '').toString().trim();
    if (!id) throw new ApiException('X-Device-ID header required', 400, 400);
    return id;
  }

  ok(data, http = 200) {
    Response.json(this.res, { code: 0, message: 'ok', data }, http);
  }

  async list() {
    const deviceId = this.deviceId();
    const favoriteOnly = (this.req.query.favorite ?? '') === '1';
    const notes = await this.repo.list(deviceId, favoriteOnly);
    this.ok({
      notes: notes.map((n) => NoteViewModel.toApiArray(n)),
    });
  }

  async batchUpsert() {
    const deviceId = this.deviceId();
    const rawNotes = (this.req.body && this.req.body.notes) || [];
    if (!Array.isArray(rawNotes)) {
      throw new ApiException('notes must be an array', 400, 400);
    }
    const notes = rawNotes.map((raw) => NoteViewModel.fromUploadArray(raw, deviceId));
    const accepted = await this.repo.upsertBatch(notes);
    this.ok({
      accepted,
      synced_at: Date.now(),
    });
  }

  async delete() {
    const deviceId = this.deviceId();
    const ids = (this.req.body && this.req.body.ids) || [];
    if (!Array.isArray(ids)) {
      throw new ApiException('ids must be an array', 400, 400);
    }
    const deleted = await this.repo.deleteBatch(ids, deviceId);
    this.ok({ deleted });
  }

  async search() {
    const deviceId = this.deviceId();
    const keyword = String(this.req.query.q ?? '');
    const notes = await this.repo.search(deviceId, keyword);
    this.ok({
      keyword,
      notes: notes.map((n) => NoteViewModel.toApiArray(n)),
    });
  }

  async pull() {
    const deviceId = this.deviceId();
    const since = parseInt(this.req.query.since ?? 0, 10);
    const notes = await this.repo.pullSince(since, deviceId);
    this.ok({
      server_time: Date.now(),
      notes: notes.map((n) => NoteViewModel.toApiArray(n)),
    });
  }
}

module.exports = { NoteController };
