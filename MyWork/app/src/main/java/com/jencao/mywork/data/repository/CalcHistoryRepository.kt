package com.jencao.mywork.data.repository

import com.jencao.mywork.data.local.dao.CalcHistoryDao
import com.jencao.mywork.data.local.entity.CalcHistoryEntity
import com.jencao.mywork.data.sync.Syncer
import com.jencao.mywork.data.util.touch
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalcHistoryRepository @Inject constructor(private val dao: CalcHistoryDao) : Syncer<CalcHistoryEntity> {
    override suspend fun getPendingUploads() = dao.getPendingUploads()
    override suspend fun getPendingDeletions() = dao.getPendingDeletions().map { it.id }
    override suspend fun mergeRemote(remote: List<CalcHistoryEntity>) = dao.upsertAll(remote)
    override suspend fun markSynced(ids: List<String>) = dao.markSynced(ids)
    override suspend fun purgeDeleted(ids: List<String>) = dao.deleteByIds(ids)

    fun observeAll(): Flow<List<CalcHistoryEntity>> = dao.observeAll()
    suspend fun getById(id: String) = dao.getById(id)
    suspend fun insert(item: CalcHistoryEntity) { item.touch(); dao.insert(item) }
    suspend fun softDelete(id: String) = dao.softDelete(id)
    suspend fun search(kw: String) = dao.search(kw)
}
