package com.vfdeginformatica.mysuperapp.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/** Decodes a gallery image, downsizes it to at most [MAX_DIMENSION] px and encodes it as JPEG. */
class ImageCompressor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun compress(uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val source = decode(uri) ?: return@withContext null
            val (w, h) = targetSize(source.width, source.height, MAX_DIMENSION)
            val scaled = if (w != source.width || h != source.height) {
                Bitmap.createScaledBitmap(source, w, h, true).also { if (it !== source) source.recycle() }
            } else {
                source
            }
            ByteArrayOutputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                scaled.recycle()
                out.toByteArray()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing image $uri", e)
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun decode(uri: Uri): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val src = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                val (w, h) = targetSize(info.size.width, info.size.height, MAX_DIMENSION)
                decoder.setTargetSize(w, h)
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
            }
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }

    companion object {
        private const val TAG = "ImageCompressor"
        const val MAX_DIMENSION = 1600
        const val JPEG_QUALITY = 85

        /** Scales (width,height) down proportionally so the larger side is at most [max]; never upscales. */
        fun targetSize(width: Int, height: Int, max: Int): Pair<Int, Int> {
            if (width <= 0 || height <= 0) return width.coerceAtLeast(1) to height.coerceAtLeast(1)
            val larger = maxOf(width, height)
            if (larger <= max) return width to height
            val ratio = max.toDouble() / larger
            return (width * ratio).toInt().coerceAtLeast(1) to (height * ratio).toInt().coerceAtLeast(1)
        }
    }
}
