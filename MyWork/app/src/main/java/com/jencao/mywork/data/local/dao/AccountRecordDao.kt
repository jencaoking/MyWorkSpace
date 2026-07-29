package com.jencao.mywork.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jencao.mywork.data.local.entity.AccountRecordEntity
import com.jencao.mywork.data.model.MonthlySummaryRaw
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountRecordDao {
    @Query("SELECT * FROM account_records WHERE is_deleted = 0 ORDER BY record_date DESC")
    fun observeAll(): Flow<List<AccountRecordEntity>>

    @Query("SELECT * FROM account_records WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): AccountRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: AccountRecordEntity)

    @Update
    suspend fun update(item: AccountRecordEntity)

    @Query("UPDATE account_records SET is_deleted = 1, last_modified = :ts, needs_sync = 1 WHERE id = :id")
    suspend fun softDelete(id: String, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM account_records WHERE needs_sync = 1 AND is_deleted = 0")
    suspend fun getPendingUploads(): List<AccountRecordEntity>

    @Query("SELECT * FROM account_records WHERE needs_sync = 1 AND is_deleted = 1")
    suspend fun getPendingDeletions(): List<AccountRecordEntity>

    @Query(
        "SELECT " +
            "COALESCE(SUM(CASE WHEN type = 'income' THEN amount ELSE 0 END), 0) AS income, " +
            "COALESCE(SUM(CASE WHEN type = 'expense' THEN amount ELSE 0 END), 0) AS expense " +
            "FROM account_records WHERE record_date BETWEEN :start AND :end AND is_deleted = 0"
    )
    suspend fun sumByTypeInRange(start: Long, end: Long): MonthlySummaryRaw

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<AccountRecordEntity>)

    @Query("UPDATE account_records SET needs_sync = 0 WHERE id IN (:ids)")
    suspend fun clearUploadFlag(ids: List<String>)

    @Query("UPDATE account_records SET needs_sync = 0 WHERE id IN (:ids)")
    suspend fun clearDeleteFlag(ids: List<String>)

    @Query("UPDATE account_records SET needs_sync = 0 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("DELETE FROM account_records WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
