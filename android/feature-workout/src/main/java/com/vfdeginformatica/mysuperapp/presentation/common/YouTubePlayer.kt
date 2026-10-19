package com.vfdeginformatica.mysuperapp.presentation.common

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vfdeginformatica.mysuperapp.domain.model.YouTubeVideo

/**
 * Inline YouTube player (16:9) backed by a [WebView] running the official IFrame embed.
 * The wrapper page is loaded with [YouTubeVideo.EMBED_ORIGIN] as base URL so YouTube receives a
 * valid `Referer` (required since 2025; otherwise it shows "vídeo indisponível" 152-x/153).
 * Links that leave the embed (e.g. "Assistir no YouTube") open in the YouTube app/browser instead
 * of inside this small view, and the player's own fullscreen button is honoured (useful for Shorts).
 * Pauses when the host lifecycle pauses and is destroyed when it leaves composition.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayer(
    videoId: String,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 16f / 9f,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(12.dp)),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                webChromeClient = FullscreenWebChromeClient(context)
                webViewClient = ExternalLinksWebViewClient()
                setBackgroundColor(android.graphics.Color.BLACK)
                tag = ""
                webViewRef.value = this
            }
        },
        update = { webView ->
            if (webView.tag != videoId) {
                webView.tag = videoId
                webView.loadDataWithBaseURL(
                    YouTubeVideo.EMBED_ORIGIN + "/",
                    embedHtml(videoId),
                    "text/html",
                    "utf-8",
                    null,
                )
            }
        },
        onRelease = { webView ->
            webViewRef.value = null
            (webView.webChromeClient as? FullscreenWebChromeClient)?.onHideCustomView()
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        },
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> webViewRef.value?.onPause()
                Lifecycle.Event.ON_RESUME -> webViewRef.value?.onResume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

/** Keeps only the embed inside the WebView; anything else is handed to the system (YouTube app). */
private class ExternalLinksWebViewClient : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        if (YouTubeVideo.isEmbedUrl(url)) return false
        runCatching {
            view.context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
        return true
    }
}

/** Shows the player's fullscreen custom view over the activity window and hides system bars. */
private class FullscreenWebChromeClient(context: Context) : WebChromeClient() {
    private val activity: Activity? = context.findActivity()
    private var customView: View? = null
    private var customViewCallback: CustomViewCallback? = null

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        val decor = activity?.window?.decorView as? ViewGroup
        if (decor == null || customView != null) {
            callback.onCustomViewHidden()
            return
        }
        customView = view
        customViewCallback = callback
        view.setBackgroundColor(android.graphics.Color.BLACK)
        decor.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        activity.window?.let { window ->
            WindowCompat.getInsetsController(window, decor).apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    override fun onHideCustomView() {
        val view = customView ?: return
        val decor = activity?.window?.decorView as? ViewGroup
        decor?.removeView(view)
        activity?.window?.let { window ->
            decor?.let { WindowCompat.getInsetsController(window, it).show(WindowInsetsCompat.Type.systemBars()) }
        }
        customViewCallback?.onCustomViewHidden()
        customView = null
        customViewCallback = null
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Dialog wrapper used from the workout screen: title (exercise name), player and a close button. */
@Composable
fun YouTubePlayerDialog(
    title: String,
    videoId: String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                )
                // Taller than wide so vertical videos (Shorts) get more room on a phone screen.
                YouTubePlayer(videoId = videoId, aspectRatio = 3f / 4f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) { Text("Fechar") }
                }
            }
        }
    }
}

private fun embedHtml(videoId: String): String = """
    <!DOCTYPE html>
    <html>
    <head>
      <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
      <style>
        html, body { margin:0; padding:0; background:#000; height:100%; overflow:hidden; }
        iframe { position:absolute; top:0; left:0; width:100%; height:100%; border:0; }
      </style>
    </head>
    <body>
      <iframe src="${YouTubeVideo.embedUrl(videoId)}"
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; fullscreen; gyroscope; picture-in-picture"
              referrerpolicy="strict-origin-when-cross-origin"
              allowfullscreen></iframe>
    </body>
    </html>
""".trimIndent()
