package com.jencao.mywork.ui.english

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.local.entity.EnglishWordEntity
import com.jencao.mywork.data.repository.EnglishWordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EnglishReviewViewModel @Inject constructor(
    private val repo: EnglishWordRepository
) : ViewModel() {

    private val _due = MutableStateFlow<List<EnglishWordEntity>>(emptyList())
    val due: StateFlow<List<EnglishWordEntity>> = _due.asStateFlow()

    private val _index = MutableStateFlow(0)
    val index: StateFlow<Int> = _index.asStateFlow()

    private val _revealed = MutableStateFlow(false)
    val revealed: StateFlow<Boolean> = _revealed.asStateFlow()

    val total: Int get() = _due.value.size
    val reviewedCount: Int get() = _index.value

    init {
        viewModelScope.launch {
            _due.value = repo.getDueForReview(System.currentTimeMillis())
        }
    }

    fun reveal() {
        _revealed.value = true
    }

    fun grade(quality: Int) = viewModelScope.launch {
        val w = _due.value.getOrNull(_index.value) ?: return@launch
        repo.reviewWord(w, quality)
        _index.value += 1
        _revealed.value = false
    }

    fun current(): EnglishWordEntity? = _due.value.getOrNull(_index.value)
}
