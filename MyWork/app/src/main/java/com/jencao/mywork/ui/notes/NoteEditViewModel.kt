package com.jencao.mywork.ui.notes

import android.app.Application
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.local.entity.NoteEntity
import com.jencao.mywork.data.remote.ApiService
import com.jencao.mywork.data.remote.model.NoteImageData
import com.jencao.mywork.data.repository.NoteRepository
import com.jencao.mywork.ui.navigation.NoteRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

/** 笔记编辑 ViewModel：noteId == "new" 时为新建，保存时创建。 */
@HiltViewModel
class NoteEditViewModel @Inject constructor(
    private val app: Application,
    private val repo: NoteRepository,
    private val api: ApiService,
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

    /** 图片上传中指示 */
    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading

    /** 图片上传错误提示 */
    private val _imageError = MutableStateFlow<String?>(null)
    val imageError: StateFlow<String?> = _imageError

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
    fun showPreview() { _preview.value = true }
    fun clearImageError() { _imageError.value = null }

    /**
     * 选图后调用：先把图片复制到应用私有目录（保证本地可永久预览），
     * 再以 Markdown 图片语法 ![图片](file://...) 追加到正文，随后上传到后端；
     * 上传成功后用返回的远程 URL 替换本地引用（多端同步即可访问）。
     * 若上传失败（如离线），保留本地引用，笔记仍可本地查看。
     */
    fun addImage(uri: Uri) {
        viewModelScope.launch {
            _uploading.value = true
            _imageError.value = null
            try {
                val localFile = copyToInternal(uri)
                val marker = "file://" + localFile.absolutePath
                appendContent("![图片]($marker)")
                val data: NoteImageData? = api.uploadNoteImage(buildPart(localFile)).data
                val url = data?.url
                if (!url.isNullOrEmpty()) {
                    _content.value = _content.value.replace(marker, url)
                }
            } catch (e: Exception) {
                _imageError.value = "图片上传失败：${e.message}"
            } finally {
                _uploading.value = false
            }
        }
    }

    /** 把 content:// 或 file:// 图片复制到 filesDir/note_images/ 下，返回本地文件 */
    private fun copyToInternal(uri: Uri): File {
        val dir = File(app.filesDir, "note_images").apply { if (!exists()) mkdirs() }
        val ext = resolveExt(uri) ?: "jpg"
        val file = File(dir, "${UUID.randomUUID()}.$ext")
        app.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { out -> input.copyTo(out) }
        } ?: throw IOException("无法读取所选图片")
        return file
    }

    private fun resolveExt(uri: Uri): String? {
        val type = app.contentResolver.getType(uri)
        if (type != null) {
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
            if (!ext.isNullOrEmpty()) return ext
        }
        val seg = uri.lastPathSegment ?: return null
        val dot = seg.lastIndexOf('.')
        if (dot in 1 until seg.length - 1) {
            val e = seg.substring(dot + 1)
            if (e.length <= 4) return e
        }
        return null
    }

    private fun buildPart(file: File): MultipartBody.Part {
        val reqFile = file.asRequestBody("image/*".toMediaType())
        return MultipartBody.Part.createFormData("file", file.name, reqFile)
    }

    private fun appendContent(snippet: String) {
        val cur = _content.value
        _content.value = if (cur.isEmpty() || cur.endsWith("\n")) cur + snippet else "$cur\n$snippet"
    }

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
