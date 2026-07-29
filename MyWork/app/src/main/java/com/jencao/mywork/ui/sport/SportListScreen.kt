package com.jencao.mywork.ui.sport

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.jencao.mywork.data.local.entity.SportRecordEntity
import com.jencao.mywork.ui.components.EmptyHint
import com.jencao.mywork.ui.navigation.SportRoutes
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportListScreen(nav: NavHostController, vm: SportViewModel = hiltViewModel()) {
    val items by vm.items.collectAsStateWithLifecycle()
    val stat by vm.monthStat.collectAsStateWithLifecycle()
    val fmt = remember { SimpleDateFormat("MM-dd", Locale.getDefault()) }
    var toDelete by remember { mutableStateOf<SportRecordEntity?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("运动记录") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate(SportRoutes.edit(SportRoutes.NEW_ID)) }) {
                Icon(Icons.Filled.Add, contentDescription = "新建运动记录")
            }
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("本月累计", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.padding(4.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatItem("${stat.totalMin}", "分钟")
                            StatItem("${stat.count}", "次")
                            StatItem(String.format(Locale.getDefault(), "%.1f", stat.distanceKm), "公里")
                            StatItem("${stat.calories}", "千卡")
                            StatItem("${stat.steps}", "步")
                        }
                    }
                }
            }
            if (items.isEmpty()) {
                item { EmptyHint("还没有运动记录，点击右下角 + 添加") }
            }
            items(items, key = { it.id }) { rec ->
                SportItemCard(rec, fmt, onClick = { nav.navigate(SportRoutes.edit(rec.id)) }) {
                    IconButton(onClick = { toDelete = rec }) {
                        Icon(Icons.Filled.Delete, contentDescription = "删除")
                    }
                }
            }
        }
    }

    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("删除记录") },
            text = { Text("确定删除「${toDelete!!.type}」这条运动记录吗？") },
            confirmButton = {
                TextButton(onClick = { vm.deleteItem(toDelete!!.id); toDelete = null }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun StatItem(value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(unit, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SportItemCard(
    rec: SportRecordEntity,
    fmt: SimpleDateFormat,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(rec.type, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.padding(2.dp))
                val parts = mutableListOf<String>()
                if (rec.durationMin > 0) parts += "${rec.durationMin} 分钟"
                rec.distanceKm?.takeIf { it > 0 }?.let { parts += String.format(Locale.getDefault(), "%.1f 公里", it) }
                rec.calories?.takeIf { it > 0 }?.let { parts += "$it 千卡" }
                rec.steps?.takeIf { it > 0 }?.let { parts += "$it 步" }
                Text(parts.joinToString(" · ").ifEmpty { "无数据" }, style = MaterialTheme.typography.bodyMedium)
                if (rec.note.isNotBlank()) {
                    Text(rec.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(fmt.format(rec.recordDate), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            trailing()
        }
    }
}
