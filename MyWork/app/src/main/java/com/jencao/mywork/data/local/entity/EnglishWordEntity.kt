package com.jencao.mywork.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.jencao.mywork.data.local.BaseEntity

/**
 * 英语单词表（P1）。familiarity: 0~5 熟悉度；next_review: 下次复习时间戳。
 */
@Entity(
    tableName = "english_words",
    indices = [Index("next_review")]
)
data class EnglishWordEntity(
    @ColumnInfo(name = "word") var word: String = "",
    @ColumnInfo(name = "phonetic") var phonetic: String = "",
    @ColumnInfo(name = "meaning") var meaning: String = "",
    @ColumnInfo(name = "example") var example: String = "",
    @ColumnInfo(name = "familiarity") var familiarity: Int = 0,
    @ColumnInfo(name = "next_review") var nextReview: Long = 0L
) : BaseEntity()
