# JS 后端编写约定（供模块移植参考）

本目录是 PHP 后端（Backend/）的 1:1 复刻，技术栈 Node.js + Express 4 + mysql2/promise + dotenv + express-session + multer + bcryptjs。
文件布局与原 PHP 版一致：

```
src/Model/       领域实体（纯值对象；含枚举常量；fromUploadArray / toApiArray / toArray）
src/Repository/  数据持久化（mysql2，DB 访问唯一出口；hydrate / list / pull / upsertBatch / deleteBatch）
src/ViewModel/   Model <-> API 数组 的双向映射（静态方法）
src/Controller/  HTTP 处理（构造参数 (db, req, res)，串联各层）
src/Router/      Router 类（方法 + 精确路径匹配）
lib/             Response / ApiResponse / ApiAuth / Logger / env
config/          database / api / api_keys / admin
routes/api.js    路由表（由主程序组装，统一挂载）
sql/schema.sql   建库建表（与 PHP 版完全一致）
```

## 1. 入口与请求对象
- 每个控制器构造：`constructor(db, req, res)`。`db` 是 mysql2 连接池（`require('../config/database')`）。
- 读取输入：
  - JSON body：`const body = this.req.body || {};`（server.js 已 `express.json()` 解析）
  - 查询参数：`this.req.query.xxx`（字符串）
  - 请求头：`this.req.headers['x-device-id']`（Express 已转小写）
  - 文件上传（仅 NoteImageController）：由 routes 中 `multer` 的 `upload.single('file')` 处理，控制器读 `this.req.file`
- 注意：Express 的 query 与 header 值永远是字符串或数组，需自行 `(int)` / `(float)` 转换，与 PHP 行为对齐。

## 2. 响应与错误
- 成功：`Response.json(this.res, { code: 0, message: 'ok', data: {...} }, 200);`
- 失败：`Response.error(this.res, 'msg', code, http, extra);`（`ApiResponse` 等同 `Response`，可直接 `require('../lib/ApiResponse').json(...)`）
- 业务异常：`const { ApiException } = require('../../src/Exception/ApiException'); throw new ApiException('msg', code, http);`
  异常会被 server.js 的错误处理中间件捕获并转为对应 JSON（`{code, message, data:{}}`），控制器无需自己 try/catch 预期异常。
- 可选令牌鉴权：`require('../../lib/ApiAuth').appApiTokenRequired(this.req);` 仅当设置了 `SELFWORK_API_TOKEN` 才强制；不匹配抛 401。

## 3. Repository 模式（mysql2/promise）
- 构造：`constructor(db)`，`db` 来自上层传入。
- 单行查询：
  ```js
  const [rows] = await db.query('SELECT * FROM t WHERE id=? AND is_deleted=0', [id]);
  return rows.length ? this.hydrate(rows[0]) : null;
  ```
- 列表：`const [rows] = await db.query(sql, params); return rows.map(r => this.hydrate(r));`
- 计数：`const [rows] = await db.query('SELECT COUNT(*) AS c FROM ...'); return rows[0].c;`
- 写入（批量 upsert）：
  ```js
  const sql = `INSERT INTO t (...) VALUES (...) ON DUPLICATE KEY UPDATE col=VALUES(col), last_modified=VALUES(last_modified), needs_sync=0`;
  for (const m of items) { await db.query(sql, [...]); }
  ```
- **事务**（如需要）：
  ```js
  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();
    // await conn.query(...)
    await conn.commit();
  } catch (e) { await conn.rollback(); throw e; }
  finally { conn.release(); }
  ```
- `needs_sync`：客户端上传后服务端的「脏标记」。PHP 版在 `ON DUPLICATE KEY UPDATE` 中显式置 `needs_sync=0`（表示已收到），pull 时取 `needs_sync=1 AND is_deleted=0`；软删除置 `is_deleted=1, needs_sync=0`。**严格按原 PHP 版 SQL 中的 needs_sync 取值移植。**
- 时间：`(int)(Date.now() / 1000 * 1000)` 或 `(int)(Date.now()/1000)` 视原字段单位（毫秒/秒）而定，必须与 PHP `microtime(true)*1000`（毫秒）或 `time()`（秒）一致。

## 4. Model / ViewModel
- Model：普通类，`static 枚举常量`，可选 `fromUploadArray(arr)`（含校验，非法时 `throw new ApiException`）与 `toApiArray()` / `toArray()`。
- ViewModel：静态方法 `toApiArray(model)`、`fromUploadArray(arr, deviceId?)`、`listToArray(list)`。多数模块直接用 Model 方法，ViewModel 仅做薄封装。

## 5. 设备隔离语义
- **Task / Category / Note**：要求 `X-Device-ID` 头，且按 `device_id` 过滤/写入。缺失头抛 400。
- **AccountRecord / SportRecord / EnglishWord / MovieBook / HealthRecord / DailyPending**：
  - 这些模块**不强制** `X-Device-ID` 头；device_id 来自上传体的 `device_id` 字段（可能为空）。
  - 列表为全局（无设备过滤）；`pull` 用 `needs_sync` 脏标记 + `findDeletedIds()`（不是按 last_modified 时间）。
  - 列表返回键名各不相同（见原 PHP 控制器），如 `accounts` / `sports` / `words` / `media` / `health` / `logs`。**严格按原控制器写返回键。**
- DeviceUser 是后台聚合视图，不按 device_id 过滤。

## 6. 不要做的事
- 不要修改 foundation 文件（lib/*、config/*、src/Router/*、src/Exception/*、server.js、routes/api.js）。
- 只创建你负责的 `src/Model/X.js`、`src/Repository/XRepository.js`、`src/ViewModel/XViewModel.js`、`src/Controller/XController.js`。
- `routes/api.js` 由主程序统一编写，你**无需**注册路由。

## 7. 参考实现（必读）
在开始之前，先阅读并实现风格一致的参考：`src/Model/Task.js`、`src/Repository/TaskRepository.js`、`src/ViewModel/TaskViewModel.js`、`src/Controller/TaskController.js`。
你的产出必须与参考在命名、返回结构、null/数字类型处理上保持一致。
