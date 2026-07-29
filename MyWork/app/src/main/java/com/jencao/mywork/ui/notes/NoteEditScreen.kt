package com.jencao.mywork.ui.notes

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import java.io.File

/** Markdown 笔记编辑页：编辑 / 预览切换，√ 保存后返回，支持插入图片 / 拍照上传。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(nav: NavHostController, vm: NoteEditViewModel = hiltViewModel()) {
    val title by vm.title.collectAsStateWithLifecycle()
    val content by vm.content.collectAsStateWithLifecycle()
    val preview by vm.preview.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    val uploading by vm.uploading.collectAsStateWithLifecycle()
    val imageError by vm.imageError.collectAsStateWithLifecycle()

    val ctx = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // 选图后插入图片并自动切换到预览
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { vm.addImage(it); vm.showPreview() }
    }
    // 拍照：先生成临时文件 Uri，再由相机写回
    var cameraTmpUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = cameraTmpUri
        if (ok && uri != null) {
            vm.addImage(uri)
            vm.showPreview()
        }
    }

    fun launchCamera() {
        val dir = File(ctx.cacheDir, "camera").apply { if (!exists()) mkdirs() }
        val file = File(dir, "note_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file)
        cameraTmpUri = uri
        cameraLauncher.launch(uri)
    }

    LaunchedEffect(saved) {
        if (saved) nav.popBackStack()
    }
    LaunchedEffect(imageError) {
        imageError?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearImageError()
        }
    }

    // 系统返回键同样先保存再退出
    BackHandler { vm.save() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (vm.isNew) "新建笔记" else "编辑笔记") },
                navigationIcon = {
                    IconButton(onClick = { vm.save() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回并保存")
                    }
                },
                actions = {
                    IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(Icons.Filled.Image, contentDescription = "插入图片")
                    }
                    IconButton(onClick = { launchCamera() }) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = "拍照")
                    }
                    IconButton(onClick = { vm.togglePreview() }) {
                        Icon(
                            if (preview) Icons.Filled.Edit else Icons.Filled.Visibility,
                            contentDescription = if (preview) "编辑" else "预览"
                        )
                    }
                    IconButton(onClick = { vm.save() }) {
                        Icon(Icons.Filled.Check, contentDescription = "保存")
                    }
                    if (uploading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .size(24.dp),
                            strokeWidth = 2.dp
                        )
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
                    placeholder = { Text("正文（支持 Markdown：# 标题、- 列表、**粗体**、`代码`、> 引用、``` 代码块，![图片]() 插入图片）") },
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
