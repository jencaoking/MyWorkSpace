package com.jencao.mywork.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jencao.mywork.data.local.entity.FlashcardDeckEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDeckDao {
    @Query("SELECT * FROM flashcard_decks WHERE is_deleted = 0 ORDER BY created_at DESC")
    fun observeAll(): Flow<List<FlashcardDeckEntity>>

    @Query("SELECT * FROM flashcard_decks WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): FlashcardDeckEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FlashcardDeckEntity)

    @Update
    suspend fun update(item: FlashcardDeckEntity)

    @Query("UPDATE flashcard_decks SET is_deleted = 1, last_modified = :ts, needs_sync = 1 WHERE id = :id")
    suspend fun softDelete(id: String, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM flashcard_decks WHERE needs_sync = 1 AND is_deleted = 0")
    suspend fun getPendingUploads(): List<FlashcardDeckEntity>

    @Query("SELECT * FROM flashcard_decks WHERE needs_sync = 1 AND is_deleted = 1")
    suspend fun getPendingDeletions(): List<FlashcardDeckEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FlashcardDeckEntity>)

    @Query("UPDATE flashcard_decks SET needs_sync = 0 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("DELETE FROM flashcard_decks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
