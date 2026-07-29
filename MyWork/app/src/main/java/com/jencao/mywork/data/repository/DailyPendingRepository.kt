package com.jencao.mywork.data.repository

import com.jencao.mywork.data.local.dao.CategoryDao
import com.jencao.mywork.data.local.dao.DailyPendingLogDao
import com.jencao.mywork.data.local.entity.DailyPendingLogEntity
import com.jencao.mywork.data.settings.UserPreferencesRepository
import com.jencao.mywork.data.sync.Syncer
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** 本周回顾聚合结果 */
data class WeeklyReview(
    val total: Int = 0,
    val completed: Int = 0,
    val rescheduled: Int = 0,
    val abandoned: Int = 0,
    val pending: Int = 0,
    /** yyyy-MM-dd -> 当日产生的作业数（近 7 天） */
    val byDate: Map<String, Int> = emptyMap(),
    /** 补做率 = completed / total */
    val makeupRate: Float = 0f
)

/**
 * 每日未完成作业仓库。
 * 归档规则：每天 00:00（Worker/应用启动兜底）把「截止日期已过且未完成」的普通任务
 * 快照进 daily_pending_log；确定性主键 taskId_logDate（logDate 取原截止日）保证
 * 客户端 Worker 与服务端 Cron 双方幂等收敛。
 */
@Singleton
class DailyPendingRepository @Inject constructor(
    private val dao: DailyPendingLogDao,
    private val taskRepo: TaskRepository,
    private val categoryDao: CategoryDao,
    private val prefs: UserPreferencesRepository
) : Syncer<DailyPendingLogEntity> {

    // —— 观察 ——
    fun observePending(): Flow<List<DailyPendingLogEntity>> = dao.observePending()
    fun observePendingCount(): Flow<Int> = dao.observePendingCount()
    fun observeDisposedToday(): Flow<List<DailyPendingLogEntity>> =
        dao.observeDisposedSince(todayStartMillis())

    suspend fun getPendingCount(): Int = dao.getPendingCount()

    // —— 每日归档（幂等，可重复调用） ——
    suspend fun archiveOverdueTasks(): Int {
        val deviceId = prefs.ensureDeviceId()
        val now = System.currentTimeMillis()
        val overdue = taskRepo.getOverdueOpenTasks(before = todayStartMillis())
        var inserted = 0
        for (task in overdue) {
            val due = task.dueDate ?: continue
            val logDate = millisToDate(due)
            val entity = DailyPendingLogEntity(
                taskId = task.id,
                taskTitle = task.title,
                categoryName = task.categoryId
                    .takeIf { it.isNotBlank() }
                    ?.let { categoryDao.getById(it)?.name }
                    ?: "",
                priority = task.priority,
                originalDueDate = due,
                logDate = logDate,
                disposition = DailyPendingLogEntity.DISPOSITION_PENDING,
                createdAt = now
            ).apply {
                id = DailyPendingLogEntity.deterministicId(task.id, logDate)
                lastModified = now
                this.deviceId = deviceId
                needsSync = true
            }
            if (dao.insertIgnore(entity) != -1L) inserted++
        }
        return inserted
    }

    // —— 处置 ——

    /** 补做完成：归档置 completed，原任务置已完成 */
    suspend fun disposeComplete(log: DailyPendingLogEntity) {
        taskRepo.completeTask(log.taskId)
        applyDisposition(log, DailyPendingLogEntity.DISPOSITION_COMPLETED, null)
    }

    /** 改期：归档置 rescheduled，原任务更新截止时间（重回任务列表） */
    suspend fun disposeReschedule(log: DailyPendingLogEntity, newDueDate: Long) {
        taskRepo.updateDueDate(log.taskId, newDueDate)
        applyDisposition(log, DailyPendingLogEntity.DISPOSITION_RESCHEDULED, newDueDate)
    }

    /** 放弃：仅标记归档，原任务保持原样（不再重复归档，因主键幂等） */
    suspend fun disposeAbandon(log: DailyPendingLogEntity) {
        applyDisposition(log, DailyPendingLogEntity.DISPOSITION_ABANDONED, null)
    }

    private suspend fun applyDisposition(log: DailyPendingLogEntity, disposition: String, newDueDate: Long?) {
        val now = System.currentTimeMillis()
        log.disposition = disposition
        log.disposedAt = now
        log.newDueDate = newDueDate
        log.lastModified = now
        log.needsSync = true
        dao.update(log)
    }

    // —— 本周回顾 ——
    suspend fun weeklyReview(): WeeklyReview {
        val startDate = LocalDate.now().minusDays(6).toString()
        val byDisposition = dao.countByDispositionSince(startDate).associate { it.disposition to it.cnt }
        val byDate = dao.countByDateSince(startDate).associate { it.logDate to it.cnt }
        val completed = byDisposition[DailyPendingLogEntity.DISPOSITION_COMPLETED] ?: 0
        val rescheduled = byDisposition[DailyPendingLogEntity.DISPOSITION_RESCHEDULED] ?: 0
        val abandoned = byDisposition[DailyPendingLogEntity.DISPOSITION_ABANDONED] ?: 0
        val pending = byDisposition[DailyPendingLogEntity.DISPOSITION_PENDING] ?: 0
        val total = completed + rescheduled + abandoned + pending
        return WeeklyReview(
            total = total,
            completed = completed,
            rescheduled = rescheduled,
            abandoned = abandoned,
            pending = pending,
            byDate = byDate,
            makeupRate = if (total > 0) completed.toFloat() / total else 0f
        )
    }

    // —— 同步支撑（由 SyncManager 驱动） ——
    override suspend fun getPendingUploads(): List<DailyPendingLogEntity> = dao.getPendingUploads()
    override suspend fun getPendingDeletions(): List<String> = dao.getPendingDeletions().map { it.id }
    override suspend fun markSynced(ids: List<String>) {
        if (ids.isNotEmpty()) dao.markSynced(ids)
    }

    override suspend fun purgeDeleted(ids: List<String>) {
        if (ids.isNotEmpty()) dao.deleteByIds(ids)
    }

    override suspend fun mergeRemote(remote: List<DailyPendingLogEntity>) {
        val toUpsert = mutableListOf<DailyPendingLogEntity>()
        for (r in remote) {
            val local = dao.getById(r.id)
            if (local == null || local.lastModified <= r.lastModified) {
                r.needsSync = false
                toUpsert.add(r)
            }
        }
        if (toUpsert.isNotEmpty()) dao.upsertAll(toUpsert)
    }

    companion object {
        private fun todayStartMillis(): Long =
            LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        private fun millisToDate(millis: Long): String =
            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString()
    }
}
