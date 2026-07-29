package com.jencao.mywork.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.local.entity.CategoryEntity
import com.jencao.mywork.data.local.entity.TaskEntity
import com.jencao.mywork.data.model.TaskSort
import com.jencao.mywork.data.repository.CategoryRepository
import com.jencao.mywork.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepo: TaskRepository,
    private val categoryRepo: CategoryRepository
) : ViewModel() {

    private val _category = MutableStateFlow("")
    val category: StateFlow<String> = _category.asStateFlow()

    private val _sort = MutableStateFlow(TaskSort.CREATED_DESC)
    val sort: StateFlow<TaskSort> = _sort.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = categoryRepo.observeAll()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = combine(_category, _sort) { cat, s -> cat to s }
        .flatMapLatest { (cat, s) -> taskRepo.observeTasks(cat, s) }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    private val _editing = MutableStateFlow<TaskEntity?>(null)
    val editing: StateFlow<TaskEntity?> = _editing.asStateFlow()

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    fun setCategory(cat: String) { _category.value = cat }
    fun setSort(s: TaskSort) { _sort.value = s }
    fun openNew() { _editing.value = null; _showDialog.value = true }
    fun openEdit(task: TaskEntity) { _editing.value = task; _showDialog.value = true }
    fun closeDialog() { _showDialog.value = false; _editing.value = null }

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
            id = id,
            title = title,
            content = content,
            categoryId = categoryId,
            priority = priority,
            dueDate = dueDate,
            reminderTime = reminderTime,
            taskType = taskType,
            repeatType = repeatType,
            repeatDays = repeatDays
        )
        closeDialog()
    }

    fun checkIn(task: TaskEntity) = viewModelScope.launch { taskRepo.checkIn(task) }
    fun delete(task: TaskEntity) = viewModelScope.launch { taskRepo.deleteTask(task) }
}
