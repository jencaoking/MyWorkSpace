<?php
/**
 * 轻量服务端日志：按日期滚动写入 storage/logs/app-YYYY-MM-DD.log。
 *
 * 目的：生产环境中 index.php 会把未捕获异常吞成 "Internal Server Error" 且不落盘，
 * 排查线上问题困难。本类把异常/错误信息（含请求上下文与堆栈）写入本地文件，
 * 失败时静默降级，绝不反向影响主流程响应。
 *
 * 约定：与 Response/ApiResponse/ApiAuth 同处 lib/ 全局命名空间，由 index.php 手动 require。
 */

class Logger
{
    /** 日志目录（相对项目根 Backend/ 的 storage/logs） */
    private const DIR = __DIR__ . '/../storage/logs';

    /** 级别权重，用于阈值过滤 */
    private const LEVELS = [
        'DEBUG'   => 10,
        'INFO'    => 20,
        'WARNING' => 30,
        'ERROR'   => 40,
    ];

    /**
     * 最小记录级别。低于此级别的日志被忽略。
     * 生产环境建议 WARNING（仅记录告警与错误）；排查期可调低到 INFO/DEBUG。
     */
    public static int $threshold = 30;

    public static function debug(string $message, array $context = []): void
    {
        self::log('DEBUG', $message, $context);
    }

    public static function info(string $message, array $context = []): void
    {
        self::log('INFO', $message, $context);
    }

    public static function warning(string $message, array $context = []): void
    {
        self::log('WARNING', $message, $context);
    }

    public static function error(string $message, array $context = []): void
    {
        self::log('ERROR', $message, $context);
    }

    /** 记录一个 Throwable 及其堆栈，附带上下文字段 */
    public static function exception(\Throwable $e, array $context = []): void
    {
        $context['exception'] = get_class($e);
        $context['file'] = $e->getFile() . ':' . $e->getLine();
        $context['trace'] = $e->getTraceAsString();
        self::log('ERROR', $e->getMessage(), $context);
    }

    private static function log(string $level, string $message, array $context): void
    {
        if ((self::LEVELS[$level] ?? 0) < self::$threshold) {
            return;
        }
        $dir = self::DIR;
        // 目录不存在则尝试创建；失败则静默降级（不写日志但不影响响应）
        if (!is_dir($dir) && !@mkdir($dir, 0755, true) && !is_dir($dir)) {
            return;
        }
        $file = $dir . '/app-' . date('Y-m-d') . '.log';
        $line = '[' . date('Y-m-d H:i:s') . "] [$level] " . $message;
        if ($context !== []) {
            $line .= ' ' . json_encode($context, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        }
        $line .= PHP_EOL;
        // 追加写入并加锁，避免并发交错；任何写入异常都不应冒泡到主流程
        try {
            file_put_contents($file, $line, FILE_APPEND | LOCK_EX);
        } catch (\Throwable $ignored) {
            // 日志子系统故障不能影响业务响应
        }
    }
}
