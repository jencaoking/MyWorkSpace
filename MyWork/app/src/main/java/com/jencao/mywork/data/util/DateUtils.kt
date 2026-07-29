package com.jencao.mywork.data.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun formatDate(ts: Long?): String = if (ts == null || ts == 0L) "" else dateFmt.format(Date(ts))
    fun formatDateTime(ts: Long?): String = if (ts == null || ts == 0L) "" else dateTimeFmt.format(Date(ts))
}
