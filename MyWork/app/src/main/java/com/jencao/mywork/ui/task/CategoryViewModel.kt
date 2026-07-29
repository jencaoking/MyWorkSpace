package com.jencao.mywork.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.local.entity.CategoryEntity
import com.jencao.mywork.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repo: CategoryRepository
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    private val _editing = MutableStateFlow<CategoryEntity?>(null)
    val editing: StateFlow<CategoryEntity?> = _editing.asStateFlow()

    fun openNew() { _editing.value = null; _showDialog.value = true }
    fun openEdit(c: CategoryEntity) { _editing.value = c; _showDialog.value = true }
    fun close() { _showDialog.value = false; _editing.value = null }

    fun save(name: String, color: String) = viewModelScope.launch {
        val e = _editing.value
        if (e != null) {
            e.name = name
            e.color = color
            repo.update(e)
        } else {
            repo.add(name, color)
        }
        close()
    }

    fun delete(c: CategoryEntity) = viewModelScope.launch { repo.delete(c) }
    fun moveUp(c: CategoryEntity, prev: CategoryEntity) = viewModelScope.launch { repo.moveUp(c, prev) }
    fun moveDown(c: CategoryEntity, next: CategoryEntity) = viewModelScope.launch { repo.moveDown(c, next) }
}
