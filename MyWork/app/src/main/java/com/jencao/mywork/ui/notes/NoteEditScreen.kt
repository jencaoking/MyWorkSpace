package com.jencao.mywork.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController

/** Markdown 笔记编辑页：编辑 / 预览切换，√ 保存后返回。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(nav: NavHostController, vm: NoteEditViewModel = hiltViewModel()) {
    val title by vm.title.collectAsStateWithLifecycle()
    val content by vm.content.collectAsStateWithLifecycle()
    val preview by vm.preview.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()

    LaunchedEffect(saved) {
        if (saved) nav.popBackStack()
    }

    // 系统返回键同样先保存再退出
    BackHandler { vm.save() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vm.isNew) "新建笔记" else "编辑笔记") },
                navigationIcon = {
                    IconButton(onClick = { vm.save() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回并保存")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.togglePreview() }) {
                        Icon(
                            if (preview) Icons.Filled.Edit else Icons.Filled.Visibility,
                            contentDescription = if (preview) "编辑" else "预览"
                        )
                    }
                    IconButton(onClick = { vm.save() }) {
                        Icon(Icons.Filled.Check, contentDescription = "保存")
                    }
                }
            )
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            TextField(
                value = title,
                onValueChange = vm::setTitle,
                placeholder = { Text("标题") },
                textStyle = MaterialTheme.typography.titleLarge,
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider()
            if (preview) {
                MarkdownText(
                    markdown = content,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                )
            } else {
                OutlinedTextField(
                    value = content,
                    onValueChange = vm::setContent,
                    placeholder = { Text("正文（支持 Markdown：# 标题、- 列表、**粗体**、`代码`、> 引用、``` 代码块）") },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Default),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp)
                )
            }
        }
    }
}
