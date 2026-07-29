package com.jencao.mywork.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jencao.mywork.data.settings.ModuleKey
import com.jencao.mywork.data.settings.ThemeMode
import com.jencao.mywork.ui.AppViewModel
import com.jencao.mywork.ui.navigation.moduleMeta

@Composable
fun SettingsScreen(appVm: AppViewModel, padding: PaddingValues) {
    val themeMode by appVm.themeMode.collectAsStateWithLifecycle()
    val deviceId by appVm.deviceId.collectAsStateWithLifecycle()
    val moduleToggles by appVm.moduleToggles.collectAsStateWithLifecycle()
    val moduleOrder by appVm.moduleOrder.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("设置", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("主题模式", style = MaterialTheme.typography.titleMedium)
                ThemeMode.entries.forEach { mode ->
                    val label = when (mode) {
                        ThemeMode.SYSTEM -> "跟随系统"
                        ThemeMode.LIGHT -> "浅色"
                        ThemeMode.DARK -> "深色"
                    }
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = themeMode == mode,
                            onClick = { appVm.setThemeMode(mode) }
                        )
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("设备标识", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (deviceId.isBlank()) "生成中…" else deviceId,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "无登录设计：以设备 ID 作为用户标识，首次启动自动生成，后端据此隔离各设备数据。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("功能板块", style = MaterialTheme.typography.titleMedium)
                Text(
                    "开关控制首页是否显示；上下箭头调整首页排列顺序。锁定的核心板块不可关闭。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                moduleOrder.forEachIndexed { index, key ->
                    val meta = moduleMeta[key] ?: return@forEachIndexed
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(meta.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(meta.label, style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(
                            onClick = {
                                val list = moduleOrder.toMutableList()
                                val t = list[index]; list[index] = list[index - 1]; list[index - 1] = t
                                appVm.setModuleOrder(list)
                            },
                            enabled = index > 0
                        ) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "上移") }
                        IconButton(
                            onClick = {
                                val list = moduleOrder.toMutableList()
                                val t = list[index]; list[index] = list[index + 1]; list[index + 1] = t
                                appVm.setModuleOrder(list)
                            },
                            enabled = index < moduleOrder.lastIndex
                        ) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "下移") }
                        Switch(
                            checked = moduleToggles[key] != false,
                            enabled = !key.locked,
                            onCheckedChange = { appVm.toggleModule(key, it) }
                        )
                    }
                }
            }
        }
    }
}
