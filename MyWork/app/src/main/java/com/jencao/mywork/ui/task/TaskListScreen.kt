package com.jencao.mywork.ui.task

import android.graphics.Color as AndroidColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.jencao.mywork.data.local.entity.CategoryEntity
import com.jencao.mywork.data.local.entity.TaskEntity
import com.jencao.mywork.data.model.Priority
import com.jencao.mywork.data.model.TaskSort
import com.jencao.mywork.data.model.TaskType
import com.jencao.mywork.data.util.DateUtils
import com.jencao.mywork.ui.navigation.TaskRoutes

@Composable
fun TaskListScreen(
    nav: NavHostController,
    vm: TaskViewModel = hiltViewModel()
) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val selectedCat by vm.category.collectAsStateWithLifecycle()
    val sort by vm.sort.collectAsStateWithLifecycle()
    val showDialog by vm.showDialog.collectAsStateWithLifecycle()
    val editing by vm.editing.collectAsStateWithLifecycle()

    val catMap = remember(categories) { categories.associateBy { it.id } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("任务") },
                actions = {
                    IconButton(onClick = { nav.navigate(TaskRoutes.CALENDAR) }) {
                        Icon(androidx.compose.material.icons.Icons.Filled.CalendarMonth, "日历")
                    }
                    IconButton(onClick = { nav.navigate(TaskRoutes.STATS) }) {
                        Icon(androidx.compose.material.icons.Icons.Filled.BarChart, "统计")
                    }
                    IconButton(onClick = { nav.navigate(TaskRoutes.CATEGORIES) }) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Label, "分类")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.openNew() }) {
                Icon(androidx.compose.material.icons.Icons.Filled.Add, "新增任务")
            }
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 12.dp)
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCat.isEmpty(),
                        onClick = { vm.setCategory("") },
                        label = { Text("全部") }
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCat == cat.id,
                        onClick = { vm.setCategory(cat.id) },
                        label = { Text(cat.name) },
                        leadingIcon = {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(AndroidColor.parseColor(cat.color)))
                            )
                        }
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("排序", style = MaterialTheme.typography.labelMedium)
                SortDropdown(sort) { vm.setSort(it) }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskItemCard(
                        task = task,
                        category = catMap[task.categoryId],
                        onCheck = { vm.checkIn(task) },
                        onClick = { nav.navigate(TaskRoutes.detail(task.id)) }
                    )
                }
                if (tasks.isEmpty()) {
                    item {
                        Text(
                            "暂无任务，点击右下角 + 新增",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        TaskEditDialog(
            editing = editing,
            categories = categories,
            onDismiss = { vm.closeDialog() },
            onSave = { id, title, content, categoryId, priority, dueDate, reminderTime, taskType, repeatType, repeatDays ->
                vm.save(id, title, content, categoryId, priority, dueDate, reminderTime, taskType, repeatType, repeatDays)
            }
        )
    }
}

@Composable
private fun SortDropdown(current: TaskSort, onSelect: (TaskSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (current) {
        TaskSort.CREATED_DESC -> "创建时间"
        TaskSort.DUE_ASC -> "截止日期"
        TaskSort.PRIORITY_DESC -> "优先级"
        TaskSort.TITLE_ASC -> "标题"
    }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(label) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TaskSort.entries.forEach { s ->
                val text = when (s) {
                    TaskSort.CREATED_DESC -> "创建时间"
                    TaskSort.DUE_ASC -> "截止日期"
                    TaskSort.PRIORITY_DESC -> "优先级"
                    TaskSort.TITLE_ASC -> "标题"
                }
                DropdownMenuItem(text = { Text(text) }, onClick = {
                    onSelect(s)
                    expanded = false
                })
            }
        }
    }
}

@Composable
private fun TaskItemCard(
    task: TaskEntity,
    category: CategoryEntity?,
    onCheck: () -> Unit,
    onClick: () -> Unit
) {
    val done = task.status == 1
    val titleColor by animateColorAsState(
        if (done) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.onSurface
    )
    val checkScale by animateFloatAsState(if (done) 1.2f else 1f)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (done) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCheck) {
                Icon(
                    imageVector = if (done) androidx.compose.material.icons.Icons.Filled.CheckCircle
                    else androidx.compose.material.icons.Icons.Filled.RadioButtonUnchecked,
                    contentDescription = "完成",
                    tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.graphicsLayer { scaleX = checkScale; scaleY = checkScale }
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            ) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor,
                    textDecoration = if (done) TextDecoration.LineThrough else null
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    category?.let {
                        Text(it.name, style = MaterialTheme.typography.labelSmall, color = Color(AndroidColor.parseColor(it.color)))
                    }
                    if (TaskType.from(task.taskType) == TaskType.REPEAT) {
                        Text(task.repeatRule ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (task.dueDate != null) {
                        Text(DateUtils.formatDate(task.dueDate), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    PriorityLabel(task.priority)
                }
            }
        }
    }
}

@Composable
fun PriorityLabel(priority: Int) {
    val (text, color) = when (Priority.from(priority)) {
        Priority.HIGH -> "高" to MaterialTheme.colorScheme.error
        Priority.MEDIUM -> "中" to MaterialTheme.colorScheme.primary
        Priority.LOW -> "低" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(text, style = MaterialTheme.typography.labelSmall, color = color)
}
