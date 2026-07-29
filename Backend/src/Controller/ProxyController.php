<?php
namespace App\Controller;

use App\Repository\ConfigRepository;

/**
 * 第三方 API 代理：把 API 密钥保留在服务端（后台管理填写），
 * 客户端只调用 /api/proxy/*，避免密钥下发到 App。
 * 默认实现有道智云开放翻译接口（openapi.youdao.com/api）。
 */
class ProxyController
{
    private ConfigRepository $config;

    public function __construct($pdo)
    {
        $this->config = new ConfigRepository($pdo);
    }

    /** GET /api/proxy/translate?text=...&from=auto&to=zh-CHS */
    public function translate(): void
    {
        $text = trim($_GET['text'] ?? '');
        $from = $_GET['from'] ?? 'auto';
        $to   = $_GET['to'] ?? 'zh-CHS';
        if ($text === '') {
            ApiResponse::json(['code' => 1, 'message' => '缺少参数 text', 'data' => null]);
        }
        $data = $this->callYoudao($text, $from, $to);
        if ($data === null) {
            ApiResponse::json(['code' => 2, 'message' => '未配置有道 API 密钥（请在后台管理填写）或请求失败', 'data' => null]);
        }
        if (!empty($data['errorCode']) && (string)$data['errorCode'] !== '0') {
            ApiResponse::json(['code' => 3, 'message' => '有道 API 返回错误：' . $data['errorCode'], 'data' => $data]);
        }
        $translation = isset($data['translation']) ? implode('', $data['translation']) : '';
        ApiResponse::json([
            'code'    => 0,
            'message' => 'ok',
            'data'    => [
                'query'       => $text,
                'from'        => $from,
                'to'          => $to,
                'translation' => $translation,
                'speak_url'   => $data['speakUrl'] ?? '',
                't_speak_url' => $data['tSpeakUrl'] ?? '',
            ],
        ]);
    }

    /** GET /api/proxy/word?text=... （单词释义 / 音标 / 发音） */
    public function word(): void
    {
        $text = trim($_GET['text'] ?? '');
        if ($text === '') {
            ApiResponse::json(['code' => 1, 'message' => '缺少参数 text', 'data' => null]);
        }
        $data = $this->callYoudao($text, 'auto', 'auto');
        if ($data === null) {
            ApiResponse::json(['code' => 2, 'message' => '未配置有道 API 密钥（请在后台管理填写）或请求失败', 'data' => null]);
        }
        if (!empty($data['errorCode']) && (string)$data['errorCode'] !== '0') {
            ApiResponse::json(['code' => 3, 'message' => '有道 API 返回错误：' . $data['errorCode'], 'data' => $data]);
        }
        $basic    = $data['basic'] ?? [];
        $explains = $basic['explains'] ?? [];
        $examples = [];
        if (!empty($data['web'])) {
            foreach ($data['web'] as $w) {
                $examples[] = [
                    'source' => $w['key'] ?? '',
                    'target' => implode('；', $w['value'] ?? []),
                ];
            }
        }
        ApiResponse::json([
            'code'    => 0,
            'message' => 'ok',
            'data'    => [
                'word'        => $text,
                'phonetic'    => $basic['phonetic'] ?? '',
                'phonetic_us' => $basic['uk-phonetic'] ?? '',
                'phonetic_uk' => $basic['us-phonetic'] ?? '',
                'explains'    => $explains,
                'translation' => $data['translation'] ?? [],
                'speak_url'   => $data['speakUrl'] ?? ($basic['uk-speech'] ?? ''),
                't_speak_url' => $data['tSpeakUrl'] ?? ($basic['us-speech'] ?? ''),
                'examples'    => $examples,
            ],
        ]);
    }

    /**
     * GET /api/proxy/tmdb/search?query=...&page=1
     * TMDB 搜索代理（search/multi，仅 movie/tv）。密钥仅服务端持有，App 经此检索，
     * 返回归一化结果：tmdb_id / media_type / title / original_title / overview /
     * poster_url / release_date / vote_average。
     */
    public function searchTmdb(): void
    {
        $query = trim((string) ($_GET['query'] ?? ''));
        $page  = max(1, (int) ($_GET['page'] ?? 1));
        if ($query === '') {
            ApiResponse::json(['code' => 1, 'message' => '缺少 query 参数', 'data' => null], 400);
            return;
        }
        $apiKey = $this->tmdbKey();
        if ($apiKey === '') {
            ApiResponse::json(['code' => 1, 'message' => '服务端未配置 TMDB API Key（请在后台管理填写）', 'data' => null], 412);
            return;
        }
        $url = 'https://api.themoviedb.org/3/search/multi?api_key=' . urlencode($apiKey)
            . '&language=zh-CN&include_adult=false&page=' . $page
            . '&query=' . urlencode($query);
        $raw = $this->httpGet($url);
        if ($raw === false || $raw === '') {
            ApiResponse::json(['code' => 1, 'message' => 'TMDB 请求失败（网络或密钥无效）', 'data' => null], 502);
            return;
        }
        $json = json_decode($raw, true);
        if (!is_array($json) || !isset($json['results'])) {
            ApiResponse::json(['code' => 1, 'message' => 'TMDB 返回异常', 'data' => null], 502);
            return;
        }
        $results = [];
        foreach ($json['results'] as $r) {
            $type = (string) ($r['media_type'] ?? '');
            if ($type === 'person') {
                continue; // 仅保留影视（movie/tv），剔除人物
            }
            $poster = isset($r['poster_path']) && $r['poster_path']
                ? 'https://image.tmdb.org/t/p/w342' . $r['poster_path']
                : '';
            if ($type === 'tv') {
                $title    = (string) ($r['name'] ?? $r['original_name'] ?? '');
                $original = (string) ($r['original_name'] ?? $r['name'] ?? '');
                $release  = (string) ($r['first_air_date'] ?? '');
            } else {
                $title    = (string) ($r['title'] ?? $r['original_title'] ?? '');
                $original = (string) ($r['original_title'] ?? $r['title'] ?? '');
                $release  = (string) ($r['release_date'] ?? '');
            }
            $results[] = [
                'tmdb_id'        => (int) ($r['id'] ?? 0),
                'media_type'     => $type,
                'title'          => $title,
                'original_title' => $original,
                'overview'       => (string) ($r['overview'] ?? ''),
                'poster_url'     => $poster,
                'release_date'   => $release,
                'vote_average'   => isset($r['vote_average']) ? (float) $r['vote_average'] : 0.0,
            ];
        }
        ApiResponse::json([
            'code'    => 0,
            'message' => 'ok',
            'data'    => [
                'query'         => $query,
                'page'          => (int) ($json['page'] ?? $page),
                'total_results' => (int) ($json['total_results'] ?? count($results)),
                'total_pages'   => (int) ($json['total_pages'] ?? 1),
                'results'       => $results,
            ],
        ]);
    }

    /** 读取 TMDB 密钥：优先后台管理填写的 app_config.tmdb_key，回退 config('api_keys')['tmdb']。 */
    private function tmdbKey(): string
    {
        $cfg = $this->config->get('tmdb_key', '');
        if ($cfg !== '') {
            return $cfg;
        }
        if (function_exists('config')) {
            $a = config('api_keys');
            return (string) ($a['tmdb'] ?? '');
        }
        return '';
    }

    /** 读取密钥：优先后台管理填写的 app_config，回退到 config/api_keys.php。 */
    private function keys(): array
    {
        $key = $this->config->get('youdao_app_key', '');
        $sec = $this->config->get('youdao_app_secret', '');
        if (($key === '' || $sec === '') && function_exists('config')) {
            $a = config('api_keys');
            if (!empty($a['youdao']['app_key'])) {
                $key = $key ?: $a['youdao']['app_key'];
            }
            if (!empty($a['youdao']['app_secret'])) {
                $sec = $sec ?: $a['youdao']['app_secret'];
            }
        }
        return [$key, $sec];
    }

    private function callYoudao(string $q, string $from, string $to): ?array
    {
        [$appKey, $secret] = $this->keys();
        if ($appKey === '' || $secret === '') {
            return null;
        }

        $salt    = uniqid();
        $curtime = (string)time();
        $len     = mb_strlen($q, 'UTF-8');
        $input   = $len > 20
            ? mb_substr($q, 0, 10, 'UTF-8') . $len . mb_substr($q, -10, null, 'UTF-8')
            : $q;
        $sign = hash('sha256', $appKey . $input . $salt . $curtime . $secret);

        $params = [
            'q'        => $q,
            'from'     => $from,
            'to'       => $to,
            'appKey'   => $appKey,
            'salt'     => $salt,
            'sign'     => $sign,
            'signType' => 'v3',
            'curtime'  => $curtime,
        ];
        $url  = 'https://openapi.youdao.com/api?' . http_build_query($params);
        $json = $this->httpGet($url);
        if ($json === false) {
            return null;
        }
        $decoded = json_decode($json, true);
        return is_array($decoded) ? $decoded : null;
    }

    private function httpGet(string $url)
    {
        $ctx = stream_context_create([
            'http' => [
                'method'  => 'GET',
                'timeout' => 8,
                'header'  => "User-Agent: MyWorkProxy/1.0\r\n",
            ],
        ]);
        return @file_get_contents($url, false, $ctx);
    }
}
