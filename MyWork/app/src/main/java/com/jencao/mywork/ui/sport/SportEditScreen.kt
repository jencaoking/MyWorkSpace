package com.jencao.mywork.ui.sport

import androidx.activity.compose.BackHandler
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.jencao.mywork.ui.components.DateField
import com.jencao.mywork.ui.components.DropdownField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportEditScreen(nav: NavHostController, vm: SportViewModel = hiltViewModel()) {
    val type by vm.type.collectAsStateWithLifecycle()
    val duration by vm.duration.collectAsStateWithLifecycle()
    val distance by vm.distance.collectAsStateWithLifecycle()
    val calories by vm.calories.collectAsStateWithLifecycle()
    val date by vm.date.collectAsStateWithLifecycle()
    val note by vm.note.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(saved) { if (saved) nav.popBackStack() }
    BackHandler { vm.save() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vm.isNew) "新建运动" else "编辑运动") },
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
            DropdownField("类型", SPORT_TYPES.map { it to it }, type, vm::setType)
            OutlinedTextField(value = duration, onValueChange = vm::setDuration, label = { Text("时长（分钟）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = distance, onValueChange = vm::setDistance, label = { Text("距离（公里，可选）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = calories, onValueChange = vm::setCalories, label = { Text("消耗（千卡，可选）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            StepRecorder(vm)
            DateField("日期", date, vm::setDate)
            OutlinedTextField(value = note, onValueChange = vm::setNote, label = { Text("备注（可选）") },
                modifier = Modifier.fillMaxWidth(), minLines = 2)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除记录") },
            text = { Text("确定删除这条运动记录吗？") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete() }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } }
        )
    }
}

/**
 * 步数录入：支持手动输入，或用设备计步传感器（STEP_COUNTER）实时记录。
 * STEP_COUNTER 返回设备开机以来的累计步数，点击「开始计步」时记录基准值，
 * 结束时用当前累计值减去基准值得到本次步数并写回 ViewModel。
 */
@Composable
private fun StepRecorder(vm: SportViewModel) {
    val steps by vm.steps.collectAsStateWithLifecycle()
    var counting by remember { mutableStateOf(false) }
    var liveSteps by remember { mutableStateOf(0) }
    var baseline by remember { mutableStateOf<Long?>(null) }
    var noSensor by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val stepSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) }

    val listener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val total = event.values[0].toLong()
                if (baseline == null) baseline = total
                liveSteps = (total - (baseline ?: total)).toInt()
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    fun startCounting() {
        if (stepSensor == null) { noSensor = true; return }
        baseline = null
        liveSteps = 0
        sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_UI)
        counting = true
    }

    fun stopCounting() {
        sensorManager.unregisterListener(listener)
        counting = false
        if (liveSteps > 0) vm.setSteps(liveSteps.toString())
    }

    DisposableEffect(Unit) {
        onDispose {
            sensorManager.unregisterListener(listener)
            if (liveSteps > 0) vm.setSteps(liveSteps.toString())
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCounting() else noSensor = false
    }

    val perm = Manifest.permission.ACTIVITY_RECOGNITION
    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = steps,
            onValueChange = vm::setSteps,
            label = { Text("步数（可选）") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        if (noSensor) {
            Text(
                "当前设备不支持计步传感器，请手动输入步数。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = {
                if (counting) {
                    stopCounting()
                } else if (ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED) {
                    startCounting()
                } else {
                    permissionLauncher.launch(perm)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (counting) "停止计步（本次 $liveSteps 步）" else "用计步器记录步数")
        }
    }
}
