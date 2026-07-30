package com.jencao.mywork.ui.toolbox

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

/** CameraX 图像分析器：直接取 YUV 的 Y 平面用 ZXing 解码条码/二维码。 */
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
            val source = imageProxy.toLuminanceSource()
            val text = source.decodeOrNull() ?: source.rotateCounterClockwise().decodeOrNull()
            if (text != null) {
                active = false
                onResult?.invoke(text)
            }
        } catch (_: Throwable) {
            // 当前帧解析失败，继续下一帧
        } finally {
            imageProxy.close()
        }
    }

    private fun LuminanceSource.decodeOrNull(): String? = try {
        reader.decodeWithState(BinaryBitmap(HybridBinarizer(this))).text
    } catch (_: Exception) {
        null
    } finally {
        reader.reset()
    }

    /** 取 YUV_420_888 的 Y 平面（灰度）构建 ZXing 亮度源，避免逐帧 Bitmap 转换。 */
    private fun ImageProxy.toLuminanceSource(): PlanarYUVLuminanceSource {
        val plane = planes[0]
        val buffer = plane.buffer.apply { rewind() }
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val data = ByteArray(width * height)

        if (pixelStride == 1 && rowStride == width) {
            buffer.get(data, 0, minOf(buffer.remaining(), data.size))
        } else {
            val row = ByteArray(rowStride)
            for (y in 0 until height) {
                val offset = y * rowStride
                if (offset >= buffer.limit()) break
                buffer.position(offset)
                val len = minOf(rowStride, buffer.remaining())
                buffer.get(row, 0, len)
                if (pixelStride == 1) {
                    System.arraycopy(row, 0, data, y * width, minOf(width, len))
                } else {
                    var x = 0
                    while (x < width && x * pixelStride < len) {
                        data[y * width + x] = row[x * pixelStride]
                        x++
                    }
                }
            }
        }
        return PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false)
    }
}
