package com.jencao.mywork.ui.task

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.jencao.mywork.data.local.entity.CategoryEntity

private val PALETTE = listOf(
    "#EF5350", "#FFA726", "#FFCA28", "#66BB6A", "#26A69A",
    "#29B6F6", "#5C6BC0", "#AB47BC", "#EC407A", "#8D6E63"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManageScreen(
    nav: NavHostController,
    vm: CategoryViewModel = hiltViewModel()
) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    val showDialog by vm.showDialog.collectAsStateWithLifecycle()
    val editing by vm.editing.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分类管理") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.openNew() }) {
                Icon(Icons.Filled.Add, "新增分类")
            }
        }
    ) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories, key = { it.id }) { cat ->
                val idx = categories.indexOf(cat)
                val prev = if (idx > 0) categories[idx - 1] else null
                val next = if (idx < categories.size - 1) categories[idx + 1] else null
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(16.dp).clip(CircleShape)
                                .background(Color(AndroidColor.parseColor(cat.color)))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(cat.name, Modifier.weight(1f))
                        IconButton(
                            onClick = { prev?.let { vm.moveUp(cat, prev) } },
                            enabled = prev != null,
                            modifier = Modifier.size(32.dp)
                        ) { Icon(Icons.Filled.ArrowUpward, "上移") }
                        IconButton(
                            onClick = { next?.let { vm.moveDown(cat, next) } },
                            enabled = next != null,
                            modifier = Modifier.size(32.dp)
                        ) { Icon(Icons.Filled.ArrowDownward, "下移") }
                        IconButton(onClick = { vm.openEdit(cat) }) {
                            Icon(Icons.Filled.Edit, "编辑")
                        }
                        if (!cat.isSystem) {
                            IconButton(onClick = { vm.delete(cat) }) {
                                Icon(Icons.Filled.Delete, "删除")
                            }
                        }
                    }
                }
            }
            if (categories.isEmpty()) {
                item { Text("暂无分类，点击 + 新增", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp)) }
            }
        }
    }

    if (showDialog) {
        CategoryEditDialog(
            editing = editing,
            onDismiss = { vm.close() },
            onSave = { name, color -> vm.save(name, color) }
        )
    }
}

@Composable
private fun CategoryEditDialog(
    editing: CategoryEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var color by remember { mutableStateOf(editing?.color ?: PALETTE.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(name, color) }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text(if (editing == null) "新增分类" else "编辑分类") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("名称") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("颜色", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PALETTE) { c ->
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(AndroidColor.parseColor(c)))
                                .clickable { color = c }
                                .border(
                                    if (color == c) 2.dp else 0.dp,
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape
                                )
                        )
                    }
                }
            }
        }
    )
}
