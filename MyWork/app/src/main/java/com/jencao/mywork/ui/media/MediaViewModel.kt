package com.jencao.mywork.ui.media

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.local.entity.MovieBookEntity
import com.jencao.mywork.data.remote.ApiService
import com.jencao.mywork.data.remote.model.TmdbItem
import com.jencao.mywork.data.repository.MovieBookRepository
import com.jencao.mywork.data.repository.ProxyRepository
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
val MEDIA_TYPES = listOf("movie" to "电影", "tv" to "剧集", "book" to "书籍")

/** 观看状态：想看 / 在看 / 看过 */
val MEDIA_STATUS = listOf("wish" to "想看", "watching" to "在看", "done" to "看过")

fun mediaStatusLabel(s: String?) = MEDIA_STATUS.firstOrNull { it.first == s }?.second ?: s ?: ""

fun mediaTypeLabel(t: String?) = MEDIA_TYPES.firstOrNull { it.first == t }?.second ?: t ?: ""

/** 影音模块 ViewModel：列表（类型/状态筛选）+ 编辑共用。 */
@HiltViewModel
class MediaViewModel @Inject constructor(
    private val repo: MovieBookRepository,
    private val api: ApiService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** 第三方 API 代理：密钥仅在服务端，App 经后端 /api/proxy/tmdb/search 检索。 */
    private val proxy = ProxyRepository(api)

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
                posterUrl = _posterUrl.value ?: ""
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

    // —— TMDB 搜索（服务端代理，密钥不落客户端） ——
    private val _tmdbQuery = mutableStateOf("")
    val tmdbQuery: State<String> get() = _tmdbQuery

    private val _tmdbResults = mutableStateOf<List<TmdbItem>>(emptyList())
    val tmdbResults: State<List<TmdbItem>> get() = _tmdbResults

    private val _tmdbSearching = mutableStateOf(false)
    val tmdbSearching: State<Boolean> get() = _tmdbSearching

    private val _tmdbError = mutableStateOf<String?>(null)
    val tmdbError: State<String?> get() = _tmdbError

    fun setTmdbQuery(v: String) { _tmdbQuery.value = v }

    /** 调用后端代理搜索 TMDB（movie / tv）。 */
    fun searchTmdb() {
        val q = _tmdbQuery.value.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            _tmdbSearching.value = true
            _tmdbError.value = null
            proxy.searchTmdb(q)
                .onSuccess { data -> _tmdbResults.value = data.results }
                .onFailure { e -> _tmdbError.value = e.message ?: "搜索失败" }
            _tmdbSearching.value = false
        }
    }

    /**
     * 由 TMDB 搜索结果直接入库（与 TMDB 数据 100% 吻合：标题、海报、原名、简介、上映日、评分）。
     * 返回新建条目 id，便于跳转编辑页补全状态 / 评分 / 备注。
     */
    suspend fun addFromTmdb(item: TmdbItem): String {
        val created = repo.createFromTmdb(
            type = item.media_type.ifBlank { "movie" },
            title = item.title.ifBlank { item.original_title },
            tmdbId = item.tmdb_id.toString(),
            posterUrl = item.poster_url,
            originalTitle = item.original_title,
            overview = item.overview,
            releaseDate = item.release_date,
            voteAverage = item.vote_average
        )
        return created.id
    }
}
