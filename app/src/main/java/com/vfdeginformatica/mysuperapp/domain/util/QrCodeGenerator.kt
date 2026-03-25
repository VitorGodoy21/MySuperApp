package com.vfdeginformatica.mysuperapp.domain.util

import android.graphics.Bitmap

interface QrCodeGenerator {
    fun generate(content: String, size: Int = 512): Bitmap?
}
