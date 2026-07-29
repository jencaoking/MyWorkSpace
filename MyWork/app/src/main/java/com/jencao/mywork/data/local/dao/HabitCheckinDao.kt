package com.jencao.mywork.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jencao.mywork.data.local.entity.HabitCheckinEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitCheckinDao {
    @Query("SELECT * FROM habit_checkins WHERE is_deleted = 0 ORDER BY date DESC")
    fun observeAll(): Flow<List<HabitCheckinEntity>>

    @Query("SELECT * FROM habit_checkins WHERE habit_id = :habitId AND is_deleted = 0 ORDER BY date DESC")
    fun observeByHabit(habitId: String): Flow<List<HabitCheckinEntity>>

    @Query("SELECT * FROM habit_checkins WHERE habit_id = :habitId AND date = :date AND is_deleted = 0 LIMIT 1")
    suspend fun getByHabitAndDate(habitId: String, date: String): HabitCheckinEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HabitCheckinEntity)

    @Update
    suspend fun update(item: HabitCheckinEntity)

    @Query("UPDATE habit_checkins SET is_deleted = 1, last_modified = :ts, needs_sync = 1 WHERE id = :id")
    suspend fun softDelete(id: String, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM habit_checkins WHERE needs_sync = 1 AND is_deleted = 0")
    suspend fun getPendingUploads(): List<HabitCheckinEntity>

    @Query("SELECT * FROM habit_checkins WHERE needs_sync = 1 AND is_deleted = 1")
    suspend fun getPendingDeletions(): List<HabitCheckinEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<HabitCheckinEntity>)

    @Query("UPDATE habit_checkins SET needs_sync = 0 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("DELETE FROM habit_checkins WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
