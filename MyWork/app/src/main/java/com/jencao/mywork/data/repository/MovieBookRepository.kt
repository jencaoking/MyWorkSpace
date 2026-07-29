package com.jencao.mywork.data.repository

import com.jencao.mywork.data.local.dao.MovieBookDao
import com.jencao.mywork.data.local.entity.MovieBookEntity
import com.jencao.mywork.data.sync.Syncer
import com.jencao.mywork.data.util.touch
import javax.inject.Inject
import javax.inject.Singleton

/** 影音书籍仓库：CRUD + 同步标记。 */
@Singleton
class MovieBookRepository @Inject constructor(
    private val dao: MovieBookDao
) : Syncer<MovieBookEntity> {
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
            posterUrl = posterUrl ?: "",
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

    /**
     * 由 TMDB 搜索结果直接创建条目，字段与 TMDB 100% 吻合。
     * type 取自 TMDB 的 media_type（movie / tv）。
     */
    suspend fun createFromTmdb(
        type: String,
        title: String,
        tmdbId: String,
        posterUrl: String = "",
        originalTitle: String = "",
        overview: String = "",
        releaseDate: String = "",
        voteAverage: Float = 0f
    ): MovieBookEntity {
        val item = MovieBookEntity(
            type = type,
            title = title,
            tmdbId = tmdbId,
            status = "wish",
            posterUrl = posterUrl,
            originalTitle = originalTitle,
            overview = overview,
            releaseDate = releaseDate,
            voteAverage = voteAverage
        ).apply { touch() }
        dao.insert(item)
        return item
    }

    suspend fun markDeleted(id: String) = dao.softDelete(id)

    override suspend fun getPendingUploads(): List<MovieBookEntity> = dao.getPendingUploads()
    override suspend fun getPendingDeletions(): List<String> = dao.getPendingDeletions().map { it.id }

    override suspend fun mergeRemote(remote: List<MovieBookEntity>) {
        val toUpsert = remote.filter { r ->
            val local = dao.getById(r.id)
            local == null || local.lastModified <= r.lastModified
        }.onEach { it.needsSync = false }
        if (toUpsert.isNotEmpty()) dao.upsertAll(toUpsert)
    }

    override suspend fun markSynced(ids: List<String>) {
        if (ids.isNotEmpty()) dao.markSynced(ids)
    }

    override suspend fun purgeDeleted(ids: List<String>) {
        if (ids.isNotEmpty()) dao.deleteByIds(ids)
    }
}
