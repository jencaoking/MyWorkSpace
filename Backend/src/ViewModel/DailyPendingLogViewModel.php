<?php
namespace App\ViewModel;

use App\Model\DailyPendingLog;

/** 每日未完成作业视图模型：负责模型 <-> 数组 转换。 */
class DailyPendingLogViewModel {
    public static function fromArray(array $a): DailyPendingLog { return DailyPendingLog::fromUploadArray($a); }
    public static function toArray(DailyPendingLog $m): array { return $m->toApiArray(); }
    public static function listToArray(array $list): array {
        return array_map(static fn(DailyPendingLog $m) => $m->toApiArray(), $list);
    }
}
