package com.jencao.mywork.ui.dailypending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.local.entity.DailyPendingLogEntity
import com.jencao.mywork.data.repository.DailyPendingRepository
import com.jencao.mywork.data.repository.WeeklyReview
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class DailyPendingViewModel @Inject constructor(
    private val repo: DailyPendingRepository
) : ViewModel() {

    /** 待处理作业（pending） */
    val pending: StateFlow<List<DailyPendingLogEntity>> = repo.observePending()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 今日已处置 */
    val disposedToday: StateFlow<List<DailyPendingLogEntity>> = repo.observeDisposedToday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _weekly = MutableStateFlow(WeeklyReview())
    val weekly: StateFlow<WeeklyReview> = _weekly.asStateFlow()

    init {
        // 进入页面先兜底归档一次（幂等），随后刷新周统计
        viewModelScope.launch {
            runCatching { repo.archiveOverdueTasks() }
            refreshWeekly()
        }
    }

    fun refreshWeekly() = viewModelScope.launch {
        _weekly.value = repo.weeklyReview()
    }

    /** 补做完成 */
    fun complete(log: DailyPendingLogEntity) = viewModelScope.launch {
        repo.disposeComplete(log)
        refreshWeekly()
    }

    /** 改期：新日期沿用原截止时刻（时分） */
    fun reschedule(log: DailyPendingLogEntity, newDate: LocalDate) = viewModelScope.launch {
        val zone = ZoneId.systemDefault()
        val originalTime = Instant.ofEpochMilli(log.originalDueDate).atZone(zone).toLocalTime()
        val time = if (originalTime == LocalTime.MIDNIGHT) LocalTime.of(20, 0) else originalTime
        val newDue = newDate.atTime(time).atZone(zone).toInstant().toEpochMilli()
        repo.disposeReschedule(log, newDue)
        refreshWeekly()
    }

    /** 放弃 */
    fun abandon(log: DailyPendingLogEntity) = viewModelScope.launch {
        repo.disposeAbandon(log)
        refreshWeekly()
    }
}
