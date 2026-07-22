package com.tarot.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

/**
 * Minimal WebView shell around the Arcanum web app (assets/www). All app logic
 * lives in the web bundle; this activity only hosts the WebView and exposes the
 * `Android.saveImage(dataUrl, filename)` bridge that js/app.js calls to save a
 * generated spread image to the gallery.
 */
class MainActivity : Activity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(AndroidBridge(this), "Android")
        setContentView(webView)

        webView.loadUrl("file:///android_asset/www/index.html")
    }

    private class AndroidBridge(private val activity: Activity) {

        @JavascriptInterface
        fun saveImage(dataUrl: String, filename: String) {
            try {
                val base64 = dataUrl.substringAfter(",", dataUrl)
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                val mime = if (filename.endsWith(".png")) "image/png" else "image/jpeg"

                val resolver = activity.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, mime)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Arcanum")
                    }
                }

                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: error("MediaStore insert failed")
                resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Could not open output stream")
            } catch (e: Exception) {
                activity.runOnUiThread {
                    Toast.makeText(activity, "Erro ao salvar imagem: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
