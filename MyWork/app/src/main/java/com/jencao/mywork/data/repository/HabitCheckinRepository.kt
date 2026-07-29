package com.jencao.mywork.data.repository

import com.jencao.mywork.data.local.dao.HabitCheckinDao
import com.jencao.mywork.data.local.entity.HabitCheckinEntity
import com.jencao.mywork.data.sync.Syncer
import com.jencao.mywork.data.util.touch
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitCheckinRepository @Inject constructor(private val dao: HabitCheckinDao) : Syncer<HabitCheckinEntity> {
    override suspend fun getPendingUploads() = dao.getPendingUploads()
    override suspend fun getPendingDeletions() = dao.getPendingDeletions().map { it.id }
    override suspend fun mergeRemote(remote: List<HabitCheckinEntity>) = dao.upsertAll(remote)
    override suspend fun markSynced(ids: List<String>) = dao.markSynced(ids)
    override suspend fun purgeDeleted(ids: List<String>) = dao.deleteByIds(ids)

    fun observeByHabit(habitId: String): Flow<List<HabitCheckinEntity>> = dao.observeByHabit(habitId)

    /** 当天是否已打卡（按本地时区日期） */
    suspend fun isCheckedToday(habitId: String, date: String = today()): Boolean =
        dao.getByHabitAndDate(habitId, date) != null

    /** 打卡；同一天重复打卡幂等（INSERT OR REPLACE） */
    suspend fun checkIn(habitId: String, date: String = today()) {
        val item = HabitCheckinEntity(habitId = habitId, date = date)
        item.touch()
        dao.insert(item)
    }

    suspend fun uncheck(habitId: String, date: String = today()) {
        dao.getByHabitAndDate(habitId, date)?.let { dao.softDelete(it.id) }
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}
