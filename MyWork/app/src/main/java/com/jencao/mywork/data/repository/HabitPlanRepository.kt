package com.jencao.mywork.data.repository

import com.jencao.mywork.data.local.dao.HabitPlanDao
import com.jencao.mywork.data.local.entity.HabitPlanEntity
import com.jencao.mywork.data.sync.Syncer
import com.jencao.mywork.data.util.touch
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitPlanRepository @Inject constructor(private val dao: HabitPlanDao) : Syncer<HabitPlanEntity> {
    override suspend fun getPendingUploads() = dao.getPendingUploads()
    override suspend fun getPendingDeletions() = dao.getPendingDeletions().map { it.id }
    override suspend fun mergeRemote(remote: List<HabitPlanEntity>) = dao.upsertAll(remote)
    override suspend fun markSynced(ids: List<String>) = dao.markSynced(ids)
    override suspend fun purgeDeleted(ids: List<String>) = dao.deleteByIds(ids)

    fun observeAll(): Flow<List<HabitPlanEntity>> = dao.observeAll()
    suspend fun getById(id: String) = dao.getById(id)
    suspend fun insert(item: HabitPlanEntity) { item.touch(); dao.insert(item) }
    suspend fun update(item: HabitPlanEntity) { item.touch(); dao.update(item) }
    suspend fun softDelete(id: String) = dao.softDelete(id)
}
