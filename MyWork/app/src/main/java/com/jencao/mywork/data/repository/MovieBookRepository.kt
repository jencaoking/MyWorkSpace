package com.jencao.mywork.data.repository

import com.jencao.mywork.data.local.dao.MovieBookDao
import com.jencao.mywork.data.local.entity.MovieBookEntity
import javax.inject.Inject
import javax.inject.Singleton

/** 影音书籍仓库：CRUD + 同步标记。 */
@Singleton
class MovieBookRepository @Inject constructor(
    private val dao: MovieBookDao
) {
    fun observeAll() = dao.observeAll()

    /** 按类型筛选：type 传 null 表示全部（"movie" / "book"）。 */
    fun observeByType(type: String?) = dao.observeByType(type)

    suspend fun getById(id: String): MovieBookEntity? = dao.getById(id)

    suspend fun create(
        type: String = "movie",
        title: String,
        tmdbId: String? = null,
        status: String = "wish",
        rating: Float? = null,
        posterUrl: String? = null,
        note: String = ""
    ): MovieBookEntity {
        val item = MovieBookEntity(
            type = type,
            title = title,
            tmdbId = tmdbId ?: "",
            status = status,
            rating = rating ?: 0f,
            posterUrl = posterUrl,
            note = note
        ).apply { touch() }
        dao.insert(item)
        return item
    }

    suspend fun upsert(item: MovieBookEntity): MovieBookEntity {
        item.touch()
        dao.insert(item)
        return item
    }

    suspend fun markDeleted(id: String) = dao.softDelete(id)

    suspend fun pendingUploads(): List<MovieBookEntity> = dao.pendingUploads()
    suspend fun pendingDeletions(): List<String> = dao.pendingDeletions()

    suspend fun clearSyncFlags(ids: List<String>, deletedIds: List<String>) {
        dao.clearUploadFlag(ids)
        dao.clearDeleteFlag(deletedIds)
    }
}
