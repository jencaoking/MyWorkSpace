package com.jencao.mywork.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jencao.mywork.data.local.entity.HealthRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthRecordDao {
    @Query("SELECT * FROM health_records WHERE is_deleted = 0 ORDER BY record_time DESC")
    fun observeAll(): Flow<List<HealthRecordEntity>>

    @Query("SELECT * FROM health_records WHERE is_deleted = 0 AND (:type IS NULL OR type = :type) ORDER BY record_time DESC")
    fun observeByType(type: String?): Flow<List<HealthRecordEntity>>

    @Query("SELECT * FROM health_records WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): HealthRecordEntity?

    @Query("SELECT * FROM health_records WHERE is_deleted = 0 AND reminder_time IS NOT NULL AND reminder_time > :now ORDER BY reminder_time ASC")
    suspend fun getUpcomingReminders(now: Long): List<HealthRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HealthRecordEntity)

    @Update
    suspend fun update(item: HealthRecordEntity)

    @Query("UPDATE health_records SET is_deleted = 1, last_modified = :ts, needs_sync = 1 WHERE id = :id")
    suspend fun softDelete(id: String, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM health_records WHERE needs_sync = 1 AND is_deleted = 0")
    suspend fun getPendingUploads(): List<HealthRecordEntity>

    @Query("SELECT * FROM health_records WHERE needs_sync = 1 AND is_deleted = 1")
    suspend fun getPendingDeletions(): List<HealthRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<HealthRecordEntity>)

    @Query("UPDATE health_records SET needs_sync = 0 WHERE id IN (:ids)")
    suspend fun clearUploadFlag(ids: List<String>)

    @Query("UPDATE health_records SET needs_sync = 0 WHERE id IN (:ids)")
    suspend fun clearDeleteFlag(ids: List<String>)

    @Query("UPDATE health_records SET needs_sync = 0 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("DELETE FROM health_records WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
