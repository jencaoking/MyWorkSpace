package com.jencao.mywork.ui.english

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.jencao.mywork.data.local.entity.EnglishWordEntity
import com.jencao.mywork.ui.components.EmptyHint
import com.jencao.mywork.ui.navigation.EnglishRoutes
import com.jencao.mywork.ui.navigation.Routes
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnglishListScreen(nav: NavHostController, vm: EnglishViewModel = hiltViewModel()) {
    val all by vm.items.collectAsStateWithLifecycle()
    val dueOnly by vm.dueOnly.collectAsStateWithLifecycle()
    val now = System.currentTimeMillis()
    val shown = if (dueOnly) all.filter { it.nextReview <= now } else all
    val dueCount = all.count { it.nextReview <= now }
    val fmt = remember { SimpleDateFormat("MM-dd", Locale.getDefault()) }
    var toDelete by remember { mutableStateOf<EnglishWordEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("英语单词") },
        actions = {
            Text("待复习 $dueCount", style = MaterialTheme.typography.labelMedium)
            if (dueCount > 0) {
                Spacer(Modifier.width(8.dp))
                Button(onClick = { nav.navigate(Routes.ENGLISH_REVIEW) }) { Text("开始复习") }
            }
            Switch(checked = dueOnly, onCheckedChange = vm::setDueOnly)
        }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate(EnglishRoutes.edit(EnglishRoutes.NEW_ID)) }) {
                Icon(Icons.Filled.Add, contentDescription = "新建单词")
            }
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (shown.isEmpty()) {
                item { EmptyHint(if (dueOnly) "今日没有待复习的单词" else "还没有单词，点击右下角 + 添加") }
            }
            items(shown, key = { it.id }) { w ->
                EnglishItemCard(w, fmt, onClick = { nav.navigate(EnglishRoutes.edit(w.id)) }) {
                    IconButton(onClick = { toDelete = w }) { Icon(Icons.Filled.Delete, contentDescription = "删除") }
                }
            }
        }
    }

    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("删除单词") },
            text = { Text("确定删除「${toDelete!!.word}」吗？") },
            confirmButton = { TextButton(onClick = { vm.deleteItem(toDelete!!.id); toDelete = null }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun EnglishItemCard(
    w: EnglishWordEntity,
    fmt: SimpleDateFormat,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(w.word, style = MaterialTheme.typography.titleMedium)
                    if (w.phonetic.isNotBlank()) {
                        Text(w.phonetic, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                trailing()
            }
            if (w.meaning.isNotBlank()) {
                Text(w.meaning, style = MaterialTheme.typography.bodyMedium)
            }
            if (w.example.isNotBlank()) {
                Text("\"${w.example}\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(5) { i ->
                    Icon(
                        imageVector = if (i < w.familiarity) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 2.dp)
                    )
                }
                Text("复习 ${fmt.format(w.nextReview)}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
