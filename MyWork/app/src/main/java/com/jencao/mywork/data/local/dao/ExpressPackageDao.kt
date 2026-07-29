package com.jencao.mywork.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jencao.mywork.data.local.entity.ExpressPackageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpressPackageDao {
    @Query("SELECT * FROM express_packages WHERE is_deleted = 0 ORDER BY created_at DESC")
    fun observeAll(): Flow<List<ExpressPackageEntity>>

    @Query("SELECT * FROM express_packages WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): ExpressPackageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ExpressPackageEntity)

    @Update
    suspend fun update(item: ExpressPackageEntity)

    @Query("UPDATE express_packages SET is_deleted = 1, last_modified = :ts, needs_sync = 1 WHERE id = :id")
    suspend fun softDelete(id: String, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM express_packages WHERE needs_sync = 1 AND is_deleted = 0")
    suspend fun getPendingUploads(): List<ExpressPackageEntity>

    @Query("SELECT * FROM express_packages WHERE needs_sync = 1 AND is_deleted = 1")
    suspend fun getPendingDeletions(): List<ExpressPackageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ExpressPackageEntity>)

    @Query("UPDATE express_packages SET needs_sync = 0 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("DELETE FROM express_packages WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("SELECT * FROM express_packages WHERE is_deleted = 0 AND (tracking_no LIKE '%' || :kw || '%' OR company_name LIKE '%' || :kw || '%' OR goods LIKE '%' || :kw || '%') ORDER BY created_at DESC LIMIT 50")
    suspend fun search(kw: String): List<ExpressPackageEntity>
}
