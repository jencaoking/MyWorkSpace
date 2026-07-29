<?php
/**
 * 自律工作台 后端入口（PHP 8.2 原生，无框架，MVVM 分层）
 * 部署：Nginx + PHP-FPM，所有请求重写到本文件做统一路由（见 nginx/selfwork.conf）
 *
 * 分层职责：
 *   Model      -> src/Model       领域实体（Task 等纯值对象 + 枚举）
 *   Repository -> src/Repository  数据持久化（PDO，DB 访问的唯一出口）
 *   ViewModel  -> src/ViewModel   Model 与 API 契约之间的双向映射（校验/投影）
 *   View       -> lib/Response    JSON 响应（API 的输出即「视图」）
 *   Controller -> src/Controller  HTTP 处理，串联各层（MVVM 中的绑定角色）
 *   Router     -> src/Router      请求调度
 *
 * 路由表见 routes/api.php。
 */

require_once __DIR__ . '/lib/Response.php';

// PSR-4 风格自动加载：App\ 命名空间映射到 src/ 目录
spl_autoload_register(function (string $class): void {
    $prefix = 'App\\';
    if (!str_starts_with($class, $prefix)) {
        return;
    }
    $rel = substr($class, strlen($prefix));
    $file = __DIR__ . '/src/' . str_replace('\\', '/', $rel) . '.php';
    if (is_file($file)) {
        require $file;
    }
});

// CORS 预检
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    \Response::json(['code' => 0, 'message' => 'ok']);
}

$method = $_SERVER['REQUEST_METHOD'];
$path = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH) ?: '/';

$pdo = null;
try {
    $pdo = require __DIR__ . '/config/database.php';
} catch (\Throwable $e) {
    // 数据库不可用时降级：保留 $pdo = null，由各 Controller 优雅处理
    // （例如 HealthController 会返回 db_connected=false 的合法信封，而非白屏 500）
    $pdo = null;
}

$router = new \App\Router\Router();
require __DIR__ . '/routes/api.php';

try {
    $router->dispatch($method, $path);
} catch (\App\Exception\ApiException $e) {
    \Response::error($e->getMessage(), $e->getCode() ?: 1, $e->httpCode);
} catch (\Throwable $e) {
    // 生产环境应写入日志，避免向客户端泄露内部细节
    \Response::error('Internal Server Error', 500, 500);
}
