<?php
namespace App\Model;

/** 运动记录模型（对应 sport_records 表）。 */
class SportRecord {
    public ?string $id;
    public ?string $type;
    public ?int $durationMin;
    public ?float $distanceKm;
    public ?int $calories;
    public ?int $recordDate;
    public ?string $note;
    public ?int $lastModified;
    public ?int $isDeleted;
    public ?string $deviceId;
    public ?int $needsSync;

    public static function fromUploadArray(array $a): self {
        $m = new self();
        $m->id = $a['id'] ?? null;
        $m->type = $a['type'] ?? '';
        $m->durationMin = isset($a['duration_min']) ? (int)$a['duration_min'] : 0;
        $m->distanceKm = isset($a['distance_km']) && $a['distance_km'] !== '' ? (float)$a['distance_km'] : null;
        $m->calories = isset($a['calories']) && $a['calories'] !== '' ? (int)$a['calories'] : null;
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
            'duration_min' => $this->durationMin,
            'distance_km' => $this->distanceKm,
            'calories' => $this->calories,
            'record_date' => $this->recordDate,
            'note' => $this->note,
            'last_modified' => $this->lastModified,
            'is_deleted' => $this->isDeleted,
            'device_id' => $this->deviceId,
            'needs_sync' => $this->needsSync,
        ];
    }
}
