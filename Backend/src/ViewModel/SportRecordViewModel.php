<?php
namespace App\ViewModel;

use App\Model\SportRecord;

/** 运动记录视图模型：负责模型 <-> 数组 转换。 */
class SportRecordViewModel {
    public static function fromArray(array $a): SportRecord { return SportRecord::fromUploadArray($a); }
    public static function toArray(SportRecord $m): array { return $m->toApiArray(); }
    public static function listToArray(array $list): array {
        return array_map(static fn(SportRecord $m) => $m->toApiArray(), $list);
    }
}
