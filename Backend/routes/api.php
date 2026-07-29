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
use App\Controller\SportRecordController;
use App\Controller\TaskController;

$health = static fn() => new HealthController($pdo);
$task = static fn() => new TaskController($pdo);
$note = static fn() => new NoteController($pdo);
$sport = static fn() => new SportRecordController($pdo);
$english = static fn() => new EnglishWordController($pdo);
$media = static fn() => new MovieBookController($pdo);
$healthRecord = static fn() => new HealthRecordController($pdo);

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

// 阶段4 专项模块接口
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
