package com.jencao.mywork.ui.toolbox

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.WifiNetworkSpecifier
import android.os.Environment
import android.provider.ContactsContract
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.jencao.mywork.data.local.entity.QrScanEntity
import com.jencao.mywork.ui.components.NeuButton
import com.jencao.mywork.ui.components.NeuCard
import com.jencao.mywork.ui.components.NeuChip
import com.jencao.mywork.ui.components.NeuIconButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import android.content.ContentValues

@Composable
fun QrScreen(navController: NavHostController, padding: PaddingValues, vm: QrViewModel = hiltViewModel()) {
    val mode by vm.mode.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val lastResult by vm.lastResult.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NeuIconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, "返回", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Text("扫码 / 二维码", style = MaterialTheme.typography.titleLarge)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeuChip("扫码", selected = mode == QrMode.SCAN, onClick = { vm.setMode(QrMode.SCAN); vm.clearResult() }, modifier = Modifier.weight(1f))
            NeuChip("生成", selected = mode == QrMode.GENERATE, onClick = { vm.setMode(QrMode.GENERATE) }, modifier = Modifier.weight(1f))
        }

        if (mode == QrMode.SCAN) {
            ScanPanel(vm = vm, lastResult = lastResult, history = history, ctx = ctx)
        } else {
            GeneratePanel(vm = vm)
        }
    }
}

@Composable
private fun ScanPanel(vm: QrViewModel, lastResult: QrScanEntity?, history: List<QrScanEntity>, ctx: Context) {
    val analyzer = remember { QrAnalyzer() }
    DisposableEffect(analyzer) { onDispose { analyzer.active = false } }
    LaunchedEffect(Unit) { analyzer.resume() }

    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> hasPermission = granted }
    LaunchedEffect(Unit) { if (!hasPermission) launcher.launch(Manifest.permission.CAMERA) }

    if (!hasPermission) {
        NeuCard(Modifier.fillMaxWidth()) {
            Text("需要相机权限才能扫码", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            NeuButton("授予相机权限", onClick = { launcher.launch(Manifest.permission.CAMERA) })
        }
    } else {
        analyzer.onResult = { vm.onScanned(it) }
        QrScanner(analyzer = analyzer, modifier = Modifier.fillMaxWidth().height(320.dp))
    }

    lastResult?.let { result ->
        NeuCard(Modifier.fillMaxWidth()) {
            Text("扫码结果（${QrViewModel.typeLabel(result.type)}）", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(result.content, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeuButton("复制", onClick = { copyText(ctx, result.content) }, modifier = Modifier.weight(1f))
                when (result.type) {
                    1 -> NeuButton("打开链接", onClick = { openUrl(ctx, result.content) }, modifier = Modifier.weight(1f))
                    2 -> NeuButton("连接WiFi", onClick = { connectWifi(ctx, result.content) }, modifier = Modifier.weight(1f))
                    3 -> NeuButton("存联系人", onClick = { saveContact(ctx, result.content) }, modifier = Modifier.weight(1f))
                    4 -> NeuButton("发短信", onClick = { sendSms(ctx, result.content) }, modifier = Modifier.weight(1f))
                    5 -> NeuButton("拨号", onClick = { dial(ctx, result.content) }, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(8.dp))
            NeuButton("继续扫描", onClick = { analyzer.resume(); vm.clearResult() })
        }
    }

    if (history.isNotEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("最近扫码", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { vm.clearAll() }) { Text("清空") }
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 360.dp)
        ) {
            items(history, key = { it.id }) { item ->
                NeuCard(
                    Modifier
                        .fillMaxWidth()
                        .clickable { copyText(ctx, item.content) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(typeIcon(item.type), null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.content, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                            Text("${QrViewModel.typeLabel(item.type)} · ${fmt(item.scannedAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { vm.delete(item.id) }) {
                            Icon(Icons.Filled.Delete, "删除", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GeneratePanel(vm: QrViewModel) {
    val genType by vm.genType.collectAsStateWithLifecycle()
    val fields by vm.genFields.collectAsStateWithLifecycle()
    val bitmap by vm.genBitmap.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        QrType.entries.forEach { t ->
            NeuChip(t.label(), selected = genType == t, onClick = { vm.setGenType(t) })
        }
    }

    when (genType) {
        QrType.TEXT, QrType.URL -> QrField(
            fields.text,
            if (genType == QrType.URL) "网址" else "文本内容",
            KeyboardType.Text
        ) { value -> vm.updateGen { it.copy(text = value) } }

        QrType.WIFI -> {
            QrField(fields.wifiSsid, "WiFi 名称 (SSID)", KeyboardType.Text) { value -> vm.updateGen { it.copy(wifiSsid = value) } }
            QrField(fields.wifiPwd, "密码", KeyboardType.Text) { value -> vm.updateGen { it.copy(wifiPwd = value) } }
            QrField(fields.wifiEnc, "加密方式 (WPA / WEP / 无)", KeyboardType.Text) { value -> vm.updateGen { it.copy(wifiEnc = value) } }
        }

        QrType.VCARD -> {
            QrField(fields.vcName, "姓名", KeyboardType.Text) { value -> vm.updateGen { it.copy(vcName = value) } }
            QrField(fields.vcPhone, "电话", KeyboardType.Phone) { value -> vm.updateGen { it.copy(vcPhone = value) } }
            QrField(fields.vcEmail, "邮箱", KeyboardType.Email) { value -> vm.updateGen { it.copy(vcEmail = value) } }
            QrField(fields.vcOrg, "公司 / 组织", KeyboardType.Text) { value -> vm.updateGen { it.copy(vcOrg = value) } }
        }

        QrType.SMS -> {
            QrField(fields.smsNumber, "手机号", KeyboardType.Phone) { value -> vm.updateGen { it.copy(smsNumber = value) } }
            QrField(fields.smsBody, "短信内容", KeyboardType.Text) { value -> vm.updateGen { it.copy(smsBody = value) } }
        }

        QrType.PHONE -> QrField(fields.phone, "电话号码", KeyboardType.Phone) { value -> vm.updateGen { it.copy(phone = value) } }
    }

    NeuButton("生成二维码", onClick = { vm.generate() }, modifier = Modifier.fillMaxWidth())

    bitmap?.let { bmp ->
        NeuCard(Modifier.fillMaxWidth(), elevation = 4.dp) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Image(bitmap = bmp.asImageBitmap(), contentDescription = "二维码", modifier = Modifier.size(240.dp))
            }
            Spacer(Modifier.height(12.dp))
            NeuButton("保存到相册", onClick = {
                val uri = saveQrToGallery(ctx, bmp)
                toast(ctx, if (uri != null) "已保存到相册" else "保存失败")
            }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun QrField(value: String, label: String, keyboard: KeyboardType, onValue: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard)
    )
}

@Composable
private fun QrScanner(analyzer: QrAnalyzer, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    Box(modifier) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        Box(
            Modifier
                .align(Alignment.Center)
                .size(220.dp)
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
        )
    }

    DisposableEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            try {
                val provider = future.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(analysisExecutor, analyzer) }
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (_: Exception) {
            }
        }
        future.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            try { future.get().unbindAll() } catch (_: Exception) { }
            analysisExecutor.shutdown()
        }
    }
}

private fun typeIcon(type: Int) = when (type) {
    1 -> Icons.Filled.Link
    2 -> Icons.Filled.Wifi
    3 -> Icons.Filled.Person
    4 -> Icons.Filled.Sms
    5 -> Icons.Filled.Phone
    else -> Icons.Filled.TextFields
}

private val qrFmt = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
private fun fmt(ts: Long): String = if (ts > 0) qrFmt.format(Date(ts)) else qrFmt.format(Date())

private fun copyText(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("qr", text))
    toast(ctx, "已复制")
}

private fun openUrl(ctx: Context, url: String) {
    val u = if (url.startsWith("http", ignoreCase = true)) url else "https://$url"
    runCatching {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure { toast(ctx, "无法打开链接") }
}

private fun dial(ctx: Context, content: String) {
    val number = content.removePrefix("tel:")
    runCatching {
        ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure { toast(ctx, "无法拨号") }
}

private fun sendSms(ctx: Context, content: String) {
    val rest = content.removePrefix("SMSTO:")
    val parts = rest.split(":", limit = 2)
    val number = parts.getOrNull(0) ?: ""
    val body = parts.getOrNull(1) ?: ""
    runCatching {
        ctx.startActivity(
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number"))
                .putExtra("sms_body", body)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.onFailure { toast(ctx, "无法发送短信") }
}

private fun saveContact(ctx: Context, vcard: String) {
    val name = vcardField(vcard, "N:")
    val phone = vcardField(vcard, "TEL:")
    val email = vcardField(vcard, "EMAIL:")
    val org = vcardField(vcard, "ORG:")
    runCatching {
        ctx.startActivity(
            Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI).apply {
                putExtra(ContactsContract.Intents.Insert.NAME, name)
                putExtra(ContactsContract.Intents.Insert.PHONE, phone)
                putExtra(ContactsContract.Intents.Insert.EMAIL, email)
                putExtra(ContactsContract.Intents.Insert.COMPANY, org)
            }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.onFailure { toast(ctx, "无法保存联系人") }
}

private fun vcardField(vcard: String, key: String): String =
    vcard.lineSequence().firstOrNull { it.startsWith(key, ignoreCase = true) }
        ?.removePrefix(key).orEmpty().trim()

private fun connectWifi(ctx: Context, content: String) {
    val ssid = Regex("S:([^;]*)").find(content)?.groupValues?.getOrNull(1) ?: ""
    val pwd = Regex("P:([^;]*)").find(content)?.groupValues?.getOrNull(1) ?: ""
    if (ssid.isBlank()) { toast(ctx, "未识别到 WiFi 名称"); return }
    runCatching {
        val spec = WifiNetworkSpecifier.Builder().setSsid(ssid)
            .apply { if (pwd.isNotBlank()) setWpa2Passphrase(pwd) }
            .build()
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(spec)
            .build()
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.requestNetwork(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) { toast(ctx, "已连接到 $ssid") }
            override fun onUnavailable() { toast(ctx, "连接失败，请手动连接") }
        })
        toast(ctx, "正在连接 $ssid …")
    }.onFailure { toast(ctx, "连接失败") }
}

private fun saveQrToGallery(ctx: Context, bitmap: Bitmap): Uri? {
    val name = "qr_${System.currentTimeMillis()}.png"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyWork")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    return try {
        ctx.contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        ctx.contentResolver.update(uri, values, null, null)
        uri
    } catch (_: Exception) {
        null
    }
}

private fun toast(ctx: Context, msg: String) {
    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
}
