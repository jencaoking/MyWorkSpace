package com.jencao.mywork.ui.account

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.model.MonthlyTrendItem
import com.jencao.mywork.data.repository.AccountRecordRepository
import com.jencao.mywork.ui.navigation.AccountRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

data class AccountStats(
    val income: Double = 0.0,
    val expense: Double = 0.0
) {
    val balance: Double get() = income - expense
}

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val repo: AccountRecordRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val items = repo.observeAll().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val stats = items.map { list ->
        var income = 0.0
        var expense = 0.0
        list.forEach {
            if (it.type == "income") income += it.amount else expense += it.amount
        }
        AccountStats(income, expense)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccountStats())

    private val zone = ZoneId.systemDefault()
    private val currentYm = YearMonth.now()
    private val monthStart = currentYm.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    private val monthEnd = currentYm.atEndOfMonth().atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

    /** 本月收支汇总。 */
    val monthly: StateFlow<MonthlyTrendItem> = flow {
        emit(repo.monthlySummary(monthStart, monthEnd))
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MonthlyTrendItem(currentYm.year, currentYm.monthValue, 0.0, 0.0)
    )

    /** 最近 6 个月收支趋势。 */
    val monthlyTrend: StateFlow<List<MonthlyTrendItem>> = flow {
        emit(repo.monthlyTrend(6))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val accountId: String = savedStateHandle["accountId"] ?: AccountRoutes.NEW_ID
    val isNew: Boolean get() = accountId == AccountRoutes.NEW_ID

    private val _type = MutableStateFlow("expense")
    val type: StateFlow<String> = _type.asStateFlow()

    private val _category = MutableStateFlow("")
    val category: StateFlow<String> = _category.asStateFlow()

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _currency = MutableStateFlow("CNY")
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _recordDate = MutableStateFlow(System.currentTimeMillis())
    val recordDate: StateFlow<Long> = _recordDate.asStateFlow()

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    init {
        if (!isNew) {
            viewModelScope.launch {
                repo.getById(accountId)?.let { item ->
                    _type.value = item.type
                    _category.value = item.category
                    _amount.value = if (item.amount == 0f) "" else item.amount.toString()
                    _currency.value = item.currency
                    _recordDate.value = item.recordDate
                    _note.value = item.note
                }
            }
        }
    }

    fun setType(v: String) { _type.value = v }
    fun setCategory(v: String) { _category.value = v }
    fun setAmount(v: String) { _amount.value = v }
    fun setCurrency(v: String) { _currency.value = v }
    fun setRecordDate(v: Long) { _recordDate.value = v }
    fun setNote(v: String) { _note.value = v }

    fun save() = viewModelScope.launch {
        val amt = _amount.value.toFloatOrNull() ?: 0f
        if (isNew) {
            repo.create(_type.value, _category.value, amt, _currency.value, _recordDate.value, _note.value)
        } else {
            repo.getById(accountId)?.let { item ->
                item.type = _type.value
                item.category = _category.value
                item.amount = amt
                item.currency = _currency.value
                item.recordDate = _recordDate.value
                item.note = _note.value
                repo.upsert(item)
            }
        }
    }

    fun delete() = viewModelScope.launch { repo.markDeleted(accountId) }

    fun deleteItem(id: String) = viewModelScope.launch { repo.markDeleted(id) }
}
