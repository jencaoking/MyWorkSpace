package com.jencao.mywork.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.jencao.mywork.data.local.BaseEntity

/** 习惯计划（长期目标，工具箱模块 P6）。 */
@Entity(tableName = "habit_plans", indices = [Index("start_date")])
data class HabitPlanEntity(
    @ColumnInfo(name = "title") var title: String = "",
    @ColumnInfo(name = "description") var description: String = "",
    @ColumnInfo(name = "period") var period: Int = 0,
    @ColumnInfo(name = "start_date") var startDate: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "created_at") var createdAt: Long = System.currentTimeMillis()
) : BaseEntity()
