package com.jencao.mywork.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.jencao.mywork.data.local.BaseEntity

/** 灵感/语录收藏（工具箱模块 P7）。 */
@Entity(tableName = "inspiration_items", indices = [Index("created_at")])
data class InspirationEntity(
    @ColumnInfo(name = "content") var content: String = "",
    @ColumnInfo(name = "author") var author: String = "",
    @ColumnInfo(name = "source") var source: String = "",
    @ColumnInfo(name = "tags") var tags: String = "",
    @ColumnInfo(name = "favorite") var favorite: Boolean = false,
    @ColumnInfo(name = "created_at") var createdAt: Long = System.currentTimeMillis()
) : BaseEntity()
