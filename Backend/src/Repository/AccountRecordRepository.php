<?php
namespace App\Repository;

use App\Model\AccountRecord;
use PDO;

/** 记账记录仓库。 */
class AccountRecordRepository {
    public function __construct(private PDO $pdo) {}

    public function list(): array {
        $stmt = $this->pdo->query("SELECT * FROM account_records WHERE is_deleted=0 ORDER BY record_date DESC");
        return array_map(static fn($r) => self::hydrate($r), $stmt->fetchAll(PDO::FETCH_ASSOC));
    }

    public function byId(string $id): ?AccountRecord {
        $stmt = $this->pdo->prepare("SELECT * FROM account_records WHERE id=?");
        $stmt->execute([$id]);
        $r = $stmt->fetch(PDO::FETCH_ASSOC);
        return $r ? self::hydrate($r) : null;
    }

    public function upsert(AccountRecord $m): void {
        $now = time() * 1000;
        $sql = "INSERT INTO account_records
            (id,type,category,amount,currency,record_date,note,last_modified,is_deleted,device_id,needs_sync)
            VALUES (?,?,?,?,?,?,?,?,?,?,0)
            ON DUPLICATE KEY UPDATE
            type=VALUES(type),category=VALUES(category),amount=VALUES(amount),currency=VALUES(currency),
            record_date=VALUES(record_date),note=VALUES(note),
            last_modified=VALUES(last_modified),is_deleted=VALUES(is_deleted),device_id=VALUES(device_id),needs_sync=0";
        $stmt = $this->pdo->prepare($sql);
        $stmt->execute([
            $m->id, $m->type, $m->category, $m->amount, $m->currency,
            $m->recordDate, $m->note, $m->lastModified ?? $now, $m->isDeleted ?? 0, $m->deviceId
        ]);
    }

    public function softDelete(string $id): void {
        $this->pdo->prepare("UPDATE account_records SET is_deleted=1, last_modified=?, needs_sync=0 WHERE id=?")
            ->execute([time() * 1000, $id]);
    }

    public function findDirty(): array {
        $stmt = $this->pdo->query("SELECT * FROM account_records WHERE needs_sync=1 AND is_deleted=0");
        return array_map(static fn($r) => self::hydrate($r), $stmt->fetchAll(PDO::FETCH_ASSOC));
    }

    public function findDeletedIds(): array {
        $stmt = $this->pdo->query("SELECT id FROM account_records WHERE is_deleted=1 AND needs_sync=1");
        return array_map(static fn($r) => $r['id'], $stmt->fetchAll(PDO::FETCH_ASSOC));
    }

    public function clearDirty(array $ids): void {
        if ($ids) {
            $ph = str_repeat('?,', count($ids) - 1) . '?';
            $this->pdo->prepare("UPDATE account_records SET needs_sync=0 WHERE id IN ($ph)")->execute($ids);
        }
    }

    public function clearDeleted(array $ids): void {
        if ($ids) {
            $ph = str_repeat('?,', count($ids) - 1) . '?';
            $this->pdo->prepare("DELETE FROM account_records WHERE id IN ($ph)")->execute($ids);
        }
    }

    private static function hydrate(array $r): AccountRecord {
        $m = new AccountRecord();
        $m->id = $r['id'];
        $m->type = $r['type'];
        $m->category = $r['category'];
        $m->amount = (float)$r['amount'];
        $m->currency = $r['currency'];
        $m->recordDate = (int)$r['record_date'];
        $m->note = $r['note'];
        $m->lastModified = (int)$r['last_modified'];
        $m->isDeleted = (int)$r['is_deleted'];
        $m->deviceId = $r['device_id'];
        $m->needsSync = (int)$r['needs_sync'];
        return $m;
    }
}
