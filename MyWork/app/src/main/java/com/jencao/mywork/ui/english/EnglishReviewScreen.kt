package com.jencao.mywork.ui.english

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.jencao.mywork.ui.components.EmptyHint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnglishReviewScreen(
    rootNav: NavHostController,
    vm: EnglishReviewViewModel = hiltViewModel()
) {
    val due by vm.due.collectAsStateWithLifecycle()
    val index by vm.index.collectAsStateWithLifecycle()
    val revealed by vm.revealed.collectAsStateWithLifecycle()
    val word = due.getOrNull(index)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("记忆曲线复习") },
                navigationIcon = {
                    IconButton(onClick = { rootNav.popBackStack() }) {
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (due.isEmpty()) {
                EmptyHint("没有待复习的单词")
                return@Scaffold
            }

            if (word == null) {
                Spacer(Modifier.height(48.dp))
                Text("本轮复习完成", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text("共复习 ${vm.reviewedCount} 个单词", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(24.dp))
                Button(onClick = { rootNav.popBackStack() }) { Text("完成") }
                return@Scaffold
            }

            val progress = vm.reviewedCount.toFloat() / vm.total.coerceAtLeast(1)
            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text("进度 ${vm.reviewedCount} / ${vm.total}", style = MaterialTheme.typography.labelMedium)

            Spacer(Modifier.height(24.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(word.word, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    if (word.phonetic.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(word.phonetic, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(16.dp))
                    if (revealed) {
                        Text(word.meaning, style = MaterialTheme.typography.titleMedium)
                        if (word.example.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(word.example, style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        OutlinedButton(onClick = { vm.reveal() }) { Text("显示释义") }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            if (revealed) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReviewGradeButton("忘记", 1, Modifier.weight(1f)) { vm.grade(1) }
                    ReviewGradeButton("困难", 3, Modifier.weight(1f)) { vm.grade(3) }
                    ReviewGradeButton("良好", 4, Modifier.weight(1f)) { vm.grade(4) }
                    ReviewGradeButton("熟悉", 5, Modifier.weight(1f)) { vm.grade(5) }
                }
            }
        }
    }
}

@Composable
private fun ReviewGradeButton(
    label: String,
    quality: Int,
    modifier: Modifier,
    onClick: (Int) -> Unit
) {
    Button(
        onClick = { onClick(quality) },
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        Text(label)
    }
}
