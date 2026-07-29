package com.jencao.mywork.ui.pomodoro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.repository.PomodoroRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PomodoroViewModel @Inject constructor(
    private val repo: PomodoroRepository
) : ViewModel() {

    val sessions = repo.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workCount = repo.observeWorkCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _mode = MutableStateFlow("work")
    val mode: StateFlow<String> = _mode.asStateFlow()

    private val _remaining = MutableStateFlow(workDuration("work") * 60)
    val remaining: StateFlow<Int> = _remaining.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private var ticker: Job? = null

    private fun workDuration(m: String) = when (m) {
        "work" -> 25
        "short" -> 5
        else -> 15
    }

    fun setMode(m: String) {
        if (_mode.value == m) return
        pause()
        _mode.value = m
        _remaining.value = workDuration(m) * 60
    }

    fun start() {
        if (_running.value) return
        _running.value = true
        ticker = viewModelScope.launch {
            while (true) {
                delay(1000)
                val left = _remaining.value - 1
                if (left <= 0) {
                    finishCycle()
                    break
                } else {
                    _remaining.value = left
                }
            }
        }
    }

    fun pause() {
        _running.value = false
        ticker?.cancel()
        ticker = null
    }

    fun reset() {
        pause()
        _remaining.value = workDuration(_mode.value) * 60
    }

    fun skip() {
        pause()
        switchMode()
    }

    private fun finishCycle() {
        pause()
        viewModelScope.launch { repo.saveSession(_mode.value, workDuration(_mode.value)) }
        switchMode()
    }

    private fun switchMode() {
        val next = if (_mode.value == "work") "short" else "work"
        _mode.value = next
        _remaining.value = workDuration(next) * 60
    }
}
