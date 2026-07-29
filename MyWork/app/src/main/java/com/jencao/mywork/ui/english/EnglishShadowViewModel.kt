package com.jencao.mywork.ui.english

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.local.entity.EnglishWordEntity
import com.jencao.mywork.data.remote.ApiService
import com.jencao.mywork.data.repository.EnglishWordRepository
import com.jencao.mywork.data.repository.ProxyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 跟读练习 ViewModel：加载单词、向服务端代理请求原音地址（有道 speak_url），
 * 并保存用户录音的本地文件路径。录音文件仅存于设备，不上传。
 */
@HiltViewModel
class EnglishShadowViewModel @Inject constructor(
    private val repo: EnglishWordRepository,
    api: ApiService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val proxy = ProxyRepository(api)

    private val wordId: String = savedStateHandle["wordId"] ?: ""

    private val _word = MutableStateFlow<EnglishWordEntity?>(null)
    val word: StateFlow<EnglishWordEntity?> = _word.asStateFlow()

    private val _speakUrl = MutableStateFlow("")
    val speakUrl: StateFlow<String> = _speakUrl.asStateFlow()

    private val _audioPath = MutableStateFlow<String?>(null)
    val audioPath: StateFlow<String?> = _audioPath.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { load() }

    private fun load() = viewModelScope.launch {
        val w = repo.getById(wordId)
        _word.value = w
        _audioPath.value = w?.audioPath
        if (w != null && w.word.isNotBlank()) {
            _loading.value = true
            proxy.lookupWord(w.word)
                .onSuccess { _speakUrl.value = it.speak_url }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    /** 保存（或清除）跟读录音的本地路径。 */
    fun saveAudio(path: String?) = viewModelScope.launch {
        _audioPath.value = path
        val w = _word.value ?: return@launch
        repo.updateAudioPath(w.id, path)
        _word.value = w.copy(audioPath = path)
    }
}
