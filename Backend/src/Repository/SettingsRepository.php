<?php
namespace App\Repository;

use PDO;

/**
 * 设置仓储（user_settings 表）。
 * 该表为单行镜像（固定 id='local'），无 is_deleted / device_id / needs_sync，
 * 因此不提供列表/删除/增量拉取，仅 get / upsert。
 */
final class SettingsRepository
{
    public function __construct(private PDO $pdo) {}

    public function get(): ?array
    {
        $stmt = $this->pdo->prepare("SELECT * FROM user_settings WHERE id = ?");
        $stmt->execute(['local']);
        $row = $stmt->fetch();
        if (!$row) {
            return null;
        }
        if (isset($row['module_toggles'])) {
            $row['module_toggles'] = json_decode($row['module_toggles'], true);
        }
        return $row;
    }

    public function upsert(string $theme, ?array $moduleToggles, ?string $language): array
    {
        $now = date('Y-m-d H:i:s');
        $togglesJson = $moduleToggles === null ? null : json_encode($moduleToggles, JSON_UNESCAPED_UNICODE);
        $sql = "INSERT INTO user_settings (id, theme, module_toggles, language, created_at, updated_at)
                VALUES ('local', ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  theme = VALUES(theme),
                  module_toggles = VALUES(module_toggles),
                  language = VALUES(language),
                  updated_at = ?";
        $this->pdo->prepare($sql)->execute([$theme, $togglesJson, $language, $now, $now, $now]);
        return $this->get() ?? [];
    }
}
