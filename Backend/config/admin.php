<?php
/**
 * 后台管理鉴权配置。
 *
 * 密码来源优先级（从高到低）：
 *   1) 环境变量 SELFWORK_ADMIN_PASSWORD（推荐生产使用，避免把密码写入仓库）。
 *      支持两种写法：
 *        - 明文：        SELFWORK_ADMIN_PASSWORD=你的密码
 *        - bcrypt 哈希：  SELFWORK_ADMIN_PASSWORD='$2y$...'  （由 password_hash 生成）
 *   2) 项目根目录 .env 中的同名变量（仅当环境变量未设置时读取，便于本地/容器部署）。
 *   3) 下方内置默认哈希（仅用于本地开发，对应密码 admin123）。
 *
 * 生成新 bcrypt 哈希：php -r "echo password_hash('你的密码', PASSWORD_BCRYPT);"
 */

// 轻量 .env 加载：仅当进程环境尚未设置目标变量时，从项目根目录 .env 读取同名变量。
if (getenv('SELFWORK_ADMIN_PASSWORD') === false && is_readable(__DIR__ . '/../.env')) {
    foreach (file(__DIR__ . '/../.env', FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES) as $line) {
        $line = trim($line);
        if ($line === '' || str_starts_with($line, '#') || !str_contains($line, '=')) {
            continue;
        }
        [$k, $v] = explode('=', $line, 2);
        $k = trim($k);
        $v = trim($v);
        if ($k !== '' && getenv($k) === false) {
            putenv("$k=$v");
        }
    }
}

$env = getenv('SELFWORK_ADMIN_PASSWORD');
if ($env !== false && $env !== '') {
    $isHash = str_starts_with($env, '$2y$') || str_starts_with($env, '$2a$') || str_starts_with($env, '$2b$');
    return [
        'source'         => 'env',
        'password_hash'  => $isHash ? $env : null,
        'password_plain' => $isHash ? null : $env,
    ];
}

// 本地开发默认密码：admin123（生产环境请改用环境变量 / .env 注入）
return [
    'source'         => 'default',
    'password_hash'  => '$2y$12$taxM81UV.TSSHkT8f8yWC..6nL6nZLQEJ8U7Wwa3pZLiMNwmZ4xoi',
    'password_plain' => null,
];
