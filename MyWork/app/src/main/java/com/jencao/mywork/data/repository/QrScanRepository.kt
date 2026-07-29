package com.jencao.mywork.data.repository

import com.jencao.mywork.data.local.dao.QrScanDao
import com.jencao.mywork.data.local.entity.QrScanEntity
import com.jencao.mywork.data.sync.Syncer
import com.jencao.mywork.data.util.touch
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QrScanRepository @Inject constructor(
    private val dao: QrScanDao
) : Syncer<QrScanEntity> {

    fun observeRecent(): Flow<List<QrScanEntity>> = dao.observeRecent()

    fun observeAll(): Flow<List<QrScanEntity>> = dao.observeAll()

    suspend fun insert(item: QrScanEntity) {
        item.touch()
        dao.insert(item)
    }

    suspend fun softDelete(id: String) = dao.softDelete(id)

    suspend fun clearAll() = dao.clearAll()

    suspend fun search(kw: String) = dao.search(kw)

    /** 兼容旧调用：通过内容查重，避免重复写入完全相同的扫码结果。 */
    suspend fun upsertAll(items: List<QrScanEntity>) = dao.upsertAll(items)

    override suspend fun getPendingUploads(): List<QrScanEntity> = dao.getPendingUploads()

    override suspend fun getPendingDeletions(): List<String> = dao.getPendingDeletionIds()

    override suspend fun mergeRemote(remote: List<QrScanEntity>) = dao.mergeRemote(remote)

    override suspend fun markSynced(ids: List<String>) = dao.markSynced(ids)

    override suspend fun purgeDeleted(ids: List<String>) = dao.purgeDeleted(ids)
}
