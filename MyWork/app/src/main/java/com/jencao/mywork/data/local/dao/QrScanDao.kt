package com.jencao.mywork.data.local.dao

import androidx.room.*
import com.jencao.mywork.data.local.entity.QrScanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QrScanDao {
    @Query("SELECT * FROM qr_scan_history WHERE is_deleted = 0 ORDER BY scanned_at DESC, created_at DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<QrScanEntity>>

    @Query("SELECT * FROM qr_scan_history WHERE is_deleted = 0 ORDER BY created_at DESC")
    fun observeAll(): Flow<List<QrScanEntity>>

    @Upsert
    suspend fun upsertAll(items: List<QrScanEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: QrScanEntity)

    @Query("UPDATE qr_scan_history SET is_deleted = 1, last_modified = :ts, needs_sync = 1 WHERE id = :id")
    suspend fun softDelete(id: String, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM qr_scan_history WHERE is_deleted = 0 AND needs_sync = 1")
    suspend fun getPendingUploads(): List<QrScanEntity>

    @Query("SELECT id FROM qr_scan_history WHERE is_deleted = 1 AND needs_sync = 1")
    suspend fun getPendingDeletionIds(): List<String>

    @Query("UPDATE qr_scan_history SET needs_sync = 0 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("DELETE FROM qr_scan_history WHERE id IN (:ids)")
    suspend fun purgeDeleted(ids: List<String>)

    @Transaction
    suspend fun mergeRemote(items: List<QrScanEntity>) {
        val local = observeAllOnce()
        val merged = items.map { remote ->
            local.firstOrNull { it.id == remote.id }?.let { old ->
                if (remote.last_modified > old.last_modified) remote else old
            } ?: remote
        }
        upsertAll(merged)
    }

    @Query("SELECT * FROM qr_scan_history")
    suspend fun observeAllOnce(): List<QrScanEntity>

    @Query("SELECT * FROM qr_scan_history WHERE id = :id")
    suspend fun getById(id: String): QrScanEntity?

    @Query("SELECT * FROM qr_scan_history WHERE is_deleted = 0 AND (content LIKE '%' || :kw || '%' OR note LIKE '%' || :kw || '%') ORDER BY created_at DESC LIMIT 50")
    suspend fun search(kw: String): List<QrScanEntity>

    @Query("DELETE FROM qr_scan_history")
    suspend fun clearAll()
}
