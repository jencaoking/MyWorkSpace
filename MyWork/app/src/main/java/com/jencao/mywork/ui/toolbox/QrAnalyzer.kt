package com.jencao.mywork.ui.toolbox

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.toBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer

/** CameraX 图像分析器：每帧用 ZXing 解码条码/二维码。 */
class QrAnalyzer : ImageAnalysis.Analyzer {
    var onResult: ((String) -> Unit)? = null
    var active = true

    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(
                    BarcodeFormat.QR_CODE,
                    BarcodeFormat.AZTEC,
                    BarcodeFormat.DATA_MATRIX,
                    BarcodeFormat.PDF_417,
                    BarcodeFormat.CODE_128,
                    BarcodeFormat.CODE_39,
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.EAN_8,
                    BarcodeFormat.UPC_A,
                    BarcodeFormat.UPC_E
                ),
                DecodeHintType.TRY_HARDER to true
            )
        )
    }

    /** 一次扫码完成后会停止，调用 resume() 可重新开始扫描。 */
    fun resume() {
        active = true
    }

    override fun analyze(imageProxy: ImageProxy) {
        if (!active) {
            imageProxy.close()
            return
        }
        try {
            val bmp = imageProxy.toBitmap()
            val w = bmp.width
            val h = bmp.height
            val pixels = IntArray(w * h)
            bmp.getPixels(pixels, 0, w, 0, 0, w, h)
            val source = RGBLuminanceSource(w, h, pixels)
            val result = reader.decode(BinaryBitmap(HybridBinarizer(source)))
            active = false
            onResult?.invoke(result.text)
        } catch (_: Exception) {
            // 当前帧未识别到码，继续下一帧
        } finally {
            imageProxy.close()
        }
    }
}
