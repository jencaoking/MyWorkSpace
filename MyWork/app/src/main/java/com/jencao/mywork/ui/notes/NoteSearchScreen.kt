package com.jencao.mywork.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.jencao.mywork.ui.navigation.NoteRoutes

/** 全文搜索页（FTS4 + LIKE 兜底，300ms 防抖）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteSearchScreen(nav: NavHostController, vm: NotesViewModel = hiltViewModel()) {
    val query by vm.query.collectAsStateWithLifecycle()
    val results by vm.searchResults.collectAsStateWithLifecycle()
    val focusRequester = FocusRequester()

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = vm::setQuery,
                        placeholder = { Text("搜索标题与正文…") },
                        singleLine = true,
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { vm.setQuery("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "清空")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { inner ->
        if (query.isBlank()) {
            Hint(inner, "输入关键字搜索全部笔记（支持全文检索）")
        } else if (results.isEmpty()) {
            Hint(inner, "未找到与「$query」匹配的笔记")
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(results, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onClick = { nav.navigate(NoteRoutes.edit(note.id)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun Hint(inner: PaddingValues, text: String) {
    Column(
        Modifier.fillMaxSize().padding(inner).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
