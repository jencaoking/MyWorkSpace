package com.jencao.mywork.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.jencao.mywork.data.local.BaseEntity

/**
 * 番茄钟会话记录。mode: work/short/long；completedAt: 完成时间戳(ms)。
 */
@Entity(
    tableName = "pomodoro_sessions",
    indices = [Index(value = ["completed_at"])]
)
data class PomodoroSessionEntity(
    @ColumnInfo(name = "mode") var mode: String = "work",
    @ColumnInfo(name = "duration_min") var durationMin: Int = 25,
    @ColumnInfo(name = "completed_at") var completedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "note") var note: String = ""
) : BaseEntity()
