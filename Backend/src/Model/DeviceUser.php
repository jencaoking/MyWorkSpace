<?php
namespace App\Model;

/** 设备用户（后台用户管理视角）：以客户端生成的 device_id 作为唯一用户标识。 */
class DeviceUser
{
    public ?string $deviceId = null;
    public string $status = 'active';   // active | banned
    public string $note = '';
    public int $createdAt = 0;
    public int $updatedAt = 0;

    // 以下为运行期聚合字段（来自各业务表统计，不持久化到 device_users 表）
    public int $totalRecords = 0;
    public int $firstSeen = 0;
    public int $lastSeen = 0;

    public function toApiArray(): array
    {
        return [
            'device_id'     => $this->deviceId,
            'status'        => $this->status,
            'note'          => $this->note,
            'total_records' => $this->totalRecords,
            'first_seen'    => $this->firstSeen,
            'last_seen'     => $this->lastSeen,
        ];
    }
}
