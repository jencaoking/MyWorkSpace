package com.jencao.mywork.data.repository

import com.jencao.mywork.data.local.dao.CategoryDao
import com.jencao.mywork.data.local.entity.CategoryEntity
import com.jencao.mywork.data.sync.Syncer
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val dao: CategoryDao
) : Syncer<CategoryEntity> {
    fun observeAll(): Flow<List<CategoryEntity>> = dao.observeAll()

    suspend fun add(name: String, color: String, sortOrder: Int = Int.MAX_VALUE): CategoryEntity {
        val c = CategoryEntity(name = name, color = color, sortOrder = sortOrder, isSystem = false)
        c.lastModified = System.currentTimeMillis()
        c.needsSync = true
        dao.insert(c)
        return c
    }

    suspend fun update(category: CategoryEntity) {
        category.lastModified = System.currentTimeMillis()
        category.needsSync = true
        dao.update(category)
    }

    suspend fun delete(category: CategoryEntity) {
        if (category.isSystem) return
        dao.softDelete(category.id)
    }

    suspend fun moveUp(category: CategoryEntity, prev: CategoryEntity) {
        val a = category.sortOrder
        category.sortOrder = prev.sortOrder
        prev.sortOrder = a
        category.lastModified = System.currentTimeMillis()
        prev.lastModified = System.currentTimeMillis()
        category.needsSync = true
        prev.needsSync = true
        dao.update(category)
        dao.update(prev)
    }

    suspend fun moveDown(category: CategoryEntity, next: CategoryEntity) {
        val a = category.sortOrder
        category.sortOrder = next.sortOrder
        next.sortOrder = a
        category.lastModified = System.currentTimeMillis()
        next.lastModified = System.currentTimeMillis()
        category.needsSync = true
        next.needsSync = true
        dao.update(category)
        dao.update(next)
    }

    override suspend fun getPendingUploads(): List<CategoryEntity> = dao.getPendingUploads()
    override suspend fun getPendingDeletions(): List<String> = dao.getPendingDeletions().map { it.id }

    override suspend fun mergeRemote(remote: List<CategoryEntity>) {
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
