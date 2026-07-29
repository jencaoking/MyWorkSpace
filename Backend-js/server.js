/**
 * 自律工作台 后端入口（Node.js + Express，1:1 复刻 PHP 版）
 *
 * 分层职责（与 PHP 版一致）：
 *   Model      -> src/Model       领域实体（纯值对象 + 枚举）
 *   Repository -> src/Repository  数据持久化（mysql2，DB 访问的唯一出口）
 *   ViewModel  -> src/ViewModel   Model 与 API 契约之间的双向映射（校验/投影）
 *   View       -> lib/Response    JSON 响应（API 的输出即「视图」）
 *   Controller -> src/Controller  HTTP 处理，串联各层
 *   Router     -> src/Router      请求调度
 *
 * 路由表见 routes/api.js。
 */

require('dotenv').config();
const path = require('path');
const express = require('express');
const cors = require('cors');
const session = require('express-session');

const { Router } = require('./src/Router/Router');
const { createApiRouter } = require('./routes/api');
const Response = require('./lib/Response');
const Logger = require('./lib/Logger');
const { ApiException } = require('./src/Exception/ApiException');

const app = express();

// CORS：保持开箱即用的跨域访问（与原 PHP 版行为一致，便于本地/容器部署）
app.use(cors());

// JSON 请求体解析（含图片等较大负载）
app.use(express.json({ limit: '10mb' }));

// 后台管理会话（基于 express-session，cookie 名 selfwork_admin，仅供管理员登录鉴权）
app.use(
  session({
    name: 'selfwork_admin',
    secret: process.env.SESSION_SECRET || 'selfwork_admin_session_secret',
    resave: false,
    saveUninitialized: false,
    cookie: { httpOnly: true, sameSite: 'lax', secure: false },
  })
);

// 上传文件静态服务
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// 健康检查
app.get('/', (req, res) => Response.json(res, { code: 0, message: 'ok' }));

// 挂载 API 路由
app.use('/', createApiRouter(require('./config/database')));

// 404
app.use((req, res) => {
  Response.json(res, { code: 404, message: 'Not Found', data: {} }, 404);
});

// 统一错误处理
// eslint-disable-next-line no-unused-vars
app.use((err, req, res, next) => {
  Logger.exception(err, { method: req.method, path: req.path });
  if (err instanceof ApiException) {
    return Response.json(
      res,
      { code: err.code || 1, message: err.message || 'error', data: {} },
      err.httpCode || 400
    );
  }
  if (err instanceof SyntaxError) {
    return Response.json(res, { code: 400, message: 'Invalid JSON body', data: {} }, 400);
  }
  return Response.json(res, { code: 500, message: 'Internal Server Error', data: {} }, 500);
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`SelfWork backend (JS) listening on :${PORT}`);
});
