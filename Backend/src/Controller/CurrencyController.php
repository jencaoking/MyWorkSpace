<?php
namespace App\Controller;

use App\Repository\ConfigRepository;
use App\Lib\ApiAuth;
use PDO;

/**
 * 汇率代理：客户端本地已能按内置系数换算，这里提供"实时汇率"作为补充。
 * 密钥（currency_key）可选：若后台填写则走密钥接口；否则回退到免密钥的公开接口，并写入 unit_conversion_cache 做缓存。
 * 仅服务端持有密钥，不下发客户端。
 */
class CurrencyController
{
    private ConfigRepository $config;
    private $pdo;

    public function __construct($pdo)
    {
        $this->pdo = $pdo;
        $this->config = new ConfigRepository($pdo);
    }

    /** GET /api/currency/rate?from=USD&to=CNY&amount=1 */
    public function rate(): void
    {
        $from = strtoupper(trim((string)($_GET['from'] ?? '')));
        $to = strtoupper(trim((string)($_GET['to'] ?? '')));
        $amount = (float)($_GET['amount'] ?? 1);
        if ($from === '' || $to === '') {
            ApiResponse::json(['code' => 1, 'message' => '缺少 from / to', 'data' => null]);
        }
        if ($from === $to) {
            ApiResponse::json(['code' => 0, 'message' => 'ok', 'data' => [
                'from' => $from, 'to' => $to, 'amount' => $amount, 'rate' => 1.0, 'result' => $amount, 'cached' => false,
            ]]);
        }

        $cached = $this->getCache($from, $to, $amount);
        if ($cached !== null) {
            ApiResponse::json(['code' => 0, 'message' => 'ok', 'data' => [
                'from' => $from, 'to' => $to, 'amount' => $amount, 'rate' => $cached['rate'], 'result' => $cached['result'], 'cached' => true,
            ]]);
        }

        $rate = $this->fetchRate($from, $to);
        if ($rate === null) {
            ApiResponse::json(['code' => 2, 'message' => '获取实时汇率失败', 'data' => null]);
        }
        $this->setCache($from, $to, $amount, $rate, $amount * $rate);
        ApiResponse::json(['code' => 0, 'message' => 'ok', 'data' => [
            'from' => $from, 'to' => $to, 'amount' => $amount, 'rate' => $rate, 'result' => $amount * $rate, 'cached' => false,
        ]]);
    }

    private function fetchRate(string $from, string $to): ?float
    {
        $key = $this->config->get('currency_key', '');
        if ($key !== '') {
            // 预留密钥接口（如 openexchangerates），失败回退公开接口
            $url = "https://openexchangerates.org/api/latest.json?app_id=$key&base=$from&symbols=$to";
        } else {
            $url = "https://api.exchangerate-api.com/v4/latest/$from";
        }
        $resp = $this->httpGet($url);
        if ($resp === null) {
            if ($key !== '') {
                $resp = $this->httpGet("https://api.exchangerate-api.com/v4/latest/$from");
            }
            if ($resp === null) {
                return null;
            }
        }
        $data = json_decode($resp, true);
        if (!isset($data['rates'][$to])) {
            return null;
        }
        return (float)$data['rates'][$to];
    }

    private function getCache(string $from, string $to, float $amount): ?array
    {
        $stmt = $this->pdo->prepare(
            'SELECT value, result FROM unit_conversion_cache WHERE from_unit = ? AND to_unit = ? AND value = ? LIMIT 1'
        );
        $stmt->execute([$from, $to, $amount]);
        $row = $stmt->fetch(PDO::FETCH_ASSOC);
        if (!$row) {
            return null;
        }
        $rate = $amount != 0 ? (float)$row['result'] / (float)$row['value'] : 0.0;
        return ['rate' => $rate, 'result' => (float)$row['result']];
    }

    private function setCache(string $from, string $to, float $amount, float $rate, float $result): void
    {
        $id = sprintf(
            '%04x%04x-%04x-%04x-%04x-%04x%04x%04x',
            random_int(0, 0xffff), random_int(0, 0xffff), random_int(0, 0xffff),
            random_int(0, 0x0fff) | 0x4000, random_int(0, 0x3fff) | 0x8000,
            random_int(0, 0xffff), random_int(0, 0xffff), random_int(0, 0xffff)
        );
        $this->pdo->prepare(
            'INSERT INTO unit_conversion_cache (id, from_unit, to_unit, value, result, created_at)
             VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE result = VALUES(result), created_at = VALUES(created_at)'
        )->execute([$id, $from, $to, $amount, $result, (int)(microtime(true) * 1000)]);
    }

    private function httpGet(string $url): ?string
    {
        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT => 15,
            CURLOPT_CONNECTTIMEOUT => 8,
            CURLOPT_USERAGENT => 'selfwork/1.0',
        ]);
        $resp = curl_exec($ch);
        $code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        return ($resp === false || $code >= 400) ? null : $resp;
    }
}
