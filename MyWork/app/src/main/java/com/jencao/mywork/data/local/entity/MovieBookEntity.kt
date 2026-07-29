package com.jencao.mywork.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.jencao.mywork.data.local.BaseEntity

/**
 * 影音书籍表（P1）。type: movie/tv/book；status: wish/watching/done。
 * tv 类型由 TMDB 搜索自动拉取，与 TMDB 数据保持 100% 吻合。
 */
@Entity(
    tableName = "movie_books",
    indices = [Index(value = ["type"]), Index(value = ["status"])]
)
data class MovieBookEntity(
    @ColumnInfo(name = "type") var type: String = "movie",
    @ColumnInfo(name = "title") var title: String = "",
    @ColumnInfo(name = "tmdb_id") var tmdbId: String = "",
    @ColumnInfo(name = "status") var status: String = "wish",
    @ColumnInfo(name = "rating") var rating: Float = 0f,
    @ColumnInfo(name = "poster_url") var posterUrl: String = "",
    @ColumnInfo(name = "note") var note: String = "",
    @ColumnInfo(name = "original_title") var originalTitle: String = "",
    @ColumnInfo(name = "overview") var overview: String = "",
    @ColumnInfo(name = "release_date") var releaseDate: String = "",
    @ColumnInfo(name = "vote_average") var voteAverage: Float = 0f,
    @ColumnInfo(name = "updated_at") var updatedAt: Long = System.currentTimeMillis()
) : BaseEntity()
