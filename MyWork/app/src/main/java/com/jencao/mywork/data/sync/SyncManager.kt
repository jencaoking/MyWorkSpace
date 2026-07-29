package com.jencao.mywork.data.sync

import com.jencao.mywork.data.remote.ApiService
import com.jencao.mywork.data.remote.model.HealthData
import com.jencao.mywork.data.remote.model.SyncPullResult
import com.jencao.mywork.data.remote.model.SyncUploadRequest
import com.jencao.mywork.data.remote.model.TaskDeleteRequest
import com.jencao.mywork.data.repository.TaskRepository
import com.jencao.mywork.data.settings.UserPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 云端同步协调器：负责「先上后下」的增量同步闭环。
 *
 * 约定（与后端 MVVM 对齐）：
 * - 设备身份经 AuthInterceptor 注入 X-Device-ID，后端据此做数据隔离；
 * - 上传走 POST /sync/upload（/api/tasks/delete 处理软删除）；
 * - 拉取走 GET /sync/pull?since=（since 为上次成功拉取的 server_time 毫秒游标）；
 * - 冲突策略为 Last-Write-Wins：以 last_modified 较大者为准；
 * - 同步完成后本地 needs_sync 清零，避免无限回传。
 */
@Singleton
class SyncManager @Inject constructor(
    private val api: ApiService,
    private val taskRepo: TaskRepository,
    private val prefs: UserPreferencesRepository
) {
    /** 探测云端连通性与数据库状态，结果回写到本地偏好（供首页展示） */
    suspend fun testConnection(): Result<HealthData> = runCatching {
        val health = api.health().data
            ?: throw IllegalStateException("云端返回空健康数据")
        prefs.setCloudConnected(true)
        health
    }.onFailure { prefs.setCloudConnected(false) }

    /** 执行一次双向增量同步，返回本次收发统计 */
    suspend fun syncOnce(): Result<SyncResult> = runCatching {
        val deviceId = prefs.ensureDeviceId()

        // 1) 上行：推送本地待上传任务
        val pending = taskRepo.getPendingUploads()
        var uploaded = 0
        if (pending.isNotEmpty()) {
            api.upload(SyncUploadRequest(device_id = deviceId, tasks = pending))
            taskRepo.markSynced(pending.map { it.id })
            uploaded = pending.size
        }

        // 2) 上行：推送本地已软删除任务（云端置墓碑）
        val deletions = taskRepo.getPendingDeletions()
        var deletedRemote = 0
        if (deletions.isNotEmpty()) {
            api.deleteTasks(TaskDeleteRequest(ids = deletions.map { it.id }))
            taskRepo.purgeDeleted(deletions.map { it.id })
            deletedRemote = deletions.size
        }

        // 3) 下行：增量拉取自上次游标以来的变更
        val since = prefs.lastSyncAt()
        val pulled: SyncPullResult? = api.pull(since = since, deviceId = deviceId).data
        val remoteTasks = pulled?.tasks ?: emptyList()
        taskRepo.mergeRemote(remoteTasks)

        // 4) 推进同步游标，避免下次全量重复拉取
        pulled?.server_time?.let { prefs.setLastSyncAt(it) }

        SyncResult(uploaded = uploaded, downloaded = remoteTasks.size, deletedRemote = deletedRemote)
    }

    data class SyncResult(
        val uploaded: Int,
        val downloaded: Int,
        val deletedRemote: Int
    ) {
        val isEmpty: Boolean get() = uploaded == 0 && downloaded == 0 && deletedRemote == 0
    }
}
