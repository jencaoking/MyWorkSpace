package com.jencao.mywork.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.local.entity.NoteEntity
import com.jencao.mywork.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 笔记列表 + 搜索共用 ViewModel（阶段3）。 */
@OptIn(FlowPreview::class)
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repo: NoteRepository
) : ViewModel() {

    /** 仅看收藏开关 */
    private val _favoritesOnly = MutableStateFlow(false)
    val favoritesOnly: StateFlow<Boolean> = _favoritesOnly

    val notes: StateFlow<List<NoteEntity>> =
        combine(repo.observeAll(), _favoritesOnly) { all, favOnly ->
            if (favOnly) all.filter { it.isFavorite } else all
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 搜索关键字与结果（FTS + LIKE 兜底，300ms 防抖） */
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _searchResults = MutableStateFlow<List<NoteEntity>>(emptyList())
    val searchResults: StateFlow<List<NoteEntity>> = _searchResults

    init {
        viewModelScope.launch {
            _query.debounce(300).collect { kw ->
                _searchResults.value = if (kw.isBlank()) emptyList() else repo.search(kw)
            }
        }
    }

    fun setFavoritesOnly(value: Boolean) { _favoritesOnly.value = value }

    fun setQuery(value: String) { _query.value = value }

    fun togglePin(note: NoteEntity) = viewModelScope.launch {
        repo.setPinned(note.id, !note.isPinned)
        refreshSearch()
    }

    fun toggleFavorite(note: NoteEntity) = viewModelScope.launch {
        repo.setFavorite(note.id, !note.isFavorite)
        refreshSearch()
    }

    fun delete(note: NoteEntity) = viewModelScope.launch {
        repo.delete(note.id)
        refreshSearch()
    }

    private suspend fun refreshSearch() {
        val kw = _query.value
        if (kw.isNotBlank()) _searchResults.value = repo.search(kw)
    }
}
