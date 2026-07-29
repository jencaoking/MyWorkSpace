<?php
/**
 * 第三方 API Key —— 仅存服务端，客户端一律通过代理接口访问，绝不暴露给前端。
 * 生产环境用环境变量或密钥管理覆盖这些值。
 */
return [
    'tmdb'    => $_ENV['TMDB_KEY'] ?? 'YOUR_TMDB_KEY',
    'qweather' => $_ENV['QWEATHER_KEY'] ?? 'YOUR_QWEATHER_KEY',
    'youdao'  => [
        'app_key'    => $_ENV['YOUDAO_APP_KEY'] ?? '',
        'app_secret' => $_ENV['YOUDAO_APP_SECRET'] ?? '',
    ],
];
