package com.jencao.mywork.data.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun formatDate(ts: Long?): String = if (ts == null || ts == 0L) "" else dateFmt.format(Date(ts))
    fun formatDateTime(ts: Long?): String = if (ts == null || ts == 0L) "" else dateTimeFmt.format(Date(ts))

    /** 返回指定年月的起止日期字符串（含首尾两天），格式 yyyy-MM-dd。 */
    fun monthRange(year: Int, month: Int): Pair<String, String> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.time
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.DAY_OF_MONTH, -1)
        return dateFmt.format(start) to dateFmt.format(cal.time)
    }

    /** 返回指定年月的天数。 */
    fun daysInMonth(year: Int, month: Int): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
}
