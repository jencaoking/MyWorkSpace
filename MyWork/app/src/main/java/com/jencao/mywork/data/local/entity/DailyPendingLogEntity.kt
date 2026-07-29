package com.jencao.mywork.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.jencao.mywork.data.local.BaseEntity

/**
 * 每日未完成作业归档表。
 * 每天 00:00 把「截止日期已过且未完成」的任务快照写入本表，形成待处理作业清单。
 * id 采用确定性主键 "taskId_yyyy-MM-dd"，客户端与服务端归档幂等收敛。
 */
@Entity(
    tableName = "daily_pending_log",
    indices = [
        Index(value = ["task_id", "log_date"], unique = true),
        Index(value = ["log_date"]),
        Index(value = ["disposition"])
    ]
)
data class DailyPendingLogEntity(
    @ColumnInfo(name = "task_id") var taskId: String = "",
    @ColumnInfo(name = "task_title") var taskTitle: String = "",
    @ColumnInfo(name = "category_name") var categoryName: String = "",
    /** 1高 2中 3低（与 TaskEntity.priority 一致） */
    @ColumnInfo(name = "priority") var priority: Int = 2,
    /** 原截止时间（毫秒） */
    @ColumnInfo(name = "original_due_date") var originalDueDate: Long = 0L,
    /** 作业产生日（原截止日），格式 yyyy-MM-dd */
    @ColumnInfo(name = "log_date") var logDate: String = "",
    /** pending | completed | rescheduled | abandoned */
    @ColumnInfo(name = "disposition") var disposition: String = DISPOSITION_PENDING,
    /** 处置时间（毫秒） */
    @ColumnInfo(name = "disposed_at") var disposedAt: Long? = null,
    /** 改期后的新截止时间（毫秒） */
    @ColumnInfo(name = "new_due_date") var newDueDate: Long? = null,
    @ColumnInfo(name = "created_at") var createdAt: Long = System.currentTimeMillis()
) : BaseEntity() {
    companion object {
        const val DISPOSITION_PENDING = "pending"
        const val DISPOSITION_COMPLETED = "completed"
        const val DISPOSITION_RESCHEDULED = "rescheduled"
        const val DISPOSITION_ABANDONED = "abandoned"

        /** 确定性主键：同一任务同一天只归档一次 */
        fun deterministicId(taskId: String, logDate: String): String = "${taskId}_$logDate"
    }
}
