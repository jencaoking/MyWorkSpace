package com.jencao.mywork.data.sync

import android.util.Log
import com.jencao.mywork.data.local.BaseEntity
import com.jencao.mywork.data.local.entity.AccountRecordEntity
import com.jencao.mywork.data.local.entity.CategoryEntity
import com.jencao.mywork.data.local.entity.EnglishWordEntity
import com.jencao.mywork.data.local.entity.HealthRecordEntity
import com.jencao.mywork.data.local.entity.MovieBookEntity
import com.jencao.mywork.data.local.entity.NoteEntity
import com.jencao.mywork.data.local.entity.SportRecordEntity
import com.jencao.mywork.data.local.entity.TaskEntity
import com.jencao.mywork.data.remote.ApiService
import com.jencao.mywork.data.remote.model.AccountPullResponse
import com.jencao.mywork.data.remote.model.AccountUploadRequest
import com.jencao.mywork.data.remote.model.CategoryPullResponse
import com.jencao.mywork.data.remote.model.CategoryUploadRequest
import com.jencao.mywork.data.remote.model.EnglishPullResponse
import com.jencao.mywork.data.remote.model.EnglishUploadRequest
import com.jencao.mywork.data.remote.model.HealthData
import com.jencao.mywork.data.remote.model.HealthPullResponse
import com.jencao.mywork.data.remote.model.MediaPullResponse
import com.jencao.mywork.data.remote.model.MediaUploadRequest
import com.jencao.mywork.data.remote.model.NotePullResponse
import com.jencao.mywork.data.remote.model.NoteUploadRequest
import com.jencao.mywork.data.remote.model.SettingsResponse
import com.jencao.mywork.data.remote.model.SettingsSaveRequest
import com.jencao.mywork.data.remote.model.SportPullResponse
import com.jencao.mywork.data.remote.model.SportUploadRequest
import com.jencao.mywork.data.remote.model.SyncPullResult
import com.jencao.mywork.data.remote.model.SyncUploadRequest
import com.jencao.mywork.data.remote.model.TaskDeleteRequest
import com.jencao.mywork.data.repository.AccountRecordRepository
import com.jencao.mywork.data.repository.CategoryRepository
import com.jencao.mywork.data.repository.EnglishWordRepository
import com.jencao.mywork.data.repository.HealthRecordRepository
import com.jencao.mywork.data.repository.MovieBookRepository
import com.jencao.mywork.data.repository.NoteRepository
import com.jencao.mywork.data.repository.SportRecordRepository
import com.jencao.mywork.data.repository.TaskRepository
import com.jencao.mywork.data.settings.ModuleKey
import com.jencao.mywork.data.settings.ThemeMode
import com.jencao.mywork.data.settings.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 云端同步协调器：负责「先上后下」的增量同步闭环，覆盖全部业务模块。
 *
 * 约定（与后端 MVVM 对齐）：
 * - 设备身份经 AuthInterceptor 注入 X-Device-ID，后端据此做数据隔离，无需共享令牌；
 * - 上传走各模块 batchUpsert 接口（删除走对应 /delete 接口）；
 * - 拉取走各模块 /pull?since=（since 为上次成功拉取的 server_time 游标）；
 * - 冲突策略为 Last-Write-Wins：以 last_modified 较大者为准；
 * - 同步完成后本地 needs_sync 清零、墓碑回收，避免无限回传。
 *
 * 调度由 SyncScheduler 驱动：应用启动时立即同步一次，之后每 15 分钟周期同步，
 * 因此整个过程对业务层完全自动、无令牌、无手动触发。
 */
@Singleton
class SyncManager @Inject constructor(
    private val api: ApiService,
    private val prefs: UserPreferencesRepository,
    private val taskRepo: TaskRepository,
    private val noteRepo: NoteRepository,
    private val sportRepo: SportRecordRepository,
    private val englishRepo: EnglishWordRepository,
    private val movieRepo: MovieBookRepository,
    private val healthRepo: HealthRecordRepository,
    private val categoryRepo: CategoryRepository,
    private val accountRepo: AccountRecordRepository
) {
    private val TAG = "SyncManager"

    /** 探测云端连通性与数据库状态，结果回写到本地偏好（供首页展示） */
    suspend fun testConnection(): Result<HealthData> = runCatching {
        val health = api.health().data
            ?: throw IllegalStateException("云端返回空健康数据")
        prefs.setCloudConnected(true)
        health
    }.onFailure { prefs.setCloudConnected(false) }

    /** 执行一次双向增量同步，覆盖所有模块；返回本次收发统计 */
    suspend fun syncOnce(): Result<SyncResult> = runCatching {
        val deviceId = prefs.ensureDeviceId()
        var lastSync = prefs.lastSyncAt()
        var uploaded = 0
        var downloaded = 0
        var deletedRemote = 0
        val results = LinkedHashMap<String, Boolean>()

        fun advance(newLast: Long?) {
            newLast?.let { lastSync = maxOf(lastSync, it) }
        }

        // —— 任务（沿用 /sync 通用接口） ——
        advance(syncModule("tasks", taskRepo,
            upload = { u, d ->
                var ok = true
                if (u.isNotEmpty()) ok = ok && api.upload(SyncUploadRequest(device_id = deviceId, tasks = u)).code == 0
                if (d.isNotEmpty()) ok = ok && api.deleteTasks(TaskDeleteRequest(ids = d)).code == 0
                uploaded += u.size; deletedRemote += d.size
                ok
            },
            pull = { t ->
                val data = api.pull(since = t, deviceId = deviceId).data
                downloaded += data?.tasks?.size ?: 0
                PullBundle(data?.serverTime ?: t, data?.tasks ?: emptyList(), emptyList())
            },
            lastSync, results
        ))

        // —— 笔记 ——
        advance(syncModule("notes", noteRepo,
            upload = { u, d ->
                var ok = true
                if (u.isNotEmpty()) ok = ok && api.uploadNotes(NoteUploadRequest(notes = u)).code == 0
                if (d.isNotEmpty()) ok = ok && api.deleteNotes(TaskDeleteRequest(ids = d)).code == 0
                uploaded += u.size; deletedRemote += d.size
                ok
            },
            pull = { t ->
                val data = api.pullNotes(t).data
                downloaded += data?.notes?.size ?: 0
                PullBundle(data?.serverTime ?: t, data?.notes ?: emptyList(), emptyList())
            },
            lastSync, results
        ))

        // —— 运动 ——
        advance(syncModule("sports", sportRepo,
            upload = { u, d ->
                var ok = true
                if (u.isNotEmpty()) ok = ok && api.uploadSports(SportUploadRequest(sports = u)).code == 0
                if (d.isNotEmpty()) ok = ok && api.deleteSports(TaskDeleteRequest(ids = d)).code == 0
                uploaded += u.size; deletedRemote += d.size
                ok
            },
            pull = { t ->
                val data = api.pullSports(t).data
                downloaded += data?.sports?.size ?: 0
                PullBundle(data?.serverTime ?: t, data?.sports ?: emptyList(), data?.deletedIds ?: emptyList())
            },
            lastSync, results
        ))

        // —— 英语 ——
        advance(syncModule("english", englishRepo,
            upload = { u, d ->
                var ok = true
                if (u.isNotEmpty()) ok = ok && api.uploadEnglish(EnglishUploadRequest(words = u)).code == 0
                if (d.isNotEmpty()) ok = ok && api.deleteEnglish(TaskDeleteRequest(ids = d)).code == 0
                uploaded += u.size; deletedRemote += d.size
                ok
            },
            pull = { t ->
                val data = api.pullEnglish(t).data
                downloaded += data?.words?.size ?: 0
                PullBundle(data?.serverTime ?: t, data?.words ?: emptyList(), data?.deletedIds ?: emptyList())
            },
            lastSync, results
        ))

        // —— 影音书籍 ——
        advance(syncModule("media", movieRepo,
            upload = { u, d ->
                var ok = true
                if (u.isNotEmpty()) ok = ok && api.uploadMedia(MediaUploadRequest(media = u)).code == 0
                if (d.isNotEmpty()) ok = ok && api.deleteMedia(TaskDeleteRequest(ids = d)).code == 0
                uploaded += u.size; deletedRemote += d.size
                ok
            },
            pull = { t ->
                val data = api.pullMedia(t).data
                downloaded += data?.media?.size ?: 0
                PullBundle(data?.serverTime ?: t, data?.media ?: emptyList(), data?.deletedIds ?: emptyList())
            },
            lastSync, results
        ))

        // —— 健康 ——
        advance(syncModule("health", healthRepo,
            upload = { u, d ->
                var ok = true
                if (u.isNotEmpty()) ok = ok && api.uploadHealth(HealthUploadRequest(health = u)).code == 0
                if (d.isNotEmpty()) ok = ok && api.deleteHealth(TaskDeleteRequest(ids = d)).code == 0
                uploaded += u.size; deletedRemote += d.size
                ok
            },
            pull = { t ->
                val data = api.pullHealth(t).data
                downloaded += data?.health?.size ?: 0
                PullBundle(data?.serverTime ?: t, data?.health ?: emptyList(), data?.deletedIds ?: emptyList())
            },
            lastSync, results
        ))

        // —— 分类 ——
        advance(syncModule("categories", categoryRepo,
            upload = { u, d ->
                var ok = true
                if (u.isNotEmpty()) ok = ok && api.uploadCategories(CategoryUploadRequest(categories = u)).code == 0
                if (d.isNotEmpty()) ok = ok && api.deleteCategories(TaskDeleteRequest(ids = d)).code == 0
                uploaded += u.size; deletedRemote += d.size
                ok
            },
            pull = { t ->
                val data = api.pullCategories(t).data
                downloaded += data?.dirty?.size ?: 0
                PullBundle(data?.serverTime ?: t, data?.dirty ?: emptyList(), data?.deletedIds ?: emptyList())
            },
            lastSync, results
        ))

        // —— 记账 ——
        advance(syncModule("accounts", accountRepo,
            upload = { u, d ->
                var ok = true
                if (u.isNotEmpty()) ok = ok && api.uploadAccounts(AccountUploadRequest(accounts = u)).code == 0
                if (d.isNotEmpty()) ok = ok && api.deleteAccounts(TaskDeleteRequest(ids = d)).code == 0
                uploaded += u.size; deletedRemote += d.size
                ok
            },
            pull = { t ->
                val data = api.pullAccounts(t).data
                downloaded += data?.accounts?.size ?: 0
                PullBundle(data?.serverTime ?: t, data?.accounts ?: emptyList(), data?.deletedIds ?: emptyList())
            },
            lastSync, results
        ))

        // —— 设置（theme + 模块开关）：拉取服务端并应用，再回推本地，保证多端一致 ——
        try {
            val remote = api.getSettings().data?.settings
            if (remote != null) {
                remote.theme?.let { prefs.setThemeMode(themeFromServer(it)) }
                remote.module_toggles?.let { map ->
                    prefs.setModuleToggles(map.mapKeys { ModuleKey.valueOf(it.key) })
                }
            }
            val themeMode = prefs.themeMode.first()
            val toggles = prefs.moduleToggles.first()
            api.saveSettings(
                SettingsSaveRequest(
                    theme = themeToServer(themeMode),
                    module_toggles = toggles.mapKeys { it.key.name }
                )
            )
            results["settings"] = true
        } catch (e: Exception) {
            Log.e(TAG, "sync settings failed", e)
            results["settings"] = false
        }

        prefs.setLastSyncAt(lastSync)
        SyncResult(
            uploaded = uploaded,
            downloaded = downloaded,
            deletedRemote = deletedRemote,
            modules = results
        )
    }

    private data class PullBundle<T>(
        val serverTime: Long,
        val items: List<T>,
        val deletedIds: List<String>
    )

    /**
     * 单个模块的同步闭环：上传本地待同步项与待删墓碑 → 拉取增量 → LWW 合并 → 回收墓碑。
     * 任一环节失败仅记录该模块结果，不影响其他模块。
     */
    private suspend fun <T : BaseEntity> syncModule(
        name: String,
        repo: Syncer<T>,
        upload: suspend (List<T>, List<String>) -> Boolean,
        pull: suspend (Long) -> PullBundle<T>,
        lastSync: Long,
        results: MutableMap<String, Boolean>
    ): Long? {
        return try {
            val uploads = repo.getPendingUploads()
            val deletions = repo.getPendingDeletions()
            if (uploads.isNotEmpty() || deletions.isNotEmpty()) {
                if (upload(uploads, deletions)) {
                    repo.markSynced(uploads.map { it.id })
                    if (deletions.isNotEmpty()) repo.purgeDeleted(deletions)
                }
            }
            val bundle = pull(lastSync)
            repo.mergeRemote(bundle.items)
            if (bundle.deletedIds.isNotEmpty()) repo.purgeDeleted(bundle.deletedIds)
            results[name] = true
            bundle.serverTime
        } catch (e: Exception) {
            Log.e(TAG, "sync $name failed", e)
            results[name] = false
            null
        }
    }

    private fun themeToServer(mode: ThemeMode): String = when (mode) {
        ThemeMode.SYSTEM -> "system"
        ThemeMode.LIGHT -> "light"
        ThemeMode.DARK -> "dark"
    }

    private fun themeFromServer(value: String): ThemeMode = when (value) {
        "light" -> ThemeMode.LIGHT
        "dark" -> ThemeMode.DARK
        else -> ThemeMode.SYSTEM
    }

    data class SyncResult(
        val uploaded: Int = 0,
        val downloaded: Int = 0,
        val deletedRemote: Int = 0,
        val modules: Map<String, Boolean> = emptyMap()
    ) {
        val isEmpty: Boolean get() = uploaded == 0 && downloaded == 0 && deletedRemote == 0
    }
}
