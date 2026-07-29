package com.jencao.mywork.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.jencao.mywork.data.settings.ModuleKey
import com.jencao.mywork.ui.components.NeuCard
import com.jencao.mywork.ui.theme.NeuRadiusSmall
import com.jencao.mywork.ui.theme.neumorphic

data class ModuleMeta(val label: String, val desc: String, val icon: ImageVector)

/** 各功能板块的展示元数据（图标、名称、一句话说明）。 */
val moduleMeta: Map<ModuleKey, ModuleMeta> = mapOf(
    ModuleKey.TASK to ModuleMeta("任务", "待办与计划", Icons.Filled.CheckCircle),
    ModuleKey.NOTE to ModuleMeta("笔记", "灵感速记", Icons.Filled.Book),
    ModuleKey.SPORT to ModuleMeta("运动", "记录每次训练", Icons.Filled.FitnessCenter),
    ModuleKey.ENGLISH to ModuleMeta("英语", "单词记忆曲线", Icons.Filled.MenuBook),
    ModuleKey.MEDIA to ModuleMeta("影音书籍", "看过听过", Icons.Filled.Movie),
    ModuleKey.HEALTH to ModuleMeta("健康", "身体数据", Icons.Filled.Favorite),
    ModuleKey.ACCOUNT to ModuleMeta("记账", "收支明细", Icons.Filled.AccountBalanceWallet),
    ModuleKey.POMODORO to ModuleMeta("番茄钟", "专注计时", Icons.Filled.Timer)
)

/** 功能板块映射到对应路由（未实现的板块回退首页）。 */
fun moduleRoute(key: ModuleKey): String = when (key) {
    ModuleKey.SPORT -> Routes.SPORT
    ModuleKey.ENGLISH -> Routes.ENGLISH
    ModuleKey.MEDIA -> Routes.MEDIA
    ModuleKey.HEALTH -> Routes.HEALTH
    ModuleKey.ACCOUNT -> Routes.ACCOUNT
    ModuleKey.POMODORO -> Routes.POMODORO
    else -> Routes.HOME
}

/** 统一的功能板块磁贴，首页与工具箱复用。 */
@Composable
fun ModuleTile(key: ModuleKey, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val meta = moduleMeta[key] ?: return
    NeuCard(
        modifier = modifier.clickable { onClick() },
        elevation = 5.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .neumorphic(NeuRadiusSmall, 3.dp, backgroundColor = MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(meta.icon, meta.label, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(meta.label, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(
                    meta.desc,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
