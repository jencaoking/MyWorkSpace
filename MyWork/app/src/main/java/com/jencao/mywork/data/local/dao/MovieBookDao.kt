package com.jencao.mywork.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jencao.mywork.data.local.entity.MovieBookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieBookDao {
    @Query("SELECT * FROM movie_books WHERE is_deleted = 0 ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<MovieBookEntity>>

    @Query("SELECT * FROM movie_books WHERE is_deleted = 0 AND (:type IS NULL OR type = :type) ORDER BY updated_at DESC")
    fun observeByType(type: String?): Flow<List<MovieBookEntity>>

    @Query("SELECT * FROM movie_books WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): MovieBookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MovieBookEntity)

    @Update
    suspend fun update(item: MovieBookEntity)

    @Query("UPDATE movie_books SET is_deleted = 1, last_modified = :ts, needs_sync = 1 WHERE id = :id")
    suspend fun softDelete(id: String, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM movie_books WHERE needs_sync = 1 AND is_deleted = 0")
    suspend fun getPendingUploads(): List<MovieBookEntity>

    @Query("SELECT * FROM movie_books WHERE needs_sync = 1 AND is_deleted = 1")
    suspend fun getPendingDeletions(): List<MovieBookEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MovieBookEntity>)

    @Query("UPDATE movie_books SET needs_sync = 0 WHERE id IN (:ids)")
    suspend fun clearUploadFlag(ids: List<String>)

    @Query("UPDATE movie_books SET needs_sync = 0 WHERE id IN (:ids)")
    suspend fun clearDeleteFlag(ids: List<String>)
}
