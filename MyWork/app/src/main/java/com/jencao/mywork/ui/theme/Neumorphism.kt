package com.jencao.mywork.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 由基底色推导新拟物的亮/暗双侧阴影
fun Color.lighten(factor: Float = 0.5f): Color = Color(
    red = red + (1f - red) * factor,
    green = green + (1f - green) * factor,
    blue = blue + (1f - blue) * factor,
    alpha = alpha
)

fun Color.darken(factor: Float = 0.5f): Color = Color(
    red = red * (1f - factor),
    green = green * (1f - factor),
    blue = blue * (1f - factor),
    alpha = alpha
)

fun neuLightShadow(base: Color): Color =
    if (base.luminance() > 0.5f) base.lighten(0.6f) else base.lighten(0.35f)

fun neuDarkShadow(base: Color): Color =
    if (base.luminance() > 0.5f) base.darken(0.32f) else base.darken(0.55f)

// 常用圆角尺度
val NeuRadius = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
val NeuRadiusSmall = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
val NeuRadiusLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)

/**
 * 新拟物（neumorphism）修饰符：以基底色绘制元素本体，
 * 左上投亮阴影、右下投暗阴影形成"凸起"质感；
 * pressed=true 时反转方向，呈现"内凹"质感（用于开关关闭态、进度槽等）。
 */
fun Modifier.neumorphic(
    shape: Shape = NeuRadius,
    elevation: Dp = 6.dp,
    backgroundColor: Color,
    lightColor: Color = neuLightShadow(backgroundColor),
    darkColor: Color = neuDarkShadow(backgroundColor),
    pressed: Boolean = false
): Modifier = this.drawWithCache {
    val outline: Outline = shape.createOutline(size, layoutDirection, this)
    val paint = Paint().apply {
        isAntiAlias = true
        color = backgroundColor
    }
    val e = elevation.toPx()
    onDrawWithContent {
        val (lx, ly) = if (pressed) (e to e) else (-e to -e)
        val (dx, dy) = if (pressed) (-e to -e) else (e to e)
        val fw = paint.asFrameworkPaint()
        drawIntoCanvas { canvas ->
            // 暗阴影（右下）
            fw.setShadowLayer(e, dx, dy, darkColor.toArgb())
            canvas.drawOutline(outline, paint)
            // 亮阴影（左上）
            fw.setShadowLayer(e, lx, ly, lightColor.toArgb())
            canvas.drawOutline(outline, paint)
            // 本体填充（无阴影）
            fw.clearShadowLayer()
            canvas.drawOutline(outline, paint)
        }
        drawContent()
    }
}
