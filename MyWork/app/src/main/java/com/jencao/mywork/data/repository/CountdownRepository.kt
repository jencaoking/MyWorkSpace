package com.jencao.mywork.data.repository

import com.jencao.mywork.data.local.dao.CountdownDao
import com.jencao.mywork.data.local.entity.CountdownEntity
import com.jencao.mywork.data.sync.Syncer
import com.jencao.mywork.data.util.touch
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CountdownRepository @Inject constructor(private val dao: CountdownDao) : Syncer<CountdownEntity> {
    override suspend fun getPendingUploads() = dao.getPendingUploads()
    override suspend fun getPendingDeletions() = dao.getPendingDeletions().map { it.id }
    override suspend fun mergeRemote(remote: List<CountdownEntity>) = dao.upsertAll(remote)
    override suspend fun markSynced(ids: List<String>) = dao.markSynced(ids)
    override suspend fun purgeDeleted(ids: List<String>) = dao.deleteByIds(ids)

    fun observeAll(): Flow<List<CountdownEntity>> = dao.observeAll()
    fun observeUpcoming(from: Long = System.currentTimeMillis()): Flow<List<CountdownEntity>> = dao.observeUpcoming(from)
    suspend fun insert(item: CountdownEntity) { item.touch(); dao.insert(item) }
    suspend fun update(item: CountdownEntity) { item.touch(); dao.update(item) }
    suspend fun softDelete(id: String) = dao.softDelete(id)
}
