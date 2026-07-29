package com.jencao.mywork.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jencao.mywork.data.local.entity.TaskCheckinEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskCheckinDao {
    @Query(
        "SELECT * FROM task_checkins WHERE task_id = :taskId AND is_deleted = 0 " +
            "ORDER BY checkin_date DESC, checkin_time DESC"
    )
    fun observeByTask(taskId: String): Flow<List<TaskCheckinEntity>>

    @Query(
        "SELECT * FROM task_checkins WHERE checkin_date BETWEEN :start AND :end " +
            "AND is_deleted = 0 ORDER BY checkin_date ASC"
    )
    fun observeByDateRange(start: String, end: String): Flow<List<TaskCheckinEntity>>

    @Query(
        "SELECT * FROM task_checkins WHERE task_id = :taskId AND checkin_date = :date " +
            "AND is_deleted = 0 LIMIT 1"
    )
    suspend fun getByTaskAndDate(taskId: String, date: String): TaskCheckinEntity?

    @Query(
        "SELECT * FROM task_checkins WHERE task_id = :taskId AND is_deleted = 0"
    )
    suspend fun getAllByTask(taskId: String): List<TaskCheckinEntity>

    @Query(
        "SELECT COUNT(DISTINCT checkin_date) FROM task_checkins " +
            "WHERE task_id = :taskId AND checkin_date BETWEEN :start AND :end AND is_deleted = 0"
    )
    suspend fun countDistinctDaysInRange(taskId: String, start: String, end: String): Int

    @Query(
        "SELECT COUNT(DISTINCT checkin_date) FROM task_checkins " +
            "WHERE checkin_date BETWEEN :start AND :end AND is_deleted = 0"
    )
    suspend fun countDistinctDaysInRangeAll(start: String, end: String): Int

    @Query("SELECT COUNT(*) FROM task_checkins WHERE task_id = :taskId AND is_deleted = 0")
    suspend fun countByTask(taskId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(checkin: TaskCheckinEntity)

    @Query(
        "UPDATE task_checkins SET is_deleted = 1, last_modified = :ts, needs_sync = 1 " +
            "WHERE task_id = :taskId AND checkin_date = :date"
    )
    suspend fun softDeleteByTaskAndDate(taskId: String, date: String, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM task_checkins WHERE needs_sync = 1 AND is_deleted = 0")
    suspend fun getPendingUploads(): List<TaskCheckinEntity>

    @Query("SELECT * FROM task_checkins WHERE needs_sync = 1 AND is_deleted = 1")
    suspend fun getPendingDeletions(): List<TaskCheckinEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<TaskCheckinEntity>)
}
