package com.paydaytracker.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var pendingCsv: ByteArray? = null
    private val createCsvRequest = 901

    inner class AndroidBridge {
        @JavascriptInterface
        fun setDarkMode(dark: Boolean) {
            runOnUiThread {
                window.statusBarColor = if (dark) Color.rgb(7, 23, 35) else getColor(R.color.navy)
                window.navigationBarColor = if (dark) Color.rgb(16, 24, 32) else getColor(R.color.white)
                window.decorView.systemUiVisibility = if (dark) 0 else View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }

        @JavascriptInterface
        fun exportCsv(base64Data: String, fileName: String) {
            try {
                pendingCsv = Base64.decode(base64Data, Base64.DEFAULT)
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/csv"
                    putExtra(Intent.EXTRA_TITLE, fileName)
                }
                runOnUiThread { startActivityForResult(intent, createCsvRequest) }
            } catch (_: Exception) {
                runOnUiThread { webView.evaluateJavascript("window.csvResult(false)", null) }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = getColor(R.color.navy)
        window.navigationBarColor = getColor(R.color.white)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        webView = WebView(this).apply {
            setBackgroundColor(getColor(R.color.app_background))
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            settings.allowFileAccess = true
            settings.allowContentAccess = false
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            addJavascriptInterface(AndroidBridge(), "Android")
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        setContentView(webView)
        webView.loadUrl("file:///android_asset/index.html")
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() { webView.destroy(); super.onDestroy() }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode != createCsvRequest) return
        var success = false
        if (resultCode == RESULT_OK) {
            resultData?.data?.let { uri ->
                try {
                    val output = contentResolver.openOutputStream(uri)
                    if (output != null) {
                        output.use { it.write(pendingCsv ?: byteArrayOf()) }
                        success = true
                    }
                } catch (_: Exception) { success = false }
            }
        }
        pendingCsv = null
        webView.evaluateJavascript("window.csvResult($success)", null)
    }
}
