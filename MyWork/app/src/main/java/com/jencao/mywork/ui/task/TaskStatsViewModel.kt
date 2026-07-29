package com.jencao.mywork.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.local.entity.TaskEntity
import com.jencao.mywork.data.model.MonthlyStats
import com.jencao.mywork.data.model.TaskType
import com.jencao.mywork.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TaskStatsViewModel @Inject constructor(
    private val taskRepo: TaskRepository
) : ViewModel() {

    private val _year = MutableStateFlow(LocalDate.now().year)
    private val _month = MutableStateFlow(LocalDate.now().monthValue)
    val year: StateFlow<Int> = _year.asStateFlow()
    val month: StateFlow<Int> = _month.asStateFlow()

    private val ymFlow = combine(_year, _month) { y, m -> y to m }

    val monthly: StateFlow<MonthlyStats> = ymFlow.flatMapLatest { (y, m) ->
        flow { emit(taskRepo.monthlyStats(null, y, m)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlyStats(LocalDate.now().year, LocalDate.now().monthValue, 0, 0, 0f))

    val perTask: StateFlow<List<Pair<TaskEntity, MonthlyStats>>> = ymFlow.flatMapLatest { (y, m) ->
        taskRepo.observeActive().map { tasks ->
            tasks.filter { TaskType.from(it.taskType) == TaskType.REPEAT }
                .map { it to taskRepo.monthlyStats(it.id, y, m) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun prevMonth() {
        val d = LocalDate.of(_year.value, _month.value, 1).minusMonths(1)
        _year.value = d.year
        _month.value = d.monthValue
    }

    fun nextMonth() {
        val d = LocalDate.of(_year.value, _month.value, 1).plusMonths(1)
        _year.value = d.year
        _month.value = d.monthValue
    }
}
