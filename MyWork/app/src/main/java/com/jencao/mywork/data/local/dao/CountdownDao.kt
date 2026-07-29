package com.jencao.mywork.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jencao.mywork.data.local.entity.CountdownEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CountdownDao {
    @Query("SELECT * FROM countdown_events WHERE is_deleted = 0 ORDER BY target_time ASC")
    fun observeAll(): Flow<List<CountdownEntity>>

    @Query("SELECT * FROM countdown_events WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): CountdownEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CountdownEntity)

    @Update
    suspend fun update(item: CountdownEntity)

    @Query("UPDATE countdown_events SET is_deleted = 1, last_modified = :ts, needs_sync = 1 WHERE id = :id")
    suspend fun softDelete(id: String, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM countdown_events WHERE needs_sync = 1 AND is_deleted = 0")
    suspend fun getPendingUploads(): List<CountdownEntity>

    @Query("SELECT * FROM countdown_events WHERE needs_sync = 1 AND is_deleted = 1")
    suspend fun getPendingDeletions(): List<CountdownEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CountdownEntity>)

    @Query("UPDATE countdown_events SET needs_sync = 0 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("DELETE FROM countdown_events WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("SELECT * FROM countdown_events WHERE is_deleted = 0 AND target_time >= :from ORDER BY target_time ASC")
    fun observeUpcoming(from: Long = System.currentTimeMillis()): Flow<List<CountdownEntity>>
}
