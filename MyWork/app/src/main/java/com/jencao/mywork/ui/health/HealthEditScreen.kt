package com.jencao.mywork.ui.health

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.jencao.mywork.ui.components.DateField
import com.jencao.mywork.ui.components.DateTimeField
import com.jencao.mywork.ui.components.DropdownField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthEditScreen(nav: NavHostController, vm: HealthViewModel = hiltViewModel()) {
    val type by vm.type.collectAsStateWithLifecycle()
    val value by vm.value.collectAsStateWithLifecycle()
    val unit by vm.unit.collectAsStateWithLifecycle()
    val time by vm.time.collectAsStateWithLifecycle()
    val note by vm.note.collectAsStateWithLifecycle()
    val reminderTime by vm.reminderTime.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }

    // 复诊 / 用药可设置提醒；设置后若未授权通知权限则申请
    val notifyLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 授权结果不影响保存，仅影响通知是否弹出 */ }
    fun requestNotifyPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifyLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val showReminder = type == "revisit" || type == "medication"

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
            DropdownField("类型", HEALTH_TYPES, type, vm::setType)
            OutlinedTextField(value = value, onValueChange = vm::setValue, label = { Text("数值/内容") },
                modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = unit, onValueChange = vm::setUnit, label = { Text("单位（可选，如 mmHg、次/分）") },
                modifier = Modifier.fillMaxWidth())
            DateField("日期", time, vm::setTime)
            if (showReminder) {
                DateTimeField(
                    label = "提醒时间（复诊 / 用药闹钟）",
                    millis = reminderTime,
                    onMillisChange = {
                        vm.setReminderTime(it)
                        if (it != null) requestNotifyPermission()
                    }
                )
            }
            OutlinedTextField(value = note, onValueChange = vm::setNote, label = { Text("备注（可选）") },
                modifier = Modifier.fillMaxWidth(), minLines = 2)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除记录") },
            text = { Text("确定删除这条健康记录吗？删除后会一并取消其提醒闹钟。") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete() }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } }
        )
    }
}
