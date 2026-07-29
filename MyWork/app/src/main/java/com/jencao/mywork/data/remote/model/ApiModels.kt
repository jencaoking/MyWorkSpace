package com.jencao.mywork.data.remote.model

import com.google.gson.annotations.SerializedName
import com.jencao.mywork.data.local.entity.AccountRecordEntity
import com.jencao.mywork.data.local.entity.CalcHistoryEntity
import com.jencao.mywork.data.local.entity.CategoryEntity
import com.jencao.mywork.data.local.entity.CountdownEntity
import com.jencao.mywork.data.local.entity.DailyPendingLogEntity
import com.jencao.mywork.data.local.entity.ExpressPackageEntity
import com.jencao.mywork.data.local.entity.FlashcardDeckEntity
import com.jencao.mywork.data.local.entity.FlashcardEntity
import com.jencao.mywork.data.local.entity.HabitCheckinEntity
import com.jencao.mywork.data.local.entity.HabitEntity
import com.jencao.mywork.data.local.entity.HabitPlanEntity
import com.jencao.mywork.data.local.entity.InspirationEntity
import com.jencao.mywork.data.local.entity.QrScanEntity
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

// 每日未完成作业归档（daily-pending）
data class DailyPendingUploadRequest(val logs: List<DailyPendingLogEntity> = emptyList())
typealias DailyPendingUploadResponse = ApiEnvelope<SyncUploadResult>
typealias DailyPendingDeleteResponse = ApiEnvelope<TaskDeleteResult>
data class DailyPendingPullData(
    val server_time: Long = 0L,
    val logs: List<DailyPendingLogEntity> = emptyList(),
    val deleted_ids: List<String> = emptyList()
)
typealias DailyPendingPullResponse = ApiEnvelope<DailyPendingPullData>

// 工具箱 8 模块通用同步信封（list/batchUpsert/delete/pull 四接口复用）
data class SyncPushRequest<T>(val logs: List<T> = emptyList())
data class SyncPullData<T>(
    val server_time: Long = 0L,
    val logs: List<T> = emptyList(),
    val deleted_ids: List<String> = emptyList()
)
typealias SyncPushResponse = ApiEnvelope<SyncUploadResult>
typealias ToolPullResponse<T> = ApiEnvelope<SyncPullData<T>>

// AI / 汇率 / 快递代理请求响应模型（密钥均在服务端后台管理，App 不持有）
data class AiRequest(val action: String = "chat", val content: String = "", val target: String = "en", val tone: String = "")
data class AiResponse(val code: Int = 0, val message: String = "", val data: AiResultData? = null)
data class AiResultData(val result: String = "")
data class AiQuotaResponse(val code: Int = 0, val message: String = "", val data: AiQuotaData? = null)
data class AiQuotaData(val limit: Int = 0, val used: Int = 0, val remaining: Int = 0)
data class CurrencyRateResponse(val code: Int = 0, val message: String = "", val data: CurrencyRateData? = null)
data class CurrencyRateData(
    val from: String = "", val to: String = "", val amount: Double = 0.0,
    val rate: Double = 0.0, val result: Double = 0.0, val cached: Boolean = false
)
data class ExpressTrackRequest(val company: String = "", val tracking_no: String = "")
data class ExpressTrackResponse(val code: Int = 0, val message: String = "", val data: ExpressTrackData? = null)
data class ExpressTrackData(
    val company: String = "", val tracking_no: String = "", val status: String = "",
    val state: String = "", val traces: List<ExpressTrace> = emptyList()
)
data class ExpressTrace(val time: String = "", val context: String = "", val status: String = "")

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

// 第三方 API 代理（密钥保存在服务端后台管理，App 不持有密钥，仅通过 /api/proxy/* 调用）

/** GET /api/proxy/translate 返回：文本翻译结果 */
data class TranslateData(
    val query: String = "",
    val from: String = "",
    val to: String = "",
    val translation: String = "",
    val speak_url: String = "",
    val t_speak_url: String = ""
)
typealias TranslateResponse = ApiEnvelope<TranslateData>

/** 例句/释义条目（有道 web 释义） */
data class WordExample(
    val source: String = "",
    val target: String = ""
)

/** GET /api/proxy/word 返回：单词音标、释义、发音地址、例句 */
data class WordLookupData(
    val word: String = "",
    val phonetic: String = "",
    val phonetic_us: String = "",
    val phonetic_uk: String = "",
    val explains: List<String> = emptyList(),
    val translation: List<String> = emptyList(),
    val speak_url: String = "",
    val t_speak_url: String = "",
    val examples: List<WordExample> = emptyList()
)
typealias WordLookupResponse = ApiEnvelope<WordLookupData>

// TMDB 搜索（后端代理 /api/proxy/tmdb/search）
/** 单条 TMDB 搜索结果（字段与后端归一化输出一一对应，保持 snake_case 以便 Gson 直接映射）。 */
data class TmdbItem(
    val tmdb_id: Int = 0,
    val media_type: String = "",
    val title: String = "",
    val original_title: String = "",
    val overview: String = "",
    val poster_url: String = "",
    val release_date: String = "",
    val vote_average: Float = 0f
)

/** TMDB 搜索返回的数据体。 */
data class TmdbSearchData(
    val query: String = "",
    val page: Int = 1,
    val total_results: Int = 0,
    val total_pages: Int = 0,
    val results: List<TmdbItem> = emptyList()
)

typealias TmdbSearchResponse = ApiEnvelope<TmdbSearchData>

// 和风天气 QWeather（后端代理 /api/proxy/weather/*）
// 注意：Gson 使用 LOWER_CASE_WITH_UNDERSCORES；QWeather 返回 camelCase 字段，故每个字段显式标注 @SerializedName。

/** 实时天气 now 对象 */
data class QweatherNow(
    @SerializedName("obsTime") val obsTime: String = "",
    @SerializedName("temp") val temp: String = "",
    @SerializedName("feelsLike") val feelsLike: String = "",
    @SerializedName("icon") val icon: String = "",
    @SerializedName("text") val text: String = "",
    @SerializedName("windDir") val windDir: String = "",
    @SerializedName("windScale") val windScale: String = "",
    @SerializedName("windSpeed") val windSpeed: String = "",
    @SerializedName("humidity") val humidity: String = "",
    @SerializedName("precip") val precip: String = "",
    @SerializedName("pressure") val pressure: String = "",
    @SerializedName("vis") val vis: String = ""
)

/** GET /api/proxy/weather/now 数据体 */
data class QweatherNowData(
    @SerializedName("updateTime") val updateTime: String = "",
    @SerializedName("fxLink") val fxLink: String = "",
    @SerializedName("now") val now: QweatherNow = QweatherNow()
)
typealias QweatherNowResponse = ApiEnvelope<QweatherNowData>

/** 7 天预报单日 */
data class QweatherDaily(
    @SerializedName("fxDate") val fxDate: String = "",
    @SerializedName("tempMax") val tempMax: String = "",
    @SerializedName("tempMin") val tempMin: String = "",
    @SerializedName("textDay") val textDay: String = "",
    @SerializedName("iconDay") val iconDay: String = "",
    @SerializedName("textNight") val textNight: String = "",
    @SerializedName("iconNight") val iconNight: String = ""
)

/** GET /api/proxy/weather/7d 数据体 */
data class QweatherDailyData(
    @SerializedName("updateTime") val updateTime: String = "",
    @SerializedName("daily") val daily: List<QweatherDaily> = emptyList()
)
typealias QweatherDailyResponse = ApiEnvelope<QweatherDailyData>

/** 城市搜索单条 */
data class QweatherCity(
    @SerializedName("name") val name: String = "",
    @SerializedName("id") val id: String = "",
    @SerializedName("lat") val lat: String = "",
    @SerializedName("lon") val lon: String = "",
    @SerializedName("adm1") val adm1: String = "",
    @SerializedName("adm2") val adm2: String = ""
)

/** GET /api/proxy/weather/city/lookup 数据体 */
data class QweatherCityData(
    @SerializedName("location") val location: List<QweatherCity> = emptyList()
)
typealias QweatherCityResponse = ApiEnvelope<QweatherCityData>
