<?php
namespace App\Model;

/** 记账记录模型（对应 account_records 表）。 */
class AccountRecord {
    public ?string $id;
    public ?string $type;
    public ?string $category;
    public ?float $amount;
    public ?string $currency;
    public ?int $recordDate;
    public ?string $note;
    public ?int $lastModified;
    public ?int $isDeleted;
    public ?string $deviceId;
    public ?int $needsSync;

    public static function fromUploadArray(array $a): self {
        $m = new self();
        $m->id = $a['id'] ?? null;
        $m->type = $a['type'] ?? 'expense';
        $m->category = $a['category'] ?? '';
        $m->amount = isset($a['amount']) && $a['amount'] !== '' ? (float)$a['amount'] : 0.0;
        $m->currency = $a['currency'] ?? 'CNY';
        $m->recordDate = isset($a['record_date']) ? (int)$a['record_date'] : (int)(microtime(true) * 1000);
        $m->note = $a['note'] ?? '';
        $m->lastModified = isset($a['last_modified']) ? (int)$a['last_modified'] : (int)(microtime(true) * 1000);
        $m->isDeleted = isset($a['is_deleted']) ? (int)$a['is_deleted'] : 0;
        $m->deviceId = $a['device_id'] ?? null;
        $m->needsSync = isset($a['needs_sync']) ? (int)$a['needs_sync'] : 0;
        return $m;
    }

    public function toApiArray(): array {
        return [
            'id' => $this->id,
            'type' => $this->type,
            'category' => $this->category,
            'amount' => $this->amount,
            'currency' => $this->currency,
            'record_date' => $this->recordDate,
            'note' => $this->note,
            'last_modified' => $this->lastModified,
            'is_deleted' => $this->isDeleted,
            'device_id' => $this->deviceId,
            'needs_sync' => $this->needsSync,
        ];
    }
}
