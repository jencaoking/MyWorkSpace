<?php
namespace App\Repository;

use App\Exception\ApiException;
use App\Model\DeviceUser;
use PDO;

/**
 * 设备用户仓库（后台用户管理）：以客户端生成的 device_id 聚合所有业务表，
 * 形成「用户」视图，并提供封禁标记、备注与一键清除数据能力。
 */
final class DeviceUserRepository
{
    public function __construct(private PDO $pdo) {}

    private const TABLE = 'device_users';

    /** 参与设备统计的业务表及其时间列（用于计算首/末活跃时间） */
    private const SOURCES = [
        'tasks'           => ['created_at', 'updated_at'],
        'categories'      => ['last_modified'],
        'notes'           => ['created_at', 'updated_at'],
        'sport_records'   => ['record_date'],
        'english_words'   => ['last_modified'],
        'movie_books'     => ['last_modified'],
        'health_records'  => ['record_time'],
        'account_records' => ['record_date'],
    ];

    /** 封禁状态持久化表（幂等建表，首次写入时自动创建） */
    public function ensureTable(): void
    {
        $this->pdo->exec(
            "CREATE TABLE IF NOT EXISTS `" . self::TABLE . "` (
                `device_id`  VARCHAR(64) NOT NULL,
                `status`     VARCHAR(16) NOT NULL DEFAULT 'active',
                `note`       VARCHAR(255) DEFAULT '',
                `created_at` BIGINT NOT NULL,
                `updated_at` BIGINT NOT NULL,
                PRIMARY KEY (`device_id`),
                KEY `idx_status` (`status`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        );
    }

    /**
     * 各设备聚合统计：记录数 + 首/末活跃时间，并左连接 device_users 取封禁状态与备注。
     * 时间列在不同表中命名不同（created_at/updated_at/last_modified/record_date/record_time），
     * 用 COALESCE 屏蔽缺失列后取 LEAST/GREATEST，统一换算成「首/末活跃时间戳」。
     */
    public function listDevices(int $limit, int $offset, ?string $q): array
    {
        $this->ensureTable();
        $union = [];
        foreach (self::SOURCES as $t => $cols) {
            $firsts = [];
            $lasts = [];
            foreach ($cols as $c) {
                $firsts[] = "COALESCE(`$c`,9223372036854775807)";
                $lasts[]  = "COALESCE(`$c`,0)";
            }
            $firstExpr = count($firsts) > 1 ? 'LEAST(' . implode(',', $firsts) . ')' : $firsts[0];
            $lastExpr  = count($lasts)  > 1 ? 'GREATEST(' . implode(',', $lasts) . ')' : $lasts[0];
            $union[] = "SELECT `device_id`, COUNT(*) AS cnt, $firstExpr AS first_at, $lastExpr AS last_at
                        FROM `$t` WHERE `device_id`<>'' AND `is_deleted`=0 GROUP BY `device_id`";
        }
        $sub = implode("\n UNION ALL\n", $union);
        $where = ($q !== null && $q !== '') ? 'WHERE d.device_id LIKE ?' : '';
        $sql = "SELECT d.device_id,
                       COALESCE(u.status,'active') AS status,
                       COALESCE(u.note,'') AS note,
                       SUM(d.cnt) AS total_records,
                       MIN(d.first_at) AS first_seen,
                       MAX(d.last_at) AS last_seen
                FROM ($sub) d
                LEFT JOIN `" . self::TABLE . "` u ON u.device_id = d.device_id
                $where
                GROUP BY d.device_id, u.status, u.note
                ORDER BY last_seen DESC
                LIMIT ? OFFSET ?";
        $stmt = $this->pdo->prepare($sql);
        $params = [];
        if ($where !== '') {
            $params[] = '%' . $q . '%';
        }
        $params[] = $limit;
        $params[] = $offset;
        $stmt->execute($params);
        return array_map(static function (array $r): DeviceUser {
            $m = new DeviceUser();
            $m->deviceId     = $r['device_id'];
            $m->status       = $r['status'];
            $m->note         = $r['note'];
            $m->totalRecords = (int) $r['total_records'];
            $m->firstSeen    = (int) $r['first_seen'];
            $m->lastSeen     = (int) $r['last_seen'];
            return $m;
        }, $stmt->fetchAll(PDO::FETCH_ASSOC));
    }

    /** 去重设备总数（用于分页，不受 LIMIT 影响） */
    public function countDevices(?string $q): int
    {
        $parts = [];
        foreach (self::SOURCES as $t => $_) {
            $parts[] = "SELECT DISTINCT `device_id` FROM `$t` WHERE `device_id`<>'' AND `is_deleted`=0";
        }
        $sub = implode("\n UNION \n", $parts);
        $where = ($q !== null && $q !== '') ? 'WHERE device_id LIKE ?' : '';
        $sql = "SELECT COUNT(*) FROM ($sub) x $where";
        $stmt = $this->pdo->prepare($sql);
        if ($where !== '') {
            $stmt->execute(['%' . $q . '%']);
        } else {
            $stmt->execute();
        }
        return (int) $stmt->fetchColumn();
    }

    /** 设置设备状态（active|banned）与/或备注（按需更新，已存在则更新，否则插入） */
    public function setStatus(string $deviceId, ?string $status, ?string $note): DeviceUser
    {
        $this->ensureTable();
        if ($status !== null && !in_array($status, ['active', 'banned'], true)) {
            throw new ApiException('invalid status', 400, 400);
        }
        $now = (int) (microtime(true) * 1000);
        $existingNote = $this->getNote($deviceId);
        if ($existingNote === null) {
            $this->pdo->prepare(
                "INSERT INTO `" . self::TABLE . "` (device_id, status, note, created_at, updated_at) VALUES (?,?,?,?,?)"
            )->execute([$deviceId, $status ?? 'active', $note ?? '', $now, $now]);
        } else {
            $finalNote = $note === null ? $existingNote : $note;
            $finalStatus = $status ?? 'active';
            $this->pdo->prepare(
                "UPDATE `" . self::TABLE . "` SET status=?, note=?, updated_at=? WHERE device_id=?"
            )->execute([$finalStatus, $finalNote, $now, $deviceId]);
        }
        $m = new DeviceUser();
        $m->deviceId = $deviceId;
        $m->status = $status ?? 'active';
        $m->note = $note === null ? ($existingNote ?? '') : $note;
        return $m;
    }

    private function getNote(string $deviceId): ?string
    {
        $stmt = $this->pdo->prepare("SELECT note FROM `" . self::TABLE . "` WHERE device_id=?");
        $stmt->execute([$deviceId]);
        $r = $stmt->fetch(PDO::FETCH_ASSOC);
        return $r ? (string) $r['note'] : null;
    }

    /**
     * 软删除某设备在所有业务表中的全部数据：标记 is_deleted=1 并置 needs_sync=1，
     * 使各客户端在下一次拉取增量时收到删除指令。返回各表受影响行数。
     */
    public function deleteDeviceData(string $deviceId): array
    {
        $now = (int) (microtime(true) * 1000);
        $tables = [
            'tasks', 'task_checkins', 'categories', 'notes',
            'sport_records', 'english_words', 'movie_books', 'health_records', 'account_records',
        ];
        $counts = [];
        foreach ($tables as $t) {
            $stmt = $this->pdo->prepare(
                "UPDATE `$t` SET is_deleted=1, last_modified=?, needs_sync=1 WHERE device_id=? AND is_deleted=0"
            );
            $stmt->execute([$now, $deviceId]);
            $counts[$t] = $stmt->rowCount();
        }
        return $counts;
    }
}
