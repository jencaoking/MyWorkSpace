// 任务控制器，对应 PHP App\Controller\TaskController
const Response = require('../../lib/Response');
const { ApiException } = require('../../src/Exception/ApiException');
const { appApiTokenRequired } = require('../../lib/ApiAuth');
const { TaskRepository } = require('../Repository/TaskRepository');
const TaskViewModel = require('../ViewModel/TaskViewModel');

class TaskController {
  constructor(db, req, res) {
    this.db = db;
    this.req = req;
    this.res = res;
    this._deviceId = null;
  }

  deviceId() {
    const id = (this.req.headers['x-device-id'] || '').toString().trim();
    if (!id) throw new ApiException('X-Device-ID required', 400, 400);
    this._deviceId = id;
    return id;
  }

  ok(data, http = 200) {
    Response.json(this.res, { code: 0, message: 'ok', data }, http);
  }

  async list() {
    const deviceId = this.deviceId();
    appApiTokenRequired(this.req);

    const query = this.req.query;
    const status = query.status !== undefined ? parseInt(query.status, 10) : -1;
    const limit = query.limit !== undefined ? Math.min(200, Math.max(1, parseInt(query.limit, 10))) : 50;
    const offset = query.offset !== undefined ? Math.max(0, parseInt(query.offset, 10)) : 0;

    const repo = new TaskRepository(this.db);
    let list;
    let total;
    if (status >= 0) {
      list = await repo.list(deviceId, status, limit, offset);
      total = await repo.countByStatus(deviceId, status);
    } else {
      list = await repo.listAll(deviceId, limit, offset);
      total = await repo.countAll(deviceId);
    }
    this.ok({ tasks: TaskViewModel.listToArray(list), total });
  }

  async upload() {
    const deviceId = this.deviceId();
    appApiTokenRequired(this.req);

    const body = this.req.body || {};
    const arr = body.tasks;
    if (!Array.isArray(arr)) throw new ApiException('tasks array required', 400, 400);
    if (arr.length > 500) throw new ApiException('too many tasks per upload', 400, 400);

    const tasks = arr
      .filter((item) => item && item.id)
      .map((item) => TaskViewModel.fromUploadArray(item, deviceId));

    const repo = new TaskRepository(this.db);
    await repo.upsertBatch(tasks);
    this.ok({ uploaded: tasks.length });
  }

  async delete() {
    const deviceId = this.deviceId();
    appApiTokenRequired(this.req);

    const body = this.req.body || {};
    const ids = body.ids;
    if (!Array.isArray(ids)) throw new ApiException('ids array required', 400, 400);

    const repo = new TaskRepository(this.db);
    await repo.deleteBatch(ids, deviceId);
    this.ok({ deleted: ids.length });
  }

  async pull() {
    const deviceId = this.deviceId();
    appApiTokenRequired(this.req);

    const since = this.req.query.since !== undefined ? parseInt(this.req.query.since, 10) : 0;

    const repo = new TaskRepository(this.db);
    const dirty = await repo.pull(deviceId, since);
    const deleted = await repo.findDeletedIds(deviceId, since);
    this.ok({
      server_time: Date.now(),
      tasks: TaskViewModel.listToArray(dirty),
      deleted_ids: deleted,
    });
  }

  async stats() {
    const deviceId = this.deviceId();
    appApiTokenRequired(this.req);

    const repo = new TaskRepository(this.db);
    const total = await repo.countAll(deviceId);
    const done = await repo.countByStatus(deviceId, 1);
    const doing = await repo.countByStatus(deviceId, 2);
    this.ok({ total, done, doing, pending: total - done });
  }
}

module.exports = { TaskController };
