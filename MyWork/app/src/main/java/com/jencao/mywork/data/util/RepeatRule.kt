package com.jencao.mywork.data.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 循环规则与日期工具（阶段2 任务打卡的核心计算）。
 * 周几位掩码：bit(value-1)，value∈[1,7]，周一=1…周日=7（与 java.time.DayOfWeek.value 一致）。
 */
object RepeatRule {
    const val TYPE_NONE = 0
    const val TYPE_DAILY = 1
    const val TYPE_WEEKLY = 2
    const val TYPE_MONTHLY = 3

    private val FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun todayStr(): String = LocalDate.now().format(FMT)
    fun dateStr(d: LocalDate): String = d.format(FMT)
    fun parse(s: String): LocalDate = LocalDate.parse(s, FMT)
    fun epochDayToStr(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(FMT)
    fun strToEpochDay(s: String): Long = LocalDate.parse(s, FMT).toEpochDay()

    fun maskFromDays(days: Set<Int>): Int = days.fold(0) { acc, d -> acc or (1 shl (d - 1)) }
    fun daysFromMask(mask: Int): Set<Int> = (1..7).filter { mask and (1 shl (it - 1)) != 0 }.toSet()

    fun describe(type: Int, mask: Int): String = when (type) {
        TYPE_NONE -> ""
        TYPE_DAILY -> "每天"
        TYPE_WEEKLY -> {
            val names = mapOf(1 to "一", 2 to "二", 3 to "三", 4 to "四", 5 to "五", 6 to "六", 7 to "日")
            "每周 " + daysFromMask(mask).sorted().joinToString("") { names[it] ?: "" }
        }
        TYPE_MONTHLY -> "每月1日"
        else -> ""
    }

    /** 从 from（含）起下一个到期日；NONE 返回 null */
    fun nextDue(from: LocalDate, type: Int, mask: Int): LocalDate? = when (type) {
        TYPE_NONE -> null
        TYPE_DAILY -> from.plusDays(1)
        TYPE_WEEKLY -> {
            var d = from.plusDays(1)
            repeat(7) {
                if (mask and (1 shl (d.dayOfWeek.value - 1)) != 0) return d
                d = d.plusDays(1)
            }
            null
        }
        TYPE_MONTHLY -> from.plusMonths(1).withDayOfMonth(1)
        else -> null
    }

    /** 指定月份内所有计划日 */
    fun scheduledDaysInMonth(year: Int, month: Int, type: Int, mask: Int): List<LocalDate> {
        if (type == TYPE_NONE) return emptyList()
        val start = LocalDate.of(year, month, 1)
        val length = start.lengthOfMonth()
        return (0 until length).map { start.plusDays(it.toLong()) }.filter { d ->
            when (type) {
                TYPE_DAILY -> true
                TYPE_WEEKLY -> mask and (1 shl (d.dayOfWeek.value - 1)) != 0
                TYPE_MONTHLY -> d.dayOfMonth == 1
                else -> false
            }
        }
    }
}
