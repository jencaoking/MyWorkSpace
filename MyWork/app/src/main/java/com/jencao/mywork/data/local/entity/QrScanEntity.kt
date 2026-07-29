package com.jencao.mywork.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.jencao.mywork.data.local.BaseEntity

/** 扫码历史（工具箱模块）。type: 0文本 1网址 2WiFi 3名片 4短信 5电话 */
@Entity(
    tableName = "qr_scan_history",
    indices = [Index("created_at"), Index("scanned_at")]
)
data class QrScanEntity(
    @ColumnInfo(name = "content") var content: String = "",
    @ColumnInfo(name = "format") var format: String = "QR",
    @ColumnInfo(name = "type") var type: Int = 0,
    @ColumnInfo(name = "note") var note: String = "",
    @ColumnInfo(name = "created_at") var createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "scanned_at") var scannedAt: Long = 0
) : BaseEntity()
