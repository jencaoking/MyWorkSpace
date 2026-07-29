package com.jencao.mywork.ui.english

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.jencao.mywork.ui.navigation.EnglishRoutes
import kotlinx.coroutines.launch
import java.io.File

/**
 * 跟读练习：播放原音（有道 speak_url，经服务端代理获取），用 MediaRecorder 录制用户跟读，
 * 再回放用户录音对比。全程不向 App 下发任何 API 密钥，录音文件仅保存在本机。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnglishShadowScreen(nav: NavHostController, vm: EnglishShadowViewModel = hiltViewModel()) {
    val word by vm.word.collectAsStateWithLifecycle()
    val speakUrl by vm.speakUrl.collectAsStateWithLifecycle()
    val audioPath by vm.audioPath.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    val audioFile = remember(word?.id) {
        val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "shadow")
        File(dir, "word_${word?.id ?: "tmp"}.m4a")
    }

    fun startRecording() {
        try {
            audioFile.parentFile?.mkdirs()
            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION") MediaRecorder()
            }
            recorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }
            isRecording = true
        } catch (e: Exception) {
            scope.launch { snackbar.showSnackbar("录音失败：${e.message}") }
        }
    }

    fun stopRecording() {
        try { recorder?.stop() } catch (_: Exception) { }
        recorder?.release()
        recorder = null
        isRecording = false
        vm.saveAudio(audioFile.absolutePath)
    }

    fun playFile(path: String?) {
        if (path.isNullOrEmpty()) return
        try {
            player?.release()
            player = MediaPlayer.create(context, Uri.fromFile(File(path)))?.apply { start() }
        } catch (e: Exception) {
            scope.launch { snackbar.showSnackbar("播放失败：${e.message}") }
        }
    }

    fun playUrl(url: String) {
        if (url.isBlank()) return
        try {
            player?.release()
            player = MediaPlayer.create(context, Uri.parse(url))?.apply { start() }
        } catch (e: Exception) {
            scope.launch { snackbar.showSnackbar("播放失败：${e.message}") }
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecording() else scope.launch { snackbar.showSnackbar("需要麦克风权限才能跟读录音") }
    }

    DisposableEffect(Unit) {
        onDispose { recorder?.release(); player?.release() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("跟读练习") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (word == null) {
                Text("未找到该单词")
            } else {
                Text(word!!.word, style = MaterialTheme.typography.headlineMedium)
                if (word!!.phonetic.isNotBlank()) Text("音标：${word!!.phonetic}")
                if (word!!.meaning.isNotBlank()) Text(word!!.meaning, style = MaterialTheme.typography.bodyMedium)
                if (loading) Text("正在获取原音…")
                if (error != null) Text("原音获取失败：$error", color = MaterialTheme.colorScheme.error)

                Button(
                    onClick = { playUrl(speakUrl) },
                    enabled = speakUrl.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("播放原音")
                }

                Spacer(Modifier.height(8.dp))
                Text("跟读对比", style = MaterialTheme.typography.titleMedium)
                if (isRecording) {
                    Button(
                        onClick = { stopRecording() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("停止并保存录音")
                    }
                } else {
                    Button(
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) startRecording() else permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Mic, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("开始跟读录音")
                    }
                }

                if (!audioPath.isNullOrEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { playFile(audioPath) }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("播放我的录音")
                        }
                        OutlinedButton(onClick = { scope.launch { vm.saveAudio(null) } }) {
                            Text("清除录音")
                        }
                    }
                }

                Text(
                    "操作建议：先听原音，再点“开始跟读录音”读出该单词，停止后对比发音；可反复练习。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
