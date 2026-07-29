package com.jencao.mywork.ui.media

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.local.entity.MovieBookEntity
import com.jencao.mywork.data.repository.MovieBookRepository
import com.jencao.mywork.ui.navigation.MediaRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 影音类型 */
val MEDIA_TYPES = listOf("movie" to "电影", "book" to "书籍")

/** 观看状态：想看 / 在看 / 看过 */
val MEDIA_STATUS = listOf("wish" to "想看", "watching" to "在看", "done" to "看过")

fun mediaStatusLabel(s: String?) = MEDIA_STATUS.firstOrNull { it.first == s }?.second ?: s ?: ""

fun mediaTypeLabel(t: String?) = MEDIA_TYPES.firstOrNull { it.first == t }?.second ?: t ?: ""

/** 影音模块 ViewModel：列表（类型/状态筛选）+ 编辑共用。 */
@HiltViewModel
class MediaViewModel @Inject constructor(
    private val repo: MovieBookRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _typeFilter = MutableStateFlow<String?>(null)
    val typeFilter: StateFlow<String?> = _typeFilter

    private val _statusFilter = MutableStateFlow<String?>(null)
    val statusFilter: StateFlow<String?> = _statusFilter

    val items: StateFlow<List<MovieBookEntity>> = _typeFilter
        .flatMapLatest { repo.observeByType(it) }
        .combine(_statusFilter) { list, status ->
            if (status == null) list else list.filter { it.status == status }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTypeFilter(t: String?) { _typeFilter.value = t }
    fun setStatusFilter(s: String?) { _statusFilter.value = s }

    // —— 编辑字段 ——
    private val mediaId: String = savedStateHandle["mediaId"] ?: MediaRoutes.NEW_ID
    val isNew: Boolean get() = mediaId == MediaRoutes.NEW_ID

    private val _type = MutableStateFlow("movie")
    val type: StateFlow<String> = _type

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title

    private val _tmdbId = MutableStateFlow("")
    val tmdbId: StateFlow<String> = _tmdbId

    private val _status = MutableStateFlow("wish")
    val status: StateFlow<String> = _status

    private val _rating = MutableStateFlow(0)
    val rating: StateFlow<Int> = _rating

    private val _posterUrl = MutableStateFlow<String?>(null)
    val posterUrl: StateFlow<String?> = _posterUrl

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    private var loaded: MovieBookEntity? = null

    init {
        if (!isNew) {
            viewModelScope.launch {
                repo.getById(mediaId)?.let {
                    loaded = it
                    _type.value = it.type.ifEmpty { "movie" }
                    _title.value = it.title
                    _tmdbId.value = it.tmdbId?.toString() ?: ""
                    _status.value = it.status.ifEmpty { "wish" }
                    _rating.value = it.rating?.toInt() ?: 0
                    _posterUrl.value = it.posterUrl
                    _note.value = it.note
                }
            }
        }
    }

    fun setType(v: String) { _type.value = v }
    fun setTitle(v: String) { _title.value = v }
    fun setTmdbId(v: String) { _tmdbId.value = v.filter { it.isDigit() } }
    fun setStatus(v: String) { _status.value = v }
    fun setRating(v: Int) { _rating.value = v.coerceIn(0, 5) }
    fun setPosterUrl(v: String) { _posterUrl.value = v.takeIf { it.isNotBlank() } }
    fun setNote(v: String) { _note.value = v }

    fun save() = viewModelScope.launch {
        val t = _title.value.trim()
        if (t.isEmpty()) { _saved.value = true; return@launch }
        val tmdb = _tmdbId.value.takeIf { it.isNotBlank() }
        val ratingVal = if (_rating.value > 0) _rating.value.toFloat() else null
        if (loaded == null) {
            repo.create(
                type = _type.value,
                title = t,
                tmdbId = tmdb,
                status = _status.value,
                rating = ratingVal,
                posterUrl = _posterUrl.value,
                note = _note.value.trim()
            )
        } else {
            loaded!!.apply {
                type = _type.value
                title = t
                this.tmdbId = tmdb ?: ""
                status = _status.value
                rating = ratingVal ?: 0f
                posterUrl = _posterUrl.value
                note = _note.value.trim()
            }
            repo.upsert(loaded!!)
        }
        _saved.value = true
    }

    fun delete() = viewModelScope.launch {
        if (!isNew) repo.markDeleted(mediaId)
        _saved.value = true
    }

    /** 列表项删除（按 id 软删）。 */
    fun deleteItem(id: String) = viewModelScope.launch { repo.markDeleted(id) }
}
