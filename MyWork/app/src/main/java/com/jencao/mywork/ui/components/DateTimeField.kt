package com.jencao.mywork.ui.components

import android.icu.util.Calendar
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 日期 + 时间选择框。只读展示，点击日历图标先用 DatePicker 选日期，再用 TimePicker 选时间；
 * 已选值时显示清除按钮。返回值为自 1970 起的毫秒（本地时区），null 表示未设置。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeField(
    label: String,
    millis: Long?,
    onMillisChange: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(millis ?: System.currentTimeMillis()) }

    val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    OutlinedTextField(
        value = if (millis == null) "" else fmt.format(java.util.Date(millis)),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        placeholder = { Text("未设置") },
        modifier = modifier,
        trailingIcon = {
            Row {
                if (millis != null) {
                    IconButton(onClick = { onMillisChange(null) }) {
                        Icon(Icons.Filled.Clear, contentDescription = "清除")
                    }
                }
                IconButton(onClick = {
                    draft = millis ?: System.currentTimeMillis()
                    showDate = true
                }) {
                    Icon(Icons.Filled.DateRange, contentDescription = "选择日期时间")
                }
            }
        }
    )

    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = draft)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    val picked = state.selectedDateMillis ?: draft
                    val cal = Calendar.getInstance().apply { timeInMillis = draft }
                    val newCal = Calendar.getInstance().apply { timeInMillis = picked }
                    newCal.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY))
                    newCal.set(Calendar.MINUTE, cal.get(Calendar.MINUTE))
                    draft = newCal.timeInMillis
                    showDate = false
                    showTime = true
                }) { Text("下一步") }
            },
            dismissButton = {
                TextButton(onClick = { showDate = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showTime) {
        val cal = Calendar.getInstance().apply { timeInMillis = draft }
        val tState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE)
        )
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = {
                    val newCal = Calendar.getInstance().apply { timeInMillis = draft }
                    newCal.set(Calendar.HOUR_OF_DAY, tState.hour)
                    newCal.set(Calendar.MINUTE, tState.minute)
                    newCal.set(Calendar.SECOND, 0)
                    newCal.set(Calendar.MILLISECOND, 0)
                    onMillisChange(newCal.timeInMillis)
                    showTime = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTime = false }) { Text("取消") }
            },
            text = { TimePicker(state = tState) }
        )
    }
}
