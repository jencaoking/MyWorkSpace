package com.jencao.mywork.ui.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.local.entity.NoteEntity
import com.jencao.mywork.data.repository.NoteRepository
import com.jencao.mywork.ui.navigation.NoteRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 笔记编辑 ViewModel：noteId == "new" 时为新建，保存时创建。 */
@HiltViewModel
class NoteEditViewModel @Inject constructor(
    private val repo: NoteRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val noteId: String = savedStateHandle["noteId"] ?: NoteRoutes.NEW_ID
    val isNew: Boolean get() = noteId == NoteRoutes.NEW_ID

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content

    /** 预览模式开关 */
    private val _preview = MutableStateFlow(false)
    val preview: StateFlow<Boolean> = _preview

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    private var loaded: NoteEntity? = null

    init {
        if (!isNew) {
            viewModelScope.launch {
                repo.getById(noteId)?.let {
                    loaded = it
                    _title.value = it.title
                    _content.value = it.content
                }
            }
        }
    }

    fun setTitle(v: String) { _title.value = v }
    fun setContent(v: String) { _content.value = v }
    fun togglePreview() { _preview.value = !_preview.value }

    /** 有实际内容才落库；保存成功置 saved 供页面返回 */
    fun save() = viewModelScope.launch {
        val t = _title.value.trim()
        val c = _content.value
        if (t.isEmpty() && c.isBlank()) {
            _saved.value = true
            return@launch
        }
        val existing = loaded
        if (existing == null) {
            repo.create(title = t.ifEmpty { "无标题" }, content = c)
        } else {
            existing.title = t.ifEmpty { "无标题" }
            existing.content = c
            repo.save(existing)
        }
        _saved.value = true
    }
}
