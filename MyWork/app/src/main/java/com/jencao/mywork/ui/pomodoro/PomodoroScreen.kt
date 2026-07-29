package com.jencao.mywork.ui.pomodoro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.jencao.mywork.ui.components.NeuButton
import com.jencao.mywork.ui.components.NeuCard
import com.jencao.mywork.ui.components.NeuChip
import com.jencao.mywork.ui.components.NeuIconButton
import com.jencao.mywork.ui.theme.neumorphic
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFmt = SimpleDateFormat("HH:mm", Locale.CHINA)

private val MODE_LABELS = mapOf("work" to "专注", "short" to "短休", "long" to "长休")

private fun totalSeconds(mode: String): Int = when (mode) {
    "work" -> 25 * 60
    "short" -> 5 * 60
    "long" -> 15 * 60
    else -> 25 * 60
}

@Composable
fun PomodoroScreen(
    rootNav: NavHostController,
    padding: PaddingValues,
    vm: PomodoroViewModel = hiltViewModel()
) {
    val mode by vm.mode.collectAsStateWithLifecycle()
    val remaining by vm.remaining.collectAsStateWithLifecycle()
    val running by vm.running.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val workCount by vm.workCount.collectAsStateWithLifecycle()

    val total = totalSeconds(mode)
    val progress = (total - remaining).coerceAtLeast(0).toFloat() / total.toFloat()
    val minutes = remaining / 60
    val seconds = remaining % 60
    val timeText = "%02d:%02d".format(minutes, seconds)

    val bg = MaterialTheme.colorScheme.surface
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeuIconButton(onClick = { rootNav.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, "返回")
            }
            Spacer(Modifier.width(12.dp))
            Text("番茄钟", style = MaterialTheme.typography.headlineMedium)
        }

        // 模式选择
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MODE_LABELS.forEach { (m, label) ->
                NeuChip(label, selected = mode == m, onClick = { vm.setMode(m) })
            }
        }

        // 中央新拟物计时圆环
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(240.dp)
                .neumorphic(CircleShape, 10.dp, backgroundColor = bg, pressed = true)
        ) {
            Canvas(Modifier.fillMaxSize().padding(18.dp)) {
                val stroke = 14.dp.toPx()
                val radius = (size.minDimension - stroke) / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(color = track, style = Stroke(stroke), radius = radius, center = center)
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    timeText,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    MODE_LABELS[mode] ?: mode,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 控制按钮
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NeuButton(
                text = if (running) "暂停" else "开始",
                onClick = { if (running) vm.pause() else vm.start() }
            )
            NeuButton(text = "重置", onClick = { vm.reset() })
            NeuButton(text = "跳过", onClick = { vm.skip() })
        }

        Text(
            "今日已完成专注：$workCount 个",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 历史记录
        Text(
            "历史记录",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(sessions, key = { it.id }) { s ->
                NeuCard(Modifier.fillMaxWidth(), elevation = 4.dp) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(MODE_LABELS[s.mode] ?: s.mode, style = MaterialTheme.typography.bodyLarge)
                        Text("${s.durationMin} 分钟", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            timeFmt.format(Date(s.completedAt)),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
