package com.jencao.mywork.data.repository

import com.jencao.mywork.data.local.dao.AccountRecordDao
import com.jencao.mywork.data.local.entity.AccountRecordEntity
import com.jencao.mywork.data.sync.Syncer
import com.jencao.mywork.data.util.touch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRecordRepository @Inject constructor(
    private val dao: AccountRecordDao
) : Syncer<AccountRecordEntity> {
    fun observeAll() = dao.observeAll()
    suspend fun getById(id: String): AccountRecordEntity? = dao.getById(id)

    suspend fun create(
        type: String,
        category: String,
        amount: Float,
        currency: String,
        recordDate: Long,
        note: String
    ): AccountRecordEntity {
        val item = AccountRecordEntity(
            type = type,
            category = category,
            amount = amount,
            currency = currency,
            recordDate = recordDate,
            note = note
        )
        item.touch()
        dao.insert(item)
        return item
    }

    suspend fun upsert(item: AccountRecordEntity): AccountRecordEntity {
        item.touch()
        dao.update(item)
        return item
    }

    suspend fun markDeleted(id: String) = dao.softDelete(id)

    override suspend fun getPendingUploads(): List<AccountRecordEntity> = dao.getPendingUploads()
    override suspend fun getPendingDeletions(): List<String> = dao.getPendingDeletions().map { it.id }

    override suspend fun mergeRemote(remote: List<AccountRecordEntity>) {
        val toUpsert = remote.filter { r ->
            val local = dao.getById(r.id)
            local == null || local.lastModified <= r.lastModified
        }.onEach { it.needsSync = false }
        if (toUpsert.isNotEmpty()) dao.upsertAll(toUpsert)
    }

    override suspend fun markSynced(ids: List<String>) {
        if (ids.isNotEmpty()) dao.markSynced(ids)
    }

    override suspend fun purgeDeleted(ids: List<String>) {
        if (ids.isNotEmpty()) dao.deleteByIds(ids)
    }
}
