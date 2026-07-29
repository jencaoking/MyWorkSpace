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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.jencao.mywork.ui.components.NeuFab
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExpressScreen(navController: NavHostController, padding: PaddingValues, vm: ExpressViewModel = hiltViewModel()) {
    val packages by vm.packages.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<com.jencao.mywork.data.remote.model.ExpressTrackData?>(null) }
    var errMsg by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(packages, key = { it.id }) { p ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(p.companyName.ifBlank { p.company }.ifBlank { "快递" } + " · " + p.trackingNo, style = MaterialTheme.typography.titleSmall)
                        if (p.goods.isNotBlank()) Text("物品：${p.goods}", style = MaterialTheme.typography.bodySmall)
                        Text(p.currentStatus.ifBlank { "暂无物流信息" }, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = { vm.track(p.id) { data, err -> detail = data; errMsg = if (err.isBlank()) null else err } }) {
                                Icon(Icons.Filled.Refresh, "刷新物流")
                            }
                            IconButton(onClick = { vm.delete(p.id) }) { Icon(Icons.Filled.Delete, "删除") }
                        }
                    }
                }
            }
        }
        NeuFab(onClick = { showAdd = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Filled.Add, "添加")
        }
    }

    if (showAdd) {
        var company by remember { mutableStateOf("") }; var name by remember { mutableStateOf("") }
        var no by remember { mutableStateOf("") }; var goods by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showAdd = false }, confirmButton = { TextButton(onClick = { vm.add(company, name, no, goods); showAdd = false }) { Text("保存") } },
            dismissButton = { TextButton({ showAdd = false }) { Text("取消") } }, title = { Text("添加快递") }, text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(company, { company = it }, label = { Text("快递编码(如 SF/YT)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(name, { name = it }, label = { Text("快递公司名") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(no, { no = it }, label = { Text("运单号") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(goods, { goods = it }, label = { Text("物品（可选）") }, modifier = Modifier.fillMaxWidth())
                }
            })
    }

    if (detail != null) {
        AlertDialog(onDismissRequest = { detail = null }, confirmButton = { TextButton({ detail = null }) { Text("关闭") } },
            title = { Text("物流轨迹") }, text = {
                Column {
                    if (detail!!.traces.isEmpty()) Text("暂无轨迹")
                    detail!!.traces.forEach { tr ->
                        Text("${tr.time}  ${tr.context}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            })
    }
    if (errMsg != null) {
        AlertDialog(onDismissRequest = { errMsg = null }, confirmButton = { TextButton({ errMsg = null }) { Text("关闭") } },
            title = { Text("提示") }, text = { Text(errMsg!!) })
    }
}

private fun fmt(ts: Long): String = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
