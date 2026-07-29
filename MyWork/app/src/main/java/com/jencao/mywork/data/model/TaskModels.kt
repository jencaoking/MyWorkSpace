package com.jencao.mywork.data.model

import com.jencao.mywork.data.util.RepeatRule

enum class TaskType(val value: Int) {
    ONCE(0), REPEAT(1), GOAL(2);
    companion object {
        fun from(v: Int) = entries.firstOrNull { it.value == v } ?: ONCE
    }
}

enum class RepeatType(val value: Int) {
    NONE(RepeatRule.TYPE_NONE),
    DAILY(RepeatRule.TYPE_DAILY),
    WEEKLY(RepeatRule.TYPE_WEEKLY),
    MONTHLY(RepeatRule.TYPE_MONTHLY);
    companion object {
        fun from(v: Int) = entries.firstOrNull { it.value == v } ?: NONE
    }
}

enum class Priority(val value: Int) {
    HIGH(1), MEDIUM(2), LOW(3);
    companion object {
        fun from(v: Int) = entries.firstOrNull { it.value == v } ?: MEDIUM
    }
}

enum class TaskSort { CREATED_DESC, DUE_ASC, PRIORITY_DESC, TITLE_ASC }

data class MonthlyStats(
    val year: Int,
    val month: Int,
    val scheduledDays: Int,
    val doneDays: Int,
    val rate: Float
) {
    val percentText: String get() = "${(rate * 100).toInt()}%"
}
