package com.jencao.mywork.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jencao.mywork.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE is_deleted = 0 ORDER BY is_pinned DESC, updated_at DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity)

    @Update
    suspend fun update(note: NoteEntity)

    @Query("UPDATE notes SET is_deleted = 1, last_modified = :ts, needs_sync = 1 WHERE id = :id")
    suspend fun softDelete(id: String, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM notes WHERE needs_sync = 1 AND is_deleted = 0")
    suspend fun getPendingUploads(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE needs_sync = 1 AND is_deleted = 1")
    suspend fun getPendingDeletions(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<NoteEntity>)
}
