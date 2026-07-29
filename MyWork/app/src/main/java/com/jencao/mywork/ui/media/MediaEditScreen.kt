package com.jencao.mywork.ui.media

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.jencao.mywork.ui.components.DropdownField
import com.jencao.mywork.ui.components.StarRating

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaEditScreen(nav: NavHostController, vm: MediaViewModel = hiltViewModel()) {
    val type by vm.type.collectAsStateWithLifecycle()
    val title by vm.title.collectAsStateWithLifecycle()
    val tmdbId by vm.tmdbId.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val rating by vm.rating.collectAsStateWithLifecycle()
    val posterUrl by vm.posterUrl.collectAsStateWithLifecycle()
    val note by vm.note.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(saved) { if (saved) nav.popBackStack() }
    BackHandler { vm.save() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vm.isNew) "新建记录" else "编辑记录") },
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
            DropdownField("类型", MEDIA_TYPES, type, vm::setType)
            OutlinedTextField(value = title, onValueChange = vm::setTitle, label = { Text("标题 *") },
                modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = tmdbId, onValueChange = vm::setTmdbId, label = { Text("TMDB ID（可选）") },
                modifier = Modifier.fillMaxWidth())
            DropdownField("状态", MEDIA_STATUS, status, vm::setStatus)
            Text("评分", style = MaterialTheme.typography.labelLarge)
            StarRating(rating = rating, onRatingChange = vm::setRating)
            OutlinedTextField(value = posterUrl ?: "", onValueChange = vm::setPosterUrl, label = { Text("海报链接（可选）") },
                modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = note, onValueChange = vm::setNote, label = { Text("备注（可选）") },
                modifier = Modifier.fillMaxWidth(), minLines = 2)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除记录") },
            text = { Text("确定删除这条记录吗？") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete() }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } }
        )
    }
}
