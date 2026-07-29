package com.jencao.mywork.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.repository.TaskRepository
import com.jencao.mywork.data.util.RepeatRule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TaskCalendarViewModel @Inject constructor(
    private val taskRepo: TaskRepository
) : ViewModel() {

    private val _year = MutableStateFlow(LocalDate.now().year)
    private val _month = MutableStateFlow(LocalDate.now().monthValue)
    val year: StateFlow<Int> = _year.asStateFlow()
    val month: StateFlow<Int> = _month.asStateFlow()

    private val ymFlow = combine(_year, _month) { y, m -> y to m }

    val checkinDays: StateFlow<Set<String>> = ymFlow.flatMapLatest { (y, m) ->
        val start = RepeatRule.dateStr(LocalDate.of(y, m, 1))
        val end = RepeatRule.dateStr(LocalDate.of(y, m, 1).plusMonths(1).minusDays(1))
        taskRepo.observeCheckinsInRange(start, end).map { list -> list.map { it.checkinDate }.toSet() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val scheduledDays: StateFlow<Set<String>> = ymFlow.flatMapLatest { (y, m) ->
        taskRepo.scheduledDays(y, m)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

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
