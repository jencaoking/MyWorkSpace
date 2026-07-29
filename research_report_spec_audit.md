# 自律工作台 方案 V1.1 落地核查报告

## 摘要

对照 `自律工作台_完整方案_V1.1.md`（约 2100 行，覆盖 Android 端、PHP 后端、离线同步、通知、第三方 API 等），逐项比对当前代码实现。结论是：本地优先的核心业务功能（任务/分类/运动/笔记/健康/记账/番茄钟）基本可用且非空壳；但所有"外接 API"类需求（翻译、录音跟读、TMDB、天气）都未真正联网，天气模块整体缺失；通知提醒、MPAndroidChart 图表、笔记图片上传、复诊提醒、跨模块全局搜索、板块开关 UI 均未落地。最大的结构性不符是：方案要求 `ViewBinding + Fragment + Activity`，实际工程使用 `Jetpack Compose + Navigation Compose`。后端核心同步骨架已实现，但第三方代理、`/sync/full`、LWW 冲突解决未完成，且目录结构、表命名、鉴权方式、响应字段名均与方案有偏差。

---

## 一、已完成（可实际使用）

Android 端：
- 任务打卡：一次性待办、循环打卡、长期目标三类均已实现，含连续打卡天数计算、循环规则、完成率统计（证据：`TaskRepository.kt`、`TaskCheckinEntity.kt`、`RepeatRule.kt`、`TaskEditDialog.kt`、`TaskDetailScreen.kt`）。
- 分类管理：自定义板块、增删改、排序、配色，系统分类保护（证据：`CategoryManageScreen.kt`、`CategoryViewModel.kt`、`CategoryEntity.kt`）。
- 运动记录：项目+时长+距离+消耗+备注+历史，并真实接入设备 `STEP_COUNTER` 计步传感器（含 `ACTIVITY_RECOGNITION` 权限与无传感器降级）（证据：`SportEditScreen.kt` 的 `StepRecorder`、`SportViewModel.kt`、`SportListScreen.kt`）。
- 番茄钟：工作/短休/长休、环形倒计时、会话落库、历史（方案外的额外功能，实现完整）。
- 离线同步骨架：SyncManager（先上后下增量闭环）、SyncWorker（WorkManager，失败重试 3 次）、SyncScheduler（每 15 分钟周期+联网约束）、`needsSync` 标记与增量游标（证据：`data/sync/` 目录、`BaseEntity.kt`、`SyncExt.kt`）。

PHP 后端：
- 单一入口与路由：`index.php` + `Router.php` + `routes/api.php`。
- 各业务模块 CRUD：Task/Category/Note/SportRecord/MovieBook/HealthRecord/AccountRecord/EnglishWord 的 list/batchUpsert/delete/pull 真实实现。
- MySQL 建表与同步字段：各表含 `device_id/last_modified/is_deleted`（+`needs_sync` 增量标记），`INSERT ... ON DUPLICATE KEY UPDATE` 幂等写入（`sql/schema.sql`、各 Repository）。
- 通用响应包络 `{code,message,data,...}`（`lib/Response.php`、`lib/ApiResponse.php`）。
- `/sync/upload`、`/sync/pull` 对 Task 模块真实实现（X-Device-ID 归属校验 + 增量拉取）。

---

## 二、部分完成 / 未实际完成

| 需求（方案） | 已实现部分 | 缺失 / 未真正落地 |
|---|---|---|
| 任务统计（完成率/日历/月度图表） | 完成率（进度条）、真实日历网格视图 | 月度图表仅为 `LinearProgressIndicator`，未引入 MPAndroidChart，无柱状/折线图 |
| 英语学习（翻译/单词/录音跟读） | 单词（音标/释义/例句/熟悉度）、SM-2 间隔重复复习流 | **已补全（2026-07-29）**：翻译页（中↔英互译，经后端 `/api/proxy/translate` 代理有道）；单词编辑页"查询释义"按钮自动拉取音标/释义/例句并播放原音（`/api/proxy/word`）；跟读练习页用 `MediaRecorder` 录音、可播放原音与我的录音对比；单词一键加入单词本。**密钥全部留在服务端后台管理，App 不持有任何 API Key**，经后端代理调用 |
| 影音书籍（TMDB） | 书单/电影清单、状态标记、星级评分、海报链接字段 | **已补全（2026-07-29）**：影音页新增"TMDB 搜索"入口，经后端 `/api/proxy/tmdb/search`（search/multi，仅 movie/tv）联机检索，结果展示海报/标题/原名/年份/评分；点击即自动拉取并入库（title、poster_url 取 TMDB 图片 CDN、original_title、overview、release_date、vote_average、tmdb_id 均与 TMDB 100% 吻合），随后跳转编辑页补全状态/评分/备注。新增 tv（剧集）类型。**密钥留在服务端后台管理，App 不持有任何 API Key**，经后端代理调用 |
| 健康就诊（复诊提醒） | 就诊/用药/复诊/体征四类记录完整 | **复诊提醒未实现**，复诊仅作数据记录，无 AlarmManager/WorkManager 定时推送 |
| 记账（月度图表） | 收支记录、分类、收/支/结余数字汇总 | 无可视化月度图表，未引入 MPAndroidChart |
| 笔记系统（Markdown+图片） | Markdown 编辑+预览（自研轻量渲染器，标题/列表/引用/代码/粗斜体）、列表/搜索/收藏/置顶 | **图片上传未实现**（实体无图片字段，编辑页无选图/上传入口） |
| 天气（实时，外接 API） | 无 | **整个模块缺失**，无界面、无网络请求、无天气 API 接入 |
| 第九章 通知提醒 | 无 | **整个通知模块缺失**：无 NotificationScheduler、无 AlarmManager 精确闹钟、无通知渠道、无 BootReceiver、Manifest 未注册任何接收器，任务提醒/复诊闹钟均不弹出 |
| 全局搜索（跨模块 FTS） | 无 | **未实现**：全工程 0 处 Room FTS（`@Fts4/@Fts5`）、无虚拟表，仅笔记可能有基础 LIKE 查询 |
| 板块开关 | 数据层有 ModuleKey/偏好 | 设置页与首页均无开关 UI 控件，首页卡片全部固定直达，方案"可勾选显示哪些模块"未落地 |
| 主题切换（浅/深/跟随） | 需进一步确认 DataStore 主题字段 | 见"不符"项，需核实是否真正可切换 |

---

## 三、与方案不符（架构 / 技术选型 / 命名偏差）

重大架构不符：
- UI 技术栈：方案 4.3 明确要求 `ViewBinding + Fragment + Activity`，分包为 `*Activity/*Fragment`；实际工程用 `Jetpack Compose + Navigation Compose`，入口为单一 `MainActivity`（`setContent{}`），所有页面为 `*Screen` 的 `@Composable` 函数，无任何 Fragment 类（`build.gradle.kts` 用 `kotlin.compose` 插件，`MainActivity.kt`、`MyWorkApp.kt` 为证据）。这是最显著的偏离。
- Markdown 渲染：方案要求 Markwon 第三方库；实际自写轻量 `MarkdownText`，`build.gradle.kts` 未引入 markwon。
- MPAndroidChart：方案多处要求（任务月度图表、记账月度图表）；实际 `build.gradle.kts` 无该依赖，全工程无图表控件使用。
- 包名：方案为 `com.selfdiscipline.app`；实际为 `com.jencao.mywork`。

后端不符：
- 目录分层：方案为 `Core + Controllers + Models + Services + Utils`；实际为 `Model + Repository + ViewModel + Controller + Router + lib` 的 MVVM 分层，无 `Services/`、无 `Core/Utils`，也无独立的 Request/Database/Auth 类（以 `lib/Response.php`、`lib/ApiResponse.php`、`lib/ApiAuth.php`、`config/` 替代）。
- 入口路径：方案 `public/index.php`；实际 `Backend/index.php`（无 public 目录）。
- 同步接口范围：方案的 `/sync/upload`、`/sync/pull` 应为覆盖所有模块的通用端点；实际只绑到 Task 模块，其余模块走各自 `/api/{module}` 端点，且 `/sync/full` 未实现、无独立 SyncController（同步逻辑分散）。
- 第三方代理：方案第七章要求 `Services/WeatherProxy/TmdbProxy/EnglishProxy` 及 `/proxy/weather|tmdb/search|translate|word`；实际已有 `ProxyController` 与 `/api/proxy/*` 路由（`translate`、`word`、`tmdb/search` 三个端点均经服务端密钥联机调用有道 / TMDB，无 `Services/` 目录），但天气代理仍缺失；`config/api_keys.php` 的 tmdb/qweather/youdao 已由后台管理 `app_config` 接管并可经管理页填写。
- LWW 冲突解决：方案要求比较 `last_modified` 仅在更新值更新时覆盖；实际所有 `upsert` 均为无条件 `VALUES(...)` 覆盖，`last_modified` 仅用于增量拉取时间戳过滤，未做冲突比对。
- 鉴权模型：方案要求 `X-Device-ID` 作为请求头鉴权维度；实际全局鉴权用 API Token（`app_api_token_required`），`X-Device-ID` 仅作数据归属 + 非空校验。
- 表命名：方案的 `ledger_entries/fitness_records/media_items` 实际为 `account_records/sport_records/movie_books`；方案 `sync_meta` 表不存在（用 per-table `needs_sync` + `last_modified` 替代）。
- 响应字段：方案 `{code,message,data,serverTime}`；实际 `serverTime` 为 `server_time` 且非所有接口通用，`message` 与 `msg` 两套字段名并存。

---

## 四、结论

工程已把"本地优先、可离线使用"的主干业务（任务体系、分类、运动含真实计步、健康/记账记录、笔记编辑、番茄钟、后端 CRUD 与基础同步）跑通，可作为可用 MVP。其中"联网与外部服务"一层已补齐：翻译、录音跟读已接入（经 `/api/proxy/translate`、`/api/proxy/word` 代理有道）；影音 TMDB 已接入（经 `/api/proxy/tmdb/search` 联机检索并自动拉取入库，与 TMDB 100% 吻合）。以上均为 App 不直接持有密钥、统一经后端 `/api/proxy/*` 代理调用、密钥在后台管理填写。但天气外接 API 仍未接入；通知/提醒、跨模块全局搜索、图表可视化、笔记图片、板块开关 UI 也未完成。同时整体技术栈与方案文档不一致（Compose 替代 ViewBinding/Fragment，自研 Markdown 替代 Markwon，无图表库），后端分层与表命名、鉴权、响应字段也与方案描述不符。若要让方案"名副其实"，下一步优先级建议为：① 通知提醒（任务/复诊闹钟）；② 全局 FTS 搜索与板块开关 UI；③ 天气 的外接 API 接入；④ MPAndroidChart 图表与笔记图片上传；⑤ 后端补充 `/sync/full`、LWW 冲突解决，并统一响应字段与表命名。

## 参考

1. 方案文档：`j:/PROJECT/Android-Project/MyWorkSpace/自律工作台_完整方案_V1.1.md`
2. Android 代码：`MyWork/app/src/main/java/com/jencao/mywork/`
3. 后端代码：`Backend/`（index.php、routes/api.php、src/Controller、src/Repository、config、sql/schema.sql）
