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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountdownScreen(navController: NavHostController, padding: PaddingValues, vm: CountdownViewModel = hiltViewModel()) {
    val items by vm.items.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); kotlinx.coroutines.delay(1000) } }

    Box(Modifier.fillMaxSize().padding(padding)) {
        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.id }) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleSmall)
                        val remain = item.targetTime - now
                        if (remain > 0) {
                            val d = remain / 86_400_000
                            val h = (remain % 86_400_000) / 3_600_000
                            val m = (remain % 3_600_000) / 60_000
                            val s = (remain % 60_000) / 1000
                            Text("还剩 ${d}天 ${h}时 ${m}分 ${s}秒", color = MaterialTheme.colorScheme.primary)
                        } else {
                            Text("已到期", color = MaterialTheme.colorScheme.error)
                        }
                        Text(fmt(item.targetTime) + if (item.remark.isNotBlank()) " · ${item.remark}" else "",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = { vm.delete(item.id) }) { Icon(Icons.Filled.Delete, "删除") }
                        }
                    }
                }
            }
        }
        NeuFab(onClick = { showAdd = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Filled.Add, "新增")
        }
    }

    if (showAdd) AddCountdownDialog(onDismiss = { showAdd = false }, onConfirm = { t, time, r -> vm.add(t, time, r); showAdd = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCountdownDialog(onDismiss: () -> Unit, onConfirm: (String, Long, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState()
    val timeState = rememberTimePickerState()
    var millis by remember { mutableLongStateOf(System.currentTimeMillis() + 86_400_000) }

    AlertDialog(onDismissRequest = onDismiss, confirmButton = {
        TextButton(onClick = { onConfirm(title, millis, remark) }) { Text("保存") }
    }, dismissButton = { TextButton(onDismiss) { Text("取消") } }, title = { Text("新增倒计时") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("标题") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(remark, { remark = it }, label = { Text("备注（可选）") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { showDate = true }) { Text("目标时间：${fmt(millis)}") }
        }
    })

    if (showDate) {
        DatePickerDialog(onDismissRequest = { showDate = false }, confirmButton = {
            TextButton(onClick = {
                val cal = Calendar.getInstance().apply { timeInMillis = millis }
                dateState.selectedDateMillis?.let { d ->
                    cal.set(Calendar.YEAR, Calendar.getInstance().apply { timeInMillis = d }.get(Calendar.YEAR))
                    cal.set(Calendar.MONTH, Calendar.getInstance().apply { timeInMillis = d }.get(Calendar.MONTH))
                    cal.set(Calendar.DAY_OF_MONTH, Calendar.getInstance().apply { timeInMillis = d }.get(Calendar.DAY_OF_MONTH))
                }
                millis = cal.timeInMillis; showDate = false; showTime = true
            }) { Text("下一步") }
        }) { DatePicker(dateState) }
    }
    if (showTime) {
        AlertDialog(onDismissRequest = { showTime = false }, confirmButton = {
            TextButton(onClick = {
                val cal = Calendar.getInstance().apply { timeInMillis = millis }
                cal.set(Calendar.HOUR_OF_DAY, timeState.hour); cal.set(Calendar.MINUTE, timeState.minute)
                millis = cal.timeInMillis; showTime = false
            }) { Text("确定") }
        }, text = { TimePicker(timeState) })
    }
}

private fun fmt(ts: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
