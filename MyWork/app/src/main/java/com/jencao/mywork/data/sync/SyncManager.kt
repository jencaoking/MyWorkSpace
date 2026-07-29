package com.jencao.mywork.data.sync

import com.jencao.mywork.data.remote.ApiService
import com.jencao.mywork.data.remote.model.HealthData
import com.jencao.mywork.data.repository.TaskRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 云端同步协调器（阶段1 为最小可用骨架）。
 * 流程：收集本地待上传 → POST /sync/upload → GET /sync/pull 落地。
 * 完整冲突解决（LWW）、失败重试、WorkManager 调度在阶段5 补全。
 */
@Singleton
class SyncManager @Inject constructor(
    private val api: ApiService,
    private val taskRepo: TaskRepository
) {
    suspend fun testConnection(): Result<HealthData> = runCatching {
        val resp = api.health()
        if (resp.code != 0 || resp.data == null) {
            error(resp.message.ifBlank { "服务端返回异常 code=${resp.code}" })
        }
        resp.data
    }

    suspend fun syncOnce(): Result<Unit> = runCatching {
        val deviceId = taskRepo.getPendingUploads().firstOrNull()?.deviceId ?: ""
        val pending = taskRepo.getPendingUploads()
        api.upload(
            com.jencao.mywork.data.remote.model.SyncUploadRequest(
                device_id = deviceId,
                tasks = pending
            )
        )
        val pulled = api.pull(since = 0, deviceId = deviceId)
        pulled.data?.tasks?.let { taskRepo.upsertRemote(it) }
    }
}
