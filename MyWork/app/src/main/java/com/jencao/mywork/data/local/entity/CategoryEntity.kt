package com.jencao.mywork.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.jencao.mywork.data.local.BaseEntity

/**
 * 任务分类表。is_system 为内置分类（不可删除）。
 */
@Entity(
    tableName = "categories",
    indices = [Index(value = ["sort_order"])]
)
data class CategoryEntity(
    @ColumnInfo(name = "name") var name: String = "",
    @ColumnInfo(name = "color") var color: String = "#2E7D62",
    @ColumnInfo(name = "sort_order") var sortOrder: Int = 0,
    @ColumnInfo(name = "is_system") var isSystem: Boolean = false
) : BaseEntity()
