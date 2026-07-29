package com.jencao.mywork.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.jencao.mywork.data.local.BaseEntity

/** 倒计时事件（工具箱模块 P3）。 */
@Entity(tableName = "countdown_events", indices = [Index("target_time")])
data class CountdownEntity(
    @ColumnInfo(name = "title") var title: String = "",
    @ColumnInfo(name = "target_time") var targetTime: Long = 0L,
    @ColumnInfo(name = "remark") var remark: String = "",
    @ColumnInfo(name = "created_at") var createdAt: Long = System.currentTimeMillis()
) : BaseEntity()
