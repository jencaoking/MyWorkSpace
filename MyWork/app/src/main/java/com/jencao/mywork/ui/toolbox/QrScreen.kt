package com.jencao.mywork.ui.toolbox

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.jencao.mywork.ui.components.NeuFab

@Composable
fun QrScreen(navController: NavHostController, padding: PaddingValues, vm: QrViewModel = hiltViewModel()) {
    val history by vm.history.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var showGen by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Box(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("扫码记录", style = MaterialTheme.typography.titleMedium)
            LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history, key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth().clickable { selected = item.content }) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(item.content, style = MaterialTheme.typography.bodyMedium)
                            if (item.note.isNotBlank())
                                Text(item.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                IconButton(onClick = { showGen = item.content }) { Icon(Icons.Filled.Add, "生成二维码") }
                                IconButton(onClick = { copy(context, item.content) }) { Icon(Icons.Filled.ContentCopy, "复制") }
                                IconButton(onClick = { vm.delete(item.id) }) { Icon(Icons.Filled.Delete, "删除") }
                            }
                        }
                    }
                }
            }
        }
        NeuFab(onClick = { showAdd = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Filled.Add, "录入")
        }
    }

    if (showAdd) {
        var content by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showAdd = false }, confirmButton = {
            TextButton(onClick = { vm.add(content, note = note); showAdd = false }) { Text("保存") }
        }, dismissButton = { TextButton({ showAdd = false }) { Text("取消") } }, title = { Text("录入扫码结果") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(content, { content = it }, label = { Text("内容 / 链接") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(note, { note = it }, label = { Text("备注（可选）") }, modifier = Modifier.fillMaxWidth())
            }
        })
    }

    if (showGen != null) {
        AlertDialog(onDismissRequest = { showGen = null }, confirmButton = { TextButton({ showGen = null }) { Text("关闭") } },
            title = { Text("二维码") }, text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(bitmap = generateQr(showGen!!).asImageBitmap(), contentDescription = null, modifier = Modifier.size(220.dp))
                    Text(showGen!!, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
            })
    }

    if (selected != null) {
        AlertDialog(onDismissRequest = { selected = null }, confirmButton = { TextButton({ selected = null }) { Text("关闭") } },
            title = { Text("内容") }, text = {
                Column {
                    Text(selected!!)
                    TextButton(onClick = { copy(context, selected!!); selected = null }) { Text("复制到剪贴板") }
                }
            })
    }
}

private fun copy(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("qr", text))
}

fun generateQr(text: String, size: Int = 512): Bitmap {
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) for (y in 0 until size)
        bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
    return bmp
}
