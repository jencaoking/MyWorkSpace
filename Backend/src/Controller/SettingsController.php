<?php
namespace App\Controller;

use App\Exception\ApiException;
use App\Repository\SettingsRepository;
use PDO;

/** 设置 Controller（user_settings 单行镜像）：读取 / 保存 */
final class SettingsController
{
    public function __construct(private PDO $pdo) {}

    private function decodeBody(): array
    {
        return json_decode((string) file_get_contents('php://input'), true) ?? [];
    }

    /** GET /api/settings —— 读取当前设置（无记录时返回默认值） */
    public function get(): void
    {
        $row = (new SettingsRepository($this->pdo))->get();
        if (!$row) {
            $row = [
                'id' => 'local',
                'theme' => 'system',
                'module_toggles' => null,
                'language' => 'zh',
                'created_at' => null,
                'updated_at' => null,
            ];
        }
        \ApiResponse::json(['code' => 0, 'msg' => 'ok', 'data' => ['settings' => $row]]);
    }

    /** POST /api/settings —— 保存设置（{ theme, module_toggles, language }） */
    public function save(): void
    {
        $raw = $this->decodeBody();
        $theme = isset($raw['theme']) ? (string) $raw['theme'] : 'system';
        $moduleToggles = $raw['module_toggles'] ?? null;
        if (!is_array($moduleToggles) && $moduleToggles !== null) {
            throw new ApiException('module_toggles must be an object/array', 400, 400);
        }
        $language = isset($raw['language']) ? (string) $raw['language'] : null;
        $row = (new SettingsRepository($this->pdo))->upsert(
            $theme,
            $moduleToggles === null ? null : $moduleToggles,
            $language
        );
        \ApiResponse::json(['code' => 0, 'msg' => 'ok', 'data' => ['settings' => $row]]);
    }
}
