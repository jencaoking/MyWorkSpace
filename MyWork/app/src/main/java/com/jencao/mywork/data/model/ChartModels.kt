package com.jencao.mywork.data.model

import androidx.room.ColumnInfo

/** 某一天的打卡次数（按日期聚合）。 */
data class DayCount(
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "cnt") val cnt: Int
)

/** 某个月的收支汇总，用于月度趋势图。 */
data class MonthlyTrendItem(
    val year: Int,
    val month: Int,
    val income: Double,
    val expense: Double
) {
    val label: String get() = "${month}月"
    val net: Double get() = income - expense
    val incomeText: String get() = "%.0f".format(income)
    val expenseText: String get() = "%.0f".format(expense)
}

/** 按类型聚合的月度收支原始结果（来自 SQL 聚合查询）。 */
data class MonthlySummaryRaw(
    @ColumnInfo(name = "income") val income: Double,
    @ColumnInfo(name = "expense") val expense: Double
)
