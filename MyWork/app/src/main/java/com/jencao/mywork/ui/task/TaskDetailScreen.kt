package com.jencao.mywork.ui.task

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.jencao.mywork.data.local.entity.TaskCheckinEntity
import com.jencao.mywork.data.local.entity.TaskEntity
import com.jencao.mywork.data.model.Priority
import com.jencao.mywork.data.model.TaskType
import com.jencao.mywork.data.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    nav: NavHostController,
    taskId: String,
    vm: TaskDetailViewModel = hiltViewModel()
) {
    val task by vm.task.collectAsStateWithLifecycle()
    val checkins by vm.checkins.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()
    val checkedToday by vm.checkedToday.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    var showEdit by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(task?.title ?: "任务详情") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showEdit = true }) {
                        Icon(Icons.Filled.Edit, "编辑")
                    }
                    IconButton(onClick = { vm.delete { nav.popBackStack() } }) {
                        Icon(Icons.Filled.Delete, "删除")
                    }
                }
            )
        }
    ) { inner ->
        if (task == null) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Text("任务不存在或已删除")
            }
            return@Scaffold
        }
        val t = task!!
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { if (checkedToday) vm.undo() else vm.checkIn() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    if (checkedToday) Icons.Filled.CheckCircle
                    else Icons.Filled.Add,
                    null
                )
                Spacer(Modifier.width(8.dp))
                Text(if (checkedToday) "已打卡 · 点击撤销" else "今日打卡")
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("连续打卡", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "$streak 天",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            InfoRow("类型", when (TaskType.from(t.taskType)) {
                TaskType.ONCE -> "一次性"
                TaskType.REPEAT -> "循环"
                TaskType.GOAL -> "长期目标"
            })
            InfoRow("优先级", when (Priority.from(t.priority)) {
                Priority.HIGH -> "高"
                Priority.MEDIUM -> "中"
                Priority.LOW -> "低"
            })
            if (TaskType.from(t.taskType) == TaskType.REPEAT) {
                InfoRow("循环", t.repeatRule ?: "")
            }
            if (t.dueDate != null) InfoRow("截止", DateUtils.formatDate(t.dueDate))
            if (t.content.isNotBlank()) InfoRow("备注", t.content)

            Text("打卡记录 (${checkins.size})", style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(checkins, key = { it.id }) { c -> HistoryRow(c) }
                if (checkins.isEmpty()) {
                    item { Text("还没有打卡记录", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }

    if (showEdit && task != null) {
        TaskEditDialog(
            editing = task,
            categories = categories,
            onDismiss = { showEdit = false },
            onSave = { id, title, content, categoryId, priority, dueDate, reminderTime, taskType, repeatType, repeatDays ->
                vm.save(id, title, content, categoryId, priority, dueDate, reminderTime, taskType, repeatType, repeatDays)
                showEdit = false
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(56.dp)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun HistoryRow(c: TaskCheckinEntity) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(c.checkinDate, style = MaterialTheme.typography.bodyMedium)
            Text(
                DateUtils.formatDateTime(c.checkinTime),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
