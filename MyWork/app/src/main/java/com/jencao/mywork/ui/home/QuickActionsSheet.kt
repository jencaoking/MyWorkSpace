package com.jencao.mywork.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jencao.mywork.data.settings.ModuleKey
import com.jencao.mywork.ui.components.NeuIconButton
import com.jencao.mywork.ui.navigation.moduleMeta
import com.jencao.mywork.ui.navigation.moduleRoute

/** 首页微信式“+”快捷功能面板。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionsSheet(onDismiss: () -> Unit, onNavigate: (String) -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    val keys = listOf(
        ModuleKey.QRCODE, ModuleKey.TASK, ModuleKey.NOTE, ModuleKey.POMODORO,
        ModuleKey.COUNTDOWN, ModuleKey.CALC, ModuleKey.CONVERTER, ModuleKey.EXPRESS,
        ModuleKey.MEDIA, ModuleKey.ACCOUNT, ModuleKey.INSPIRATION, ModuleKey.HABIT,
        ModuleKey.HEALTH, ModuleKey.SPORT, ModuleKey.FLASHCARD, ModuleKey.ENGLISH
    )

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(16.dp).navigationBarsPadding()) {
            Text("快捷功能", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(keys) { key ->
                    val meta = moduleMeta[key] ?: return@items
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onNavigate(moduleRoute(key)) }
                    ) {
                        NeuIconButton(onClick = { onNavigate(moduleRoute(key)) }) {
                            Icon(meta.icon, meta.label, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            meta.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
