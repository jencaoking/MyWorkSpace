package com.jencao.mywork.data.repository

import com.jencao.mywork.data.local.dao.QrScanDao
import com.jencao.mywork.data.local.entity.QrScanEntity
import com.jencao.mywork.data.sync.Syncer
import com.jencao.mywork.data.util.touch
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QrScanRepository @Inject constructor(private val dao: QrScanDao) : Syncer<QrScanEntity> {
    override suspend fun getPendingUploads() = dao.getPendingUploads()
    override suspend fun getPendingDeletions() = dao.getPendingDeletions().map { it.id }
    override suspend fun mergeRemote(remote: List<QrScanEntity>) = dao.upsertAll(remote)
    override suspend fun markSynced(ids: List<String>) = dao.markSynced(ids)
    override suspend fun purgeDeleted(ids: List<String>) = dao.deleteByIds(ids)

    fun observeAll(): Flow<List<QrScanEntity>> = dao.observeAll()
    suspend fun insert(item: QrScanEntity) { item.touch(); dao.insert(item) }
    suspend fun softDelete(id: String) = dao.softDelete(id)
    suspend fun search(kw: String) = dao.search(kw)
}
