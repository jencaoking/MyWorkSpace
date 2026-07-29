package com.jencao.mywork.ui.health

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
import com.jencao.mywork.data.local.entity.HealthRecordEntity
import com.jencao.mywork.ui.components.EmptyHint
import com.jencao.mywork.ui.navigation.HealthRoutes
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HealthListScreen(nav: NavHostController, vm: HealthViewModel = hiltViewModel()) {
    val items by vm.items.collectAsStateWithLifecycle()
    val typeFilter by vm.typeFilter.collectAsStateWithLifecycle()
    val fmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var toDelete by remember { mutableStateOf<HealthRecordEntity?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("健康记录") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate(HealthRoutes.edit(HealthRoutes.NEW_ID)) }) {
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
                HEALTH_TYPES.forEach { (v, label) ->
                    FilterChip(selected = typeFilter == v, onClick = { vm.setTypeFilter(v) }, label = { Text(label) })
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (items.isEmpty()) {
                    item { EmptyHint("还没有健康记录，点击右下角 + 添加") }
                }
                items(items, key = { it.id }) { h ->
                    HealthItemCard(h, fmt, onClick = { nav.navigate(HealthRoutes.edit(h.id)) }) {
                        IconButton(onClick = { toDelete = h }) { Icon(Icons.Filled.Delete, contentDescription = "删除") }
                    }
                }
            }
        }
    }

    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("删除记录") },
            text = { Text("确定删除这条健康记录吗？") },
            confirmButton = { TextButton(onClick = { vm.deleteItem(toDelete!!.id); toDelete = null }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun HealthItemCard(
    h: HealthRecordEntity,
    fmt: SimpleDateFormat,
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
                    Text(
                        if (h.value.isNotBlank()) "${h.value}${if (h.unit.isNotBlank()) " ${h.unit}" else ""}" else healthTypeLabel(h.type),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                AssistChip(onClick = {}, label = { Text(healthTypeLabel(h.type)) })
                trailing()
            }
            if (h.note.isNotBlank()) {
                Text(h.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(fmt.format(h.recordTime), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}
