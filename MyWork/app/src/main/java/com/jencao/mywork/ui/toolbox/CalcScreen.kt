package com.jencao.mywork.ui.toolbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.jencao.mywork.ui.components.NeuFab

@Composable
fun CalcScreen(navController: NavHostController, padding: PaddingValues, vm: CalcViewModel = hiltViewModel()) {
    val history by vm.history.collectAsStateWithLifecycle()
    val expr by vm.expr.collectAsStateWithLifecycle()
    val result by vm.result.collectAsStateWithLifecycle()
    val keys = listOf("7","8","9","/","4","5","6","*","1","2","3","-","0",".","%","+","(",")","^","=")

    Box(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = expr, onValueChange = vm::input,
                label = { Text("表达式") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                trailingIcon = { IconButton(vm::backspace) { Icon(Icons.Filled.Backspace, "删除") } }
            )
            Text(
                text = if (result.isBlank()) "= " else "= $result",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(keys.size) { i ->
                    val k = keys[i]
                    TextButton(
                        onClick = { if (k == "=") vm.compute() else vm.input(k) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(k, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            TextButton(onClick = vm::clear, modifier = Modifier.align(Alignment.End)) { Text("清空输入") }

            Text("历史", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
            LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history, key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        androidx.compose.foundation.layout.Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.expr, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                                Text("= ${item.result}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { vm.deleteHistory(item.id) }) { Icon(Icons.Filled.Delete, "删除") }
                        }
                    }
                }
            }
            if (history.isNotEmpty()) {
                TextButton(onClick = vm::clearHistory) { Text("清空历史") }
            }
        }
        NeuFab(onClick = vm::clear, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Filled.Delete, "清空")
        }
    }
}
