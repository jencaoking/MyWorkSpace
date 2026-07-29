package com.jencao.mywork.ui.task

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jencao.mywork.data.local.entity.CategoryEntity
import com.jencao.mywork.data.local.entity.TaskEntity
import com.jencao.mywork.data.model.Priority
import com.jencao.mywork.data.model.RepeatType
import com.jencao.mywork.data.model.TaskType
import com.jencao.mywork.data.util.DateUtils
import com.jencao.mywork.data.util.RepeatRule
import java.util.Calendar

@Composable
fun TaskEditDialog(
    editing: TaskEntity?,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (
        id: String?, title: String, content: String, categoryId: String,
        priority: Int, dueDate: Long?, reminderTime: Long?,
        taskType: Int, repeatType: Int, repeatDays: String?
    ) -> Unit
) {
    var title by remember { mutableStateOf(editing?.title ?: "") }
    var content by remember { mutableStateOf(editing?.content ?: "") }
    var categoryId by remember { mutableStateOf(editing?.categoryId ?: "") }
    var priority by remember { mutableStateOf(editing?.priority ?: Priority.MEDIUM.value) }
    var taskType by remember { mutableStateOf(editing?.taskType ?: TaskType.ONCE.value) }
    var repeatType by remember { mutableStateOf(editing?.repeatType ?: RepeatType.NONE.value) }
    var weekDays by remember {
        mutableStateOf(
            editing?.repeatDays?.split(",")?.mapNotNull { it.toIntOrNull() }?.toSet() ?: setOf()
        )
    }
    var dueDate by remember { mutableStateOf(editing?.dueDate) }

    val isRepeat = TaskType.from(taskType) == TaskType.REPEAT

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    val repeatDays = if (isRepeat && RepeatType.from(repeatType) == RepeatType.WEEKLY) {
                        weekDays.sorted().joinToString(",")
                    } else null
                    onSave(
                        editing?.id, title, content, categoryId, priority,
                        dueDate, editing?.reminderTime, taskType,
                        if (isRepeat) repeatType else RepeatType.NONE.value, repeatDays
                    )
                }
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text(if (editing == null) "新增任务" else "编辑任务") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("标题") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content, onValueChange = { content = it },
                    label = { Text("备注") }, modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                // 分类
                Text("分类", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(selected = categoryId.isEmpty(), onClick = { categoryId = "" }, label = { Text("无") })
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected = categoryId == cat.id,
                            onClick = { categoryId = cat.id },
                            label = { Text(cat.name) }
                        )
                    }
                }

                // 优先级
                Text("优先级", style = MaterialTheme.typography.labelMedium)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Priority.entries.forEachIndexed { index, p ->
                        val text = when (p) {
                            Priority.HIGH -> "高"
                            Priority.MEDIUM -> "中"
                            Priority.LOW -> "低"
                        }
                        SegmentedButton(
                            selected = priority == p.value,
                            onClick = { priority = p.value },
                            shape = SegmentedButtonDefaults.itemShape(index, Priority.entries.size)
                        ) { Text(text) }
                    }
                }

                // 任务类型
                Text("类型", style = MaterialTheme.typography.labelMedium)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    TaskType.entries.forEachIndexed { index, t ->
                        val text = when (t) {
                            TaskType.ONCE -> "一次性"
                            TaskType.REPEAT -> "循环"
                            TaskType.GOAL -> "长期目标"
                        }
                        SegmentedButton(
                            selected = taskType == t.value,
                            onClick = { taskType = t.value },
                            shape = SegmentedButtonDefaults.itemShape(index, TaskType.entries.size)
                        ) { Text(text) }
                    }
                }

                if (isRepeat) {
                    Text("循环方式", style = MaterialTheme.typography.labelMedium)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val repeatOptions = RepeatType.entries.filter { it != RepeatType.NONE }
                    repeatOptions.forEachIndexed { index, r ->
                        val text = when (r) {
                            RepeatType.DAILY -> "每天"
                            RepeatType.WEEKLY -> "每周"
                            RepeatType.MONTHLY -> "每月"
                            else -> ""
                        }
                        SegmentedButton(
                            selected = repeatType == r.value,
                            onClick = { repeatType = r.value },
                            shape = SegmentedButtonDefaults.itemShape(index, repeatOptions.size)
                        ) { Text(text) }
                    }
                }
                    if (RepeatType.from(repeatType) == RepeatType.WEEKLY) {
                        val weekNames = listOf("一", "二", "三", "四", "五", "六", "日")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items((1..7).toList()) { d ->
                                val selected = weekDays.contains(d)
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        weekDays = if (selected) weekDays - d else weekDays + d
                                    },
                                    label = { Text(weekNames[d - 1]) }
                                )
                            }
                        }
                    }
                }

                // 截止日期
                Text("截止日期", style = MaterialTheme.typography.labelMedium)
                val context = androidx.compose.ui.platform.LocalContext.current
                val cal = Calendar.getInstance().apply { dueDate?.let { timeInMillis = it } }
                OutlinedButton(onClick = {
                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            dueDate = Calendar.getInstance().apply {
                                set(y, m, d, 0, 0, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                        },
                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }) {
                    Text(if (dueDate != null) DateUtils.formatDate(dueDate) else "选择日期")
                }
            }
        }
    )
}
