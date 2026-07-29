package com.jencao.mywork.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.jencao.mywork.ui.AppViewModel
import com.jencao.mywork.ui.components.NeuButton
import com.jencao.mywork.ui.components.NeuCard
import com.jencao.mywork.ui.navigation.ModuleTile
import com.jencao.mywork.ui.navigation.Routes
import com.jencao.mywork.ui.navigation.moduleMeta
import com.jencao.mywork.ui.navigation.moduleRoute

@Composable
fun HomeScreen(
    appVm: AppViewModel,
    padding: PaddingValues,
    nav: NavHostController,
    homeVm: HomeViewModel = hiltViewModel()
) {
    val activeCount by homeVm.activeCount.collectAsStateWithLifecycle()
    val syncStatus by homeVm.syncStatus.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("嗨，今天也要好好生活", style = MaterialTheme.typography.headlineMedium)
        Text(
            "自律工作台 · 当前待办 $activeCount 项",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 概览统计
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NeuCard(Modifier.weight(1f)) {
                Text("待办", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$activeCount", style = MaterialTheme.typography.headlineMedium)
            }
            NeuCard(Modifier.weight(1f)) {
                Text("模块", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${moduleMeta.size}", style = MaterialTheme.typography.headlineMedium)
            }
        }

        // 快捷专注
        NeuButton(
            "开始专注 · 番茄钟",
            onClick = { nav.navigate(Routes.POMODORO) },
            modifier = Modifier.fillMaxWidth()
        )

        // 功能板块磁贴网格（首页直达，不再藏开关里）
        Text("功能板块", style = MaterialTheme.typography.titleMedium)
        val modules = moduleMeta.keys.toList()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            modules.chunked(2).forEach { rowKeys ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowKeys.forEach { key ->
                        ModuleTile(
                            key,
                            onClick = { nav.navigate(moduleRoute(key)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowKeys.size == 1) {
                        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        NeuButton(
            "查看全部工具箱",
            onClick = { nav.navigate(Routes.TOOLS) },
            modifier = Modifier.fillMaxWidth()
        )

        // 云端同步（保留，移除原"云端连接"测试卡片）
        NeuCard(Modifier.fillMaxWidth()) {
            Text("云端同步", style = MaterialTheme.typography.titleMedium)
            when (val s = syncStatus) {
                SyncStatus.Idle -> Text(
                    "点击同步，将本地数据推送到云端并拉取远端变更",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SyncStatus.Syncing -> Text(
                    "同步中…", style = MaterialTheme.typography.bodySmall
                )

                is SyncStatus.Success -> Text(
                    "同步完成 ✓ 上行 ${s.uploaded} · 下行 ${s.downloaded} · 删除 ${s.deleted}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                is SyncStatus.Error -> Text(
                    "同步失败：${s.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            NeuButton(
                "同步数据",
                onClick = { homeVm.syncNow() },
                enabled = syncStatus != SyncStatus.Syncing,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
