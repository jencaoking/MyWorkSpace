package com.jencao.mywork.ui.pomodoro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFmt = SimpleDateFormat("HH:mm", Locale.CHINA)

private val MODE_LABELS = mapOf("work" to "专注", "short" to "短休", "long" to "长休")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    rootNav: NavHostController,
    vm: PomodoroViewModel = hiltViewModel()
) {
    val mode by vm.mode.collectAsStateWithLifecycle()
    val remaining by vm.remaining.collectAsStateWithLifecycle()
    val running by vm.running.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val workCount by vm.workCount.collectAsStateWithLifecycle()

    val minutes = remaining / 60
    val seconds = remaining % 60
    val timeText = "%02d:%02d".format(minutes, seconds)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("番茄钟") },
                navigationIcon = {
                    IconButton(onClick = { rootNav.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MODE_LABELS.forEach { (m, label) ->
                    Button(
                        onClick = { vm.setMode(m) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (mode == m) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) { Text(label) }
                }
            }

            Spacer(Modifier.height(32.dp))
            Text(timeText, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
            Text("今日已完成专注：$workCount 个", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { if (running) vm.pause() else vm.start() }) {
                    Text(if (running) "暂停" else "开始")
                }
                OutlinedButton(onClick = { vm.reset() }) { Text("重置") }
                OutlinedButton(onClick = { vm.skip() }) { Text("跳过") }
            }

            Spacer(Modifier.height(24.dp))
            Text("历史记录", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessions, key = { it.id }) { s ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(MODE_LABELS[s.mode] ?: s.mode, style = MaterialTheme.typography.bodyLarge)
                        Text("${s.durationMin} 分钟", style = MaterialTheme.typography.bodyMedium)
                        Text(timeFmt.format(Date(s.completedAt)), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
