# 自律工作台 (Self-Discipline Workspace)

一个面向个人自我管理的一体化工作台应用。移动端（Android）负责采集与展示，后端提供多端数据同步与聚合，所有模块围绕「自律」这一核心场景组织：任务清单、习惯打卡、记账、运动、健康、英语学习、影视书记录等，并配套「每日未完成作业」提醒与「工具箱」实用工具集。

本仓库是一个 monorepo，包含三套可独立运行的代码：

| 目录 | 技术栈 | 说明 |
| --- | --- | --- |
| `MyWork/` | Kotlin + Jetpack Compose (Android) | 主客户端 App |
| `Backend/` | PHP | 后端服务（原始实现），提供 REST API 与同步 |
| `Backend-js/` | Node.js + Express 4 | 后端服务的 1:1 复刻实现，便于本地轻量部署 |

后端 PHP 版与 Node.js 版接口、库表结构、同步语义完全一致，可任选其一对接 Android 端。

## 功能模块

- 任务与分类（Task / Category）：支持多设备按 `device_id` 隔离、增量同步
- 习惯打卡 / 每日未完成作业（DailyPending）：到期未完成任务自动进入「待办作业」提醒
- 记账（AccountRecord）、运动（SportRecord）、健康（HealthRecord）
- 英语学习（EnglishWord）、影视书记录（MovieBook）
- 笔记（Note）含图片上传
- 设备用户聚合视图（DeviceUser，后台用）
- 工具箱（Toolbox）：二维码扫码与生成、天气、快捷入口等
- 数据同步：基于 `last_modified` / `needs_sync` / `is_deleted` / `device_id` 的增量 pull & push，支持软删除与离线合并

## 技术栈

客户端（Android）：
- Kotlin，Jetpack Compose + Material 3
- 架构：MVVM + Repository，本地 Room 数据库 + DataStore
- 相机：CameraX（扫码）、ZXing 二维码编解码
- 最小 SDK 31，目标 SDK 36，应用 ID `com.jencao.mywork`
- 网络：Retrofit / OkHttp，配合后端增量同步协议

后端（二选一）：
- PHP 版：原生 PHP，目录式路由（`routes/api.php`），MySQL
- Node.js 版：Express 4 + mysql2/promise + dotenv + express-session + multer + bcryptjs
- 鉴权：可选令牌（`SELFWORK_API_TOKEN` / 后台 `admin` 配置），设备隔离依赖 `X-Device-ID` 请求头

## 目录结构

```
.
├── MyWork/                  # Android 客户端（Kotlin / Compose）
│   └── app/                 # 应用模块（build.gradle.kts, src/...）
├── Backend/                 # PHP 后端
│   ├── routes/api.php       # 路由表
│   ├── src/                 # 控制器 / 模型 / 仓储
│   ├── config/              # 数据库 / API / admin 配置
│   ├── sql/                 # 建库建表 SQL
│   └── nginx/               # 示例 Nginx 配置
├── Backend-js/              # Node.js 后端（PHP 版复刻）
│   ├── server.js            # 入口
│   ├── routes/api.js        # 路由表
│   ├── src/                 # Model / Repository / ViewModel / Controller
│   ├── config/              # 数据库 / API 配置
│   ├── sql/schema.sql       # 建库建表 SQL（与 PHP 版一致）
│   └── CONVENTIONS.md       # 后端编写约定（移植参考）
├── 自律工作台_完整方案_V1.1.md   # 产品完整方案
├── 工具箱模块规格.md             # 工具箱模块规格
└── 每日未完成作业功能规格.md     # 每日未完成作业功能规格
```

## 快速开始

### 1. 客户端（Android / MyWork）

环境要求：Android Studio（最新稳定版）、JDK 17、Android SDK（min 31）。

```bash
cd MyWork
./gradlew assembleDebug        # 构建 Debug APK
# 或直接在 Android Studio 中打开 MyWork/ 目录运行
```

- 包名：`com.jencao.mywork`
- 在 `app/build.gradle.kts` 中配置后端基地址（BASE_URL）、API Token 等

### 2. 后端 — Node.js（推荐本地开发）

```bash
cd Backend-js
npm install
cp config/example.*.js config/   # 按示例创建本地配置（如有）
# 编辑 config/database.js / config/api.js 配置 MySQL 连接与令牌
mysql -u <user> -p < sql/schema.sql   # 初始化数据库
npm run dev                         # 开发模式（node --watch）
# 或 npm start
```

服务默认监听 `server.js` 中配置的端口，路由统一挂载于 `routes/api.js`。

### 3. 后端 — PHP

```bash
cd Backend
# 配置 config/ 下的数据库连接与 api_keys / admin
mysql -u <user> -p < sql/<schema>.sql   # 初始化数据库
# 将本目录部署到支持 PHP 的 Web 服务器（参考 nginx/ 配置）
```

所有请求经 `index.php` → `routes/api.php` 统一分发。

## 数据同步协议（要点）

- 客户端上传：以 `last_modified` 为版本依据，服务端收到后置 `needs_sync=0`
- 客户端拉取（pull）：取 `needs_sync=1 AND is_deleted=0` 的记录；软删除通过 `findDeletedIds()` 返回已删 ID
- 设备隔离：`Task / Category / Note` 强制 `X-Device-ID` 头并按 `device_id` 过滤；`AccountRecord / SportRecord / EnglishWord / MovieBook / HealthRecord / DailyPending` 为全局列表，不按设备过滤
- 冲突处理：以 `last_modified` 较新者为准合并（见 `QrScanDao.mergeRemote` 等）

详细接口与字段约定见 `Backend-js/CONVENTIONS.md` 及各模块控制器。

## 设计文档

- `自律工作台_完整方案_V1.1.md`：产品定位、信息架构、模块总览
- `工具箱模块规格.md`：工具箱（含二维码、天气、快捷入口）详细规格
- `每日未完成作业功能规格.md`：每日未完成作业生成与提醒逻辑

## 许可证

见各子项目内部说明（如有）。
