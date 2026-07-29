package com.jencao.mywork.ui.toolbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.local.entity.CountdownEntity
import com.jencao.mywork.data.repository.CountdownRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CountdownViewModel @Inject constructor(private val repo: CountdownRepository) : ViewModel() {
    val items: StateFlow<List<CountdownEntity>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(title: String, targetTime: Long, remark: String) = viewModelScope.launch {
        if (title.isBlank()) return@launch
        repo.insert(CountdownEntity(title = title, targetTime = targetTime, remark = remark))
    }

    fun delete(id: String) = viewModelScope.launch { repo.softDelete(id) }
}
