package com.jencao.mywork.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.local.entity.TaskEntity
import com.jencao.mywork.data.remote.model.HealthData
import com.jencao.mywork.data.repository.TaskRepository
import com.jencao.mywork.data.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ServerStatus {
    data object Idle : ServerStatus
    data object Loading : ServerStatus
    data class Success(val data: HealthData) : ServerStatus
    data class Error(val message: String) : ServerStatus
}

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data class Success(val uploaded: Int, val downloaded: Int, val deleted: Int) : SyncStatus
    data class Error(val message: String) : SyncStatus
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val taskRepo: TaskRepository,
    private val sync: SyncManager
) : ViewModel() {

    val activeTasks: StateFlow<List<TaskEntity>> = taskRepo.observeActive().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val activeCount: StateFlow<Int> = taskRepo.observeActiveCount().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )

    private val _serverStatus = MutableStateFlow<ServerStatus>(ServerStatus.Idle)
    val serverStatus: StateFlow<ServerStatus> = _serverStatus.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    fun testConnection() = viewModelScope.launch {
        _serverStatus.value = ServerStatus.Loading
        sync.testConnection()
            .onSuccess { _serverStatus.value = ServerStatus.Success(it) }
            .onFailure { _serverStatus.value = ServerStatus.Error(it.message ?: "连接失败") }
    }

    fun syncNow() = viewModelScope.launch {
        _syncStatus.value = SyncStatus.Syncing
        sync.syncOnce()
            .onSuccess {
                _syncStatus.value = SyncStatus.Success(it.uploaded, it.downloaded, it.deletedRemote)
            }
            .onFailure { _syncStatus.value = SyncStatus.Error(it.message ?: "同步失败") }
    }
}
