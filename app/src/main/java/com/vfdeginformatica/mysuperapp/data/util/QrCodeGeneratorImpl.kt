package com.vfdeginformatica.mysuperapp.data.util

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.vfdeginformatica.mysuperapp.domain.util.QrCodeGenerator
import javax.inject.Inject

class QrCodeGeneratorImpl @Inject constructor() : QrCodeGenerator {
    override fun generate(content: String, size: Int): Bitmap? {
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                size,
                size
            )
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(
                        x,
                        y,
                        if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                    )
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
