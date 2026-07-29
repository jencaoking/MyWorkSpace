package com.jencao.mywork.ui.toolbox

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.jencao.mywork.data.local.entity.QrScanEntity
import com.jencao.mywork.data.repository.QrScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class QrMode { SCAN, GENERATE }
enum class QrType(val code: Int) { TEXT(0), URL(1), WIFI(2), VCARD(3), SMS(4), PHONE(5) }

data class GenFields(
    val text: String = "",
    val wifiSsid: String = "",
    val wifiPwd: String = "",
    val wifiEnc: String = "WPA",
    val vcName: String = "",
    val vcPhone: String = "",
    val vcEmail: String = "",
    val vcOrg: String = "",
    val smsNumber: String = "",
    val smsBody: String = "",
    val phone: String = ""
)

@HiltViewModel
class QrViewModel @Inject constructor(private val repo: QrScanRepository) : ViewModel() {

    private val _mode = MutableStateFlow(QrMode.SCAN)
    val mode: StateFlow<QrMode> = _mode.asStateFlow()
    fun setMode(m: QrMode) { _mode.value = m }

    val history: StateFlow<List<QrScanEntity>> =
        repo.observeRecent().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _lastResult = MutableStateFlow<QrScanEntity?>(null)
    val lastResult: StateFlow<QrScanEntity?> = _lastResult.asStateFlow()
    fun clearResult() { _lastResult.value = null }

    fun onScanned(content: String) {
        val c = content.trim()
        if (c.isBlank()) return
        val entity = QrScanEntity(content = c, type = inferType(c), scanned_at = System.currentTimeMillis())
        viewModelScope.launch {
            repo.insert(entity)
            _lastResult.value = entity
        }
    }

    fun delete(id: String) = viewModelScope.launch { repo.softDelete(id) }
    fun clearAll() = viewModelScope.launch { repo.clearAll() }

    private val _genType = MutableStateFlow(QrType.TEXT)
    val genType: StateFlow<QrType> = _genType.asStateFlow()
    fun setGenType(t: QrType) { _genType.value = t }

    private val _genFields = MutableStateFlow(GenFields())
    val genFields: StateFlow<GenFields> = _genFields.asStateFlow()
    fun updateGen(transform: (GenFields) -> GenFields) { _genFields.value = transform(_genFields.value) }

    private val _genBitmap = MutableStateFlow<Bitmap?>(null)
    val genBitmap: StateFlow<Bitmap?> = _genBitmap.asStateFlow()

    fun generate() {
        val f = _genFields.value
        val type = _genType.value
        if (type == QrType.WIFI && f.wifiSsid.isBlank()) { _genBitmap.value = null; return }
        val content = buildContent(type, f)
        if (content.isBlank()) { _genBitmap.value = null; return }
        _genBitmap.value = encodeQr(content)
    }

    companion object {
        fun inferType(content: String): Int {
            val c = content.trim()
            return when {
                c.startsWith("WIFI:", ignoreCase = true) -> 2
                c.startsWith("BEGIN:VCARD", ignoreCase = true) -> 3
                c.startsWith("SMSTO:", ignoreCase = true) -> 4
                c.startsWith("tel:", ignoreCase = true) -> 5
                c.startsWith("http://", ignoreCase = true) || c.startsWith("https://", ignoreCase = true) -> 1
                c.startsWith("www.", ignoreCase = true) -> 1
                Patterns.WEB_URL.matcher(c).matches() -> 1
                c.matches(Regex("^[+]?[0-9 ()\\-]{6,}$")) -> 5
                else -> 0
            }
        }

        fun typeLabel(type: Int): String = when (type) {
            1 -> "网址"; 2 -> "WiFi"; 3 -> "名片"; 4 -> "短信"; 5 -> "电话"; else -> "文本"
        }

        fun buildContent(type: QrType, f: GenFields): String = when (type) {
            QrType.TEXT -> f.text
            QrType.URL -> f.text
            QrType.WIFI -> "WIFI:T:${f.wifiEnc};S:${f.wifiSsid};P:${f.wifiPwd};;"
            QrType.VCARD -> "BEGIN:VCARD\nVERSION:3.0\nN:${f.vcName}\nTEL:${f.vcPhone}\nEMAIL:${f.vcEmail}\nORG:${f.vcOrg}\nEND:VCARD"
            QrType.SMS -> "SMSTO:${f.smsNumber}:${f.smsBody}"
            QrType.PHONE -> "tel:${f.phone}"
        }

        fun encodeQr(content: String, sizePx: Int = 720): Bitmap? {
            return try {
                val hints = mapOf(
                    EncodeHintType.MARGIN to 1,
                    EncodeHintType.CHARACTER_SET to "UTF-8"
                )
                val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
                val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
                for (x in 0 until sizePx) for (y in 0 until sizePx) {
                    bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                }
                bmp
            } catch (_: WriterException) { null }
        }
    }
}

fun QrType.label(): String = when (this) {
    QrType.TEXT -> "文本"; QrType.URL -> "网址"; QrType.WIFI -> "WiFi"
    QrType.VCARD -> "名片"; QrType.SMS -> "短信"; QrType.PHONE -> "电话"
}
