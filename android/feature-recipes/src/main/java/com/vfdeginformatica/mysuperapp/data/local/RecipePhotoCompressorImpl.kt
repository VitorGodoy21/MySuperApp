package com.vfdeginformatica.mysuperapp.data.local

import androidx.core.net.toUri
import com.vfdeginformatica.mysuperapp.domain.repository.RecipePhotoCompressor

class RecipePhotoCompressorImpl(private val compressor: ImageCompressor) : RecipePhotoCompressor {
    override suspend fun compress(uri: String): ByteArray =
        checkNotNull(compressor.compress(uri.toUri())) { "Não foi possível preparar a foto. Selecione-a novamente" }
}
