package com.jencao.mywork.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.jencao.mywork.data.local.BaseEntity

/**
 * 记账记录表（P1）。type: income/expense；currency 默认 CNY。
 */
@Entity(
    tableName = "account_records",
    indices = [Index(value = ["record_date"]), Index(value = ["type"])]
)
data class AccountRecordEntity(
    @ColumnInfo(name = "type") var type: String = "expense",
    @ColumnInfo(name = "category") var category: String = "",
    @ColumnInfo(name = "amount") var amount: Float = 0f,
    @ColumnInfo(name = "currency") var currency: String = "CNY",
    @ColumnInfo(name = "record_date") var recordDate: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "note") var note: String = ""
) : BaseEntity()
