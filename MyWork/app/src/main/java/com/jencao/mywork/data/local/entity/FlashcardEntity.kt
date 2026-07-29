package com.jencao.mywork.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.jencao.mywork.data.local.BaseEntity

/** 闪卡（工具箱模块 P5，含 SM-2 间隔复习字段）。 */
@Entity(tableName = "flashcards", indices = [Index("deck_id"), Index("next_review")])
data class FlashcardEntity(
    @ColumnInfo(name = "deck_id") var deckId: String = "",
    @ColumnInfo(name = "front") var front: String = "",
    @ColumnInfo(name = "back") var back: String = "",
    @ColumnInfo(name = "next_review") var nextReview: Long = 0L,
    @ColumnInfo(name = "interval_days") var intervalDays: Int = 0,
    @ColumnInfo(name = "ease") var ease: Float = 2.5f,
    @ColumnInfo(name = "created_at") var createdAt: Long = System.currentTimeMillis()
) : BaseEntity()
