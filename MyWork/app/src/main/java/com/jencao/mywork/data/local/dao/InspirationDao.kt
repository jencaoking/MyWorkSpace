package com.jencao.mywork.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jencao.mywork.data.local.entity.InspirationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InspirationDao {
    @Query("SELECT * FROM inspiration_items WHERE is_deleted = 0 ORDER BY created_at DESC")
    fun observeAll(): Flow<List<InspirationEntity>>

    @Query("SELECT * FROM inspiration_items WHERE favorite = 1 AND is_deleted = 0 ORDER BY created_at DESC")
    fun observeFavorites(): Flow<List<InspirationEntity>>

    @Query("SELECT * FROM inspiration_items WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): InspirationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: InspirationEntity)

    @Update
    suspend fun update(item: InspirationEntity)

    @Query("UPDATE inspiration_items SET is_deleted = 1, last_modified = :ts, needs_sync = 1 WHERE id = :id")
    suspend fun softDelete(id: String, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM inspiration_items WHERE needs_sync = 1 AND is_deleted = 0")
    suspend fun getPendingUploads(): List<InspirationEntity>

    @Query("SELECT * FROM inspiration_items WHERE needs_sync = 1 AND is_deleted = 1")
    suspend fun getPendingDeletions(): List<InspirationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<InspirationEntity>)

    @Query("UPDATE inspiration_items SET needs_sync = 0 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("DELETE FROM inspiration_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("UPDATE inspiration_items SET favorite = :fav, last_modified = :ts, needs_sync = 1 WHERE id = :id")
    suspend fun setFavorite(id: String, fav: Boolean, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM inspiration_items WHERE is_deleted = 0 AND (content LIKE '%' || :kw || '%' OR author LIKE '%' || :kw || '%' OR tags LIKE '%' || :kw || '%') ORDER BY created_at DESC LIMIT 50")
    suspend fun search(kw: String): List<InspirationEntity>
}
