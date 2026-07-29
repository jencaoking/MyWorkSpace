package com.jencao.mywork.ui.dailypending

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.jencao.mywork.data.local.entity.DailyPendingLogEntity
import com.jencao.mywork.data.repository.WeeklyReview
import com.jencao.mywork.ui.components.NeuButton
import com.jencao.mywork.ui.components.NeuCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

private val dateFmt = DateTimeFormatter.ofPattern("MM月dd日")

/** 每日未完成作业：待处理清单（补做/改期/放弃）+ 今日已处置 + 本周回顾。 */
@Composable
fun DailyPendingScreen(
    nav: NavHostController,
    padding: PaddingValues,
    vm: DailyPendingViewModel = hiltViewModel()
) {
    val pending by vm.pending.collectAsStateWithLifecycle()
    val disposedToday by vm.disposedToday.collectAsStateWithLifecycle()
    val weekly by vm.weekly.collectAsStateWithLifecycle()

    var rescheduleTarget by remember { mutableStateOf<DailyPendingLogEntity?>(null) }
    var abandonTarget by remember { mutableStateOf<DailyPendingLogEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text("每日作业", style = MaterialTheme.typography.headlineSmall)
        }

        // 待处理
        Text(
            if (pending.isEmpty()) "太棒了，没有待处理的作业" else "待处理（${pending.size}）",
            style = MaterialTheme.typography.titleMedium
        )
        pending.forEach { log ->
            PendingCard(
                log = log,
                onComplete = { vm.complete(log) },
                onReschedule = { rescheduleTarget = log },
                onAbandon = { abandonTarget = log }
            )
        }

        // 今日已处置
        if (disposedToday.isNotEmpty()) {
            Text("今日已处置（${disposedToday.size}）", style = MaterialTheme.typography.titleMedium)
            disposedToday.forEach { log -> DisposedRow(log) }
        }

        // 本周回顾
        WeeklyReviewCard(weekly)
    }

    // 改期对话框
    rescheduleTarget?.let { target ->
        RescheduleDialog(
            onDismiss = { rescheduleTarget = null },
            onPick = { date ->
                vm.reschedule(target, date)
                rescheduleTarget = null
            }
        )
    }

    // 放弃确认
    abandonTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { abandonTarget = null },
            title = { Text("放弃这项任务？") },
            text = { Text("「${target.taskTitle}」将被标记为已放弃，任务保留在任务列表中，不会再出现在每日作业里。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.abandon(target)
                    abandonTarget = null
                }) { Text("放弃", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { abandonTarget = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun PendingCard(
    log: DailyPendingLogEntity,
    onComplete: () -> Unit,
    onReschedule: () -> Unit,
    onAbandon: () -> Unit
) {
    val overdueDays = TimeUnit.MILLISECONDS.toDays(
        (System.currentTimeMillis() - log.originalDueDate).coerceAtLeast(0)
    ).toInt().coerceAtLeast(1)
    NeuCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PriorityDot(log.priority)
            Spacer(Modifier.width(8.dp))
            Text(
                log.taskTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            buildString {
                if (log.categoryName.isNotBlank()) append("${log.categoryName} · ")
                append("原定 ${LocalDate.parse(log.logDate).format(dateFmt)}")
                append(" · 逾期 $overdueDays 天")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeuButton("补做", onClick = onComplete, modifier = Modifier.weight(1f))
            NeuButton("改期", onClick = onReschedule, modifier = Modifier.weight(1f))
            NeuButton("放弃", onClick = onAbandon, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DisposedRow(log: DailyPendingLogEntity) {
    val (label, color) = when (log.disposition) {
        DailyPendingLogEntity.DISPOSITION_COMPLETED -> "已补做" to Color(0xFF4CAF50)
        DailyPendingLogEntity.DISPOSITION_RESCHEDULED -> "已改期" to Color(0xFF2196F3)
        DailyPendingLogEntity.DISPOSITION_ABANDONED -> "已放弃" to Color(0xFF9E9E9E)
        else -> "待处理" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    NeuCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                log.taskTitle,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(label, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}

@Composable
private fun WeeklyReviewCard(weekly: WeeklyReview) {
    NeuCard(Modifier.fillMaxWidth()) {
        Text("本周回顾", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (weekly.total == 0) {
            Text(
                "近 7 天没有产生未完成作业，保持住！",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                "共 ${weekly.total} 项 · 补做率 ${(weekly.makeupRate * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            StatRow("补做", weekly.completed, weekly.total, Color(0xFF4CAF50))
            StatRow("改期", weekly.rescheduled, weekly.total, Color(0xFF2196F3))
            StatRow("放弃", weekly.abandoned, weekly.total, Color(0xFF9E9E9E))
            StatRow("待处理", weekly.pending, weekly.total, MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun StatRow(label: String, count: Int, total: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(48.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            Modifier
                .weight(1f)
                .height(8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
        ) {
            val fraction = if (total > 0) count.toFloat() / total else 0f
            if (fraction > 0f) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(8.dp)
                        .background(color, RoundedCornerShape(4.dp))
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text("$count", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun PriorityDot(priority: Int) {
    val color = when (priority) {
        1 -> Color(0xFFE53935)
        2 -> Color(0xFFFB8C00)
        else -> Color(0xFF43A047)
    }
    Box(
        Modifier
            .width(10.dp)
            .height(10.dp)
            .background(color, CircleShape)
    )
}

/** 改期选择：今天 / 明天 / 后天 / 自定义日期 */
@Composable
private fun RescheduleDialog(onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val ctx = LocalContext.current
    val today = LocalDate.now()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("改期到哪天？") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                QuickDateOption("今天（${today.format(dateFmt)}）") { onPick(today) }
                QuickDateOption("明天（${today.plusDays(1).format(dateFmt)}）") { onPick(today.plusDays(1)) }
                QuickDateOption("后天（${today.plusDays(2).format(dateFmt)}）") { onPick(today.plusDays(2)) }
                QuickDateOption("选择日期…") {
                    DatePickerDialog(
                        ctx,
                        { _, y, m, d -> onPick(LocalDate.of(y, m + 1, d)) },
                        today.year, today.monthValue - 1, today.dayOfMonth
                    ).apply {
                        datePicker.minDate = System.currentTimeMillis()
                    }.show()
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun QuickDateOption(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    )
}
