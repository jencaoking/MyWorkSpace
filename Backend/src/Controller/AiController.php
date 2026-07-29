<?php
namespace App\Controller;

use App\Repository\ConfigRepository;
use App\Lib\ApiAuth;

/**
 * AI 统一代理：把大模型密钥保留在服务端（后台管理填写，存 app_config），
 * 客户端只调用 /api/ai/*，并受"每日调用上限"约束。
 * 支持的 provider：qwen（阿里云百炼 dashscope）、openai（兼容 /v1/chat/completions 的任意服务）。
 */
class AiController
{
    private ConfigRepository $config;

    public function __construct($pdo)
    {
        $this->config = new ConfigRepository($pdo);
    }

    /** 统一入口：POST /api/ai {action, content, target?, tone?} */
    public function handle(): void
    {
        $device = ApiAuth::deviceId();
        if ($device === '') {
            ApiResponse::json(['code' => 401, 'message' => '缺少 X-Device-ID', 'data' => null]);
        }
        $body = ApiAuth::jsonBody();
        $action = strtolower((string)($body['action'] ?? ''));
        $content = trim((string)($body['content'] ?? ''));
        if ($content === '') {
            ApiResponse::json(['code' => 1, 'message' => '缺少参数 content', 'data' => null]);
        }

        [$system, $user] = $this->buildPrompt($action, $content, $body);
        if ($system === null) {
            ApiResponse::json(['code' => 2, 'message' => "不支持的 action: $action", 'data' => null]);
        }

        if (!$this->checkQuota()) {
            ApiResponse::json(['code' => 429, 'message' => '今日 AI 调用次数已用完，请明日再来或联系管理员调高额度', 'data' => null]);
        }

        $result = $this->callAi($system, $user);
        if ($result === null) {
            ApiResponse::json(['code' => 3, 'message' => '未配置 AI 密钥（请在后台管理填写 ai_qwen_key / ai_openai_key）或请求失败', 'data' => null]);
        }
        $this->incQuota();
        ApiResponse::json(['code' => 0, 'message' => 'ok', 'data' => ['result' => $result]]);
    }

    /** GET /api/ai/quota —— 返回今日剩余次数（供前端展示） */
    public function quota(): void
    {
        $limit = (int)$this->config->get('ai_daily_limit', '20');
        $used = (int)$this->config->get($this->quotaKey(), '0');
        ApiResponse::json([
            'code' => 0, 'message' => 'ok',
            'data' => ['limit' => $limit, 'used' => $used, 'remaining' => max(0, $limit - $used)],
        ]);
    }

    private function buildPrompt(string $action, string $content, array $body): array
    {
        switch ($action) {
            case 'chat':
                return ['你是自律工作台里的热心 AI 助手，用简洁友好的中文回答。', $content];
            case 'summarize':
                return ["你是一个文本摘要助手，用简洁的中文总结下面文字，突出要点，不要复述。", $content];
            case 'rewrite':
                $tone = trim((string)($body['tone'] ?? '自然通顺'));
                return ["你是中文润色助手。请把下面文字润色得更$tone，保持原意，直接输出润色后的文字，不要解释。", $content];
            case 'translate':
                $target = trim((string)($body['target'] ?? 'en'));
                return ["You are a translator. Translate the following text into $target. Output only the translation, no extra words.", $content];
            case 'explain':
                return ["请用通俗易懂、带生活化例子的方式解释下面的概念或问题，使用中文，必要时分点。", $content];
            case 'extract':
                return ["请从下面的文本中提取结构化要点，使用中文、分条列出关键结论，不要啰嗦。", $content];
            default:
                return [null, null];
        }
    }

    private function quotaKey(): string
    {
        return 'ai_usage_' . date('Y-m-d');
    }

    private function checkQuota(): bool
    {
        $limit = (int)$this->config->get('ai_daily_limit', '20');
        if ($limit <= 0) {
            return true; // 0 表示不限
        }
        $used = (int)$this->config->get($this->quotaKey(), '0');
        return $used < $limit;
    }

    private function incQuota(): void
    {
        $limit = (int)$this->config->get('ai_daily_limit', '20');
        if ($limit <= 0) {
            return;
        }
        $used = (int)$this->config->get($this->quotaKey(), '0') + 1;
        $this->config->set($this->quotaKey(), (string)$used);
    }

    private function callAi(string $system, string $user): ?string
    {
        $provider = $this->config->get('ai_provider', 'qwen');
        if ($provider === 'openai') {
            $key = $this->config->get('ai_openai_key', '');
            $base = rtrim($this->config->get('ai_openai_base', 'https://api.openai.com/v1'), '/');
            $model = $this->config->get('ai_openai_model', 'gpt-4o-mini');
        } else {
            $key = $this->config->get('ai_qwen_key', '');
            $base = 'https://dashscope.aliyuncs.com/compatible-mode/v1';
            $model = $this->config->get('ai_qwen_model', 'qwen-plus');
        }
        if ($key === '') {
            return null;
        }
        $url = "$base/chat/completions";
        $payload = json_encode([
            'model' => $model,
            'messages' => [
                ['role' => 'system', 'content' => $system],
                ['role' => 'user', 'content' => $user],
            ],
            'temperature' => 0.7,
        ], JSON_UNESCAPED_UNICODE);
        $resp = $this->httpPost($url, $payload, [
            'Authorization: Bearer ' . $key,
            'Content-Type: application/json',
        ]);
        if ($resp === null) {
            return null;
        }
        $data = json_decode($resp, true);
        if (!isset($data['choices'][0]['message']['content'])) {
            return null;
        }
        return trim($data['choices'][0]['message']['content']);
    }

    private function httpPost(string $url, string $payload, array $headers): ?string
    {
        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_POST => true,
            CURLOPT_HTTPHEADER => $headers,
            CURLOPT_POSTFIELDS => $payload,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT => 30,
            CURLOPT_CONNECTTIMEOUT => 10,
        ]);
        $resp = curl_exec($ch);
        $code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        return ($resp === false || $code >= 500) ? null : $resp;
    }
}
