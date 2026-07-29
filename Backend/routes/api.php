<?php
/**
 * 路由表：把 HTTP method + path 映射到 Controller 动作。
 * 由 index.php 在创建 $router 与 $pdo 之后 require 本文件。
 */

use App\Controller\EnglishWordController;
use App\Controller\HealthController;
use App\Controller\HealthRecordController;
use App\Controller\MovieBookController;
use App\Controller\NoteController;
use App\Controller\NoteImageController;
use App\Controller\SportRecordController;
use App\Controller\TaskController;
use App\Controller\AccountRecordController;

$health = static fn() => new HealthController($pdo);
$task = static fn() => new TaskController($pdo);
$note = static fn() => new NoteController($pdo);
$noteImage = static fn() => new NoteImageController($pdo);
$sport = static fn() => new SportRecordController($pdo);
$english = static fn() => new EnglishWordController($pdo);
$media = static fn() => new MovieBookController($pdo);
$healthRecord = static fn() => new HealthRecordController($pdo);
$account = static fn() => new AccountRecordController($pdo);

// 健康检查
$router->add('GET', '/api/health', static fn() => $health()->index());

// 任务同步（阶段1 实现）
$router->add('POST', '/sync/upload', static fn() => $task()->upload());
$router->add('GET', '/sync/pull', static fn() => $task()->pull());

// 阶段2 任务接口：list / batchUpsert / delete / stats
$router->add('GET', '/api/tasks', static fn() => $task()->list());
$router->add('POST', '/api/tasks', static fn() => $task()->batchUpsert());
$router->add('POST', '/api/tasks/delete', static fn() => $task()->delete());
$router->add('GET', '/api/tasks/stats', static fn() => $task()->stats());

// 阶段3 笔记接口：list / batchUpsert / delete / search（FULLTEXT）/ pull
$router->add('GET', '/api/notes', static fn() => $note()->list());
$router->add('POST', '/api/notes', static fn() => $note()->batchUpsert());
$router->add('POST', '/api/notes/delete', static fn() => $note()->delete());
$router->add('GET', '/api/notes/search', static fn() => $note()->search());
$router->add('GET', '/api/notes/pull', static fn() => $note()->pull());
// 笔记图片上传（multipart）
$router->add('POST', '/api/notes/image', static fn() => $noteImage()->upload());

// 阶段4 专项模块接口
// 记账记录
$router->add('GET', '/api/accounts', static fn() => $account()->list());
$router->add('POST', '/api/accounts', static fn() => $account()->batchUpsert());
$router->add('POST', '/api/accounts/delete', static fn() => $account()->delete());
$router->add('GET', '/api/accounts/pull', static fn() => $account()->pull());
// 运动记录
$router->add('GET', '/api/sports', static fn() => $sport()->list());
$router->add('POST', '/api/sports', static fn() => $sport()->batchUpsert());
$router->add('POST', '/api/sports/delete', static fn() => $sport()->delete());
$router->add('GET', '/api/sports/pull', static fn() => $sport()->pull());
// 英语单词
$router->add('GET', '/api/english', static fn() => $english()->list());
$router->add('POST', '/api/english', static fn() => $english()->batchUpsert());
$router->add('POST', '/api/english/delete', static fn() => $english()->delete());
$router->add('GET', '/api/english/pull', static fn() => $english()->pull());
// 影音书籍
$router->add('GET', '/api/media', static fn() => $media()->list());
$router->add('POST', '/api/media', static fn() => $media()->batchUpsert());
$router->add('POST', '/api/media/delete', static fn() => $media()->delete());
$router->add('GET', '/api/media/pull', static fn() => $media()->pull());
// 健康记录（避免与 /api/health 健康检查冲突）
$router->add('GET', '/api/health-records', static fn() => $healthRecord()->list());
$router->add('POST', '/api/health-records', static fn() => $healthRecord()->batchUpsert());
$router->add('POST', '/api/health-records/delete', static fn() => $healthRecord()->delete());
$router->add('GET', '/api/health-records/pull', static fn() => $healthRecord()->pull());

// 每日未完成作业：list / batchUpsert / delete / pull / dispose / weekly / archive
use App\Controller\DailyPendingController;
$dailyPending = static fn() => new DailyPendingController($pdo);
$router->add('GET', '/api/daily-pending', static fn() => $dailyPending()->list());
$router->add('POST', '/api/daily-pending', static fn() => $dailyPending()->batchUpsert());
$router->add('POST', '/api/daily-pending/delete', static fn() => $dailyPending()->delete());
$router->add('GET', '/api/daily-pending/pull', static fn() => $dailyPending()->pull());
$router->add('POST', '/api/daily-pending/dispose', static fn() => $dailyPending()->dispose());
$router->add('GET', '/api/daily-pending/weekly', static fn() => $dailyPending()->weekly());
$router->add('POST', '/api/daily-pending/archive', static fn() => $dailyPending()->archive());

// ===== 工具箱模块（8 个独立模块：计算器/扫码/倒计时/习惯/闪卡/灵感/快递/单位换算） =====
use App\Controller\SyncTableController;
use App\Controller\AiController;
use App\Controller\CurrencyController;
use App\Controller\ExpressController;

// AI 统一代理（密钥存 app_config：ai_provider / ai_qwen_key / ai_openai_key ...）
$ai = static fn() => new AiController($pdo);
$router->add('POST', '/api/ai', static fn() => $ai()->handle());
$router->add('GET', '/api/ai/quota', static fn() => $ai()->quota());

// 实时汇率（密钥 currency_key 可选）
$currency = static fn() => new CurrencyController($pdo);
$router->add('GET', '/api/currency/rate', static fn() => $currency()->rate());

// 快递实时查询（密钥 express_key / express_secret 存 app_config）
$express = static fn() => new ExpressController($pdo);
$router->add('POST', '/api/express/track', static fn() => $express()->track());

// 通用同步表路由（表名 + 列白名单），统一 list / batchUpsert / delete / pull
$toolTables = [
    'calc'           => ['table' => 'calc_history',      'cols' => ['expr' => 'string', 'result' => 'string']],
    'qr'             => ['table' => 'qr_scan_history',  'cols' => ['content' => 'string', 'format' => 'string', 'note' => 'string']],
    'countdown'      => ['table' => 'countdown_events', 'cols' => ['title' => 'string', 'target_time' => 'long', 'remark' => 'string']],
    'habit-plan'     => ['table' => 'habit_plans',      'cols' => ['title' => 'string', 'description' => 'string', 'period' => 'int', 'start_date' => 'long']],
    'habit'          => ['table' => 'habits',           'cols' => ['plan_id' => 'string', 'title' => 'string', 'frequency' => 'int', 'days' => 'string', 'time_min' => 'int']],
    'habit-checkin'  => ['table' => 'habit_checkins',   'cols' => ['habit_id' => 'string', 'date' => 'string']],
    'flashcard-deck' => ['table' => 'flashcard_decks',  'cols' => ['name' => 'string', 'description' => 'string']],
    'flashcard'      => ['table' => 'flashcards',       'cols' => ['deck_id' => 'string', 'front' => 'string', 'back' => 'string', 'next_review' => 'long', 'interval_days' => 'int', 'ease' => 'float']],
    'inspiration'    => ['table' => 'inspiration_items','cols' => ['content' => 'string', 'author' => 'string', 'source' => 'string', 'tags' => 'string', 'favorite' => 'bool']],
    'express'        => ['table' => 'express_packages', 'cols' => ['company' => 'string', 'company_name' => 'string', 'tracking_no' => 'string', 'goods' => 'string', 'current_status' => 'string', 'last_update' => 'long']],
];
foreach ($toolTables as $base => $cfg) {
    $ctl = static fn() => new SyncTableController($pdo, $cfg['table'], $cfg['cols']);
    $router->add('GET', "/api/$base", static fn() => $ctl()->list());
    $router->add('POST', "/api/$base", static fn() => $ctl()->batchUpsert());
    $router->add('POST', "/api/$base/delete", static fn() => $ctl()->delete());
    $router->add('GET', "/api/$base/pull", static fn() => $ctl()->pull());
}

// 后台管理（只读）：概览 + 通用数据浏览
use App\Controller\AdminController;
use App\Controller\DeviceUserController;
$admin = static fn() => new AdminController($pdo);
$users = static fn() => new DeviceUserController($pdo);
$router->add('GET', '/admin/overview', static fn() => $admin()->overview());
$router->add('GET', '/admin/browse', static fn() => $admin()->browse());

// 后台用户管理：以设备 ID 聚合的「用户」视图（列表 / 封禁备注 / 清除数据）
$router->add('GET', '/admin/users', static fn() => $users()->list());
$router->add('POST', '/admin/users/set', static fn() => $users()->set());
$router->add('POST', '/admin/users/delete', static fn() => $users()->delete());

// 第三方 API 代理：密钥保留在服务端后台管理，App 只调用 /api/proxy/*
use App\Controller\ProxyController;
$proxy = static fn() => new ProxyController($pdo);
$router->add('GET', '/api/proxy/translate', static fn() => $proxy()->translate());
$router->add('GET', '/api/proxy/word', static fn() => $proxy()->word());
$router->add('GET', '/api/proxy/tmdb/search', static fn() => $proxy()->searchTmdb());

// 和风天气代理（密钥在后台管理：qweather_key / qweather_token / qweather_host）
$router->add('GET', '/api/proxy/weather/now', static fn() => $proxy()->weatherNow());
$router->add('GET', '/api/proxy/weather/7d', static fn() => $proxy()->weather7d());
$router->add('GET', '/api/proxy/weather/city/lookup', static fn() => $proxy()->cityLookup());

// 后台管理：API 密钥（有道翻译等）配置读写
$router->add('GET', '/admin/apikeys', static fn() => $admin()->apiKeys());
$router->add('POST', '/admin/apikeys', static fn() => $admin()->saveApiKeys());

// 分类（categories）：列表 / 批量 upsert / 删除 / 增量拉取
use App\Controller\CategoryController;
$category = static fn() => new CategoryController($pdo);
$router->add('GET', '/api/categories', static fn() => $category()->list());
$router->add('POST', '/api/categories', static fn() => $category()->batchUpsert());
$router->add('POST', '/api/categories/delete', static fn() => $category()->delete());
$router->add('GET', '/api/categories/pull', static fn() => $category()->pull());

// 设置（user_settings 单行镜像）：读取 / 保存
use App\Controller\SettingsController;
$settings = static fn() => new SettingsController($pdo);
$router->add('GET', '/api/settings', static fn() => $settings()->get());
$router->add('POST', '/api/settings', static fn() => $settings()->save());

// 后台鉴权与数据写操作（编辑/删除行）
$router->add('POST', '/admin/login', static fn() => $admin()->login());
$router->add('POST', '/admin/logout', static fn() => $admin()->logout());
$router->add('POST', '/admin/update', static fn() => $admin()->update());
$router->add('POST', '/admin/delete', static fn() => $admin()->delete());
$router->add('GET', '/admin/audit', static fn() => $admin()->audit());
