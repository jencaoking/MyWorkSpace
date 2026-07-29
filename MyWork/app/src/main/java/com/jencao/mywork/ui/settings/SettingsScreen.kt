package com.jencao.mywork.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jencao.mywork.data.settings.ThemeMode
import com.jencao.mywork.ui.AppViewModel

@Composable
fun SettingsScreen(appVm: AppViewModel, padding: PaddingValues) {
    val themeMode by appVm.themeMode.collectAsStateWithLifecycle()
    val deviceId by appVm.deviceId.collectAsStateWithLifecycle()

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
                    "无登录设计：以设备 ID 作为用户标识，首次启动自动生成。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        ApiTokenCard(appVm = appVm)
    }
}

@Composable
private fun ApiTokenCard(appVm: AppViewModel) {
    val savedToken by appVm.apiToken.collectAsStateWithLifecycle()
    var text by remember(savedToken) { mutableStateOf(savedToken) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("同步令牌 (API Token)", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("留空表示不启用鉴权") },
                label = { Text("SELFWORK_API_TOKEN") }
            )
            Text(
                "与后端 .env 中的 SELFWORK_API_TOKEN 保持一致。配置后所有 /api 与 /sync 请求将自动携带 Authorization: Bearer 头；留空则兼容未启用鉴权的开发模式。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { appVm.setApiToken(text.trim()) },
                enabled = text.trim() != savedToken,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存令牌")
            }
        }
    }
}
