package com.jencao.mywork.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jencao.mywork.data.local.entity.DailyPendingLogEntity
import kotlinx.coroutines.flow.Flow

/** 每日处置分布统计行 */
data class DispositionCount(
    val disposition: String,
    val cnt: Int
)

/** 按日数量统计行 */
data class DateCount(
    val logDate: String,
    val cnt: Int
)

@Dao
interface DailyPendingLogDao {
    @Query("SELECT * FROM daily_pending_log WHERE is_deleted = 0 ORDER BY log_date DESC, priority ASC")
    fun observeAll(): Flow<List<DailyPendingLogEntity>>

    @Query("SELECT * FROM daily_pending_log WHERE is_deleted = 0 AND disposition = 'pending' ORDER BY log_date DESC, priority ASC")
    fun observePending(): Flow<List<DailyPendingLogEntity>>

    @Query("SELECT COUNT(*) FROM daily_pending_log WHERE is_deleted = 0 AND disposition = 'pending'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM daily_pending_log WHERE is_deleted = 0 AND disposition = 'pending'")
    suspend fun getPendingCount(): Int

    @Query("SELECT * FROM daily_pending_log WHERE is_deleted = 0 AND disposition != 'pending' AND disposed_at >= :dayStart ORDER BY disposed_at DESC")
    fun observeDisposedSince(dayStart: Long): Flow<List<DailyPendingLogEntity>>

    @Query("SELECT * FROM daily_pending_log WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): DailyPendingLogEntity?

    @Query("SELECT id FROM daily_pending_log WHERE task_id = :taskId AND log_date = :logDate LIMIT 1")
    suspend fun findIdByTaskAndDate(taskId: String, logDate: String): String?

    /** 归档去重：任务已有 pending 归档时不重复产生新一天的记录 */
    @Query("SELECT COUNT(*) FROM daily_pending_log WHERE task_id = :taskId AND disposition = 'pending' AND is_deleted = 0")
    suspend fun countPendingByTask(taskId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(item: DailyPendingLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DailyPendingLogEntity)

    @Update
    suspend fun update(item: DailyPendingLogEntity)

    // ---- 周回顾统计（近 7 天） ----
    @Query("SELECT disposition, COUNT(*) AS cnt FROM daily_pending_log WHERE is_deleted = 0 AND log_date >= :startDate GROUP BY disposition")
    suspend fun countByDispositionSince(startDate: String): List<DispositionCount>

    @Query("SELECT log_date AS logDate, COUNT(*) AS cnt FROM daily_pending_log WHERE is_deleted = 0 AND log_date >= :startDate GROUP BY log_date ORDER BY log_date ASC")
    suspend fun countByDateSince(startDate: String): List<DateCount>

    // ---- 同步接口（与其它模块一致） ----
    @Query("SELECT * FROM daily_pending_log WHERE needs_sync = 1 AND is_deleted = 0")
    suspend fun getPendingUploads(): List<DailyPendingLogEntity>

    @Query("SELECT * FROM daily_pending_log WHERE needs_sync = 1 AND is_deleted = 1")
    suspend fun getPendingDeletions(): List<DailyPendingLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<DailyPendingLogEntity>)

    @Query("UPDATE daily_pending_log SET needs_sync = 0 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("DELETE FROM daily_pending_log WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
