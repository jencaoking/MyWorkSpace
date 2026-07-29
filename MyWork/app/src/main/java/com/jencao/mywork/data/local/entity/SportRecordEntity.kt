package com.jencao.mywork.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.jencao.mywork.data.local.BaseEntity

/**
 * 运动记录表（P1）。
 */
@Entity(
    tableName = "sport_records",
    indices = [Index("record_date")]
)
data class SportRecordEntity(
    @ColumnInfo(name = "type") var type: String = "",
    @ColumnInfo(name = "duration_min") var durationMin: Int = 0,
    @ColumnInfo(name = "distance_km") var distanceKm: Float? = null,
    @ColumnInfo(name = "calories") var calories: Int? = null,
    @ColumnInfo(name = "steps") var steps: Int? = null,
    @ColumnInfo(name = "record_date") var recordDate: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "note") var note: String = ""
) : BaseEntity()
