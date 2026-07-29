package com.jencao.mywork.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.jencao.mywork.data.local.BaseEntity

/** 计算器历史（工具箱模块 P1）。 */
@Entity(tableName = "calc_history", indices = [Index("created_at")])
data class CalcHistoryEntity(
    @ColumnInfo(name = "expr") var expr: String = "",
    @ColumnInfo(name = "result") var result: String = "",
    @ColumnInfo(name = "created_at") var createdAt: Long = System.currentTimeMillis()
) : BaseEntity()
