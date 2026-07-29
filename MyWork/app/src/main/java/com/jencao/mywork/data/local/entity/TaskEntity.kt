package com.jencao.mywork.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.jencao.mywork.data.local.BaseEntity

/**
 * 任务表。
 * status:   0 待办 / 1 已完成 / 2 进行中（长期目标）
 * priority: 1 高 / 2 中 / 3 低
 * taskType: 0 一次性 / 1 循环 / 2 长期目标
 * repeatType: 0 无 / 1 每日 / 2 每周 / 3 每月
 * repeatDays: 循环类型为“每周”时的周几位掩码字符串，如 "1,3,5"（周一三五）
 * repeatRule: 人类可读描述，冗余字段便于云端展示/统计
 */
@Entity(
    tableName = "tasks",
    indices = [Index(value = ["category_id"]), Index(value = ["status"]), Index(value = ["due_date"])]
)
data class TaskEntity(
    @ColumnInfo(name = "title") var title: String = "",
    @ColumnInfo(name = "content") var content: String = "",
    @ColumnInfo(name = "category_id") var categoryId: String = "",
    @ColumnInfo(name = "status") var status: Int = 0,
    @ColumnInfo(name = "priority") var priority: Int = 2,
    @ColumnInfo(name = "due_date") var dueDate: Long? = null,
    @ColumnInfo(name = "reminder_time") var reminderTime: Long? = null,
    @ColumnInfo(name = "task_type") var taskType: Int = 0,
    @ColumnInfo(name = "repeat_type") var repeatType: Int = 0,
    @ColumnInfo(name = "repeat_days") var repeatDays: String? = null,
    @ColumnInfo(name = "repeat_rule") var repeatRule: String? = null,
    @ColumnInfo(name = "parent_goal_id") var parentGoalId: String = "",
    @ColumnInfo(name = "created_at") var createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") var updatedAt: Long = System.currentTimeMillis()
) : BaseEntity()
