package com.jencao.mywork.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.search.GlobalSearchRepository
import com.jencao.mywork.data.search.GlobalSearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 全局搜索 ViewModel：对输入做 300ms 防抖，触发跨模块检索并暴露结果流。
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class GlobalSearchViewModel @Inject constructor(
    private val repo: GlobalSearchRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _results = MutableStateFlow<List<GlobalSearchResult>>(emptyList())
    val results: StateFlow<List<GlobalSearchResult>> = _results

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    init {
        viewModelScope.launch {
            _query
                .debounce(300)
                .distinctUntilChanged()
                .collect { kw ->
                    if (kw.isBlank()) {
                        _results.value = emptyList()
                        _isSearching.value = false
                        return@collect
                    }
                    _isSearching.value = true
                    _results.value = repo.search(kw)
                    _isSearching.value = false
                }
        }
    }

    fun setQuery(value: String) {
        _query.value = value
    }

    fun clear() {
        _query.value = ""
    }
}
