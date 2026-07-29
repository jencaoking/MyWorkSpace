package com.jencao.mywork.ui.task

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.local.entity.CategoryEntity
import com.jencao.mywork.data.local.entity.TaskCheckinEntity
import com.jencao.mywork.data.local.entity.TaskEntity
import com.jencao.mywork.data.repository.CategoryRepository
import com.jencao.mywork.data.repository.TaskRepository
import com.jencao.mywork.data.util.RepeatRule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskRepo: TaskRepository,
    private val categoryRepo: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: String = savedStateHandle.get<String>("taskId") ?: ""

    private val _task = MutableStateFlow<TaskEntity?>(null)
    val task: StateFlow<TaskEntity?> = _task.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = categoryRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val checkins: StateFlow<List<TaskCheckinEntity>> = taskRepo.observeCheckins(taskId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak.asStateFlow()

    val checkedToday: StateFlow<Boolean> = checkins.map { list ->
        list.firstOrNull()?.checkinDate == RepeatRule.todayStr()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            _task.value = taskRepo.getTaskById(taskId)
            _streak.value = taskRepo.computeStreak(taskId)
        }
    }

    fun checkIn() = viewModelScope.launch {
        _task.value?.let { if (taskRepo.checkIn(it)) refresh() }
    }

    fun undo() = viewModelScope.launch {
        _task.value?.let { if (taskRepo.undoCheckIn(it)) refresh() }
    }

    fun delete(onDeleted: () -> Unit) = viewModelScope.launch {
        _task.value?.let {
            taskRepo.deleteTask(it)
            onDeleted()
        }
    }

    fun save(
        id: String?,
        title: String,
        content: String,
        categoryId: String,
        priority: Int,
        dueDate: Long?,
        reminderTime: Long?,
        taskType: Int,
        repeatType: Int,
        repeatDays: String?
    ) = viewModelScope.launch {
        taskRepo.upsertTask(
            id = id, title = title, content = content, categoryId = categoryId,
            priority = priority, dueDate = dueDate, reminderTime = reminderTime,
            taskType = taskType, repeatType = repeatType, repeatDays = repeatDays
        )
        refresh()
    }

    private fun refresh() = viewModelScope.launch {
        _task.value = taskRepo.getTaskById(taskId)
        _streak.value = taskRepo.computeStreak(taskId)
    }
}
