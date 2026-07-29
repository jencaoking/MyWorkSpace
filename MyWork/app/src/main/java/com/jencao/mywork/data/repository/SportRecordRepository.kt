package com.jencao.mywork.data.repository

import com.jencao.mywork.data.local.dao.SportRecordDao
import com.jencao.mywork.data.local.entity.SportRecordEntity
import com.jencao.mywork.data.util.touch
import javax.inject.Inject
import javax.inject.Singleton

/** 运动记录仓库：CRUD + 同步标记。 */
@Singleton
class SportRecordRepository @Inject constructor(
    private val dao: SportRecordDao
) {
    fun observeAll() = dao.observeAll()

    suspend fun getById(id: String): SportRecordEntity? = dao.getById(id)

    /** 新建：构建实体并写入（自动标记 needsSync）。 */
    suspend fun create(
        type: String = "其他",
        durationMin: Int = 0,
        distanceKm: Float? = null,
        calories: Int? = null,
        recordDate: Long = System.currentTimeMillis(),
        note: String = ""
    ): SportRecordEntity {
        val item = SportRecordEntity(
            type = type,
            durationMin = durationMin,
            distanceKm = distanceKm,
            calories = calories,
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

    /** 待上传的本地变更（含新增/修改）。 */
    suspend fun pendingUploads(): List<SportRecordEntity> = dao.getPendingUploads()

    /** 待删除的 id 列表。 */
    suspend fun pendingDeletions(): List<String> = dao.getPendingDeletions().map { it.id }

    /** 同步完成后清空标记。 */
    suspend fun clearSyncFlags(ids: List<String>, deletedIds: List<String>) {
        dao.clearUploadFlag(ids)
        dao.clearDeleteFlag(deletedIds)
    }
}
