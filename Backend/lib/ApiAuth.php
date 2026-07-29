<?php
/**
 * App 接口鉴权守卫（全局函数）
 * 通过环境变量 SELFWORK_API_TOKEN 注入共享令牌；仅当令牌已配置时才强制校验，
 * 未配置时保持开放（开发态兼容，避免无令牌时整体不可用）。
 * 客户端需在请求头携带：Authorization: Bearer <token>  或  X-Api-Token: <token>
 */

use App\Exception\ApiException;

function app_api_token_required(): void
{
    static $token = null;
    static $loaded = false;
    if (!$loaded) {
        $cfg = require __DIR__ . '/../config/api.php';
        $token = is_string($cfg) ? $cfg : '';
        $loaded = true;
    }
    if ($token === '') {
        return; // 未配置令牌：不强制鉴权
    }

    $provided = '';
    $header = $_SERVER['HTTP_AUTHORIZATION'] ?? '';
    if (preg_match('/^Bearer\s+(.+)$/i', $header, $m)) {
        $provided = trim($m[1]);
    } elseif (!empty($_SERVER['HTTP_X_API_TOKEN'])) {
        $provided = (string) $_SERVER['HTTP_X_API_TOKEN'];
    }

    if ($provided === '' || !hash_equals($token, $provided)) {
        throw new ApiException('未授权：缺少或错误的 API Token', 401, 401);
    }
}
