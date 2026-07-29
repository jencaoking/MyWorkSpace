package com.jencao.mywork.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jencao.mywork.data.local.entity.HabitPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitPlanDao {
    @Query("SELECT * FROM habit_plans WHERE is_deleted = 0 ORDER BY start_date DESC")
    fun observeAll(): Flow<List<HabitPlanEntity>>

    @Query("SELECT * FROM habit_plans WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): HabitPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HabitPlanEntity)

    @Update
    suspend fun update(item: HabitPlanEntity)

    @Query("UPDATE habit_plans SET is_deleted = 1, last_modified = :ts, needs_sync = 1 WHERE id = :id")
    suspend fun softDelete(id: String, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM habit_plans WHERE needs_sync = 1 AND is_deleted = 0")
    suspend fun getPendingUploads(): List<HabitPlanEntity>

    @Query("SELECT * FROM habit_plans WHERE needs_sync = 1 AND is_deleted = 1")
    suspend fun getPendingDeletions(): List<HabitPlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<HabitPlanEntity>)

    @Query("UPDATE habit_plans SET needs_sync = 0 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("DELETE FROM habit_plans WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
