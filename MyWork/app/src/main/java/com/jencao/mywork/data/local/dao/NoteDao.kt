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

    @Query("SELECT * FROM notes WHERE is_deleted = 0 AND is_favorite = 1 ORDER BY is_pinned DESC, updated_at DESC")
    fun observeFavorites(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<NoteEntity?>

    /**
     * 全文搜索：FTS4 MATCH（英文/数字分词）+ LIKE 兜底（中文子串），结果去重。
     * :ftsQuery 由仓库层规范化（转义引号、追加 * 前缀匹配），:raw 为原始关键字。
     */
    @Query(
        """
        SELECT * FROM notes
        WHERE is_deleted = 0 AND (
            rowid IN (SELECT rowid FROM notes_fts WHERE notes_fts MATCH :ftsQuery)
            OR title LIKE '%' || :raw || '%'
            OR content LIKE '%' || :raw || '%'
        )
        ORDER BY is_pinned DESC, updated_at DESC
        """
    )
    suspend fun search(ftsQuery: String, raw: String): List<NoteEntity>

    /** LIKE 兜底版本：当 FTS 查询串非法（全符号等）时使用 */
    @Query(
        """
        SELECT * FROM notes
        WHERE is_deleted = 0 AND (title LIKE '%' || :raw || '%' OR content LIKE '%' || :raw || '%')
        ORDER BY is_pinned DESC, updated_at DESC
        """
    )
    suspend fun searchLike(raw: String): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity)

    @Update
    suspend fun update(note: NoteEntity)

    @Query("UPDATE notes SET is_pinned = :pinned, last_modified = :ts, needs_sync = 1 WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean, ts: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET is_favorite = :favorite, last_modified = :ts, needs_sync = 1 WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean, ts: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET is_deleted = 1, last_modified = :ts, needs_sync = 1 WHERE id = :id")
    suspend fun softDelete(id: String, ts: Long = System.currentTimeMillis())

    @Query("SELECT * FROM notes WHERE needs_sync = 1 AND is_deleted = 0")
    suspend fun getPendingUploads(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE needs_sync = 1 AND is_deleted = 1")
    suspend fun getPendingDeletions(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<NoteEntity>)
}
