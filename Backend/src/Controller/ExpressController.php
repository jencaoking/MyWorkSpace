<?php
namespace App\Controller;

use App\Repository\ConfigRepository;
use App\Lib\ApiAuth;

/**
 * 快递查询代理：调用快递100实时查询接口，密钥（express_key / express_secret）存于 app_config，不下发客户端。
 * POST /api/express/track {company, tracking_no}
 */
class ExpressController
{
    private ConfigRepository $config;

    public function __construct($pdo)
    {
        $this->config = new ConfigRepository($pdo);
    }

    public function track(): void
    {
        $device = ApiAuth::deviceId();
        if ($device === '') {
            ApiResponse::json(['code' => 401, 'message' => '缺少 X-Device-ID', 'data' => null]);
        }
        $body = ApiAuth::jsonBody();
        $company = trim((string)($body['company'] ?? ''));
        $trackingNo = trim((string)($body['tracking_no'] ?? ''));
        if ($company === '' || $trackingNo === '') {
            ApiResponse::json(['code' => 1, 'message' => '缺少 company / tracking_no', 'data' => null]);
        }
        $key = $this->config->get('express_key', '');
        $customer = $this->config->get('express_secret', ''); // 快递100 的 customer 参数
        if ($key === '' || $customer === '') {
            ApiResponse::json(['code' => 2, 'message' => '未配置快递100密钥（请在后台管理填写 express_key / express_secret）', 'data' => null]);
        }

        $param = json_encode([
            'com' => $company,
            'num' => $trackingNo,
            'resultv2' => 1,
            'show' => 0,
        ], JSON_UNESCAPED_UNICODE);

        $sign = strtoupper(md5($param . $key . $customer));
        $post = json_encode([
            'customer' => $customer,
            'sign' => $sign,
            'param' => $param,
        ], JSON_UNESCAPED_UNICODE);

        $resp = $this->httpPost('https://poll.kuaidi100.com/poll', $post, [
            'Content-Type: application/x-www-form-urlencoded',
        ]);
        if ($resp === null) {
            ApiResponse::json(['code' => 3, 'message' => '快递100 请求失败', 'data' => null]);
        }
        $data = json_decode($resp, true);
        if (!isset($data['status']) || (string)$data['status'] !== '200') {
            ApiResponse::json(['code' => 4, 'message' => $data['message'] ?? '快递100 返回异常', 'data' => null]);
        }
        ApiResponse::json([
            'code' => 0,
            'message' => 'ok',
            'data' => [
                'company' => $data['com'] ?? $company,
                'tracking_no' => $trackingNo,
                'state' => $data['state'] ?? '',
                'status' => $data['status'] ?? '',
                'traces' => $data['data'] ?? [],
            ],
        ]);
    }

    private function httpPost(string $url, string $payload, array $headers): ?string
    {
        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_POST => true,
            CURLOPT_HTTPHEADER => $headers,
            CURLOPT_POSTFIELDS => 'poll=' . urlencode($payload),
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT => 20,
            CURLOPT_CONNECTTIMEOUT => 10,
        ]);
        $resp = curl_exec($ch);
        $code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        return ($resp === false || $code >= 500) ? null : $resp;
    }
}
