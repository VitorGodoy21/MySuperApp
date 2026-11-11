package com.vfdeginformatica.mysuperapp.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.vfdeginformatica.mysuperapp.domain.repository.RecipeImageRepository
import kotlinx.coroutines.CancellationException

@Composable
internal fun RecipePhoto(
    userId: String,
    path: String,
    images: RecipeImageRepository,
    modifier: Modifier = Modifier,
) {
    var bytes by remember(userId, path) { mutableStateOf<ByteArray?>(null) }
    var failed by remember(userId, path) { mutableStateOf(false) }
    var retry by remember(userId, path) { mutableIntStateOf(0) }
    LaunchedEffect(userId, path, retry) {
        failed = false
        try {
            bytes = images.read(userId, path)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            failed = true
        }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        when {
            failed -> TextButton(onClick = { retry++ }) { Text("Recarregar foto") }
            bytes == null -> CircularProgressIndicator()
            else -> AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(bytes)
                    .memoryCachePolicy(CachePolicy.DISABLED).diskCachePolicy(CachePolicy.DISABLED).build(),
                contentDescription = "Foto da receita",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onError = { failed = true },
            )
        }
    }
}
