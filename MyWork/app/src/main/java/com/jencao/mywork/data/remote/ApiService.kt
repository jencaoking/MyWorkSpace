package com.jencao.mywork.data.remote

import com.jencao.mywork.data.remote.model.EnglishDeleteResponse
import com.jencao.mywork.data.remote.model.EnglishListResponse
import com.jencao.mywork.data.remote.model.EnglishPullResponse
import com.jencao.mywork.data.remote.model.EnglishUploadRequest
import com.jencao.mywork.data.remote.model.EnglishUploadResponse
import com.jencao.mywork.data.remote.model.HealthDeleteResponse
import com.jencao.mywork.data.remote.model.HealthListResponse
import com.jencao.mywork.data.remote.model.HealthPullResponse
import com.jencao.mywork.data.remote.model.HealthUploadRequest
import com.jencao.mywork.data.remote.model.HealthUploadResponse
import com.jencao.mywork.data.remote.model.HealthResponse
import com.jencao.mywork.data.remote.model.MediaDeleteResponse
import com.jencao.mywork.data.remote.model.MediaListResponse
import com.jencao.mywork.data.remote.model.MediaPullResponse
import com.jencao.mywork.data.remote.model.MediaUploadRequest
import com.jencao.mywork.data.remote.model.MediaUploadResponse
import com.jencao.mywork.data.remote.model.NoteDeleteResponse
import com.jencao.mywork.data.remote.model.NoteListResponse
import com.jencao.mywork.data.remote.model.NotePullResponse
import com.jencao.mywork.data.remote.model.NoteSearchResponse
import com.jencao.mywork.data.remote.model.NoteUploadRequest
import com.jencao.mywork.data.remote.model.NoteImageUploadResponse
import com.jencao.mywork.data.remote.model.NoteUploadResponse
import com.jencao.mywork.data.remote.model.SportDeleteResponse
import com.jencao.mywork.data.remote.model.SportListResponse
import com.jencao.mywork.data.remote.model.SportPullResponse
import com.jencao.mywork.data.remote.model.AccountDeleteResponse
import com.jencao.mywork.data.remote.model.AccountPullResponse
import com.jencao.mywork.data.remote.model.AccountUploadRequest
import com.jencao.mywork.data.remote.model.AccountUploadResponse
import com.jencao.mywork.data.remote.model.CategoryDeleteResponse
import com.jencao.mywork.data.remote.model.DailyPendingDeleteResponse
import com.jencao.mywork.data.remote.model.DailyPendingPullResponse
import com.jencao.mywork.data.remote.model.DailyPendingUploadRequest
import com.jencao.mywork.data.remote.model.DailyPendingUploadResponse
import com.jencao.mywork.data.remote.model.CategoryPullResponse
import com.jencao.mywork.data.remote.model.CategoryUploadRequest
import com.jencao.mywork.data.remote.model.CategoryUploadResponse
import com.jencao.mywork.data.remote.model.SettingsResponse
import com.jencao.mywork.data.remote.model.SettingsSaveRequest
import com.jencao.mywork.data.remote.model.SettingsSaveResponse
import com.jencao.mywork.data.remote.model.SportUploadRequest
import com.jencao.mywork.data.remote.model.TranslateResponse
import com.jencao.mywork.data.remote.model.WordLookupResponse
import com.jencao.mywork.data.remote.model.TmdbSearchResponse
import com.jencao.mywork.data.remote.model.QweatherNowResponse
import com.jencao.mywork.data.remote.model.QweatherDailyResponse
import com.jencao.mywork.data.remote.model.QweatherCityResponse
import com.jencao.mywork.data.remote.model.SportUploadResponse
import com.jencao.mywork.data.remote.model.SyncPullResponse
import com.jencao.mywork.data.remote.model.SyncUploadRequest
import com.jencao.mywork.data.remote.model.SyncUploadResponse
import com.jencao.mywork.data.remote.model.TaskDeleteRequest
import com.jencao.mywork.data.remote.model.TaskDeleteResponse
import com.jencao.mywork.data.remote.model.TaskListResponse
import com.jencao.mywork.data.remote.model.TaskStatsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import okhttp3.MultipartBody

/**
 * 后端接口定义。路径与方案 V1.1 一致：/api/health、/sync/upload、/sync/pull。
 * 阶段2 扩展：api/tasks（list / stats / delete）。
 */
interface ApiService {

    @GET("api/health")
    suspend fun health(): HealthResponse

    @POST("sync/upload")
    suspend fun upload(@Body payload: SyncUploadRequest): SyncUploadResponse

    @GET("sync/pull")
    suspend fun pull(
        @Query("since") since: Long,
        @Query("device_id") deviceId: String
    ): SyncPullResponse

    // —— 阶段2 任务接口（本地优先；云端接口已就绪，供后续同步/跨端使用） ——
    @GET("api/tasks")
    suspend fun listTasks(
        @Query("category_id") categoryId: String = "",
        @Query("status") status: Int? = null
    ): TaskListResponse

    @GET("api/tasks/stats")
    suspend fun taskStats(): TaskStatsResponse

    @POST("api/tasks/delete")
    suspend fun deleteTasks(@Body req: TaskDeleteRequest): TaskDeleteResponse

    // —— 阶段3 笔记接口（本地优先；云端接口已就绪，供后续同步/跨端使用） ——
    @GET("api/notes")
    suspend fun listNotes(@Query("favorite") favorite: String = ""): NoteListResponse

    @POST("api/notes")
    suspend fun uploadNotes(@Body req: NoteUploadRequest): NoteUploadResponse

    @POST("api/notes/delete")
    suspend fun deleteNotes(@Body req: TaskDeleteRequest): NoteDeleteResponse

    @GET("api/notes/search")
    suspend fun searchNotes(@Query("q") keyword: String): NoteSearchResponse

    @GET("api/notes/pull")
    suspend fun pullNotes(@Query("since") since: Long): NotePullResponse

    /** 笔记图片上传（multipart）：返回可访问的图片 URL */
    @Multipart
    @POST("api/notes/image")
    suspend fun uploadNoteImage(@Part file: MultipartBody.Part): NoteImageUploadResponse

    // —— 阶段4 专项模块接口（本地优先；云端接口已就绪，供后续同步/跨端使用） ——
    // 运动记录
    @GET("api/sports")
    suspend fun listSports(): SportListResponse

    @POST("api/sports")
    suspend fun uploadSports(@Body req: SportUploadRequest): SportUploadResponse

    @POST("api/sports/delete")
    suspend fun deleteSports(@Body req: TaskDeleteRequest): SportDeleteResponse

    @GET("api/sports/pull")
    suspend fun pullSports(@Query("since") since: Long): SportPullResponse

    // 英语单词
    @GET("api/english")
    suspend fun listEnglish(): EnglishListResponse

    @POST("api/english")
    suspend fun uploadEnglish(@Body req: EnglishUploadRequest): EnglishUploadResponse

    @POST("api/english/delete")
    suspend fun deleteEnglish(@Body req: TaskDeleteRequest): EnglishDeleteResponse

    @GET("api/english/pull")
    suspend fun pullEnglish(@Query("since") since: Long): EnglishPullResponse

    // 影音书籍
    @GET("api/media")
    suspend fun listMedia(): MediaListResponse

    @POST("api/media")
    suspend fun uploadMedia(@Body req: MediaUploadRequest): MediaUploadResponse

    @POST("api/media/delete")
    suspend fun deleteMedia(@Body req: TaskDeleteRequest): MediaDeleteResponse

    @GET("api/media/pull")
    suspend fun pullMedia(@Query("since") since: Long): MediaPullResponse

    // 健康记录（路径避免与 /api/health 健康检查冲突）
    @GET("api/health-records")
    suspend fun listHealth(): HealthListResponse

    @POST("api/health-records")
    suspend fun uploadHealth(@Body req: HealthUploadRequest): HealthUploadResponse

    @POST("api/health-records/delete")
    suspend fun deleteHealth(@Body req: TaskDeleteRequest): HealthDeleteResponse

    @GET("api/health-records/pull")
    suspend fun pullHealth(@Query("since") since: Long): HealthPullResponse

    // —— 分类（categories） ——
    @POST("api/categories")
    suspend fun uploadCategories(@Body req: CategoryUploadRequest): CategoryUploadResponse

    @POST("api/categories/delete")
    suspend fun deleteCategories(@Body req: TaskDeleteRequest): CategoryDeleteResponse

    @GET("api/categories/pull")
    suspend fun pullCategories(@Query("since") since: Long): CategoryPullResponse

    // —— 记账（accounts） ——
    @POST("api/accounts")
    suspend fun uploadAccounts(@Body req: AccountUploadRequest): AccountUploadResponse

    @POST("api/accounts/delete")
    suspend fun deleteAccounts(@Body req: TaskDeleteRequest): AccountDeleteResponse

    @GET("api/accounts/pull")
    suspend fun pullAccounts(@Query("since") since: Long): AccountPullResponse

    // —— 每日未完成作业归档（daily-pending） ——
    @POST("api/daily-pending")
    suspend fun uploadDailyPending(@Body req: DailyPendingUploadRequest): DailyPendingUploadResponse

    @POST("api/daily-pending/delete")
    suspend fun deleteDailyPending(@Body req: TaskDeleteRequest): DailyPendingDeleteResponse

    @GET("api/daily-pending/pull")
    suspend fun pullDailyPending(@Query("since") since: Long): DailyPendingPullResponse

    // —— 工具箱 8 模块（通用 list / batchUpsert / delete / pull） ——
    @POST("api/calc") suspend fun uploadCalc(@Body req: SyncPushRequest<CalcHistoryEntity>): SyncPushResponse
    @POST("api/calc/delete") suspend fun deleteCalc(@Body req: TaskDeleteRequest): TaskDeleteResponse
    @GET("api/calc/pull") suspend fun pullCalc(@Query("since") since: Long): ToolPullResponse<CalcHistoryEntity>

    @POST("api/qr") suspend fun uploadQr(@Body req: SyncPushRequest<QrScanEntity>): SyncPushResponse
    @POST("api/qr/delete") suspend fun deleteQr(@Body req: TaskDeleteRequest): TaskDeleteResponse
    @GET("api/qr/pull") suspend fun pullQr(@Query("since") since: Long): ToolPullResponse<QrScanEntity>

    @POST("api/countdown") suspend fun uploadCountdown(@Body req: SyncPushRequest<CountdownEntity>): SyncPushResponse
    @POST("api/countdown/delete") suspend fun deleteCountdown(@Body req: TaskDeleteRequest): TaskDeleteResponse
    @GET("api/countdown/pull") suspend fun pullCountdown(@Query("since") since: Long): ToolPullResponse<CountdownEntity>

    @POST("api/habit-plan") suspend fun uploadHabitPlan(@Body req: SyncPushRequest<HabitPlanEntity>): SyncPushResponse
    @POST("api/habit-plan/delete") suspend fun deleteHabitPlan(@Body req: TaskDeleteRequest): TaskDeleteResponse
    @GET("api/habit-plan/pull") suspend fun pullHabitPlan(@Query("since") since: Long): ToolPullResponse<HabitPlanEntity>

    @POST("api/habit") suspend fun uploadHabit(@Body req: SyncPushRequest<HabitEntity>): SyncPushResponse
    @POST("api/habit/delete") suspend fun deleteHabit(@Body req: TaskDeleteRequest): TaskDeleteResponse
    @GET("api/habit/pull") suspend fun pullHabit(@Query("since") since: Long): ToolPullResponse<HabitEntity>

    @POST("api/habit-checkin") suspend fun uploadHabitCheckin(@Body req: SyncPushRequest<HabitCheckinEntity>): SyncPushResponse
    @POST("api/habit-checkin/delete") suspend fun deleteHabitCheckin(@Body req: TaskDeleteRequest): TaskDeleteResponse
    @GET("api/habit-checkin/pull") suspend fun pullHabitCheckin(@Query("since") since: Long): ToolPullResponse<HabitCheckinEntity>

    @POST("api/flashcard-deck") suspend fun uploadFlashcardDeck(@Body req: SyncPushRequest<FlashcardDeckEntity>): SyncPushResponse
    @POST("api/flashcard-deck/delete") suspend fun deleteFlashcardDeck(@Body req: TaskDeleteRequest): TaskDeleteResponse
    @GET("api/flashcard-deck/pull") suspend fun pullFlashcardDeck(@Query("since") since: Long): ToolPullResponse<FlashcardDeckEntity>

    @POST("api/flashcard") suspend fun uploadFlashcard(@Body req: SyncPushRequest<FlashcardEntity>): SyncPushResponse
    @POST("api/flashcard/delete") suspend fun deleteFlashcard(@Body req: TaskDeleteRequest): TaskDeleteResponse
    @GET("api/flashcard/pull") suspend fun pullFlashcard(@Query("since") since: Long): ToolPullResponse<FlashcardEntity>

    @POST("api/inspiration") suspend fun uploadInspiration(@Body req: SyncPushRequest<InspirationEntity>): SyncPushResponse
    @POST("api/inspiration/delete") suspend fun deleteInspiration(@Body req: TaskDeleteRequest): TaskDeleteResponse
    @GET("api/inspiration/pull") suspend fun pullInspiration(@Query("since") since: Long): ToolPullResponse<InspirationEntity>

    @POST("api/express") suspend fun uploadExpress(@Body req: SyncPushRequest<ExpressPackageEntity>): SyncPushResponse
    @POST("api/express/delete") suspend fun deleteExpress(@Body req: TaskDeleteRequest): TaskDeleteResponse
    @GET("api/express/pull") suspend fun pullExpress(@Query("since") since: Long): ToolPullResponse<ExpressPackageEntity>

    // AI 统一代理（密钥在服务端后台管理）
    @POST("api/ai") suspend fun ai(@Body req: AiRequest): AiResponse
    @GET("api/ai/quota") suspend fun aiQuota(): AiQuotaResponse

    // 实时汇率（密钥在服务端后台管理）
    @GET("api/currency/rate")
    suspend fun currencyRate(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("amount") amount: Double = 1.0
    ): CurrencyRateResponse

    // 快递实时查询（密钥在服务端后台管理）
    @POST("api/express/track")
    suspend fun expressTrack(@Body req: ExpressTrackRequest): ExpressTrackResponse

    // —— 设置（settings，单行镜像） ——
    @GET("api/settings")
    suspend fun getSettings(): SettingsResponse

    @POST("api/settings")
    suspend fun saveSettings(@Body req: SettingsSaveRequest): SettingsSaveResponse

    // —— 第三方 API 代理（密钥在服务端后台管理，App 不持有密钥） ——
    @GET("api/proxy/translate")
    suspend fun translate(
        @Query("text") text: String,
        @Query("from") from: String = "auto",
        @Query("to") to: String = "zh-CHS"
    ): TranslateResponse

    @GET("api/proxy/word")
    suspend fun lookupWord(@Query("text") text: String): WordLookupResponse

    @GET("api/proxy/tmdb/search")
    suspend fun searchTmdb(
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    // —— 和风天气代理（密钥在服务端后台管理，App 不持有密钥） ——
    @GET("api/proxy/weather/now")
    suspend fun getWeatherNow(@Query("location") location: String): QweatherNowResponse

    @GET("api/proxy/weather/7d")
    suspend fun getWeather7d(@Query("location") location: String): QweatherDailyResponse

    @GET("api/proxy/weather/city/lookup")
    suspend fun lookupCity(@Query("keyword") keyword: String): QweatherCityResponse
}
