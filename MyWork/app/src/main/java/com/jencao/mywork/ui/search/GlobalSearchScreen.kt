package com.jencao.mywork.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.jencao.mywork.data.search.GlobalSearchResult
import com.jencao.mywork.data.search.SearchModule
import com.jencao.mywork.ui.components.NeuCard
import com.jencao.mywork.ui.navigation.moduleRoute

/** 板块展示顺序（搜索结果按此顺序分组）。 */
private val MODULE_ORDER = listOf(
    SearchModule.TASK,
    SearchModule.NOTE,
    SearchModule.MOVIE_BOOK,
    SearchModule.SPORT,
    SearchModule.ENGLISH,
    SearchModule.HEALTH,
    SearchModule.ACCOUNT,
    SearchModule.POMODORO
)

private fun moduleIcon(module: SearchModule): ImageVector = when (module) {
    SearchModule.TASK -> Icons.Filled.CheckCircle
    SearchModule.NOTE -> Icons.Filled.Book
    SearchModule.MOVIE_BOOK -> Icons.Filled.Movie
    SearchModule.SPORT -> Icons.Filled.FitnessCenter
    SearchModule.ENGLISH -> Icons.Filled.AutoStories
    SearchModule.HEALTH -> Icons.Filled.Favorite
    SearchModule.ACCOUNT -> Icons.Filled.AccountBalanceWallet
    SearchModule.POMODORO -> Icons.Filled.Timer
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    navController: NavController,
    padding: PaddingValues,
    viewModel: GlobalSearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    Scaffold(
        modifier = Modifier.padding(padding),
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = viewModel::setQuery,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("搜索任务、笔记、影音、记账…") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = if (query.isNotEmpty()) {
                            {
                                IconButton(onClick = viewModel::clear) {
                                    Icon(Icons.Filled.Clear, contentDescription = "清除")
                                }
                            }
                        } else null
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { inner ->
        Box(modifier = Modifier.fillMaxSize().padding(inner)) {
            when {
                query.isBlank() -> SearchHint()
                isSearching -> LoadingState()
                results.isEmpty() -> EmptyState(query)
                else -> ResultList(results, navController)
            }
        }
    }
}

@Composable
private fun SearchHint() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text("输入关键词，跨模块检索", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(query: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("未找到与“$query”相关的内容", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ResultList(results: List<GlobalSearchResult>, navController: NavController) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MODULE_ORDER.forEach { module ->
            val items = results.filter { it.module == module }
            if (items.isNotEmpty()) {
                item {
                    Text(
                        text = "${module.label} (${items.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(items, key = { "${module.name}_${it.id}" }) { result ->
                    SearchResultCard(result, navController)
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(result: GlobalSearchResult, navController: NavController) {
    NeuCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { navController.navigate(moduleRoute(result.module.moduleKey)) }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = moduleIcon(result.module),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (result.snippet.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = result.snippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
