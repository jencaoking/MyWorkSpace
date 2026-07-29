<?php
/**
 * 数据库连接（PDO / MySQL 8.0）
 * 生产环境请改用环境变量或只读配置文件，切勿将真实密码提交到仓库。
 *
 * 返回 PDO 实例： $pdo = require __DIR__ . '/config/database.php';
 */
return new PDO(
    'mysql:host=' . ($_ENV['DB_HOST'] ?? 'localhost') .
    ';dbname=' . ($_ENV['DB_NAME'] ?? 'mywork') .
    ';charset=utf8mb4',
    $_ENV['DB_USER'] ?? 'mywork',
    $_ENV['DB_PASS'] ?? 'mywork_pass',
    [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    ]
);
