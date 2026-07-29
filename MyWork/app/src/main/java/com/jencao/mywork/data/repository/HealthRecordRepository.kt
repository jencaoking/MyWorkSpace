package com.jencao.mywork.data.repository

import com.jencao.mywork.data.local.dao.HealthRecordDao
import com.jencao.mywork.data.local.entity.HealthRecordEntity
import javax.inject.Inject
import javax.inject.Singleton

/** 健康记录仓库：CRUD + 同步标记。 */
@Singleton
class HealthRecordRepository @Inject constructor(
    private val dao: HealthRecordDao
) {
    fun observeAll() = dao.observeAll()

    /** 按类型（visit / medication / revisit / sign）筛选，null 表示全部。 */
    fun observeByType(type: String?) = dao.observeByType(type)

    suspend fun getById(id: String): HealthRecordEntity? = dao.getById(id)

    suspend fun create(
        type: String = "visit",
        value: Float = 0f,
        unit: String = "",
        recordTime: Long = System.currentTimeMillis(),
        note: String = ""
    ): HealthRecordEntity {
        val item = HealthRecordEntity(
            type = type,
            value = value,
            unit = unit,
            recordTime = recordTime,
            note = note
        ).apply { touch() }
        dao.insert(item)
        return item
    }

    suspend fun upsert(item: HealthRecordEntity): HealthRecordEntity {
        item.touch()
        dao.insert(item)
        return item
    }

    suspend fun markDeleted(id: String) = dao.softDelete(id)

    suspend fun pendingUploads(): List<HealthRecordEntity> = dao.pendingUploads()
    suspend fun pendingDeletions(): List<String> = dao.pendingDeletions()

    suspend fun clearSyncFlags(ids: List<String>, deletedIds: List<String>) {
        dao.clearUploadFlag(ids)
        dao.clearDeleteFlag(deletedIds)
    }
}
