package com.jencao.mywork.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import java.io.File

/**
 * 轻量 Markdown 渲染器（阶段3，无第三方依赖）。
 * 支持：# 标题(1-3级)、- / * 无序列表、1. 有序列表、> 引用、--- 分隔线、
 * ``` 代码块、行内 **粗体**、*斜体*、`代码`、~~删除线~~，以及 ![alt](url) 图片。
 */
@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    val lines = markdown.lines()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trimStart()
            when {
                trimmed.startsWith("```") -> {
                    // 代码块：收集到下一个 ```
                    val buf = StringBuilder()
                    i++
                    while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                        buf.appendLine(lines[i])
                        i++
                    }
                    CodeBlock(buf.toString().trimEnd())
                }
                trimmed.startsWith("### ") -> Text(
                    inline(trimmed.removePrefix("### ")),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                trimmed.startsWith("## ") -> Text(
                    inline(trimmed.removePrefix("## ")),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                trimmed.startsWith("# ") -> Text(
                    inline(trimmed.removePrefix("# ")),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                trimmed == "---" || trimmed == "***" -> HorizontalDivider(Modifier.padding(vertical = 4.dp))
                trimmed.startsWith("> ") -> QuoteBlock(inline(trimmed.removePrefix("> ")))
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> BulletLine("•", inline(trimmed.drop(2)))
                Regex("^\\d+\\.\\s").containsMatchIn(trimmed) -> {
                    val idx = trimmed.indexOf(". ")
                    BulletLine(trimmed.substring(0, idx + 1), inline(trimmed.substring(idx + 2)))
                }
                trimmed.isEmpty() -> Spacer(Modifier.height(2.dp))
                // 整行图片语法 ![alt](url)：本地 file:// 或远程 http(s) URL
                Regex("^!\\[(.*?)\\]\\((.*?)\\)$").containsMatchIn(trimmed) -> {
                    val m = Regex("^!\\[(.*?)\\]\\((.*?)\\)$").find(trimmed)!!
                    val (alt, url) = m.destructured
                    MarkdownImageBlock(url, alt)
                }
                else -> Text(inline(line), style = MaterialTheme.typography.bodyMedium)
            }
            i++
        }
    }
}

@Composable
private fun CodeBlock(code: String) {
    Text(
        code,
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(12.dp)
    )
}

@Composable
private fun QuoteBlock(text: AnnotatedString) {
    Row(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .width(3.dp)
                .height(20.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
        ) {}
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BulletLine(marker: String, text: AnnotatedString) {
    Row(Modifier.fillMaxWidth()) {
        Text(marker, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun MarkdownImageBlock(url: String, alt: String) {
    // 本地引用 file:///... 转换为 File 直接加载（Coil 读取应用私有目录）
    val model: Any = if (url.startsWith("file://")) File(url.removePrefix("file://")) else url
    Spacer(Modifier.height(4.dp))
    AsyncImage(
        model = model,
        contentDescription = alt.ifEmpty { "图片" },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .heightIn(max = 320.dp),
        contentScale = ContentScale.FillWidth
    )
}

/** 行内样式解析：**粗体**、*斜体*、`代码`、~~删除线~~ */
private fun inline(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end > 0) {
                    withStyleText(text.substring(i + 2, end), SpanStyle(fontWeight = FontWeight.Bold))
                    i = end + 2
                } else { append(text[i]); i++ }
            }
            text.startsWith("~~", i) -> {
                val end = text.indexOf("~~", i + 2)
                if (end > 0) {
                    withStyleText(text.substring(i + 2, end), SpanStyle(textDecoration = TextDecoration.LineThrough))
                    i = end + 2
                } else { append(text[i]); i++ }
            }
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end > 0) {
                    withStyleText(text.substring(i + 1, end), SpanStyle(fontFamily = FontFamily.Monospace))
                    i = end + 1
                } else { append(text[i]); i++ }
            }
            text[i] == '*' -> {
                val end = text.indexOf('*', i + 1)
                if (end > 0) {
                    withStyleText(text.substring(i + 1, end), SpanStyle(fontStyle = FontStyle.Italic))
                    i = end + 1
                } else { append(text[i]); i++ }
            }
            else -> { append(text[i]); i++ }
        }
    }
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.withStyleText(s: String, style: SpanStyle) {
    pushStyle(style)
    append(s)
    pop()
}
