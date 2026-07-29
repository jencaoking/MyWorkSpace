package com.jencao.mywork.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.jencao.mywork.data.local.entity.NoteEntity
import com.jencao.mywork.ui.navigation.NoteRoutes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(nav: NavHostController, vm: NotesViewModel = hiltViewModel()) {
    val notes by vm.notes.collectAsStateWithLifecycle()
    val favoritesOnly by vm.favoritesOnly.collectAsStateWithLifecycle()
    var deleteTarget by remember { mutableStateOf<NoteEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("笔记") },
                actions = {
                    IconButton(onClick = { nav.navigate(NoteRoutes.SEARCH) }) {
                        Icon(Icons.Filled.Search, contentDescription = "搜索")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate(NoteRoutes.edit(NoteRoutes.NEW_ID)) }) {
                Icon(Icons.Filled.Add, contentDescription = "新建笔记")
            }
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !favoritesOnly,
                    onClick = { vm.setFavoritesOnly(false) },
                    label = { Text("全部") }
                )
                FilterChip(
                    selected = favoritesOnly,
                    onClick = { vm.setFavoritesOnly(true) },
                    label = { Text("收藏") }
                )
            }
            if (notes.isEmpty()) {
                EmptyNotes()
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { nav.navigate(NoteRoutes.edit(note.id)) },
                            onPin = { vm.togglePin(note) },
                            onFavorite = { vm.toggleFavorite(note) },
                            onDelete = { deleteTarget = note }
                        )
                    }
                }
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除笔记") },
            text = { Text("确定删除「${target.title}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(target)
                    deleteTarget = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun EmptyNotes() {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Icon(Icons.Filled.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text("还没有笔记", style = MaterialTheme.typography.titleMedium)
        Text("点击右下角 + 新建一篇 Markdown 笔记", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun NoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    onPin: (() -> Unit)? = null,
    onFavorite: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val dateFmt = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (note.isPinned)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (note.isPinned) {
                    Icon(
                        Icons.Filled.PushPin, contentDescription = "已置顶",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                Text(
                    note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            if (note.content.isNotBlank()) {
                Text(
                    note.content.lineSequence().firstOrNull { it.isNotBlank() } ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    dateFmt.format(Date(note.updatedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.weight(1f))
                if (onPin != null) {
                    IconButton(onClick = onPin) {
                        Icon(
                            if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (note.isPinned) "取消置顶" else "置顶",
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                    }
                }
                if (onFavorite != null) {
                    IconButton(onClick = onFavorite) {
                        Icon(
                            if (note.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (note.isFavorite) "取消收藏" else "收藏",
                            tint = if (note.isFavorite) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.outline
                        )
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete, contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
