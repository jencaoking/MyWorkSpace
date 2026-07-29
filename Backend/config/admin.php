<?php
/**
 * 后台管理鉴权配置（务必在生产环境修改密码）。
 *
 * 默认管理员密码：admin123
 * 想自定义密码：用 `php -r "echo password_hash('你的密码', PASSWORD_BCRYPT);"`
 * 生成新哈希后替换下面的 password_hash 即可。
 */
return [
    'password_hash' => '$2y$12$taxM81UV.TSSHkT8f8yWC..6nL6nZLQEJ8U7Wwa3pZLiMNwmZ4xoi',
    'session_name'  => 'selfwork_admin',
];
