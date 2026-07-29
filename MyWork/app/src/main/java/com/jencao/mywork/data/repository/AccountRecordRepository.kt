package com.jencao.mywork.data.repository

import com.jencao.mywork.data.local.dao.AccountRecordDao
import com.jencao.mywork.data.local.entity.AccountRecordEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRecordRepository @Inject constructor(
    private val dao: AccountRecordDao
) {
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

    suspend fun pendingUploads() = dao.getPendingUploads()
    suspend fun pendingDeletions() = dao.getPendingDeletions()

    suspend fun clearSyncFlags(ids: List<String>, deletedIds: List<String>) {
        if (ids.isNotEmpty()) dao.clearUploadFlag(ids)
        if (deletedIds.isNotEmpty()) dao.clearDeleteFlag(deletedIds)
    }
}
