package com.aamsco.awardswithfriends.ui.components

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class TrailerPlayerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_YOUTUBE_ID = "youtube_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val youTubeId = intent.getStringExtra(EXTRA_YOUTUBE_ID)?.trim() ?: run {
            finish()
            return
        }

        setContent {
            TrailerPlayerScreen(
                youTubeId = youTubeId,
                onClose = { finish() }
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun TrailerPlayerScreen(
    youTubeId: String,
    onClose: () -> Unit
) {
    val appOrigin = "https://awardswithfriends-25718.web.app"
    val html = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                html, body { width: 100%; height: 100%; background: #000; overflow: hidden; }
                iframe { position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none; }
            </style>
        </head>
        <body>
            <iframe
                src="https://www.youtube.com/embed/${youTubeId}?autoplay=1&rel=0&playsinline=1&modestbranding=1&origin=${appOrigin}"
                allow="autoplay; encrypted-media; fullscreen"
                allowfullscreen
                frameborder="0">
            </iframe>
        </body>
        </html>
    """.trimIndent()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.BLACK)
                    settings.javaScriptEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                    webViewClient = WebViewClient()
                    webChromeClient = object : WebChromeClient() {
                        private var customView: android.view.View? = null

                        override fun onShowCustomView(
                            view: android.view.View?,
                            callback: CustomViewCallback?
                        ) {
                            customView = view
                            (context as? ComponentActivity)?.let { activity ->
                                val decorView = activity.window.decorView as FrameLayout
                                decorView.addView(
                                    view,
                                    FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        FrameLayout.LayoutParams.MATCH_PARENT
                                    )
                                )
                            }
                        }

                        override fun onHideCustomView() {
                            customView?.let { view ->
                                (context as? ComponentActivity)?.let { activity ->
                                    val decorView = activity.window.decorView as FrameLayout
                                    decorView.removeView(view)
                                }
                            }
                            customView = null
                        }
                    }

                    loadDataWithBaseURL(
                        appOrigin,
                        html,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .align(Alignment.Center)
        )

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White
            )
        }
    }
}
