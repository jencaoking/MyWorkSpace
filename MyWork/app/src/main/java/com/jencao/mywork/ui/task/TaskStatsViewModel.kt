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
import java.time.YearMonth
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

    /** 选中月份里每一天的打卡次数（day -> count）。 */
    val dailyCounts: StateFlow<Map<Int, Int>> = ymFlow.flatMapLatest { (y, m) ->
        flow { emit(taskRepo.dailyCheckinCounts(y, m)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** 以选中月份为终点的最近 6 个月完成率趋势。 */
    val trend: StateFlow<List<MonthlyStats>> = ymFlow.flatMapLatest { (y, m) ->
        flow {
            val end = YearMonth.of(y, m)
            val list = (0 until 6).map { i ->
                val ym = end.minusMonths(i.toLong())
                taskRepo.monthlyStats(null, ym.year, ym.monthValue)
            }
            emit(list.reversed())
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
