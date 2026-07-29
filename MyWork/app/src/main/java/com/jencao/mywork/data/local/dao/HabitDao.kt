package com.jencao.mywork.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jencao.mywork.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE is_deleted = 0 ORDER BY title ASC")
    fun observeAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): HabitEntity?

    @Query("SELECT * FROM habits WHERE plan_id = :planId AND is_deleted = 0 ORDER BY title ASC")
    fun observeByPlan(planId: String): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HabitEntity)

    @Update
    suspend fun update(item: HabitEntity)

    @Query("UPDATE habits SET is_deleted = 1, last_modified = :ts, needs_sync = 1 WHERE id = :id")
    suspend fun softDelete(id: String, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM habits WHERE needs_sync = 1 AND is_deleted = 0")
    suspend fun getPendingUploads(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE needs_sync = 1 AND is_deleted = 1")
    suspend fun getPendingDeletions(): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<HabitEntity>)

    @Query("UPDATE habits SET needs_sync = 0 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("DELETE FROM habits WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
