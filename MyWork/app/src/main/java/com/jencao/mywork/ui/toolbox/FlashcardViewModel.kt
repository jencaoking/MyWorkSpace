package com.jencao.mywork.ui.toolbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jencao.mywork.data.local.entity.FlashcardDeckEntity
import com.jencao.mywork.data.local.entity.FlashcardEntity
import com.jencao.mywork.data.repository.FlashcardDeckRepository
import com.jencao.mywork.data.repository.FlashcardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlashcardViewModel @Inject constructor(
    private val deckRepo: FlashcardDeckRepository,
    private val cardRepo: FlashcardRepository
) : ViewModel() {
    val decks: StateFlow<List<FlashcardDeckEntity>> =
        deckRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun observeCards(deckId: String): Flow<List<FlashcardEntity>> = cardRepo.observeByDeck(deckId)

    fun addDeck(name: String, desc: String) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        deckRepo.insert(FlashcardDeckEntity(name = name, description = desc))
    }
    fun deleteDeck(id: String) = viewModelScope.launch { deckRepo.softDelete(id) }
    fun addCard(deckId: String, front: String, back: String) = viewModelScope.launch {
        if (front.isBlank()) return@launch
        cardRepo.insert(FlashcardEntity(deckId = deckId, front = front, back = back))
    }
    fun deleteCard(id: String) = viewModelScope.launch { cardRepo.softDelete(id) }
    fun review(card: FlashcardEntity, quality: Int) = viewModelScope.launch { cardRepo.review(card, quality) }
    suspend fun due(deckId: String) = cardRepo.getDue(deckId)
}
