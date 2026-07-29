package com.jencao.mywork.data.repository

import com.jencao.mywork.data.local.dao.TaskCheckinDao
import com.jencao.mywork.data.local.dao.TaskDao
import com.jencao.mywork.data.local.entity.TaskCheckinEntity
import com.jencao.mywork.data.local.entity.TaskEntity
import com.jencao.mywork.data.model.MonthlyStats
import com.jencao.mywork.data.sync.Syncer
import com.jencao.mywork.data.model.TaskSort
import com.jencao.mywork.data.model.TaskType
import com.jencao.mywork.data.settings.UserPreferencesRepository
import com.jencao.mywork.data.util.RepeatRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val dao: TaskDao,
    private val checkinDao: TaskCheckinDao,
    private val prefs: UserPreferencesRepository
) : Syncer<TaskEntity> {
    // —— 观察 ——
    fun observeActive(): Flow<List<TaskEntity>> = dao.observeActive()
    fun observeActiveCount(): Flow<Int> = dao.observeActiveCount()
    fun observeAll(): Flow<List<TaskEntity>> = dao.observeAll()

    fun observeTasks(categoryId: String, sort: TaskSort): Flow<List<TaskEntity>> =
        dao.observeByCategoryOrAll(categoryId).map { list ->
            when (sort) {
                TaskSort.CREATED_DESC -> list.sortedByDescending { it.createdAt }
                TaskSort.DUE_ASC -> list.sortedWith(compareBy({ it.dueDate ?: Long.MAX_VALUE }, { it.createdAt }))
                TaskSort.PRIORITY_DESC -> list.sortedWith(compareBy({ it.priority }, { it.createdAt }))
                TaskSort.TITLE_ASC -> list.sortedBy { it.title }
            }
        }

    suspend fun getTaskById(id: String): TaskEntity? = dao.getById(id)

    // —— 增改删 ——
    suspend fun upsertTask(
        id: String? = null,
        title: String,
        content: String = "",
        categoryId: String = "",
        priority: Int = 2,
        dueDate: Long? = null,
        reminderTime: Long? = null,
        taskType: Int = 0,
        repeatType: Int = 0,
        repeatDays: String? = null,
        parentGoalId: String = ""
    ): TaskEntity {
        val now = System.currentTimeMillis()
        val existing = if (id != null) dao.getById(id) else null
        val entity = existing ?: TaskEntity().apply { this.id = id ?: this.id }
        entity.title = title
        entity.content = content
        entity.categoryId = categoryId
        entity.priority = priority
        entity.dueDate = dueDate
        entity.reminderTime = reminderTime
        entity.taskType = taskType
        entity.repeatType = repeatType
        entity.repeatDays = repeatDays
        entity.repeatRule = buildRepeatRule(taskType, repeatType, repeatDays)
        entity.parentGoalId = parentGoalId
        entity.updatedAt = now
        entity.lastModified = now
        entity.needsSync = true
        if (existing != null) dao.update(entity) else dao.insert(entity)
        return entity
    }

    suspend fun deleteTask(task: TaskEntity) = dao.softDelete(task.id)

    // —— 打卡 ——
    fun observeCheckins(taskId: String): Flow<List<TaskCheckinEntity>> = checkinDao.observeByTask(taskId)

    suspend fun hasCheckedInToday(taskId: String): Boolean =
        checkinDao.getByTaskAndDate(taskId, RepeatRule.todayStr()) != null

    /** 今日打卡；已打卡则返回 false（幂等）。 */
    suspend fun checkIn(task: TaskEntity): Boolean {
        val today = RepeatRule.todayStr()
        if (checkinDao.getByTaskAndDate(task.id, today) != null) return false
        val c = TaskCheckinEntity(taskId = task.id, checkinDate = today, checkinTime = System.currentTimeMillis())
        c.lastModified = System.currentTimeMillis()
        c.needsSync = true
        c.deviceId = task.deviceId
        checkinDao.insert(c)
        // 一次性/循环任务打卡即完成；长期目标保持“进行中”
        task.status = if (TaskType.from(task.taskType) == TaskType.GOAL) 2 else 1
        task.updatedAt = System.currentTimeMillis()
        task.lastModified = System.currentTimeMillis()
        task.needsSync = true
        dao.update(task)
        return true
    }

    /** 撤销今日打卡；若再无记录则任务回到待办。 */
    suspend fun undoCheckIn(task: TaskEntity): Boolean {
        val today = RepeatRule.todayStr()
        if (checkinDao.getByTaskAndDate(task.id, today) == null) return false
        checkinDao.softDeleteByTaskAndDate(task.id, today)
        if (checkinDao.countByTask(task.id) == 0) {
            task.status = 0
            task.updatedAt = System.currentTimeMillis()
            task.lastModified = System.currentTimeMillis()
            task.needsSync = true
            dao.update(task)
        }
        return true
    }

    /** 当前连续打卡天数（截至今天或昨天）。 */
    suspend fun computeStreak(taskId: String): Int {
        val set = checkinDao.getAllByTask(taskId).map { it.checkinDate }.toSet()
        if (set.isEmpty()) return 0
        var streak = 0
        var d = LocalDate.now()
        if (!set.contains(RepeatRule.dateStr(d))) d = d.minusDays(1)
        while (set.contains(RepeatRule.dateStr(d))) {
            streak++
            d = d.minusDays(1)
        }
        return streak
    }

    /** 月度统计。taskId 为 null 表示聚合（当月打卡天数 / 当月天数）。 */
    suspend fun monthlyStats(taskId: String? = null, year: Int, month: Int): MonthlyStats {
        val start = LocalDate.of(year, month, 1)
        val end = start.plusMonths(1).minusDays(1)
        val startStr = RepeatRule.dateStr(start)
        val endStr = RepeatRule.dateStr(end)
        val doneDays = if (taskId == null) {
            checkinDao.countDistinctDaysInRangeAll(startStr, endStr)
        } else {
            checkinDao.countDistinctDaysInRange(taskId, startStr, endStr)
        }
        val scheduledDays = if (taskId == null) {
            start.lengthOfMonth()
        } else {
            val task = dao.getById(taskId)
            if (task != null && TaskType.from(task.taskType) == TaskType.REPEAT) {
                RepeatRule.scheduledDaysInMonth(
                    year, month, task.repeatType, maskOf(task.repeatDays)
                ).size
            } else 0
        }
        val rate = when {
            scheduledDays > 0 -> doneDays.toFloat() / scheduledDays
            doneDays > 0 -> 1f
            else -> 0f
        }
        return MonthlyStats(year, month, scheduledDays, doneDays, rate)
    }

    fun observeCheckinsInRange(start: String, end: String): Flow<List<TaskCheckinEntity>> =
        checkinDao.observeByDateRange(start, end)

    fun scheduledDays(year: Int, month: Int): Flow<Set<String>> =
        dao.observeActive().map { tasks ->
            tasks.filter { TaskType.from(it.taskType) == TaskType.REPEAT }
                .flatMap { t ->
                    RepeatRule.scheduledDaysInMonth(year, month, t.repeatType, maskOf(t.repeatDays))
                        .map { RepeatRule.dateStr(it) }
                }.toSet()
        }

    private fun maskOf(repeatDays: String?): Int {
        if (repeatDays.isNullOrBlank()) return 0
        val days = repeatDays.split(",").mapNotNull { it.toIntOrNull() }.toSet()
        return RepeatRule.maskFromDays(days)
    }

    private fun buildRepeatRule(taskType: Int, repeatType: Int, repeatDays: String?): String {
        if (TaskType.from(taskType) != TaskType.REPEAT) return ""
        return RepeatRule.describe(repeatType, maskOf(repeatDays))
    }

    // —— 同步支撑 ——
    suspend fun getPendingUploads() = dao.getPendingUploads()
    suspend fun getPendingDeletions() = dao.getPendingDeletions()
    suspend fun upsertRemote(tasks: List<TaskEntity>) = dao.upsertAll(tasks)
    suspend fun getCheckinPendingUploads() = checkinDao.getPendingUploads()
    suspend fun getCheckinPendingDeletions() = checkinDao.getPendingDeletions()
    suspend fun upsertRemoteCheckins(items: List<TaskCheckinEntity>) = checkinDao.upsertAll(items)

    /** 上传成功后清除本地 needs_sync 标记，避免重复上传 */
    suspend fun markSynced(ids: List<String>) {
        if (ids.isNotEmpty()) dao.markSynced(ids)
    }

    /** 服务器确认删除后，本地硬删这些记录（墓碑回收） */
    suspend fun purgeDeleted(ids: List<String>) {
        if (ids.isNotEmpty()) dao.deleteByIds(ids)
    }

    /**
     * LWW 合并云端拉取的任务：本地更晚修改则保留本地，否则以云端覆盖。
     * 合并结果 needs_sync 置 0，避免被当成待上传项无限回传。
     */
    suspend fun mergeRemote(remote: List<TaskEntity>) {
        val toUpsert = mutableListOf<TaskEntity>()
        for (r in remote) {
            val local = dao.getById(r.id)
            if (local == null || local.lastModified <= r.lastModified) {
                r.needsSync = false
                toUpsert.add(r)
            }
        }
        if (toUpsert.isNotEmpty()) dao.upsertAll(toUpsert)
    }
}
