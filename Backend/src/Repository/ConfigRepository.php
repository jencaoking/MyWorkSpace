<?php
namespace App\Repository;

use PDO;

/**
 * 通用键值配置存储（用于后台管理中填写的第三方 API 密钥等）。
 * 表中仅存放服务端密钥，绝不通过接口下发给客户端。
 */
class ConfigRepository
{
    private $pdo;

    public function __construct(PDO $pdo)
    {
        $this->pdo = $pdo;
    }

    public function get(string $key, $default = null)
    {
        $stmt = $this->pdo->prepare('SELECT cfg_value FROM app_config WHERE cfg_key = ?');
        $stmt->execute([$key]);
        $v = $stmt->fetchColumn();
        return $v === false ? $default : $v;
    }

    public function getAll(array $keys): array
    {
        $out = [];
        foreach ($keys as $k) {
            $out[$k] = $this->get($k, '');
        }
        return $out;
    }

    public function set(string $key, string $value): void
    {
        $this->pdo->prepare(
            'INSERT INTO app_config (cfg_key, cfg_value) VALUES (?, ?)
             ON DUPLICATE KEY UPDATE cfg_value = VALUES(cfg_value)'
        )->execute([$key, $value]);
    }

    public function setMany(array $kv): void
    {
        foreach ($kv as $k => $v) {
            if (is_string($v)) {
                $this->set($k, $v);
            }
        }
    }
}
