package com.jencao.mywork.ui.sport

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.local.entity.SportRecordEntity
import com.jencao.mywork.data.repository.SportRecordRepository
import com.jencao.mywork.ui.navigation.SportRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/** 运动类型预设 */
val SPORT_TYPES = listOf("跑步", "骑行", "健身", "游泳", "瑜伽", "徒步", "球类", "其他")

data class SportMonthStat(
    val totalMin: Int = 0,
    val count: Int = 0,
    val distanceKm: Float = 0f,
    val calories: Int = 0
)

/** 运动模块 ViewModel：列表（含本月统计）+ 编辑共用。 */
@HiltViewModel
class SportViewModel @Inject constructor(
    private val repo: SportRecordRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val items: StateFlow<List<SportRecordEntity>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthStat: StateFlow<SportMonthStat> = items.map { list ->
        val cal = Calendar.getInstance()
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH)
        var totalMin = 0
        var count = 0
        var distance = 0f
        var calo = 0
        for (it in list) {
            val c = Calendar.getInstance().apply { timeInMillis = it.recordDate }
            if (c.get(Calendar.YEAR) == y && c.get(Calendar.MONTH) == m) {
                totalMin += it.durationMin
                count += 1
                distance += it.distanceKm ?: 0f
                calo += it.calories ?: 0
            }
        }
        SportMonthStat(totalMin, count, distance, calo)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SportMonthStat())

    // —— 编辑字段 ——
    private val recordId: String = savedStateHandle["sportId"] ?: SportRoutes.NEW_ID
    val isNew: Boolean get() = recordId == SportRoutes.NEW_ID

    private val _type = MutableStateFlow(SPORT_TYPES.last())
    val type: StateFlow<String> = _type

    private val _duration = MutableStateFlow("")
    val duration: StateFlow<String> = _duration

    private val _distance = MutableStateFlow("")
    val distance: StateFlow<String> = _distance

    private val _calories = MutableStateFlow("")
    val calories: StateFlow<String> = _calories

    private val _date = MutableStateFlow(System.currentTimeMillis())
    val date: StateFlow<Long> = _date

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    private var loaded: SportRecordEntity? = null

    init {
        if (!isNew) {
            viewModelScope.launch {
                repo.getById(recordId)?.let {
                    loaded = it
                    _type.value = it.type.ifEmpty { SPORT_TYPES.last() }
                    _duration.value = if (it.durationMin > 0) it.durationMin.toString() else ""
                    _distance.value = it.distanceKm?.takeIf { d -> d > 0 }?.toString() ?: ""
                    _calories.value = it.calories?.takeIf { c -> c > 0 }?.toString() ?: ""
                    _date.value = it.recordDate
                    _note.value = it.note
                }
            }
        }
    }

    fun setType(v: String) { _type.value = v }
    fun setDuration(v: String) { _duration.value = v.filter { it.isDigit() } }
    fun setDistance(v: String) { _distance.value = v }
    fun setCalories(v: String) { _calories.value = v.filter { it.isDigit() } }
    fun setDate(v: Long) { _date.value = v }
    fun setNote(v: String) { _note.value = v }

    fun save() = viewModelScope.launch {
        val dur = _duration.value.toIntOrNull() ?: 0
        val dis = _distance.value.toFloatOrNull()
        val cal = _calories.value.toIntOrNull()
        if (loaded == null) {
            repo.create(
                type = _type.value,
                durationMin = dur,
                distanceKm = dis,
                calories = cal,
                recordDate = _date.value,
                note = _note.value.trim()
            )
        } else {
            loaded!!.apply {
                type = _type.value
                durationMin = dur
                distanceKm = dis
                calories = cal
                recordDate = _date.value
                note = _note.value.trim()
            }
            repo.upsert(loaded!!)
        }
        _saved.value = true
    }

    fun delete() = viewModelScope.launch {
        if (!isNew) repo.markDeleted(recordId)
        _saved.value = true
    }

    /** 列表项删除（按 id 软删）。 */
    fun deleteItem(id: String) = viewModelScope.launch { repo.markDeleted(id) }
}
