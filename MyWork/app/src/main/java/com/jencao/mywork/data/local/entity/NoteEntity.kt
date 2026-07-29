package com.jencao.mywork.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.jencao.mywork.data.local.BaseEntity

/**
 * 笔记表（P0，Markdown 正文）。阶段3 会建立 FTS 全文索引。
 */
@Entity(
    tableName = "notes",
    indices = [Index(value = ["is_pinned"]), Index(value = ["needs_sync"])]
)
data class NoteEntity(
    @ColumnInfo(name = "title") var title: String = "",
    @ColumnInfo(name = "content") var content: String = "",
    @ColumnInfo(name = "is_pinned") var isPinned: Boolean = false,
    @ColumnInfo(name = "is_favorite") var isFavorite: Boolean = false,
    @ColumnInfo(name = "created_at") var createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") var updatedAt: Long = System.currentTimeMillis()
) : BaseEntity()
