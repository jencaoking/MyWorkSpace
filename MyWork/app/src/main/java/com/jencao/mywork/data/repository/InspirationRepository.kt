package com.jencao.mywork.data.repository

import com.jencao.mywork.data.local.dao.InspirationDao
import com.jencao.mywork.data.local.entity.InspirationEntity
import com.jencao.mywork.data.sync.Syncer
import com.jencao.mywork.data.util.touch
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InspirationRepository @Inject constructor(private val dao: InspirationDao) : Syncer<InspirationEntity> {
    override suspend fun getPendingUploads() = dao.getPendingUploads()
    override suspend fun getPendingDeletions() = dao.getPendingDeletions().map { it.id }
    override suspend fun mergeRemote(remote: List<InspirationEntity>) = dao.upsertAll(remote)
    override suspend fun markSynced(ids: List<String>) = dao.markSynced(ids)
    override suspend fun purgeDeleted(ids: List<String>) = dao.deleteByIds(ids)

    fun observeAll(): Flow<List<InspirationEntity>> = dao.observeAll()
    fun observeFavorites(): Flow<List<InspirationEntity>> = dao.observeFavorites()
    suspend fun insert(item: InspirationEntity) { item.touch(); dao.insert(item) }
    suspend fun update(item: InspirationEntity) { item.touch(); dao.update(item) }
    suspend fun softDelete(id: String) = dao.softDelete(id)
    suspend fun setFavorite(id: String, fav: Boolean) = dao.setFavorite(id, fav)
    suspend fun search(kw: String) = dao.search(kw)
}
