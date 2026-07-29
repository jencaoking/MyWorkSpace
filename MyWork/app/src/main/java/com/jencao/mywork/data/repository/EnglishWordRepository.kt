package com.jencao.mywork.data.repository

import com.jencao.mywork.data.local.dao.EnglishWordDao
import com.jencao.mywork.data.local.entity.EnglishWordEntity
import javax.inject.Inject
import javax.inject.Singleton

/** 英语单词仓库：CRUD + 同步标记。 */
@Singleton
class EnglishWordRepository @Inject constructor(
    private val dao: EnglishWordDao
) {
    fun observeAll() = dao.observeAll()

    /** 今日待复习（nextReview <= now，且未删除）。 */
    fun observeDue() = dao.observeDue(System.currentTimeMillis())

    suspend fun getById(id: String): EnglishWordEntity? = dao.getById(id)

    suspend fun create(
        word: String,
        phonetic: String = "",
        meaning: String = "",
        example: String = "",
        familiarity: Int = 0
    ): EnglishWordEntity {
        val item = EnglishWordEntity(
            word = word,
            phonetic = phonetic,
            meaning = meaning,
            example = example,
            familiarity = familiarity.coerceIn(0, 5)
        ).apply { touch() }
        dao.insert(item)
        return item
    }

    suspend fun upsert(item: EnglishWordEntity): EnglishWordEntity {
        item.familiarity = item.familiarity.coerceIn(0, 5)
        item.touch()
        dao.insert(item)
        return item
    }

    suspend fun markDeleted(id: String) = dao.softDelete(id)

    suspend fun pendingUploads(): List<EnglishWordEntity> = dao.pendingUploads()
    suspend fun pendingDeletions(): List<String> = dao.pendingDeletions()

    suspend fun clearSyncFlags(ids: List<String>, deletedIds: List<String>) {
        dao.clearUploadFlag(ids)
        dao.clearDeleteFlag(deletedIds)
    }
}
