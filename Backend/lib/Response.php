<?php
/**
 * JSON 响应辅助（跨切面工具，全局类）
 * 作为 MVVM 中的 View 层：负责把数据序列化为 HTTP JSON 响应。
 */

final class Response
{
    public static function json(array $data, int $httpCode = 200): void
    {
        http_response_code($httpCode);
        header('Content-Type: application/json; charset=utf-8');
        header('Access-Control-Allow-Origin: *');
        header('Access-Control-Allow-Headers: X-Device-ID, Content-Type, Accept');
        header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
        echo json_encode($data, JSON_UNESCAPED_UNICODE);
        exit;
    }

    /** 统一错误响应：code 为业务码，http 为 HTTP 状态码，extra 可附加字段 */
    public static function error(string $message, int $code = 1, int $http = 400, array $extra = []): void
    {
        self::json(['code' => $code, 'message' => $message] + $extra, $http);
    }
}
