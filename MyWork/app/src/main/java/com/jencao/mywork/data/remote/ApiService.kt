package com.jencao.mywork.data.remote

import com.jencao.mywork.data.remote.model.HealthResponse
import com.jencao.mywork.data.remote.model.NoteDeleteResponse
import com.jencao.mywork.data.remote.model.NoteListResponse
import com.jencao.mywork.data.remote.model.NotePullResponse
import com.jencao.mywork.data.remote.model.NoteSearchResponse
import com.jencao.mywork.data.remote.model.NoteUploadRequest
import com.jencao.mywork.data.remote.model.NoteUploadResponse
import com.jencao.mywork.data.remote.model.SyncPullResponse
import com.jencao.mywork.data.remote.model.SyncUploadRequest
import com.jencao.mywork.data.remote.model.SyncUploadResponse
import com.jencao.mywork.data.remote.model.TaskDeleteRequest
import com.jencao.mywork.data.remote.model.TaskDeleteResponse
import com.jencao.mywork.data.remote.model.TaskListResponse
import com.jencao.mywork.data.remote.model.TaskStatsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

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
}
