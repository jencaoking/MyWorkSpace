package com.jencao.mywork.ui.toolbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(navController: NavHostController, padding: PaddingValues, vm: ConverterViewModel = hiltViewModel()) {
    val catIdx by vm.catIdx.collectAsStateWithLifecycle()
    val fromIdx by vm.fromIdx.collectAsStateWithLifecycle()
    val toIdx by vm.toIdx.collectAsStateWithLifecycle()
    val input by vm.input.collectAsStateWithLifecycle()
    val result by vm.result.collectAsStateWithLifecycle()
    val cat = vm.current
    var msg by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 类别选择
        var catExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
            OutlinedTextField(
                value = cat.name, onValueChange = {}, readOnly = true, label = { Text("类别") },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
            )
            androidx.compose.material3.DropdownMenu(catExpanded, { catExpanded = false }) {
                vm.categories.forEachIndexed { i, c ->
                    DropdownMenuItem(text = { Text(c.name) }, onClick = { vm.setCategory(i); catExpanded = false })
                }
            }
        }

        OutlinedTextField(
            value = input, onValueChange = vm::setInput, label = { Text("数值") },
            modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        // 从 -> 到
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UnitSpinner(cat.units.map { it.name }, fromIdx, "从", Modifier.weight(1f)) { vm.setFrom(it) }
            IconButton(onClick = { vm.setFrom(toIdx); vm.setTo(fromIdx) }) { Icon(Icons.Filled.SwapVert, "交换") }
            UnitSpinner(cat.units.map { it.name }, toIdx, "到", Modifier.weight(1f)) { vm.setTo(it) }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("结果", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(result.ifBlank { "—" }, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            }
        }

        if (cat.isCurrency) {
            TextButton(onClick = { vm.refreshLive { msg = it } }) {
                Icon(Icons.Filled.Refresh, null); Text(" 刷新实时汇率")
            }
            if (msg.isNotBlank()) Text(msg, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitSpinner(options: List<String>, selected: Int, label: String, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = options.getOrNull(selected) ?: "", onValueChange = {}, readOnly = true, label = { Text(label) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
        )
        androidx.compose.material3.DropdownMenu(expanded, { expanded = false }) {
            options.forEachIndexed { i, o -> DropdownMenuItem(text = { Text(o) }, onClick = { onSelect(i); expanded = false }) }
        }
    }
}
