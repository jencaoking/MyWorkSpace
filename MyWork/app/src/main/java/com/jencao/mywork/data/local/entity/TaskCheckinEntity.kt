package com.jencao.mywork.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.jencao.mywork.data.local.BaseEntity

/**
 * 打卡记录表。每个任务每天可有一条记录，支撑「打卡详情」、日历标记与连续天数统计。
 * checkin_date 使用 yyyy-MM-dd 字符串，便于按天聚合与跨时区一致性。
 */
@Entity(
    tableName = "task_checkins",
    indices = [Index("task_id"), Index("checkin_date")]
)
data class TaskCheckinEntity(
    @ColumnInfo(name = "task_id") var taskId: String = "",
    @ColumnInfo(name = "checkin_date") var checkinDate: String = "",
    @ColumnInfo(name = "checkin_time") var checkinTime: Long = 0L,
    @ColumnInfo(name = "note") var note: String = ""
) : BaseEntity()
