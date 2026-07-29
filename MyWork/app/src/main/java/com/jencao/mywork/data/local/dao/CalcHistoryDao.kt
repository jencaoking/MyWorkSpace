package com.jencao.mywork.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jencao.mywork.data.local.entity.CalcHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalcHistoryDao {
    @Query("SELECT * FROM calc_history WHERE is_deleted = 0 ORDER BY created_at DESC")
    fun observeAll(): Flow<List<CalcHistoryEntity>>

    @Query("SELECT * FROM calc_history WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): CalcHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CalcHistoryEntity)

    @Update
    suspend fun update(item: CalcHistoryEntity)

    @Query("UPDATE calc_history SET is_deleted = 1, last_modified = :ts, needs_sync = 1 WHERE id = :id")
    suspend fun softDelete(id: String, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM calc_history WHERE needs_sync = 1 AND is_deleted = 0")
    suspend fun getPendingUploads(): List<CalcHistoryEntity>

    @Query("SELECT * FROM calc_history WHERE needs_sync = 1 AND is_deleted = 1")
    suspend fun getPendingDeletions(): List<CalcHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CalcHistoryEntity>)

    @Query("UPDATE calc_history SET needs_sync = 0 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("DELETE FROM calc_history WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("SELECT * FROM calc_history WHERE is_deleted = 0 AND (expr LIKE '%' || :kw || '%' OR result LIKE '%' || :kw || '%') ORDER BY created_at DESC LIMIT 50")
    suspend fun search(kw: String): List<CalcHistoryEntity>
}
