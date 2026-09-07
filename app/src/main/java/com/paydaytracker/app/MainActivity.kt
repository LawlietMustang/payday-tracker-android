package com.paydaytracker.app

import android.annotation.SuppressLint
import android.app.Activity
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.FrameLayout
import android.widget.Toast
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var pendingCsv: ByteArray? = null
    private val createCsvRequest = 901
    private lateinit var appLock: AppLock
    private val devicePrefs by lazy { getSharedPreferences("device", MODE_PRIVATE) }
    private fun nativeChanged() { if (!isDestroyed) webView.evaluateJavascript("window.refreshDeviceSettings && window.refreshDeviceSettings()", null) }

    inner class AndroidBridge {
        @JavascriptInterface
        fun deviceSettings(): String = JSONObject().apply {
            put("lock", devicePrefs.getBoolean("lock", false))
            put("reminder", devicePrefs.getBoolean("reminder", false))
            put("hour", devicePrefs.getInt("reminderHour", 20))
            put("minute", devicePrefs.getInt("reminderMinute", 0))
            put("days", devicePrefs.getInt("reminderDays", 62))
            put("notifications", getSystemService(NotificationManager::class.java).areNotificationsEnabled())
        }.toString()

        @JavascriptInterface
        fun setAppLock(enabled: Boolean) { runOnUiThread { appLock.change(enabled) } }

        @JavascriptInterface
        fun saveReminder(enabled: Boolean, hour: Int, minute: Int, days: Int, language: String) {
            if (hour !in 0..23 || minute !in 0..59 || days !in 0..127 || (enabled && days == 0)) return
            runOnUiThread {
                devicePrefs.edit().putBoolean("reminder", enabled).putInt("reminderHour", hour).putInt("reminderMinute", minute).putInt("reminderDays", days).putString("language", if (language == "en") "en" else "de").apply()
                ReminderReceiver.schedule(this@MainActivity)
                if (enabled && Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 224)
                nativeChanged()
            }
        }

        @JavascriptInterface
        fun notificationSettings() { runOnUiThread { startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)) } }

        @JavascriptInterface
        fun addWidget() { runOnUiThread {
            val manager = AppWidgetManager.getInstance(this@MainActivity)
            if (manager.isRequestPinAppWidgetSupported) manager.requestPinAppWidget(ComponentName(this@MainActivity, PaydayWidget::class.java), null, null)
            else Toast.makeText(this@MainActivity, if (devicePrefs.getString("language", "de") == "en") "Long-press your home screen and choose Widgets → Payday Tracker." else "Startbildschirm gedrückt halten und Widgets → Payday Tracker wählen.", Toast.LENGTH_LONG).show()
        } }

        @JavascriptInterface
        fun syncWidget(state: String, elapsedMs: Double, language: String) {
            if (state !in listOf("idle", "working", "break") || !elapsedMs.isFinite() || elapsedMs < 0) return
            devicePrefs.edit().putString("timerState", state).putLong("timerBase", System.currentTimeMillis() - elapsedMs.toLong()).putString("language", if (language == "en") "en" else "de").apply()
            PaydayWidget.update(this@MainActivity)
        }
        @JavascriptInterface
        fun setDarkMode(dark: Boolean) {
            runOnUiThread {
                window.statusBarColor = if (dark) Color.rgb(7, 23, 35) else getColor(R.color.navy)
                window.navigationBarColor = if (dark) Color.rgb(16, 24, 32) else getColor(R.color.white)
                window.decorView.systemUiVisibility = if (dark) 0 else View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }

        @JavascriptInterface
        fun updateTimerNotification(state: String, elapsedMs: Double, language: String) {
            runOnUiThread {
                if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 902)
                }
                val intent = Intent(this@MainActivity, TimerNotificationService::class.java).apply {
                    action = TimerNotificationService.ACTION_UPDATE
                    putExtra(TimerNotificationService.EXTRA_STATE, state)
                    putExtra(TimerNotificationService.EXTRA_ELAPSED, elapsedMs.toLong())
                    putExtra(TimerNotificationService.EXTRA_LANGUAGE, language)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
            }
        }

        @JavascriptInterface
        fun stopTimerNotification() {
            runOnUiThread {
                startService(Intent(this@MainActivity, TimerNotificationService::class.java).apply { action = TimerNotificationService.ACTION_STOP })
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
        val root = FrameLayout(this)
        root.addView(webView, FrameLayout.LayoutParams(-1, -1))
        appLock = AppLock(this, root, webView) { nativeChanged() }
        setContentView(root)
        ReminderReceiver.schedule(this)
        webView.loadUrl("file:///android_asset/index.html")
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onResume() { super.onResume(); if (::appLock.isInitialized) appLock.resume(); if (::webView.isInitialized) nativeChanged() }
    override fun onPause() { if (::appLock.isInitialized) appLock.pause(); super.onPause() }
    override fun onDestroy() { appLock.destroy(); webView.destroy(); super.onDestroy() }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) { super.onRequestPermissionsResult(requestCode, permissions, grantResults); nativeChanged() }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (appLock.result(requestCode, resultCode)) return
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
