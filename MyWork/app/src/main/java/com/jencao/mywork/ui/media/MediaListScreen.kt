package com.jencao.mywork.ui.media

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.jencao.mywork.data.local.entity.MovieBookEntity
import com.jencao.mywork.ui.components.EmptyHint
import com.jencao.mywork.ui.navigation.MediaRoutes

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MediaListScreen(nav: NavHostController, vm: MediaViewModel = hiltViewModel()) {
    val items by vm.items.collectAsStateWithLifecycle()
    val typeFilter by vm.typeFilter.collectAsStateWithLifecycle()
    val statusFilter by vm.statusFilter.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<MovieBookEntity?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("影音书籍") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate(MediaRoutes.edit(MediaRoutes.NEW_ID)) }) {
                Icon(Icons.Filled.Add, contentDescription = "新建")
            }
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = typeFilter == null, onClick = { vm.setTypeFilter(null) }, label = { Text("全部") })
                MEDIA_TYPES.forEach { (v, label) ->
                    FilterChip(selected = typeFilter == v, onClick = { vm.setTypeFilter(v) }, label = { Text(label) })
                }
                FilterChip(selected = statusFilter == null, onClick = { vm.setStatusFilter(null) }, label = { Text("全部状态") })
                MEDIA_STATUS.forEach { (v, label) ->
                    FilterChip(selected = statusFilter == v, onClick = { vm.setStatusFilter(v) }, label = { Text(label) })
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (items.isEmpty()) {
                    item { EmptyHint("还没有影音/书籍记录，点击右下角 + 添加") }
                }
                items(items, key = { it.id }) { m ->
                    MediaItemCard(m, onClick = { nav.navigate(MediaRoutes.edit(m.id)) }) {
                        IconButton(onClick = { toDelete = m }) { Icon(Icons.Filled.Delete, contentDescription = "删除") }
                    }
                }
            }
        }
    }

    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("删除记录") },
            text = { Text("确定删除「${toDelete!!.title}」吗？") },
            confirmButton = { TextButton(onClick = { vm.deleteItem(toDelete!!.id); toDelete = null }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun MediaItemCard(
    m: MovieBookEntity,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(m.title, style = MaterialTheme.typography.titleMedium)
                    Text(mediaTypeLabel(m.type), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
                AssistChip(onClick = {}, label = { Text(mediaStatusLabel(m.status)) }, enabled = true)
                trailing()
            }
            if (m.rating != null && m.rating!! > 0) {
                androidx.compose.foundation.layout.Row {
                    repeat(5) { i ->
                        Icon(
                            imageVector = if (i < m.rating!!.toInt()) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 2.dp)
                        )
                    }
                }
            }
            if (m.note.isNotBlank()) {
                Text(m.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
