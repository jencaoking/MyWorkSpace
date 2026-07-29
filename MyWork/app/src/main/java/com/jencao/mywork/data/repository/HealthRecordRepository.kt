package com.jencao.mywork.data.repository

import com.jencao.mywork.data.local.dao.HealthRecordDao
import com.jencao.mywork.data.local.entity.HealthRecordEntity
import com.jencao.mywork.data.sync.Syncer
import com.jencao.mywork.data.util.touch
import javax.inject.Inject
import javax.inject.Singleton

/** 健康记录仓库：CRUD + 同步标记。 */
@Singleton
class HealthRecordRepository @Inject constructor(
    private val dao: HealthRecordDao
) : Syncer<HealthRecordEntity> {
    fun observeAll() = dao.observeAll()

    /** 按类型（visit / medication / revisit / sign）筛选，null 表示全部。 */
    fun observeByType(type: String?) = dao.observeByType(type)

    suspend fun getById(id: String): HealthRecordEntity? = dao.getById(id)

    suspend fun create(
        type: String = "visit",
        value: Float = 0f,
        unit: String = "",
        recordTime: Long = System.currentTimeMillis(),
        note: String = "",
        reminderTime: Long? = null
    ): HealthRecordEntity {
        val item = HealthRecordEntity(
            type = type,
            value = value,
            unit = unit,
            recordTime = recordTime,
            note = note,
            reminderTime = reminderTime
        ).apply { touch() }
        dao.insert(item)
        return item
    }

    suspend fun getUpcomingReminders(now: Long = System.currentTimeMillis()): List<HealthRecordEntity> =
        dao.getUpcomingReminders(now)

    suspend fun upsert(item: HealthRecordEntity): HealthRecordEntity {
        item.touch()
        dao.insert(item)
        return item
    }

    suspend fun markDeleted(id: String) = dao.softDelete(id)

    override suspend fun getPendingUploads(): List<HealthRecordEntity> = dao.getPendingUploads()
    override suspend fun getPendingDeletions(): List<String> = dao.getPendingDeletions().map { it.id }

    override suspend fun mergeRemote(remote: List<HealthRecordEntity>) {
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
