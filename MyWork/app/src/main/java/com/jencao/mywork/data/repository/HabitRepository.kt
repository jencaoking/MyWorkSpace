package com.jencao.mywork.data.repository

import com.jencao.mywork.data.local.dao.HabitDao
import com.jencao.mywork.data.local.entity.HabitEntity
import com.jencao.mywork.data.sync.Syncer
import com.jencao.mywork.data.util.touch
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepository @Inject constructor(private val dao: HabitDao) : Syncer<HabitEntity> {
    override suspend fun getPendingUploads() = dao.getPendingUploads()
    override suspend fun getPendingDeletions() = dao.getPendingDeletions().map { it.id }
    override suspend fun mergeRemote(remote: List<HabitEntity>) = dao.upsertAll(remote)
    override suspend fun markSynced(ids: List<String>) = dao.markSynced(ids)
    override suspend fun purgeDeleted(ids: List<String>) = dao.deleteByIds(ids)

    fun observeAll(): Flow<List<HabitEntity>> = dao.observeAll()
    fun observeByPlan(planId: String): Flow<List<HabitEntity>> = dao.observeByPlan(planId)
    suspend fun insert(item: HabitEntity) { item.touch(); dao.insert(item) }
    suspend fun update(item: HabitEntity) { item.touch(); dao.update(item) }
    suspend fun softDelete(id: String) = dao.softDelete(id)
}
