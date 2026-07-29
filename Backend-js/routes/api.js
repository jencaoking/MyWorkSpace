// 路由表（与 PHP 版 routes/api.php 1:1 对应），组装为一个 Express Router。
// 每个路由：方法 + 精确路径 → 对应 Controller 的方法。
const path = require('path');
const multer = require('multer');
const { Router } = require('../src/Router/Router');

const { TaskController } = require('../src/Controller/TaskController');
const { NoteController } = require('../src/Controller/NoteController');
const { NoteImageController } = require('../src/Controller/NoteImageController');
const { AccountRecordController } = require('../src/Controller/AccountRecordController');
const { SportRecordController } = require('../src/Controller/SportRecordController');
const { EnglishWordController } = require('../src/Controller/EnglishWordController');
const { MovieBookController } = require('../src/Controller/MovieBookController');
const { HealthRecordController } = require('../src/Controller/HealthRecordController');
const { DailyPendingController } = require('../src/Controller/DailyPendingController');
const { AdminController } = require('../src/Controller/AdminController');
const { DeviceUserController } = require('../src/Controller/DeviceUserController');
const { ProxyController } = require('../src/Controller/ProxyController');
const { CategoryController } = require('../src/Controller/CategoryController');
const { SettingsController } = require('../src/Controller/SettingsController');
const { HealthController } = require('../src/Controller/HealthController');

function wrap(fn) {
  return (req, res, next) => {
    Promise.resolve()
      .then(() => fn(req, res, next))
      .catch(next);
  };
}

function createApiRouter(db) {
  const router = new Router();

  // 控制器方法 → 请求处理器（每次请求新建 Controller 实例）
  const h = (Ctl, method) =>
    wrap((req, res, next) => new Ctl(db, req, res)[method](req, res, next));

  // 笔记图片上传（multer 处理 multipart，限制 10MB）
  const upload = multer({
    dest: path.join(__dirname, '..', 'uploads', 'notes'),
    limits: { fileSize: 10 * 1024 * 1024 },
  });
  const noteImage = (req, res, next) => {
    upload.single('file')(req, res, (err) => {
      if (err) return next(err);
      Promise.resolve()
        .then(() => new NoteImageController(db, req, res).upload(req, res))
        .catch(next);
    });
  };

  // 健康检查
  router.add('GET', '/api/health', h(HealthController, 'index'));

  // 任务
  router.add('POST', '/sync/upload', h(TaskController, 'upload'));
  router.add('GET', '/sync/pull', h(TaskController, 'pull'));
  router.add('GET', '/api/tasks', h(TaskController, 'list'));
  router.add('POST', '/api/tasks', h(TaskController, 'batchUpsert'));
  router.add('POST', '/api/tasks/delete', h(TaskController, 'delete'));
  router.add('GET', '/api/tasks/stats', h(TaskController, 'stats'));

  // 笔记
  router.add('GET', '/api/notes', h(NoteController, 'list'));
  router.add('POST', '/api/notes', h(NoteController, 'batchUpsert'));
  router.add('POST', '/api/notes/delete', h(NoteController, 'delete'));
  router.add('GET', '/api/notes/search', h(NoteController, 'search'));
  router.add('GET', '/api/notes/pull', h(NoteController, 'pull'));
  router.add('POST', '/api/notes/image', noteImage);

  // 记账
  router.add('GET', '/api/accounts', h(AccountRecordController, 'list'));
  router.add('POST', '/api/accounts', h(AccountRecordController, 'batchUpsert'));
  router.add('POST', '/api/accounts/delete', h(AccountRecordController, 'delete'));
  router.add('GET', '/api/accounts/pull', h(AccountRecordController, 'pull'));

  // 运动
  router.add('GET', '/api/sports', h(SportRecordController, 'list'));
  router.add('POST', '/api/sports', h(SportRecordController, 'batchUpsert'));
  router.add('POST', '/api/sports/delete', h(SportRecordController, 'delete'));
  router.add('GET', '/api/sports/pull', h(SportRecordController, 'pull'));

  // 英语单词
  router.add('GET', '/api/english', h(EnglishWordController, 'list'));
  router.add('POST', '/api/english', h(EnglishWordController, 'batchUpsert'));
  router.add('POST', '/api/english/delete', h(EnglishWordController, 'delete'));
  router.add('GET', '/api/english/pull', h(EnglishWordController, 'pull'));

  // 影音书籍
  router.add('GET', '/api/media', h(MovieBookController, 'list'));
  router.add('POST', '/api/media', h(MovieBookController, 'batchUpsert'));
  router.add('POST', '/api/media/delete', h(MovieBookController, 'delete'));
  router.add('GET', '/api/media/pull', h(MovieBookController, 'pull'));

  // 健康记录
  router.add('GET', '/api/health-records', h(HealthRecordController, 'list'));
  router.add('POST', '/api/health-records', h(HealthRecordController, 'batchUpsert'));
  router.add('POST', '/api/health-records/delete', h(HealthRecordController, 'delete'));
  router.add('GET', '/api/health-records/pull', h(HealthRecordController, 'pull'));

  // 每日未完成作业归档
  router.add('GET', '/api/daily-pending', h(DailyPendingController, 'list'));
  router.add('POST', '/api/daily-pending', h(DailyPendingController, 'batchUpsert'));
  router.add('POST', '/api/daily-pending/delete', h(DailyPendingController, 'delete'));
  router.add('GET', '/api/daily-pending/pull', h(DailyPendingController, 'pull'));
  router.add('POST', '/api/daily-pending/dispose', h(DailyPendingController, 'dispose'));
  router.add('GET', '/api/daily-pending/weekly', h(DailyPendingController, 'weekly'));
  router.add('POST', '/api/daily-pending/archive', h(DailyPendingController, 'archive'));

  // 分类
  router.add('GET', '/api/categories', h(CategoryController, 'list'));
  router.add('POST', '/api/categories', h(CategoryController, 'batchUpsert'));
  router.add('POST', '/api/categories/delete', h(CategoryController, 'delete'));
  router.add('GET', '/api/categories/pull', h(CategoryController, 'pull'));

  // 设置
  router.add('GET', '/api/settings', h(SettingsController, 'get'));
  router.add('POST', '/api/settings', h(SettingsController, 'save'));

  // 后台管理
  router.add('GET', '/admin/overview', h(AdminController, 'overview'));
  router.add('GET', '/admin/browse', h(AdminController, 'browse'));
  router.add('POST', '/admin/update', h(AdminController, 'update'));
  router.add('POST', '/admin/delete', h(AdminController, 'delete'));
  router.add('GET', '/admin/audit', h(AdminController, 'audit'));
  router.add('GET', '/admin/apikeys', h(AdminController, 'apiKeys'));
  router.add('POST', '/admin/apikeys', h(AdminController, 'saveApiKeys'));
  router.add('POST', '/admin/login', h(AdminController, 'login'));
  router.add('POST', '/admin/logout', h(AdminController, 'logout'));

  // 后台用户管理
  router.add('GET', '/admin/users', h(DeviceUserController, 'list'));
  router.add('POST', '/admin/users/set', h(DeviceUserController, 'set'));
  router.add('POST', '/admin/users/delete', h(DeviceUserController, 'delete'));

  // 第三方 API 代理
  router.add('GET', '/api/proxy/translate', h(ProxyController, 'translate'));
  router.add('GET', '/api/proxy/word', h(ProxyController, 'word'));
  router.add('GET', '/api/proxy/tmdb/search', h(ProxyController, 'searchTmdb'));
  router.add('GET', '/api/proxy/weather/now', h(ProxyController, 'weatherNow'));
  router.add('GET', '/api/proxy/weather/7d', h(ProxyController, 'weather7d'));
  router.add('GET', '/api/proxy/weather/city/lookup', h(ProxyController, 'cityLookup'));

  return router.dispatch();
}

module.exports = { createApiRouter };
