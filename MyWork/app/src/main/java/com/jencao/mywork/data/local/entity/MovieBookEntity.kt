package com.jencao.mywork.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.jencao.mywork.data.local.BaseEntity

/**
 * 影音书籍表（P1）。type: movie/book；status: want/watching/done。
 */
@Entity(
    tableName = "movie_books",
    indices = [Index("type"), Index("status")]
)
data class MovieBookEntity(
    @ColumnInfo(name = "type") var type: String = "movie",
    @ColumnInfo(name = "title") var title: String = "",
    @ColumnInfo(name = "tmdb_id") var tmdbId: String = "",
    @ColumnInfo(name = "status") var status: String = "want",
    @ColumnInfo(name = "rating") var rating: Float = 0f,
    @ColumnInfo(name = "poster_url") var posterUrl: String = "",
    @ColumnInfo(name = "note") var note: String = "",
    @ColumnInfo(name = "updated_at") var updatedAt: Long = System.currentTimeMillis()
) : BaseEntity()
