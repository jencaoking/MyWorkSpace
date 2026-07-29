package com.jencao.mywork.data.repository

import com.jencao.mywork.data.local.dao.FlashcardDao
import com.jencao.mywork.data.local.entity.FlashcardEntity
import com.jencao.mywork.data.sync.Syncer
import com.jencao.mywork.data.util.touch
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlashcardRepository @Inject constructor(private val dao: FlashcardDao) : Syncer<FlashcardEntity> {
    override suspend fun getPendingUploads() = dao.getPendingUploads()
    override suspend fun getPendingDeletions() = dao.getPendingDeletions().map { it.id }
    override suspend fun mergeRemote(remote: List<FlashcardEntity>) = dao.upsertAll(remote)
    override suspend fun markSynced(ids: List<String>) = dao.markSynced(ids)
    override suspend fun purgeDeleted(ids: List<String>) = dao.deleteByIds(ids)

    fun observeAll(): Flow<List<FlashcardEntity>> = dao.observeAll()
    fun observeByDeck(deckId: String): Flow<List<FlashcardEntity>> = dao.observeByDeck(deckId)
    suspend fun getDue(deckId: String, now: Long = System.currentTimeMillis()) = dao.getDue(deckId, now)
    suspend fun getById(id: String) = dao.getById(id)
    suspend fun insert(item: FlashcardEntity) { item.touch(); dao.insert(item) }
    suspend fun update(item: FlashcardEntity) { item.touch(); dao.update(item) }
    suspend fun softDelete(id: String) = dao.softDelete(id)

    /** SM-2 间隔重复：quality 0..5（<3 视为遗忘，重置间隔） */
    suspend fun review(card: FlashcardEntity, quality: Int) {
        val now = System.currentTimeMillis()
        var interval = card.intervalDays
        var ease = card.ease
        if (quality < 3) {
            interval = 1
            ease = (ease - 0.2f).coerceAtLeast(1.3f)
        } else {
            ease = (ease + 0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f)).coerceAtLeast(1.3f)
            interval = when (interval) {
                0 -> 1
                1 -> 6
                else -> (interval * ease).toInt().coerceAtLeast(1)
            }
        }
        card.intervalDays = interval
        card.ease = ease
        card.nextReview = now + interval * 86_400_000L
        card.touch()
        dao.update(card)
    }
}
