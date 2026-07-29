package com.jencao.mywork.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** 一组柱子（对应一个 X 轴标签），values 为同一组内各数据系列的取值。 */
data class BarGroup(val label: String, val values: List<Float>)

/** 收入色（绿）与支出色（红），独立于主题以保证语义清晰。 */
val IncomeColor = Color(0xFF2E7D32)
val ExpenseColor = Color(0xFFC62828)

/**
 * 轻量柱状图，使用 Compose Canvas 绘制，无第三方依赖。
 * 支持单系列与多系列（分组）柱状图，自动计算 Y 轴上限并绘制网格与坐标轴标签。
 */
@Composable
fun BarChart(
    groups: List<BarGroup>,
    modifier: Modifier = Modifier,
    seriesColors: List<Color> = listOf(MaterialTheme.colorScheme.primary),
    maxValue: Float? = null,
    ySteps: Int = 4,
    showValueLabels: Boolean = false,
    labelStep: Int = 1
) {
    val surface = MaterialTheme.colorScheme.onSurfaceVariant
    val grid = MaterialTheme.colorScheme.outlineVariant
    val defaultColor = MaterialTheme.colorScheme.primary

    Canvas(modifier) {
        val scale = this.density
        val px = { dp: Int -> dp * scale }

        if (groups.isEmpty()) {
            val p = Paint().apply {
                color = surface.toArgb()
                textAlign = Paint.Align.CENTER
                textSize = 11f * scale
            }
            drawContext.canvas.nativeCanvas.drawText(
                "暂无数据",
                size.width / 2f,
                size.height / 2f,
                p
            )
            return@Canvas
        }

        val left = px(34)
        val bottom = px(22)
        val top = px(8)
        val right = px(6)
        val chartW = size.width - left - right
        val chartH = size.height - top - bottom

        val rawMax = maxValue ?: groups.maxOf { it.values.maxOrNull() ?: 0f }.coerceAtLeast(0f)
        val topVal = if (rawMax <= 0f) 1f else niceCeil(rawMax)
        val steps = if (topVal <= 8f) topVal.toInt().coerceAtLeast(1) else ySteps
        val step = topVal / steps

        val yLabelPaint = Paint().apply {
            color = surface.toArgb()
            textAlign = Paint.Align.RIGHT
            textSize = 9f * scale
        }
        val xLabelPaint = Paint().apply {
            color = surface.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = 9f * scale
        }
        val valuePaint = Paint().apply {
            color = surface.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = 9f * scale
        }

        // 网格线与 Y 轴标签
        for (i in 0..steps) {
            val y = top + chartH * (1f - i.toFloat() / steps)
            drawLine(grid, Offset(left, y), Offset(left + chartW, y), strokeWidth = px(1))
            val v = step * i
            val txt = if (v % 1f == 0f) v.toInt().toString() else "%.1f".format(v)
            drawContext.canvas.nativeCanvas.drawText(
                txt,
                left - px(4),
                y + px(3),
                yLabelPaint
            )
        }

        val seriesCount = groups.maxOfOrNull { it.values.size } ?: 0
        val groupW = chartW / groups.size
        val barArea = groupW * 0.72f
        val barW = if (seriesCount > 0) barArea / seriesCount else barArea

        groups.forEachIndexed { idx, g ->
            val gx = left + groupW * idx + (groupW - barArea) / 2f
            g.values.forEachIndexed { s, v ->
                val h = (v / topVal) * chartH
                val x = gx + s * barW
                val color = seriesColors.getOrElse(s) { defaultColor }
                drawRect(color, Offset(x, top + chartH - h), Size(barW * 0.88f, h))
                if (showValueLabels && groups.size <= 16 && v > 0f) {
                    val vt = if (v % 1f == 0f) v.toInt().toString() else "%.1f".format(v)
                    drawContext.canvas.nativeCanvas.drawText(
                        vt,
                        x + barW * 0.44f,
                        top + chartH - h - px(3),
                        valuePaint
                    )
                }
            }
            if (labelStep <= 1 || idx % labelStep == 0) {
                drawContext.canvas.nativeCanvas.drawText(
                    g.label,
                    left + groupW * idx + groupW / 2f,
                    top + chartH + px(14),
                    xLabelPaint
                )
            }
        }
    }
}

/** 图例。 */
@Composable
fun ChartLegend(items: List<Pair<String, Color>>) {
    if (items.isEmpty()) return
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (label, color) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp)
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(color, MaterialTheme.shapes.small)
                )
                Spacer(Modifier.width(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** 把数值向上取整到「好看」的上限（1,2,2.5,5,10 的倍数）。 */
private fun niceCeil(v: Float): Float {
    if (v <= 0f) return 1f
    val mag = 10f.pow(floor(log10(v)))
    val norm = v / mag
    val nice = when {
        norm <= 1f -> 1f
        norm <= 2f -> 2f
        norm <= 2.5f -> 2.5f
        norm <= 5f -> 5f
        else -> 10f
    }
    return nice * mag
}
