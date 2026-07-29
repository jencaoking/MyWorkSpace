<?php
/**
 * 路由表：把 HTTP method + path 映射到 Controller 动作。
 * 由 index.php 在创建 $router 与 $pdo 之后 require 本文件。
 */

use App\Controller\HealthController;
use App\Controller\TaskController;

$health = static fn() => new HealthController($pdo);
$task = static fn() => new TaskController($pdo);

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
