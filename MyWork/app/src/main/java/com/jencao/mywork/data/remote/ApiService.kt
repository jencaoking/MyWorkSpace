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
}
