package com.jencao.mywork.ui.media

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.jencao.mywork.ui.navigation.MediaRoutes
import com.jencao.mywork.data.remote.model.TmdbItem
import kotlinx.coroutines.launch

/**
 * TMDB 搜索页：仅“搜索 → 添加”，数据 100% 来自 TMDB（经后端代理 /api/proxy/tmdb/search）。
 * 密钥在服务端后台管理，App 不持有。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaSearchScreen(
    navController: NavHostController,
    vm: MediaViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TMDB 搜索") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = vm.tmdbQuery.value,
                    onValueChange = vm::setTmdbQuery,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("电影 / 剧集名称，如 batman") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(onSearch = { vm.searchTmdb() })
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = { vm.searchTmdb() }) {
                    Icon(Icons.Filled.Search, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("搜索")
                }
            }

            Spacer(Modifier.height(12.dp))

            val error = vm.tmdbError.value
            if (vm.tmdbSearching.value) {
                Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Text(
                    text = "搜索失败：$error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else {
                val results = vm.tmdbResults.value
                if (results.isEmpty()) {
                    Text(
                        text = "输入关键字后点击搜索，结果可直接添加。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(results, key = { it.tmdb_id }) { item ->
                            TmdbResultCard(item) {
                                scope.launch {
                                    val newId = vm.addFromTmdb(item)
                                    navController.popBackStack()
                                    navController.navigate(MediaRoutes.edit(newId))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TmdbResultCard(item: TmdbItem, onAdd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            if (item.poster_url.isNotBlank()) {
                AsyncImage(
                    model = item.poster_url,
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clickable(onClick = onAdd),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) { Text("无海报") }
            }
            Column(Modifier.padding(10.dp)) {
                Text(
                    text = item.title.ifBlank { item.original_title },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append(mediaTypeLabel(item.media_type))
                        if (item.release_date.length >= 4) append(" · ${item.release_date.take(4)}")
                        if (item.vote_average > 0f) append(" · ★ ${"%.1f".format(item.vote_average)}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                    Text("添加")
                }
            }
        }
    }
}
