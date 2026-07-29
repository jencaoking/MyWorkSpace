package com.jencao.mywork.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.jencao.mywork.data.local.BaseEntity

/** 习惯项（隶属于计划，工具箱模块 P6）。 */
@Entity(tableName = "habits", indices = [Index("plan_id")])
data class HabitEntity(
    @ColumnInfo(name = "plan_id") var planId: String = "",
    @ColumnInfo(name = "title") var title: String = "",
    @ColumnInfo(name = "frequency") var frequency: Int = 1,
    @ColumnInfo(name = "days") var days: String = "",
    @ColumnInfo(name = "time_min") var timeMin: Int = 0,
    @ColumnInfo(name = "created_at") var createdAt: Long = System.currentTimeMillis()
) : BaseEntity()
