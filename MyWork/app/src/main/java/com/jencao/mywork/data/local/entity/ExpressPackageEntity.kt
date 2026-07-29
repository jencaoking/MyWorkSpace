package com.jencao.mywork.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.jencao.mywork.data.local.BaseEntity

/** 快递包裹（工具箱模块 P8，实时状态由服务端 /api/express/track 拉取）。 */
@Entity(tableName = "express_packages", indices = [Index("tracking_no")])
data class ExpressPackageEntity(
    @ColumnInfo(name = "company") var company: String = "",
    @ColumnInfo(name = "company_name") var companyName: String = "",
    @ColumnInfo(name = "tracking_no") var trackingNo: String = "",
    @ColumnInfo(name = "goods") var goods: String = "",
    @ColumnInfo(name = "current_status") var currentStatus: String = "",
    @ColumnInfo(name = "last_update") var lastUpdate: Long = 0L,
    @ColumnInfo(name = "created_at") var createdAt: Long = System.currentTimeMillis()
) : BaseEntity()
