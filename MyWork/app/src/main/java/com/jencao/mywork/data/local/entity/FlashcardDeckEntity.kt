package com.jencao.mywork.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.jencao.mywork.data.local.BaseEntity

/** 闪卡牌组（工具箱模块 P5）。 */
@Entity(tableName = "flashcard_decks", indices = [Index("created_at")])
data class FlashcardDeckEntity(
    @ColumnInfo(name = "name") var name: String = "",
    @ColumnInfo(name = "description") var description: String = "",
    @ColumnInfo(name = "created_at") var createdAt: Long = System.currentTimeMillis()
) : BaseEntity()
