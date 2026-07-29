package com.jencao.mywork.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.local.entity.TaskEntity
import com.jencao.mywork.data.repository.TaskRepository
import com.jencao.mywork.data.settings.UserPreferencesRepository
import com.jencao.mywork.data.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val taskRepo: TaskRepository,
    private val prefs: UserPreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val activeTasks: StateFlow<List<TaskEntity>> = taskRepo.observeActive().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val activeCount: StateFlow<Int> = taskRepo.observeActiveCount().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )

    /** 上次成功同步的服务器时间戳（毫秒），随自动同步推进 */
    val lastSyncAt: StateFlow<Long> = prefs.lastSyncAtFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    /** 自动同步开关（持久化） */
    val autoSyncEnabled: StateFlow<Boolean> = prefs.autoSyncEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    /** 当前是否正在同步（自动或手动），由 WorkManager 状态驱动 */
    val isSyncing: StateFlow<Boolean> = SyncScheduler.observeRunning(context)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** 上一次同步是否失败（WorkManager 标记 FAILED），稍后由下一周期自动重试 */
    val lastSyncFailed: StateFlow<Boolean> = SyncScheduler.observeState(context)
        .map { it.failed }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** 手动触发一次即时同步（仍走 WorkManager，受网络约束） */
    fun syncNow() = SyncScheduler.syncNow(context)

    /** 开启/关闭后台周期同步 */
    fun setAutoSync(enabled: Boolean) = viewModelScope.launch {
        prefs.setAutoSyncEnabled(enabled)
        if (enabled) SyncScheduler.schedule(context) else SyncScheduler.cancel(context)
    }
}
