package com.jencao.mywork.ui.english

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.local.entity.EnglishWordEntity
import com.jencao.mywork.data.remote.ApiService
import com.jencao.mywork.data.remote.model.WordLookupData
import com.jencao.mywork.data.remote.model.TranslateData
import com.jencao.mywork.data.repository.EnglishWordRepository
import com.jencao.mywork.data.repository.ProxyRepository
import com.jencao.mywork.ui.navigation.EnglishRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 英语模块 ViewModel：列表（含今日待复习切换）+ 编辑共用。 */
@HiltViewModel
class EnglishViewModel @Inject constructor(
    private val repo: EnglishWordRepository,
    api: ApiService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val proxy = ProxyRepository(api)

    /** 是否只看今日待复习（nextReview <= now）。 */
    private val _dueOnly = MutableStateFlow(false)
    val dueOnly: StateFlow<Boolean> = _dueOnly

    val items: StateFlow<List<EnglishWordEntity>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setDueOnly(v: Boolean) { _dueOnly.value = v }

    // —— 编辑字段 ——
    private val wordId: String = savedStateHandle["wordId"] ?: EnglishRoutes.NEW_ID
    val isNew: Boolean get() = wordId == EnglishRoutes.NEW_ID

    private val _word = MutableStateFlow("")
    val word: StateFlow<String> = _word

    private val _phonetic = MutableStateFlow("")
    val phonetic: StateFlow<String> = _phonetic

    private val _meaning = MutableStateFlow("")
    val meaning: StateFlow<String> = _meaning

    private val _example = MutableStateFlow("")
    val example: StateFlow<String> = _example

    private val _familiarity = MutableStateFlow(0)
    val familiarity: StateFlow<Int> = _familiarity

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    private var loaded: EnglishWordEntity? = null

    init {
        if (!isNew) {
            viewModelScope.launch {
                repo.getById(wordId)?.let {
                    loaded = it
                    _word.value = it.word
                    _phonetic.value = it.phonetic
                    _meaning.value = it.meaning
                    _example.value = it.example
                    _familiarity.value = it.familiarity
                }
            }
        }
    }

    fun setWord(v: String) { _word.value = v }
    fun setPhonetic(v: String) { _phonetic.value = v }
    fun setMeaning(v: String) { _meaning.value = v }
    fun setExample(v: String) { _example.value = v }
    fun setFamiliarity(v: Int) { _familiarity.value = v.coerceIn(0, 5) }

    fun save() = viewModelScope.launch {
        val w = _word.value.trim()
        if (w.isEmpty()) { _saved.value = true; return@launch }
        if (loaded == null) {
            repo.create(
                word = w,
                phonetic = _phonetic.value.trim(),
                meaning = _meaning.value.trim(),
                example = _example.value.trim(),
                familiarity = _familiarity.value
            )
        } else {
            loaded!!.apply {
                word = w
                phonetic = _phonetic.value.trim()
                meaning = _meaning.value.trim()
                example = _example.value.trim()
                familiarity = _familiarity.value
            }
            repo.upsert(loaded!!)
        }
        _saved.value = true
    }

    fun delete() = viewModelScope.launch {
        if (!isNew) repo.markDeleted(wordId)
        _saved.value = true
    }

    /** 列表项删除（按 id 软删）。 */
    fun deleteItem(id: String) = viewModelScope.launch { repo.markDeleted(id) }

    // —— 外部 API 代理（密钥在服务端后台管理，App 不持有密钥） ——

    /** 查询单词音标 / 释义 / 发音（有道），供编辑页自动填充。 */
    suspend fun lookupWord(text: String) = proxy.lookupWord(text)

    /** 文本翻译。 */
    suspend fun translate(
        text: String,
        from: String = "auto",
        to: String = "zh-CHS"
    ) = proxy.translate(text, from, to)

    /** 从翻译结果快速新建单词（返回新 id，用于跳转编辑补全释义）。 */
    suspend fun createWord(word: String, meaning: String = ""): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val entity = EnglishWordEntity(word = word, meaning = meaning)
        entity.id = id
        entity.lastModified = now
        entity.needsSync = true
        repo.upsert(entity)
        return id
    }
}
