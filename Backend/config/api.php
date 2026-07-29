<?php
/**
 * App 接口共享令牌配置（环境变量注入）。
 * 通过 .env 的 SELFWORK_API_TOKEN 注入；未配置则返回空字符串（不强制鉴权）。
 * 客户端请求时需在头中携带：Authorization: Bearer <token>  或  X-Api-Token: <token>
 */
$envFile = __DIR__ . '/../.env';
if (is_file($envFile)) {
    foreach (file($envFile, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES) as $line) {
        $line = trim($line);
        if ($line === '' || strpos($line, '#') === 0 || strpos($line, '=') === false) {
            continue;
        }
        [$k, $v] = explode('=', $line, 2);
        $k = trim($k);
        $v = trim($v);
        if (!isset($_ENV[$k])) {
            $_ENV[$k] = $v;
            putenv("$k=$v");
        }
    }
}

$token = $_ENV['SELFWORK_API_TOKEN'] ?? '';
return is_string($token) ? $token : '';
