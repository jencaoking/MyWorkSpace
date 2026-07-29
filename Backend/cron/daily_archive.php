<?php
/**
 * 每日未完成作业归档 Cron 脚本（服务端兜底，与客户端 Worker 幂等收敛）。
 * 建议 crontab：5 0 * * * php /path/to/Backend/cron/daily_archive.php >> /var/log/selfwork_archive.log 2>&1
 * 仅限 CLI 执行。
 */

if (PHP_SAPI !== 'cli') {
    http_response_code(403);
    echo "CLI only\n";
    exit(1);
}

require_once __DIR__ . '/../lib/Logger.php';

spl_autoload_register(function (string $class): void {
    $prefix = 'App\\';
    if (!str_starts_with($class, $prefix)) return;
    $file = __DIR__ . '/../src/' . str_replace('\\', '/', substr($class, strlen($prefix))) . '.php';
    if (is_file($file)) require $file;
});

try {
    /** @var PDO $pdo */
    $pdo = require __DIR__ . '/../config/database.php';
    $repo = new \App\Repository\DailyPendingLogRepository($pdo);
    $count = $repo->archiveOverdue();
    $msg = sprintf('[%s] daily_archive done, archived=%d', date('Y-m-d H:i:s'), $count);
    echo $msg . "\n";
    \Logger::info('daily_archive: archived=' . $count);
    exit(0);
} catch (\Throwable $e) {
    echo '[' . date('Y-m-d H:i:s') . '] daily_archive failed: ' . $e->getMessage() . "\n";
    if (class_exists('Logger')) {
        \Logger::exception($e, ['stage' => 'daily_archive']);
    }
    exit(1);
}
