package com.jencao.mywork.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

private val THEME_MODE = intPreferencesKey("theme_mode")
private val DEVICE_ID = stringPreferencesKey("device_id")

private fun moduleKey(key: ModuleKey) = booleanPreferencesKey("module_${key.name}")

/** 板块默认开启状态（P0 板块恒为 true） */
private val DEFAULT_TOGGLES: Map<ModuleKey, Boolean> = mapOf(
    ModuleKey.TASK to true,
    ModuleKey.NOTE to true,
    ModuleKey.SPORT to true,
    ModuleKey.ENGLISH to true,
    ModuleKey.MEDIA to false,
    ModuleKey.HEALTH to false,
    ModuleKey.ACCOUNT to false,
    ModuleKey.WEATHER to false
)

/**
 * 本地偏好存储（DataStore）：主题模式、设备标识、板块开关。
 * 这些信息属于“即时生效的本地设置”，由 DataStore 直接托管；
 * 云端的 user_settings 镜像在阶段5 同步时再处理，避免双源竞争。
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore: DataStore<Preferences> = context.dataStore

    val themeMode: Flow<ThemeMode> = dataStore.data
        .map { prefs -> ThemeMode.fromOrdinal(prefs[THEME_MODE] ?: ThemeMode.SYSTEM.ordinal) }

    /** 所有板块开关映射 */
    val moduleToggles: Flow<Map<ModuleKey, Boolean>> = dataStore.data
        .map { prefs ->
            ModuleKey.entries.associateWith { key ->
                if (key.locked) true else (prefs[moduleKey(key)] ?: (DEFAULT_TOGGLES[key] ?: false))
            }
        }

    fun isModuleEnabled(key: ModuleKey): Flow<Boolean> = moduleToggles.map { it[key] ?: false }

    /** 读取设备标识，首次访问时自动生成并落盘 */
    suspend fun ensureDeviceId(): String {
        val existing = dataStore.data.first()[DEVICE_ID]
        if (!existing.isNullOrBlank()) return existing
        val newId = UUID.randomUUID().toString()
        dataStore.edit { it[DEVICE_ID] = newId }
        return newId
    }

    val deviceId: Flow<String> = dataStore.data.map { it[DEVICE_ID] ?: "" }

    /** 云端已连接状态（供首页连接测试展示） */
    private val CLOUD_CONNECTED = booleanPreferencesKey("cloud_connected")
    fun observeCloudConnected(): Flow<Boolean> = dataStore.data.map { it[CLOUD_CONNECTED] ?: false }
    suspend fun setCloudConnected(connected: Boolean) =
        dataStore.edit { it[CLOUD_CONNECTED] = connected }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.ordinal }
    }

    suspend fun setModuleEnabled(key: ModuleKey, enabled: Boolean) {
        if (key.locked) return
        dataStore.edit { it[moduleKey(key)] = enabled }
    }

    /** 上一次成功增量拉取的服务器时间戳（毫秒），用于 /sync/pull?since= 游标 */
    private val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
    suspend fun lastSyncAt(): Long = dataStore.data.first()[LAST_SYNC_AT] ?: 0L
    suspend fun setLastSyncAt(ts: Long) = dataStore.edit { it[LAST_SYNC_AT] = ts }

    /** 上次同步时间戳的响应式流，供首页展示“上次同步时间” */
    val lastSyncAtFlow: Flow<Long> = dataStore.data.map { it[LAST_SYNC_AT] ?: 0L }

    /** 自动同步开关（默认开启），控制后台周期同步是否生效 */
    private val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
    val autoSyncEnabled: Flow<Boolean> = dataStore.data.map { it[AUTO_SYNC_ENABLED] ?: true }
    suspend fun setAutoSyncEnabled(enabled: Boolean) =
        dataStore.edit { it[AUTO_SYNC_ENABLED] = enabled }
}
