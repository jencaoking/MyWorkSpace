package com.jencao.mywork.data.repository

import com.jencao.mywork.data.local.dao.FlashcardDeckDao
import com.jencao.mywork.data.local.entity.FlashcardDeckEntity
import com.jencao.mywork.data.sync.Syncer
import com.jencao.mywork.data.util.touch
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlashcardDeckRepository @Inject constructor(private val dao: FlashcardDeckDao) : Syncer<FlashcardDeckEntity> {
    override suspend fun getPendingUploads() = dao.getPendingUploads()
    override suspend fun getPendingDeletions() = dao.getPendingDeletions().map { it.id }
    override suspend fun mergeRemote(remote: List<FlashcardDeckEntity>) = dao.upsertAll(remote)
    override suspend fun markSynced(ids: List<String>) = dao.markSynced(ids)
    override suspend fun purgeDeleted(ids: List<String>) = dao.deleteByIds(ids)

    fun observeAll(): Flow<List<FlashcardDeckEntity>> = dao.observeAll()
    suspend fun getById(id: String) = dao.getById(id)
    suspend fun insert(item: FlashcardDeckEntity) { item.touch(); dao.insert(item) }
    suspend fun update(item: FlashcardDeckEntity) { item.touch(); dao.update(item) }
    suspend fun softDelete(id: String) = dao.softDelete(id)
}
