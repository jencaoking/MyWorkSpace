package com.jencao.mywork.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jencao.mywork.data.local.entity.FlashcardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards WHERE is_deleted = 0 ORDER BY created_at DESC")
    fun observeAll(): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE deck_id = :deckId AND is_deleted = 0 ORDER BY created_at DESC")
    fun observeByDeck(deckId: String): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE deck_id = :deckId AND is_deleted = 0 AND next_review <= :now ORDER BY next_review ASC")
    suspend fun getDue(deckId: String, now: Long = System.currentTimeMillis()): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: String): FlashcardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FlashcardEntity)

    @Update
    suspend fun update(item: FlashcardEntity)

    @Query("UPDATE flashcards SET is_deleted = 1, last_modified = :ts, needs_sync = 1 WHERE id = :id")
    suspend fun softDelete(id: String, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM flashcards WHERE needs_sync = 1 AND is_deleted = 0")
    suspend fun getPendingUploads(): List<FlashcardEntity>

    @Query("SELECT * FROM flashcards WHERE needs_sync = 1 AND is_deleted = 1")
    suspend fun getPendingDeletions(): List<FlashcardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FlashcardEntity>)

    @Query("UPDATE flashcards SET needs_sync = 0 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("DELETE FROM flashcards WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
