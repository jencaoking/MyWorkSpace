package com.jencao.mywork.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.jencao.mywork.data.local.BaseEntity

/** 习惯打卡记录（工具箱模块 P6）。 */
@Entity(tableName = "habit_checkins", indices = [Index("habit_id"), Index("date")])
data class HabitCheckinEntity(
    @ColumnInfo(name = "habit_id") var habitId: String = "",
    @ColumnInfo(name = "date") var date: String = "",
    @ColumnInfo(name = "created_at") var createdAt: Long = System.currentTimeMillis()
) : BaseEntity()
