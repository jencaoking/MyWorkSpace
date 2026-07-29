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
require_once __DIR__ . '/lib/ApiResponse.php';
require_once __DIR__ . '/lib/ApiAuth.php';
require_once __DIR__ . '/lib/Logger.php';

/** 收集当前请求的关键上下文，便于错误日志排查（不记录敏感体） */
function requestContext(): array
{
    $fwd = $_SERVER['HTTP_X_FORWARDED_FOR'] ?? '';
    $ip = $fwd !== '' ? trim(explode(',', $fwd)[0]) : ($_SERVER['REMOTE_ADDR'] ?? '');
    return [
        'method' => $_SERVER['REQUEST_METHOD'] ?? '',
        'path'   => parse_url($_SERVER['REQUEST_URI'] ?? '', PHP_URL_PATH) ?? '',
        'device' => $_SERVER['HTTP_X_DEVICE_ID'] ?? '',
        'ip'     => $ip,
    ];
}

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

// 后台管理会话（基于 PHP 原生 Session，cookie 为同站，仅供管理员登录鉴权）
session_name('selfwork_admin');
session_start();

$method = $_SERVER['REQUEST_METHOD'];
$path = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH) ?: '/';

$pdo = null;
try {
    $pdo = require __DIR__ . '/config/database.php';
} catch (\Throwable $e) {
    // 数据库不可用时降级：保留 $pdo = null，由各 Controller 优雅处理
    // （例如 HealthController 会返回 db_connected=false 的合法信封，而非白屏 500）
    $pdo = null;
    \Logger::exception($e, ['stage' => 'db_connect'] + requestContext());
}

$router = new \App\Router\Router();
require __DIR__ . '/routes/api.php';

try {
    // 同步鉴权：采用「无登录 + 设备 ID 隔离」方案（各 Controller 已校验 X-Device-ID）。
    // App 接口不强制共享令牌，保持本地/自用部署的开箱即用与自动化同步体验。
    $router->dispatch($method, $path);
} catch (\App\Exception\ApiException $e) {
    // 业务级预期错误（参数/鉴权失败等）：以 warning 记录便于审计，但保留原始文案返回
    \Logger::warning('api_error: ' . $e->getMessage(), requestContext());
    \Response::error($e->getMessage(), $e->getCode() ?: 1, $e->httpCode);
} catch (\Throwable $e) {
    // 未捕获异常：写入堆栈日志（含请求上下文），仅向客户端返回笼统文案，避免泄露内部细节
    \Logger::exception($e, requestContext());
    \Response::error('Internal Server Error', 500, 500);
}
