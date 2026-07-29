package com.jencao.mywork.ui.weather

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jencao.mywork.ui.components.NeuCard

/** 天气视觉映射：根据 QWeather 的 text 与 icon 代码，给出 emoji 与强调色，让卡片随天气状况变化。 */
object WeatherVisuals {
    data class Visual(val emoji: String, val color: Color)

    fun of(text: String, icon: String): Visual {
        val code = icon.toIntOrNull()
        val group = code?.div(100) ?: 0
        return when {
            group == 1 -> Visual("☀️", Color(0xFFFFB300))
            group == 2 -> Visual("⛅", Color(0xFF90A4AE))
            group == 3 -> Visual("🌧️", Color(0xFF4F86C6))
            group == 4 -> Visual("❄️", Color(0xFF81D4FA))
            group == 5 -> Visual("🌫️", Color(0xFFB0BEC5))
            group == 7 -> Visual("💨", Color(0xFFB0BEC5))
            text.contains("晴") -> Visual("☀️", Color(0xFFFFB300))
            text.contains("云") || text.contains("阴") -> Visual("⛅", Color(0xFF90A4AE))
            text.contains("雨") -> Visual("🌧️", Color(0xFF4F86C6))
            text.contains("雪") -> Visual("❄️", Color(0xFF81D4FA))
            text.contains("雾") || text.contains("霾") -> Visual("🌫️", Color(0xFFB0BEC5))
            else -> Visual("🌡️", Color(0xFF4F86C6))
        }
    }
}

@Composable
fun WeatherCard(
    state: WeatherUiState,
    onOpenPicker: () -> Unit
) {
    val vis = if (state.now != null) {
        WeatherVisuals.of(state.now.text, state.now.icon)
    } else {
        WeatherVisuals.of("", "")
    }
    NeuCard(
        modifier = Modifier.clickable { onOpenPicker() },
        elevation = 6.dp,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(vis.emoji, fontSize = 26.sp)
            Spacer(Modifier.width(8.dp))
            Column {
                if (state.now != null) {
                    Text(
                        text = "${state.now.temp}°",
                        style = MaterialTheme.typography.titleMedium,
                        color = vis.color,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = if (state.loading) "加载中" else "天气",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = state.cityName.ifBlank { "点击设置" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
