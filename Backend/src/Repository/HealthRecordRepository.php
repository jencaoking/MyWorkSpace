<?php
namespace App\Repository;

use App\Model\HealthRecord;
use PDO;

/** 健康记录仓库。 */
class HealthRecordRepository {
    public function __construct(private PDO $pdo) {}

    public function list(): array {
        $stmt = $this->pdo->query("SELECT * FROM health_records WHERE is_deleted=0 ORDER BY record_time DESC");
        return array_map(static fn($r) => self::hydrate($r), $stmt->fetchAll(PDO::FETCH_ASSOC));
    }

    public function byId(string $id): ?HealthRecord {
        $stmt = $this->pdo->prepare("SELECT * FROM health_records WHERE id=?");
        $stmt->execute([$id]);
        $r = $stmt->fetch(PDO::FETCH_ASSOC);
        return $r ? self::hydrate($r) : null;
    }

    public function upsert(HealthRecord $m): void {
        $now = time() * 1000;
        $sql = "INSERT INTO health_records
            (id,type,value,unit,record_time,note,last_modified,is_deleted,device_id,needs_sync)
            VALUES (?,?,?,?,?,?,?,?,?,0)
            ON DUPLICATE KEY UPDATE
            type=VALUES(type),value=VALUES(value),unit=VALUES(unit),record_time=VALUES(record_time),
            note=VALUES(note),last_modified=VALUES(last_modified),is_deleted=VALUES(is_deleted),
            device_id=VALUES(device_id),needs_sync=0";
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([
            $m->id, $m->type, $m->value, $m->unit, $m->recordTime, $m->note,
            $m->lastModified ?? $now, $m->isDeleted ?? 0, $m->deviceId
        ]);
    }

    public function softDelete(string $id): void {
        $this->pdo->prepare("UPDATE health_records SET is_deleted=1, last_modified=?, needs_sync=0 WHERE id=?")
            ->execute([time() * 1000, $id]);
    }

    public function findDirty(): array {
        $stmt = $this->pdo->query("SELECT * FROM health_records WHERE needs_sync=1 AND is_deleted=0");
        return array_map(static fn($r) => self::hydrate($r), $stmt->fetchAll(PDO::FETCH_ASSOC));
    }

    public function findDeletedIds(): array {
        $stmt = $this->pdo->query("SELECT id FROM health_records WHERE is_deleted=1 AND needs_sync=1");
        return array_map(static fn($r) => $r['id'], $stmt->fetchAll(PDO::FETCH_ASSOC));
    }

    public function clearDirty(array $ids): void {
        if ($ids) {
            $ph = str_repeat('?,', count($ids) - 1) . '?';
            $this->pdo->prepare("UPDATE health_records SET needs_sync=0 WHERE id IN ($ph)")->execute($ids);
        }
    }

    public function clearDeleted(array $ids): void {
        if ($ids) {
            $ph = str_repeat('?,', count($ids) - 1) . '?';
            $this->pdo->prepare("DELETE FROM health_records WHERE id IN ($ph)")->execute($ids);
        }
    }

    private static function hydrate(array $r): HealthRecord {
        $m = new HealthRecord();
        $m->id = $r['id'];
        $m->type = $r['type'];
        $m->value = $r['value'] === null ? null : (float)$r['value'];
        $m->unit = $r['unit'];
        $m->recordTime = (int)$r['record_time'];
        $m->note = $r['note'];
        $m->lastModified = (int)$r['last_modified'];
        $m->isDeleted = (int)$r['is_deleted'];
        $m->deviceId = $r['device_id'];
        $m->needsSync = (int)$r['needs_sync'];
        return $m;
    }
}
