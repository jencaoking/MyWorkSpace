package com.jencao.mywork.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.jencao.mywork.data.local.BaseEntity

/**
 * 健康就诊记录表（P1）。
 */
@Entity(
    tableName = "health_records",
    indices = [Index("record_time")]
)
data class HealthRecordEntity(
    @ColumnInfo(name = "type") var type: String = "",
    @ColumnInfo(name = "value") var value: Float = 0f,
    @ColumnInfo(name = "unit") var unit: String = "",
    @ColumnInfo(name = "record_time") var recordTime: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "note") var note: String = ""
) : BaseEntity()
