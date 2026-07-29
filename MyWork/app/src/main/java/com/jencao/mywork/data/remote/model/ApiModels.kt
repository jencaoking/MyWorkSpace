package com.jencao.mywork.data.remote.model

import com.jencao.mywork.data.local.entity.TaskEntity

/** 统一响应信封：{ code, message, data } */
data class ApiEnvelope<T>(
    val code: Int = 0,
    val message: String = "",
    val data: T? = null
)

data class HealthData(
    val server_time: Long = 0L,
    val app_version: String = "",
    val php_version: String = ""
)

typealias HealthResponse = ApiEnvelope<HealthData>

/** /sync/upload 请求体（阶段1 先实现任务表，其余表后续补字段） */
data class SyncUploadRequest(
    val device_id: String = "",
    val last_pull: Long = 0L,
    val tasks: List<TaskEntity> = emptyList()
)

data class SyncUploadResult(
    val accepted: Int = 0,
    val synced_at: Long = 0L
)

typealias SyncUploadResponse = ApiEnvelope<SyncUploadResult>

/** /sync/pull 响应体 */
data class SyncPullResult(
    val server_time: Long = 0L,
    val tasks: List<TaskEntity> = emptyList()
)

typealias SyncPullResponse = ApiEnvelope<SyncPullResult>

/** 阶段2 任务接口模型 */

data class TaskListData(
    val tasks: List<TaskEntity> = emptyList()
)

typealias TaskListResponse = ApiEnvelope<TaskListData>

data class TaskStatsData(
    val total: Int = 0,
    val done: Int = 0,
    val active: Int = 0,
    val repeat: Int = 0,
    val by_category: Map<String, Int> = emptyMap()
)

typealias TaskStatsResponse = ApiEnvelope<TaskStatsData>

data class TaskDeleteRequest(
    val ids: List<String> = emptyList()
)

data class TaskDeleteResult(
    val deleted: Int = 0
)

typealias TaskDeleteResponse = ApiEnvelope<TaskDeleteResult>

