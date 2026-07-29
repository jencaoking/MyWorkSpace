package com.jencao.mywork.data.remote.model

import com.jencao.mywork.data.local.entity.AccountRecordEntity
import com.jencao.mywork.data.local.entity.CategoryEntity
import com.jencao.mywork.data.local.entity.EnglishWordEntity
import com.jencao.mywork.data.local.entity.HealthRecordEntity
import com.jencao.mywork.data.local.entity.MovieBookEntity
import com.jencao.mywork.data.local.entity.NoteEntity
import com.jencao.mywork.data.local.entity.SportRecordEntity
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

/** 阶段3 笔记接口模型 */

data class NoteListData(
    val notes: List<NoteEntity> = emptyList()
)

typealias NoteListResponse = ApiEnvelope<NoteListData>

data class NoteSearchData(
    val keyword: String = "",
    val notes: List<NoteEntity> = emptyList()
)

typealias NoteSearchResponse = ApiEnvelope<NoteSearchData>

data class NoteUploadRequest(
    val notes: List<NoteEntity> = emptyList()
)

typealias NoteUploadResponse = ApiEnvelope<SyncUploadResult>

data class NotePullData(
    val server_time: Long = 0L,
    val notes: List<NoteEntity> = emptyList()
)

typealias NotePullResponse = ApiEnvelope<NotePullData>

typealias NoteDeleteResponse = ApiEnvelope<TaskDeleteResult>

/** 阶段4 专项模块接口模型（复用本地实体，由 Gson 蛇形命名序列化） */

// 运动记录
data class SportListData(val sports: List<SportRecordEntity> = emptyList())
typealias SportListResponse = ApiEnvelope<SportListData>
data class SportUploadRequest(val sports: List<SportRecordEntity> = emptyList())
typealias SportUploadResponse = ApiEnvelope<SyncUploadResult>
typealias SportDeleteResponse = ApiEnvelope<TaskDeleteResult>
data class SportPullData(
    val server_time: Long = 0L,
    val sports: List<SportRecordEntity> = emptyList(),
    val deleted_ids: List<String> = emptyList()
)
typealias SportPullResponse = ApiEnvelope<SportPullData>

// 英语单词
data class EnglishListData(val words: List<EnglishWordEntity> = emptyList())
typealias EnglishListResponse = ApiEnvelope<EnglishListData>
data class EnglishUploadRequest(val words: List<EnglishWordEntity> = emptyList())
typealias EnglishUploadResponse = ApiEnvelope<SyncUploadResult>
typealias EnglishDeleteResponse = ApiEnvelope<TaskDeleteResult>
data class EnglishPullData(
    val server_time: Long = 0L,
    val words: List<EnglishWordEntity> = emptyList(),
    val deleted_ids: List<String> = emptyList()
)
typealias EnglishPullResponse = ApiEnvelope<EnglishPullData>

// 影音书籍
data class MediaListData(val media: List<MovieBookEntity> = emptyList())
typealias MediaListResponse = ApiEnvelope<MediaListData>
data class MediaUploadRequest(val media: List<MovieBookEntity> = emptyList())
typealias MediaUploadResponse = ApiEnvelope<SyncUploadResult>
typealias MediaDeleteResponse = ApiEnvelope<TaskDeleteResult>
data class MediaPullData(
    val server_time: Long = 0L,
    val media: List<MovieBookEntity> = emptyList(),
    val deleted_ids: List<String> = emptyList()
)
typealias MediaPullResponse = ApiEnvelope<MediaPullData>

// 健康记录
data class HealthListData(val health: List<HealthRecordEntity> = emptyList())
typealias HealthListResponse = ApiEnvelope<HealthListData>
data class HealthUploadRequest(val health: List<HealthRecordEntity> = emptyList())
typealias HealthUploadResponse = ApiEnvelope<SyncUploadResult>
typealias HealthDeleteResponse = ApiEnvelope<TaskDeleteResult>
data class HealthPullData(
    val server_time: Long = 0L,
    val health: List<HealthRecordEntity> = emptyList(),
    val deleted_ids: List<String> = emptyList()
)
typealias HealthPullResponse = ApiEnvelope<HealthPullData>

// 分类（categories）
data class CategoryUploadRequest(val categories: List<CategoryEntity> = emptyList())
typealias CategoryUploadResponse = ApiEnvelope<SyncUploadResult>
typealias CategoryDeleteResponse = ApiEnvelope<TaskDeleteResult>
data class CategoryPullData(
    val server_time: Long = 0L,
    val dirty: List<CategoryEntity> = emptyList(),
    val deleted_ids: List<String> = emptyList()
)
typealias CategoryPullResponse = ApiEnvelope<CategoryPullData>

// 记账（accounts）
data class AccountUploadRequest(val accounts: List<AccountRecordEntity> = emptyList())
typealias AccountUploadResponse = ApiEnvelope<SyncUploadResult>
typealias AccountDeleteResponse = ApiEnvelope<TaskDeleteResult>
data class AccountPullData(
    val server_time: Long = 0L,
    val accounts: List<AccountRecordEntity> = emptyList(),
    val deleted_ids: List<String> = emptyList()
)
typealias AccountPullResponse = ApiEnvelope<AccountPullData>

// 设置（settings）：单行镜像，theme + module_toggles
data class UserSettingsDto(
    val id: String = "local",
    val theme: String? = null,
    val module_toggles: Map<String, Boolean>? = null,
    val language: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)
data class SettingsData(val settings: UserSettingsDto? = null)
typealias SettingsResponse = ApiEnvelope<SettingsData>
data class SettingsSaveRequest(
    val theme: String = "system",
    val module_toggles: Map<String, Boolean>? = null
)
typealias SettingsSaveResponse = ApiEnvelope<Any>

