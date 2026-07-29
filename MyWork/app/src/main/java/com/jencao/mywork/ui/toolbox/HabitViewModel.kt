package com.jencao.mywork.ui.toolbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.local.entity.HabitEntity
import com.jencao.mywork.data.local.entity.HabitPlanEntity
import com.jencao.mywork.data.repository.HabitCheckinRepository
import com.jencao.mywork.data.repository.HabitPlanRepository
import com.jencao.mywork.data.repository.HabitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HabitViewModel @Inject constructor(
    private val planRepo: HabitPlanRepository,
    private val habitRepo: HabitRepository,
    private val checkinRepo: HabitCheckinRepository
) : ViewModel() {
    val plans: StateFlow<List<HabitPlanEntity>> =
        planRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun observeHabits(planId: String): Flow<List<HabitEntity>> = habitRepo.observeByPlan(planId)
    fun observeCheckins(habitId: String): Flow<List<com.jencao.mywork.data.local.entity.HabitCheckinEntity>> =
        checkinRepo.observeByHabit(habitId)

    fun addPlan(title: String, desc: String, period: Int) = viewModelScope.launch {
        if (title.isBlank()) return@launch
        planRepo.insert(HabitPlanEntity(title = title, description = desc, period = period))
    }
    fun deletePlan(id: String) = viewModelScope.launch { planRepo.softDelete(id) }
    fun addHabit(planId: String, title: String, freq: Int, days: String, timeMin: Int) = viewModelScope.launch {
        if (title.isBlank()) return@launch
        habitRepo.insert(HabitEntity(planId = planId, title = title, frequency = freq, days = days, timeMin = timeMin))
    }
    fun deleteHabit(id: String) = viewModelScope.launch { habitRepo.softDelete(id) }
    fun toggleCheck(habitId: String) = viewModelScope.launch {
        if (checkinRepo.isCheckedToday(habitId)) checkinRepo.uncheck(habitId) else checkinRepo.checkIn(habitId)
    }
}
