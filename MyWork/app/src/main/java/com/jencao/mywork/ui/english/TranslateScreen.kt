package com.jencao.mywork.ui.english

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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.jencao.mywork.ui.navigation.EnglishRoutes
import kotlinx.coroutines.launch

/**
 * 翻译页：文本翻译通过服务端代理（/api/proxy/translate），App 不持有任何密钥。
 * 支持把翻译出的英文单词一键加入单词本。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateScreen(nav: NavHostController, vm: EnglishViewModel = hiltViewModel()) {
    var text by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var toChinese by remember { mutableStateOf(true) } // true: 中→英，false: 英→中
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("翻译") },
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("方向：", style = MaterialTheme.typography.labelLarge)
                FilterChip(selected = toChinese, onClick = { toChinese = true }, label = { Text("中 → 英") })
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = !toChinese, onClick = { toChinese = false }, label = { Text("英 → 中") })
            }

            OutlinedTextField(
                value = text, onValueChange = { text = it },
                label = { Text("输入要翻译的文本") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 5
            )

            Button(
                onClick = {
                    if (text.isBlank()) return@Button
                    scope.launch {
                        loading = true
                        result = null
                        val (from, to) = if (toChinese) "zh-CHS" to "en" else "en" to "zh-CHS"
                        vm.translate(text.trim(), from, to)
                            .onSuccess { result = it.translation }
                            .onFailure { snackbar.showSnackbar("翻译失败：${it.message}") }
                        loading = false
                    }
                },
                enabled = text.isNotBlank() && !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (loading) "翻译中…" else "翻译")
            }

            result?.let { r ->
                Text("结果", style = MaterialTheme.typography.labelLarge)
                Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Text(r, Modifier.padding(12.dp))
                }
                if (!toChinese) {
                    TextButton(onClick = {
                        scope.launch {
                            val id = vm.createWord(text.trim(), r)
                            nav.navigate(EnglishRoutes.edit(id))
                        }
                    }) { Text("加入单词本（可在编辑页自动补全音标/例句）") }
                }
            }
        }
    }
}
