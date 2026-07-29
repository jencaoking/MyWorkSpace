<?php
namespace App\ViewModel;

use App\Model\DeviceUser;

/** 设备用户视图模型：负责模型 <-> 数组 转换。 */
class DeviceUserViewModel
{
    public static function fromArray(array $a): DeviceUser
    {
        $m = new DeviceUser();
        $m->deviceId = $a['device_id'] ?? null;
        $m->status = $a['status'] ?? 'active';
        $m->note = $a['note'] ?? '';
        return $m;
    }

    public static function toArray(DeviceUser $m): array
    {
        return $m->toApiArray();
    }

    public static function listToArray(array $list): array
    {
        return array_map(static fn(DeviceUser $m) => $m->toApiArray(), $list);
    }
}
