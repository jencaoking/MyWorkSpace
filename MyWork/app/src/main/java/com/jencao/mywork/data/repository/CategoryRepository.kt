package com.jencao.mywork.data.repository

import com.jencao.mywork.data.local.dao.CategoryDao
import com.jencao.mywork.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val dao: CategoryDao
) {
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
}
