package com.jencao.mywork.data.repository

import com.jencao.mywork.data.local.dao.EnglishWordDao
import com.jencao.mywork.data.local.entity.EnglishWordEntity
import com.jencao.mywork.data.util.touch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnglishWordRepository @Inject constructor(
    private val dao: EnglishWordDao
) {
    fun observeAll() = dao.observeAll()
    fun observeDue() = dao.observeDue(System.currentTimeMillis())
    suspend fun getById(id: String): EnglishWordEntity? = dao.getById(id)

    /** 记忆曲线复习：返回待复习（含未学过）单词列表，用于复习页一次性加载。 */
    suspend fun getDueForReview(now: Long = System.currentTimeMillis()): List<EnglishWordEntity> =
        dao.getDueForReview(now)

    suspend fun dueCount(now: Long = System.currentTimeMillis()): Int = dao.countDue(now)

    suspend fun create(
        word: String,
        phonetic: String,
        meaning: String,
        example: String,
        familiarity: Int = 0
    ): EnglishWordEntity {
        val item = EnglishWordEntity(
            word = word,
            phonetic = phonetic,
            meaning = meaning,
            example = example,
            familiarity = familiarity
        )
        item.touch()
        dao.insert(item)
        return item
    }

    suspend fun upsert(item: EnglishWordEntity): EnglishWordEntity {
        item.touch()
        dao.update(item)
        return item
    }

    suspend fun markDeleted(id: String) = dao.softDelete(id)

    suspend fun pendingUploads() = dao.getPendingUploads()
    suspend fun pendingDeletions() = dao.getPendingDeletions()

    suspend fun clearSyncFlags(ids: List<String>, deletedIds: List<String>) {
        if (ids.isNotEmpty()) dao.clearUploadFlag(ids)
        if (deletedIds.isNotEmpty()) dao.clearDeleteFlag(deletedIds)
    }

    /** 复习评分（quality 0~5），按 SM-2 更新间隔/熟悉度/下次复习时间。 */
    suspend fun reviewWord(word: EnglishWordEntity, quality: Int) {
        val now = System.currentTimeMillis()
        val r = SpacedRepetition.sm2(word.intervalDays, word.easeFactor, word.repetitions, quality)
        word.intervalDays = r.intervalDays
        word.easeFactor = r.easeFactor
        word.repetitions = r.repetitions
        word.familiarity = quality.coerceIn(0, 5)
        word.nextReview = now + r.intervalDays * 24L * 3600 * 1000
        word.lastReviewedAt = now
        word.touch()
        dao.update(word)
    }
}
