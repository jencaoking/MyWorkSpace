package com.jencao.mywork.ui.toolbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.jencao.mywork.ui.components.NeuFab
import kotlinx.coroutines.launch

@Composable
fun FlashcardScreen(navController: NavHostController, padding: PaddingValues, vm: FlashcardViewModel = hiltViewModel()) {
    val decks by vm.decks.collectAsStateWithLifecycle()
    var selectedDeckId by remember { mutableStateOf<String?>(null) }
    var showDeck by remember { mutableStateOf(false) }
    var showCard by remember { mutableStateOf(false) }
    var studying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val cards by vm.observeCards(selectedDeckId ?: "").collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            if (selectedDeckId == null) {
                Text("闪卡牌组", style = MaterialTheme.typography.titleMedium)
                LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(decks, key = { it.id }) { deck ->
                        Card(Modifier.fillMaxWidth().clickable { selectedDeckId = deck.id }) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Text(deck.name, style = MaterialTheme.typography.titleSmall)
                                if (deck.description.isNotBlank()) Text(deck.description, style = MaterialTheme.typography.bodySmall)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    IconButton(onClick = { vm.deleteDeck(deck.id); selectedDeckId = null }) { Icon(Icons.Filled.Delete, "删除") }
                                }
                            }
                        }
                    }
                }
            } else {
                val deck = decks.firstOrNull { it.id == selectedDeckId }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { selectedDeckId = null }) { Text("← 返回") }
                    Text(deck?.name ?: "", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { scope.launch { if (vm.due(selectedDeckId!!).isNotEmpty()) studying = true } }) { Text("学习") }
                }
                LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(cards, key = { it.id }) { card ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Text(card.front, style = MaterialTheme.typography.bodyMedium)
                                Text(card.back, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    IconButton(onClick = { vm.deleteCard(card.id) }) { Icon(Icons.Filled.Delete, "删除") }
                                }
                            }
                        }
                    }
                }
                NeuFab(onClick = { showCard = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
                    Icon(Icons.Filled.Add, "加卡")
                }
            }
        }
        if (selectedDeckId == null) {
            NeuFab(onClick = { showDeck = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
                Icon(Icons.Filled.Add, "加组")
            }
        }
    }

    if (showDeck) AddDeckDialog(onDismiss = { showDeck = false }, onConfirm = { n, d -> vm.addDeck(n, d); showDeck = false })
    if (showCard && selectedDeckId != null) AddCardDialog(onDismiss = { showCard = false }, onConfirm = { f, b -> vm.addCard(selectedDeckId!!, f, b); showCard = false })

    if (studying && selectedDeckId != null) {
        StudySheet(deckId = selectedDeckId!!, vm = vm, onClose = { studying = false })
    }
}

@Composable
private fun StudySheet(deckId: String, vm: FlashcardViewModel, onClose: () -> Unit) {
    var due by remember { mutableStateOf(emptyList<com.jencao.mywork.data.local.entity.FlashcardEntity>()) }
    var idx by remember { mutableStateOf(0) }
    var flipped by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    androidx.compose.runtime.LaunchedEffect(deckId) { due = vm.due(deckId) }

    AlertDialog(onDismissRequest = onClose, confirmButton = {}, title = { Text("学习（${idx + 1}/${due.size}）") }, text = {
        if (due.isEmpty()) { Text("今日没有待复习卡片 🎉"); TextButton(onClose) { Text("关闭") } }
        else {
            val card = due[idx]
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (flipped) card.back else card.front, style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = { flipped = !flipped }) { Text(if (flipped) "看正面" else "看答案") }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("忘了" to 1, "模糊" to 3, "记得" to 5).forEach { (label, q) ->
                        TextButton(onClick = {
                            scope.launch {
                                vm.review(card, q)
                                val rest = vm.due(deckId)
                                due = rest; idx = 0; flipped = false
                                if (rest.isEmpty()) onClose()
                            }
                        }) { Text(label) }
                    }
                }
            }
        }
    })
}

@Composable
private fun AddDeckDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var n by remember { mutableStateOf("") }; var d by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, confirmButton = { TextButton(onClick = { onConfirm(n, d) }) { Text("保存") } },
        dismissButton = { TextButton(onDismiss) { Text("取消") } }, title = { Text("新增牌组") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(n, { n = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(d, { d = it }, label = { Text("描述（可选）") }, modifier = Modifier.fillMaxWidth())
            }
        })
}

@Composable
private fun AddCardDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var f by remember { mutableStateOf("") }; var b by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, confirmButton = { TextButton(onClick = { onConfirm(f, b) }) { Text("保存") } },
        dismissButton = { TextButton(onDismiss) { Text("取消") } }, title = { Text("新增卡片") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(f, { f = it }, label = { Text("正面") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(b, { b = it }, label = { Text("背面") }, modifier = Modifier.fillMaxWidth())
            }
        })
}
