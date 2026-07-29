package com.jencao.mywork.ui.toolbox

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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
fun HabitScreen(navController: NavHostController, padding: PaddingValues, vm: HabitViewModel = hiltViewModel()) {
    val plans by vm.plans.collectAsStateWithLifecycle()
    var selectedPlanId by remember { mutableStateOf<String?>(null) }
    var showPlan by remember { mutableStateOf(false) }
    var showHabit by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            if (selectedPlanId == null) {
                Text("习惯养成计划", style = MaterialTheme.typography.titleMedium)
                LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(plans, key = { it.id }) { plan ->
                        Card(Modifier.fillMaxWidth().clickable { selectedPlanId = plan.id }) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Text(plan.title, style = MaterialTheme.typography.titleSmall)
                                if (plan.description.isNotBlank()) Text(plan.description, style = MaterialTheme.typography.bodySmall)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    IconButton(onClick = { vm.deletePlan(plan.id); selectedPlanId = null }) { Icon(Icons.Filled.Delete, "删除") }
                                }
                            }
                        }
                    }
                }
            } else {
                val habits by vm.observeHabits(selectedPlanId!!).collectAsStateWithLifecycle(emptyList())
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { selectedPlanId = null }) { Text("← 返回") }
                    Text("习惯项", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { showHabit = true }) { Text("+ 习惯") }
                }
                LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(habits, key = { it.id }) { habit ->
                        HabitRow(habit, vm)
                    }
                }
            }
        }
        if (selectedPlanId == null) {
            NeuFab(onClick = { showPlan = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
                Icon(Icons.Filled.Add, "加计划")
            }
        }
    }

    if (showPlan) AddPlanDialog(onDismiss = { showPlan = false }, onConfirm = { t, d, p -> vm.addPlan(t, d, p); showPlan = false })
    if (showHabit && selectedPlanId != null) AddHabitDialog(onDismiss = { showHabit = false }, onConfirm = { t, f, days, tm -> vm.addHabit(selectedPlanId!!, t, f, days, tm); showHabit = false })
}

@Composable
private fun HabitRow(habit: com.jencao.mywork.data.local.entity.HabitEntity, vm: HabitViewModel) {
    val checkins by vm.observeCheckins(habit.id).collectAsStateWithLifecycle(emptyList())
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val checked = checkins.any { it.date == today }
    val streak = run {
        val days = checkins.map { it.date }.toSet()
        var count = 0
        var d = java.util.Calendar.getInstance()
        while (days.contains(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(d.time))) {
            count++; d.add(java.util.Calendar.DAY_OF_MONTH, -1)
        }
        count
    }
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.toggleCheck(habit.id) }) {
                Icon(if (checked) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, "打卡",
                    tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f)) {
                Text(habit.title, style = MaterialTheme.typography.bodyMedium)
                Text("连续 $streak 天" + if (habit.timeMin > 0) " · 提醒 ${fmtMin(habit.timeMin)}" else "",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { vm.deleteHabit(habit.id) }) { Icon(Icons.Filled.Delete, "删除") }
        }
    }
}

private fun fmtMin(min: Int): String = "%02d:%02d".format(min / 60, min % 60)

@Composable
private fun AddPlanDialog(onDismiss: () -> Unit, onConfirm: (String, String, Int) -> Unit) {
    var t by remember { mutableStateOf("") }; var d by remember { mutableStateOf("") }; var p by remember { mutableStateOf(0) }
    AlertDialog(onDismissRequest = onDismiss, confirmButton = { TextButton(onClick = { onConfirm(t, d, p) }) { Text("保存") } },
        dismissButton = { TextButton(onDismiss) { Text("取消") } }, title = { Text("新增计划") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(t, { t = it }, label = { Text("目标") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(d, { d = it }, label = { Text("描述（可选）") }, modifier = Modifier.fillMaxWidth())
            }
        })
}

@Composable
private fun AddHabitDialog(onDismiss: () -> Unit, onConfirm: (String, Int, String, Int) -> Unit) {
    var t by remember { mutableStateOf("") }; var days by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, confirmButton = { TextButton(onClick = { onConfirm(t, 1, days, 0) }) { Text("保存") } },
        dismissButton = { TextButton(onDismiss) { Text("取消") } }, title = { Text("新增习惯") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(t, { t = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(days, { days = it }, label = { Text("每周几（如 1,3,5，可选）") }, modifier = Modifier.fillMaxWidth())
            }
        })
}
