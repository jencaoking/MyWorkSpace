package com.jencao.mywork.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jencao.mywork.ui.theme.NeuRadius
import com.jencao.mywork.ui.theme.NeuRadiusSmall
import com.jencao.mywork.ui.theme.neumorphic

/** 新拟物卡片：凸起质感的容器，内容默认带 16dp 内边距。 */
@Composable
fun NeuCard(
    modifier: Modifier = Modifier,
    shape: Shape = NeuRadius,
    elevation: Dp = 6.dp,
    pressed: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val bg = MaterialTheme.colorScheme.surface
    Column(
        modifier = modifier
            .neumorphic(shape, elevation, backgroundColor = bg, pressed = pressed)
            .padding(16.dp),
        content = content
    )
}

/** 新拟物容器（Box 版），用于需要自定义对齐的内容（如计时圆环）。 */
@Composable
fun NeuSurface(
    modifier: Modifier = Modifier,
    shape: Shape = NeuRadius,
    elevation: Dp = 6.dp,
    pressed: Boolean = false,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    val bg = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .neumorphic(shape, elevation, backgroundColor = bg, pressed = pressed)
            .padding(16.dp),
        contentAlignment = contentAlignment,
        content = content
    )
}

/** 新拟物按钮：按下时本体呈现内凹质感。 */
@Composable
fun NeuButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color = MaterialTheme.colorScheme.primary,
    shape: Shape = NeuRadiusSmall
) {
    val bg = MaterialTheme.colorScheme.surface
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) { onClick() }
            .neumorphic(shape, 5.dp, backgroundColor = bg, pressed = pressed)
            .padding(horizontal = 22.dp, vertical = 13.dp)
    ) {
        androidx.compose.material3.Text(
            text = text,
            color = if (enabled) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/** 新拟物悬浮按钮（圆形），常驻底部导航中心。 */
@Composable
fun NeuFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val bg = MaterialTheme.colorScheme.surface
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(62.dp)
            .clickable { onClick() }
            .neumorphic(CircleShape, 8.dp, backgroundColor = bg)
            .padding(8.dp),
        content = content
    )
}

/** 新拟物开关：关闭态轨道内凹、开启态轨道凸起且滑块带主色。 */
@Composable
fun NeuSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = MaterialTheme.colorScheme.surface
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(52.dp, 30.dp)
            .neumorphic(NeuRadiusSmall, 4.dp, backgroundColor = bg, pressed = !checked)
            .clickable { onCheckedChange(!checked) }
            .padding(4.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .neumorphic(CircleShape, 3.dp, backgroundColor = if (checked) accent else bg)
        )
    }
}

/** 新拟物线性进度条：轨道内凹，已填充部分用主色。 */
@Composable
fun NeuLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 10.dp
) {
    val bg = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .neumorphic(NeuRadiusSmall, 3.dp, backgroundColor = bg, pressed = true)
            .padding(3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(NeuRadiusSmall)
                .background(color)
        )
    }
}

/** 新拟物筛选标签：选中时内凹。 */
@Composable
fun NeuChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    val bg = MaterialTheme.colorScheme.surface
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clickable { onClick() }
            .neumorphic(NeuRadiusSmall, 4.dp, backgroundColor = bg, pressed = selected)
            .padding(horizontal = 18.dp, vertical = 11.dp)
    ) {
        androidx.compose.material3.Text(
            text = text,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/** 新拟物图标按钮（圆形凹槽，用于返回、设置等）。 */
@Composable
fun NeuIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val bg = MaterialTheme.colorScheme.surface
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .clickable { onClick() }
            .neumorphic(CircleShape, 4.dp, backgroundColor = bg)
            .padding(8.dp),
        content = content
    )
}
