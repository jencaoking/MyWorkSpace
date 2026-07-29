package com.jencao.mywork.ui.english

import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.jencao.mywork.ui.components.StarRating
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnglishEditScreen(nav: NavHostController, vm: EnglishViewModel = hiltViewModel()) {
    val word by vm.word.collectAsStateWithLifecycle()
    val phonetic by vm.phonetic.collectAsStateWithLifecycle()
    val meaning by vm.meaning.collectAsStateWithLifecycle()
    val example by vm.example.collectAsStateWithLifecycle()
    val familiarity by vm.familiarity.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()

    var confirmDelete by remember { mutableStateOf(false) }
    var lookupLoading by remember { mutableStateOf(false) }
    var speakUrl by remember { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    fun play(url: String) {
        try {
            player?.release()
            player = MediaPlayer.create(context, Uri.parse(url))?.apply { start() }
        } catch (e: Exception) {
            scope.launch { snackbar.showSnackbar("播放失败：${e.message}") }
        }
    }

    LaunchedEffect(saved) { if (saved) nav.popBackStack() }
    BackHandler { vm.save() }
    DisposableEffect(Unit) { onDispose { player?.release() } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(if (vm.isNew) "新建单词" else "编辑单词") },
                navigationIcon = {
                    IconButton(onClick = { vm.save() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回并保存")
                    }
                },
                actions = {
                    if (!vm.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "删除")
                        }
                    }
                    IconButton(onClick = { vm.save() }) {
                        Icon(Icons.Filled.Check, contentDescription = "保存")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = word, onValueChange = vm::setWord, label = { Text("单词 *") },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
                TextButton(
                    onClick = {
                        if (word.isBlank()) return@TextButton
                        scope.launch {
                            lookupLoading = true
                            vm.lookupWord(word.trim())
                                .onSuccess { r ->
                                    if (r.phonetic.isNotBlank()) vm.setPhonetic(r.phonetic)
                                    if (r.explains.isNotEmpty()) vm.setMeaning(r.explains.joinToString("\n"))
                                    else if (r.translation.isNotEmpty()) vm.setMeaning(r.translation.joinToString("\n"))
                                    if (r.examples.isNotEmpty()) {
                                        vm.setExample(r.examples.joinToString("\n") { "${it.source} — ${it.target}" })
                                    }
                                    speakUrl = r.speak_url
                                }
                                .onFailure { e -> snackbar.showSnackbar("查询失败：${e.message}") }
                            lookupLoading = false
                        }
                    },
                    enabled = word.isNotBlank() && !lookupLoading
                ) {
                    Text(if (lookupLoading) "查询中…" else "查询释义")
                }
            }

            if (speakUrl.isNotBlank()) {
                TextButton(onClick = { play(speakUrl) }) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                    Text("  播放原音")
                }
            }

            OutlinedTextField(value = phonetic, onValueChange = vm::setPhonetic, label = { Text("音标（可选）") },
                modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = meaning, onValueChange = vm::setMeaning, label = { Text("释义（可选）") },
                modifier = Modifier.fillMaxWidth(), minLines = 2)
            OutlinedTextField(value = example, onValueChange = vm::setExample, label = { Text("例句（可选）") },
                modifier = Modifier.fillMaxWidth(), minLines = 2)
            Text("熟悉度", style = MaterialTheme.typography.labelLarge)
            StarRating(rating = familiarity, onRatingChange = vm::setFamiliarity)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除单词") },
            text = { Text("确定删除这个单词吗？") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete() }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } }
        )
    }
}
