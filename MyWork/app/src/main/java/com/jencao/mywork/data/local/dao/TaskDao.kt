package com.jencao.mywork.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jencao.mywork.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE is_deleted = 0 ORDER BY created_at DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE is_deleted = 0 AND status != 1 ORDER BY created_at DESC")
    fun observeActive(): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE is_deleted = 0 AND status != 1")
    fun observeActiveCount(): Flow<Int>

    @Query(
        "SELECT * FROM tasks WHERE is_deleted = 0 AND " +
            "(:categoryId = '' OR category_id = :categoryId) ORDER BY created_at DESC"
    )
    fun observeByCategoryOrAll(categoryId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id AND is_deleted = 0 LIMIT 1")
    suspend fun getById(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("UPDATE tasks SET is_deleted = 1, last_modified = :ts, needs_sync = 1 WHERE id = :id")
    suspend fun softDelete(id: String, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM tasks WHERE needs_sync = 1 AND is_deleted = 0")
    suspend fun getPendingUploads(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE needs_sync = 1 AND is_deleted = 1")
    suspend fun getPendingDeletions(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<TaskEntity>)

    @Query("UPDATE tasks SET needs_sync = 0 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("DELETE FROM tasks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("SELECT * FROM tasks WHERE is_deleted = 0 AND (title LIKE '%' || :kw || '%' OR content LIKE '%' || :kw || '%') ORDER BY updated_at DESC LIMIT 50")
    suspend fun search(kw: String): List<TaskEntity>

    /** 截止时间早于 before、仍待办的普通任务（排除长期目标），供每日归档使用 */
    @Query("SELECT * FROM tasks WHERE is_deleted = 0 AND status = 0 AND task_type != 2 AND due_date IS NOT NULL AND due_date < :before")
    suspend fun getOverdueOpen(before: Long): List<TaskEntity>
}
