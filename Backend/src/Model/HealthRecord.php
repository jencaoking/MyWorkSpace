<?php
namespace App\Model;

/** 健康记录模型（对应 health_records 表）。value 为数值（体征），文本描述放 note。 */
class HealthRecord {
    public ?string $id;
    public ?string $type;
    public ?float $value;
    public ?string $unit;
    public ?int $recordTime;
    public ?string $note;
    public ?int $lastModified;
    public ?int $isDeleted;
    public ?string $deviceId;
    public ?int $needsSync;

    public static function fromUploadArray(array $a): self {
        $m = new self();
        $m->id = $a['id'] ?? null;
        $m->type = $a['type'] ?? 'visit';
        $m->value = isset($a['value']) && $a['value'] !== '' ? (float)$a['value'] : null;
        $m->unit = $a['unit'] ?? '';
        $m->recordTime = isset($a['record_time']) ? (int)$a['record_time'] : (int)(microtime(true) * 1000);
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
            'value' => $this->value,
            'unit' => $this->unit,
            'record_time' => $this->recordTime,
            'note' => $this->note,
            'last_modified' => $this->lastModified,
            'is_deleted' => $this->isDeleted,
            'device_id' => $this->deviceId,
            'needs_sync' => $this->needsSync,
        ];
    }
}
