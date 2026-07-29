package com.jencao.mywork.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEditScreen(
    nav: NavHostController,
    vm: AccountViewModel = hiltViewModel()
) {
    val type by vm.type.collectAsStateWithLifecycle()
    val category by vm.category.collectAsStateWithLifecycle()
    val amount by vm.amount.collectAsStateWithLifecycle()
    val currency by vm.currency.collectAsStateWithLifecycle()
    val recordDate by vm.recordDate.collectAsStateWithLifecycle()
    val note by vm.note.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = recordDate)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vm.isNew) "记一笔" else "编辑记录") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (!vm.isNew) {
                        IconButton(onClick = { vm.delete(); nav.popBackStack() }) {
                            Icon(Icons.Filled.Delete, contentDescription = "删除")
                        }
                    }
                    IconButton(onClick = { vm.save(); nav.popBackStack() }) {
                        Icon(Icons.Filled.Check, contentDescription = "保存")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { vm.setType("expense") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == "expense") MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) { Text("支出") }
                Button(
                    onClick = { vm.setType("income") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == "income") MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) { Text("收入") }
            }
            OutlinedTextField(
                value = category,
                onValueChange = vm::setCategory,
                label = { Text("分类") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = amount,
                onValueChange = vm::setAmount,
                label = { Text("金额") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            OutlinedTextField(
                value = currency,
                onValueChange = vm::setCurrency,
                label = { Text("货币") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text("日期：${dateFmt.format(Date(recordDate))}")
            }
            OutlinedTextField(
                value = note,
                onValueChange = vm::setNote,
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { vm.setRecordDate(it) }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }
}
