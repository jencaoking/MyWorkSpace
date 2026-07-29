package com.jencao.mywork.data.repository

/**
 * SM-2 间隔重复算法（SuperMemo 2），用于英语单词记忆曲线复习。
 * quality: 0~5 评分（本应用复习页用 4 档映射到 1/3/4/5）。
 */
object SpacedRepetition {
    data class Result(val intervalDays: Int, val easeFactor: Float, val repetitions: Int)

    fun sm2(prevInterval: Int, prevEf: Float, prevRep: Int, quality: Int): Result {
        val q = quality.coerceIn(0, 5)
        var ef = prevEf + (0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f))
        if (ef < 1.3f) ef = 1.3f

        val (interval, rep) = if (q < 3) {
            // 记错：间隔重置为 1 天，重复次数清零
            1 to 0
        } else {
            val next = when (prevRep) {
                0 -> 1
                1 -> 6
                else -> (prevInterval * ef).toInt().coerceAtLeast(1)
            }
            next to prevRep + 1
        }
        return Result(interval, ef, rep)
    }
}
