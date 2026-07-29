package com.jencao.mywork.ui.health

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.local.entity.HealthRecordEntity
import com.jencao.mywork.data.repository.HealthRecordRepository
import com.jencao.mywork.reminder.ReminderScheduler
import com.jencao.mywork.ui.navigation.HealthRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 健康记录类型：就诊 / 用药 / 复诊 / 体征 */
val HEALTH_TYPES = listOf("visit" to "就诊", "medication" to "用药", "revisit" to "复诊", "sign" to "体征")

fun healthTypeLabel(t: String?) = HEALTH_TYPES.firstOrNull { it.first == t }?.second ?: t ?: ""

/** 健康模块 ViewModel：列表（类型筛选）+ 编辑共用。 */
@HiltViewModel
class HealthViewModel @Inject constructor(
    private val repo: HealthRecordRepository,
    private val application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val _typeFilter = MutableStateFlow<String?>(null)
    val typeFilter: StateFlow<String?> = _typeFilter

    val items: StateFlow<List<HealthRecordEntity>> = _typeFilter
        .flatMapLatest { repo.observeByType(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTypeFilter(t: String?) { _typeFilter.value = t }

    // —— 编辑字段 ——
    private val recordId: String = savedStateHandle["healthId"] ?: HealthRoutes.NEW_ID
    val isNew: Boolean get() = recordId == HealthRoutes.NEW_ID

    private val _type = MutableStateFlow("visit")
    val type: StateFlow<String> = _type

    private val _value = MutableStateFlow("")
    val value: StateFlow<String> = _value

    private val _unit = MutableStateFlow("")
    val unit: StateFlow<String> = _unit

    private val _time = MutableStateFlow(System.currentTimeMillis())
    val time: StateFlow<Long> = _time

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note

    private val _reminderTime = MutableStateFlow<Long?>(null)
    val reminderTime: StateFlow<Long?> = _reminderTime

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    private var loaded: HealthRecordEntity? = null

    init {
        if (!isNew) {
            viewModelScope.launch {
                repo.getById(recordId)?.let {
                    loaded = it
                    _type.value = it.type.ifEmpty { "visit" }
                    _value.value = it.value.toString()
                    _unit.value = it.unit
                    _time.value = it.recordTime
                    _note.value = it.note
                    _reminderTime.value = it.reminderTime
                }
            }
        }
    }

    fun setType(v: String) { _type.value = v }
    fun setValue(v: String) { _value.value = v }
    fun setUnit(v: String) { _unit.value = v }
    fun setTime(v: Long) { _time.value = v }
    fun setNote(v: String) { _note.value = v }
    fun setReminderTime(v: Long?) { _reminderTime.value = v }

    /** 安排 / 取消该记录的提醒（保存或删除后调用）。 */
    private fun armReminder(entity: HealthRecordEntity) {
        val ctx = getApplication<Application>().applicationContext
        ReminderScheduler.reschedule(ctx, entity)
    }

    fun save() = viewModelScope.launch {
        val v = _value.value.toFloatOrNull() ?: 0f
        val reminder = _reminderTime.value
        val entity = if (loaded == null) {
            repo.create(
                type = _type.value,
                value = v,
                unit = _unit.value.trim(),
                recordTime = _time.value,
                note = _note.value.trim(),
                reminderTime = reminder
            )
        } else {
            loaded!!.apply {
                type = _type.value
                value = v
                unit = _unit.value.trim()
                recordTime = _time.value
                note = _note.value.trim()
                reminderTime = reminder
            }
            repo.upsert(loaded!!)
            loaded!!
        }
        armReminder(entity)
        _saved.value = true
    }

    fun delete() = viewModelScope.launch {
        if (!isNew) {
            ReminderScheduler.cancel(getApplication<Application>().applicationContext, recordId)
            repo.markDeleted(recordId)
        }
        _saved.value = true
    }

    /** 列表项删除（按 id 软删）；同时取消其提醒闹钟。 */
    fun deleteItem(id: String) = viewModelScope.launch {
        ReminderScheduler.cancel(getApplication<Application>().applicationContext, id)
        repo.markDeleted(id)
    }
}
