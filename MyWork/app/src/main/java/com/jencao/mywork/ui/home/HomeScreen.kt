package com.jencao.mywork.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.jencao.mywork.data.settings.ModuleKey
import com.jencao.mywork.ui.AppViewModel
import com.jencao.mywork.ui.navigation.Routes
import java.text.SimpleDateFormat
import java.util.Locale

/** 将功能板块映射到对应路由（未实现的板块回退首页）。 */
private fun moduleRoute(key: ModuleKey): String = when (key) {
    ModuleKey.SPORT -> Routes.SPORT
    ModuleKey.ENGLISH -> Routes.ENGLISH
    ModuleKey.MEDIA -> Routes.MEDIA
    ModuleKey.HEALTH -> Routes.HEALTH
    ModuleKey.ACCOUNT -> Routes.ACCOUNT
    ModuleKey.POMODORO -> Routes.POMODORO
    else -> Routes.HOME
}

@Composable
fun HomeScreen(
    appVm: AppViewModel,
    padding: PaddingValues,
    nav: NavHostController,
    homeVm: HomeViewModel = hiltViewModel()
) {
    val toggles by appVm.moduleToggles.collectAsStateWithLifecycle()
    val activeCount by homeVm.activeCount.collectAsStateWithLifecycle()
    val serverStatus by homeVm.serverStatus.collectAsStateWithLifecycle()
    val isSyncing by homeVm.isSyncing.collectAsStateWithLifecycle()
    val lastSyncFailed by homeVm.lastSyncFailed.collectAsStateWithLifecycle()
    val lastSyncAt by homeVm.lastSyncAt.collectAsStateWithLifecycle()
    val autoSyncEnabled by homeVm.autoSyncEnabled.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("自律工作台", style = MaterialTheme.typography.headlineMedium)
        Text(
            "今天也要好好生活 ✦ 当前待办：$activeCount 项",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 功能板块开关
        Text("功能板块", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ModuleKey.entries.forEach { key ->
                val enabled = toggles[key] ?: false
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = enabled) { nav.navigate(moduleRoute(key)) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(key.displayName, style = MaterialTheme.typography.titleSmall)
                        if (key.locked) {
                            Text("核心模块（常开）", style = MaterialTheme.typography.labelSmall)
                        } else if (enabled) {
                            Text("已开启 · 点击进入", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                        } else {
                            Text("未开启", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = enabled,
                            enabled = !key.locked,
                            onCheckedChange = { appVm.toggleModule(key, it) }
                        )
                    }
                }
            }
        }

        // 服务器连接测试
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("云端连接", style = MaterialTheme.typography.titleMedium)
                when (val s = serverStatus) {
                    ServerStatus.Idle -> Text(
                        "点击按钮测试后端接口（需在设置中配置 API 域名）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ServerStatus.Loading -> Text("连接中…", style = MaterialTheme.typography.bodySmall)
                    is ServerStatus.Success -> {
                        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .format(s.data.server_time)
                        Text(
                            "已连接 ✓ 服务端时间：$time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    is ServerStatus.Error -> Text(
                        "连接失败：${s.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                OutlinedButton(
                    onClick = { homeVm.testConnection() },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("测试服务器连接")
                }
            }
        }

        // 数据同步（自动）
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("云端同步", style = MaterialTheme.typography.titleMedium)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("自动同步", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "后台每 15 分钟及启动时自动同步",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoSyncEnabled,
                        onCheckedChange = { homeVm.setAutoSync(it) }
                    )
                }

                Spacer(Modifier.height(8.dp))

                when {
                    isSyncing -> Text(
                        "同步中…", style = MaterialTheme.typography.bodySmall
                    )

                    lastSyncFailed -> Text(
                        "上次同步失败，稍后将自动重试",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )

                    lastSyncAt > 0 -> Text(
                        "上次同步：${formatSyncTime(lastSyncAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    else -> Text(
                        "尚未同步，点击「立即同步」或等待自动同步",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = { homeVm.syncNow() },
                    enabled = !isSyncing,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("立即同步")
                }
            }
        }
    }
}

/** 将服务器时间戳格式化为可读的“上次同步”时间 */
private fun formatSyncTime(ts: Long): String =
    if (ts <= 0) "从未" else SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(ts)
