package com.jencao.mywork.data.repository

import com.jencao.mywork.data.local.dao.NoteDao
import com.jencao.mywork.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 笔记仓库（阶段3）：Markdown 笔记 CRUD + FTS 全文搜索。本地优先。
 */
@Singleton
class NoteRepository @Inject constructor(
    private val dao: NoteDao
) {
    fun observeAll(): Flow<List<NoteEntity>> = dao.observeAll()

    fun observeFavorites(): Flow<List<NoteEntity>> = dao.observeFavorites()

    fun observeById(id: String): Flow<NoteEntity?> = dao.observeById(id)

    suspend fun getById(id: String): NoteEntity? = dao.getById(id)

    /** 新建笔记，返回实体（含生成的 UUID） */
    suspend fun create(title: String, content: String): NoteEntity {
        val now = System.currentTimeMillis()
        val note = NoteEntity(
            title = title,
            content = content,
            createdAt = now,
            updatedAt = now
        )
        note.lastModified = now
        note.needsSync = true
        dao.insert(note)
        return note
    }

    suspend fun save(note: NoteEntity) {
        val now = System.currentTimeMillis()
        note.updatedAt = now
        note.lastModified = now
        note.needsSync = true
        dao.update(note)
    }

    suspend fun setPinned(id: String, pinned: Boolean) = dao.setPinned(id, pinned)

    suspend fun setFavorite(id: String, favorite: Boolean) = dao.setFavorite(id, favorite)

    suspend fun delete(id: String) = dao.softDelete(id)

    /**
     * 全文搜索：把关键字规范化为 FTS MATCH 串（去引号、按空白分词、每词加 * 前缀匹配），
     * FTS 命中 + LIKE 中文子串兜底，DAO 内一次查询完成。
     */
    suspend fun search(keyword: String): List<NoteEntity> {
        val raw = keyword.trim()
        if (raw.isEmpty()) return emptyList()
        val ftsQuery = raw
            .replace("\"", " ")
            .replace("'", " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { "$it*" }
        return if (ftsQuery.isBlank()) dao.searchLike(raw) else runCatching {
            dao.search(ftsQuery, raw)
        }.getOrElse { dao.searchLike(raw) }
    }
}
