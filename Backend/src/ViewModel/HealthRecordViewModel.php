<?php
namespace App\ViewModel;

use App\Model\HealthRecord;

/** 健康记录视图模型。 */
class HealthRecordViewModel {
    public static function fromArray(array $a): HealthRecord { return HealthRecord::fromUploadArray($a); }
    public static function toArray(HealthRecord $m): array { return $m->toApiArray(); }
    public static function listToArray(array $list): array {
        return array_map(static fn(HealthRecord $m) => $m->toApiArray(), $list);
    }
}
