<?php
/**
 * App 数据接口统一 JSON 响应（全局类）
 * 约定业务包络：{ code, msg, data }，与 accounts/sports/english/media/health 等模块一致。
 * 同时为之前缺失该类的控制器补齐定义（历史代码直接调用 ApiResponse::json 但未定义）。
 */
final class ApiResponse
{
    public static function json(array $data, int $httpCode = 200): void
    {
        http_response_code($httpCode);
        header('Content-Type: application/json; charset=utf-8');
        header('Access-Control-Allow-Origin: *');
        header('Access-Control-Allow-Headers: X-Device-ID, Authorization, X-Api-Token, Content-Type, Accept');
        header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
        echo json_encode($data, JSON_UNESCAPED_UNICODE);
        exit;
    }
}
