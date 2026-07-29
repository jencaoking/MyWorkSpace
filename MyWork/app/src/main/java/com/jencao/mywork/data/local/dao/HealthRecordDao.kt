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
}
