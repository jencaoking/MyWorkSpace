package com.jencao.mywork.data.repository

import com.jencao.mywork.data.local.dao.SportRecordDao
import com.jencao.mywork.data.local.entity.SportRecordEntity
import com.jencao.mywork.data.sync.Syncer
import com.jencao.mywork.data.util.touch
import javax.inject.Inject
import javax.inject.Singleton

/** 运动记录仓库：CRUD + 同步标记。 */
@Singleton
class SportRecordRepository @Inject constructor(
    private val dao: SportRecordDao
) : Syncer<SportRecordEntity> {
    fun observeAll() = dao.observeAll()

    suspend fun getById(id: String): SportRecordEntity? = dao.getById(id)

    /** 新建：构建实体并写入（自动标记 needsSync）。 */
    suspend fun create(
        type: String = "其他",
        durationMin: Int = 0,
        distanceKm: Float? = null,
        calories: Int? = null,
        steps: Int? = null,
        recordDate: Long = System.currentTimeMillis(),
        note: String = ""
    ): SportRecordEntity {
        val item = SportRecordEntity(
            type = type,
            durationMin = durationMin,
            distanceKm = distanceKm,
            calories = calories,
            steps = steps,
            recordDate = recordDate,
            note = note
        ).apply { touch() }
        dao.insert(item)
        return item
    }

    /** 保存（新增或更新）：写入前刷新同步标记。 */
    suspend fun upsert(item: SportRecordEntity): SportRecordEntity {
        item.touch()
        dao.insert(item)
        return item
    }

    /** 软删除：标记 isDeleted 而非物理删除，便于后续同步。 */
    suspend fun markDeleted(id: String) = dao.softDelete(id)

    override suspend fun getPendingUploads(): List<SportRecordEntity> = dao.getPendingUploads()
    override suspend fun getPendingDeletions(): List<String> = dao.getPendingDeletions().map { it.id }

    override suspend fun mergeRemote(remote: List<SportRecordEntity>) {
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
