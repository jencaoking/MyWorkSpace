package com.jencao.mywork.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

/**
 * 笔记 FTS4 全文索引表（阶段3）。
 * contentEntity 绑定 notes 表，Room 自动生成触发器保持索引与源表同步。
 * unicode61 分词对英文/数字友好；中文按连续 CJK 串成词，DAO 搜索时叠加 LIKE 兜底。
 */
@Fts4(contentEntity = NoteEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "notes_fts")
data class NoteFtsEntity(
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "content") val content: String
)
