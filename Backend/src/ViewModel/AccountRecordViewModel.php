<?php
namespace App\ViewModel;

use App\Model\AccountRecord;

/** 记账记录视图模型：负责模型 <-> 数组 转换。 */
class AccountRecordViewModel {
    public static function fromArray(array $a): AccountRecord { return AccountRecord::fromUploadArray($a); }
    public static function toArray(AccountRecord $m): array { return $m->toApiArray(); }
    public static function listToArray(array $list): array {
        return array_map(static fn(AccountRecord $m) => $m->toApiArray(), $list);
    }
}
