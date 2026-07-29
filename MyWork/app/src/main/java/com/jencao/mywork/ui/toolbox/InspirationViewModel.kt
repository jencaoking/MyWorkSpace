package com.jencao.mywork.ui.toolbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.local.entity.InspirationEntity
import com.jencao.mywork.data.remote.ApiService
import com.jencao.mywork.data.remote.model.AiRequest
import com.jencao.mywork.data.repository.InspirationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InspirationViewModel @Inject constructor(
    private val repo: InspirationRepository,
    private val api: ApiService
) : ViewModel() {
    val items: StateFlow<List<InspirationEntity>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favorites = repo.observeFavorites().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(content: String, author: String, source: String, tags: String) = viewModelScope.launch {
        if (content.isBlank()) return@launch
        repo.insert(InspirationEntity(content = content, author = author, source = source, tags = tags))
    }
    fun toggleFav(id: String, fav: Boolean) = viewModelScope.launch { repo.setFavorite(id, fav) }
    fun delete(id: String) = viewModelScope.launch { repo.softDelete(id) }

    /** AI 提取要点（密钥在服务端后台管理） */
    fun extract(content: String, onResult: (String) -> Unit) = viewModelScope.launch {
        try {
            val r = api.ai(AiRequest(action = "extract", content = content))
            onResult(if (r.code == 0) (r.data?.result ?: "无结果") else r.message)
        } catch (e: Exception) {
            onResult("网络错误：${e.message}")
        }
    }
}
