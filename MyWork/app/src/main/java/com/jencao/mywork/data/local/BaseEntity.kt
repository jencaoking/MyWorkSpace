package com.jencao.mywork.data.local

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 所有云端同步表的公共字段（方案 V1.1 统一四字段 + needsSync 标记）。
 * - id: UUID 字符串主键
 * - lastModified: 最后修改时间戳（毫秒），用于增量拉取与 LWW 冲突解决
 * - isDeleted: 软删除标记，保留 30 天后由后端清理
 * - deviceId: 创建设备标识，用于多端归属判断
 * - needsSync: 本地变更待上传标记
 */
abstract class BaseEntity {
    @PrimaryKey
    @ColumnInfo(name = "id")
    var id: String = UUID.randomUUID().toString()

    @ColumnInfo(name = "last_modified")
    var lastModified: Long = System.currentTimeMillis()

    @ColumnInfo(name = "is_deleted")
    var isDeleted: Boolean = false

    @ColumnInfo(name = "device_id")
    var deviceId: String = ""

    @ColumnInfo(name = "needs_sync")
    var needsSync: Boolean = true
}
