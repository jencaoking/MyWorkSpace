package com.jencao.mywork.ui.toolbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.jencao.mywork.ui.components.NeuFab

@Composable
fun InspirationScreen(navController: NavHostController, padding: PaddingValues, vm: InspirationViewModel = hiltViewModel()) {
    val items by vm.items.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var aiResult by remember { mutableStateOf<String?>(null) }
    val list = if (tab == 0) items else favorites

    Box(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            TabRow(tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }) { Text("全部") }
                Tab(selected = tab == 1, onClick = { tab = 1 }) { Text("收藏") }
            }
            LazyColumn(Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text("“${item.content}”", style = MaterialTheme.typography.bodyMedium)
                            val meta = listOfNotNull(
                                item.author.takeIf { it.isNotBlank() },
                                item.source.takeIf { it.isNotBlank() },
                                item.tags.takeIf { it.isNotBlank() }
                            ).joinToString(" · ")
                            if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                IconButton(onClick = { vm.toggleFav(item.id, !item.favorite) }) {
                                    Icon(if (item.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "收藏")
                                }
                                IconButton(onClick = { vm.extract(item.content) { aiResult = it } }) { Icon(Icons.Filled.AutoAwesome, "AI 要点") }
                                IconButton(onClick = { vm.delete(item.id) }) { Icon(Icons.Filled.Delete, "删除") }
                            }
                        }
                    }
                }
            }
        }
        NeuFab(onClick = { showAdd = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Filled.Add, "新增")
        }
    }

    if (showAdd) {
        var c by remember { mutableStateOf("") }; var a by remember { mutableStateOf("") }
        var s by remember { mutableStateOf("") }; var t by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showAdd = false }, confirmButton = { TextButton(onClick = { vm.add(c, a, s, t); showAdd = false }) { Text("保存") } },
            dismissButton = { TextButton({ showAdd = false }) { Text("取消") } }, title = { Text("收藏语录") }, text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(c, { c = it }, label = { Text("内容") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(a, { a = it }, label = { Text("作者（可选）") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(s, { s = it }, label = { Text("出处（可选）") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(t, { t = it }, label = { Text("标签（可选）") }, modifier = Modifier.fillMaxWidth())
                }
            })
    }

    if (aiResult != null) {
        AlertDialog(onDismissRequest = { aiResult = null }, confirmButton = { TextButton({ aiResult = null }) { Text("关闭") } },
            title = { Text("AI 提取要点") }, text = { Text(aiResult!!) })
    }
}
