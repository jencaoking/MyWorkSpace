package com.jencao.mywork.ui.toolbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.local.entity.QrScanEntity
import com.jencao.mywork.data.repository.QrScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QrViewModel @Inject constructor(private val repo: QrScanRepository) : ViewModel() {
    val history: StateFlow<List<QrScanEntity>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(content: String, format: String = "QR", note: String = "") = viewModelScope.launch {
        if (content.isBlank()) return@launch
        repo.insert(QrScanEntity(content = content, format = format, note = note))
    }

    fun delete(id: String) = viewModelScope.launch { repo.softDelete(id) }
}
