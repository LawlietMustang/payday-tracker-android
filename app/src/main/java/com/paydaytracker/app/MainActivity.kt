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
    private lateinit var contentRoot: FrameLayout
    private var pendingBackup: ByteArray? = null
    private var pendingCsv: ByteArray? = null
    private val createCsvRequest = 901
    private lateinit var appLock: AppLock
    private val googleAccount by lazy { GoogleAccount(this) { code ->
        if (!isDestroyed && ::webView.isInitialized) webView.evaluateJavascript("window.googleAccountChanged && window.googleAccountChanged(" + JSONObject.quote(code) + ")", null)
    } }
    private val devicePrefs by lazy { getSharedPreferences("device", MODE_PRIVATE) }
    private fun nativeChanged() { if (!isDestroyed) webView.evaluateJavascript("window.refreshDeviceSettings && window.refreshDeviceSettings()", null) }

    inner class AndroidBridge {
        @JavascriptInterface
        fun googleAccountState(): String = googleAccount.state()
        @JavascriptInterface
        fun googleSignIn() { runOnUiThread { if (webView.visibility == View.VISIBLE) googleAccount.signIn() } }
        @JavascriptInterface
        fun googleSignOut() { runOnUiThread { if (webView.visibility == View.VISIBLE) googleAccount.signOut() } }
        @JavascriptInterface
        fun shiftReminderStatus(): String = ShiftReminders.status(this@MainActivity).toString()
        @JavascriptInterface
        fun syncShiftReminders(json: String) { runOnUiThread {
            val ok = ShiftReminders.replace(this@MainActivity, json)
            webView.evaluateJavascript("window.shiftReminderSyncResult && window.shiftReminderSyncResult($ok)", null)
        } }
        @JavascriptInterface
        fun retryShiftReminders() { runOnUiThread { ShiftReminders.retry(this@MainActivity); nativeChanged() } }
        @JavascriptInterface
        fun testShiftReminder() { runOnUiThread {
            val ok = ShiftReminders.test(this@MainActivity)
            webView.evaluateJavascript("window.shiftReminderTestResult && window.shiftReminderTestResult($ok)", null)
        } }
        @JavascriptInterface
        fun requestShiftNotifications() { runOnUiThread {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 225)
        } }
        @JavascriptInterface
        fun exportBackup(json: String) { runOnUiThread {
            if (json.toByteArray().size > 10 * 1024 * 1024) return@runOnUiThread
            pendingBackup = json.toByteArray(Charsets.UTF_8)
            startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); type = "application/json"
                putExtra(Intent.EXTRA_TITLE, "WageTrack-${java.time.LocalDate.now()}.json")
            }, 903)
        } }
        @JavascriptInterface
        fun importBackup() { runOnUiThread {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"
            }, 904)
        } }
        @JavascriptInterface
        fun openHome() { runOnUiThread { startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)) } }
        @JavascriptInterface
        fun clearAppData() = runOnUiThread {
            // Android clears this app's private data and stops its processes/alarms.
            val accepted = getSystemService(android.app.ActivityManager::class.java).clearApplicationUserData()
            if (!accepted) Toast.makeText(this@MainActivity, if (devicePrefs.getString("language", "de") == "en") "Could not delete data. Try again." else "Daten konnten nicht gelöscht werden. Bitte erneut versuchen.", Toast.LENGTH_LONG).show()
        }
        @JavascriptInterface
        fun deviceSettings(): String = JSONObject().apply {
            put("lock", devicePrefs.getBoolean("lock", false))
            put("reminder", devicePrefs.getBoolean("reminder", false))
            put("hour", devicePrefs.getInt("reminderHour", 20))
            put("minute", devicePrefs.getInt("reminderMinute", 0))
            put("days", devicePrefs.getInt("reminderDays", 62))
            put("notifications", ReminderReceiver.notificationsAllowed(this@MainActivity))
            put("nextReminder", devicePrefs.getLong("nextReminder", 0))
            put("exact", Build.VERSION.SDK_INT < 31 || getSystemService(android.app.AlarmManager::class.java).canScheduleExactAlarms())
            put("screenLock", getSystemService(android.app.KeyguardManager::class.java).isDeviceSecure)
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
        fun testReminder() { runOnUiThread {
            val ok = ReminderReceiver.post(this@MainActivity, true)
            val en = devicePrefs.getString("language", "de") == "en"
            Toast.makeText(this@MainActivity, if (ok) { if (en) "Test sent. Check your notifications." else "Test gesendet. Prüfe deine Benachrichtigungen." } else { if (en) "Notifications are blocked. Allow them in phone settings." else "Benachrichtigungen sind gesperrt. Bitte in den Telefoneinstellungen erlauben." }, Toast.LENGTH_LONG).show()
            if (!ok && Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 224)
            nativeChanged()
        } }

        @JavascriptInterface
        fun exactReminderSettings() { runOnUiThread {
            if (Build.VERSION.SDK_INT >= 31) startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, android.net.Uri.parse("package:$packageName")))
        } }

        @JavascriptInterface
        fun backgroundSettings() { runOnUiThread { startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:$packageName"))) } }

        @JavascriptInterface
        fun addWidget() { runOnUiThread {
            val manager = AppWidgetManager.getInstance(this@MainActivity)
            val accepted = try { manager.isRequestPinAppWidgetSupported && manager.requestPinAppWidget(ComponentName(this@MainActivity, PaydayWidget::class.java), null, null) } catch (_: Exception) { false }
            webView.evaluateJavascript("window.widgetPinResult && window.widgetPinResult($accepted)", null)
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
                val background = if (dark) Color.rgb(17, 10, 38) else Color.rgb(24, 14, 51)
                window.statusBarColor = background
                window.decorView.setBackgroundColor(background)
                if (::contentRoot.isInitialized) contentRoot.setBackgroundColor(background)
                window.navigationBarColor = background
                window.decorView.systemUiVisibility = 0
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
        if (Build.VERSION.SDK_INT >= 33) onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT) { navigateBack() }
        window.statusBarColor = Color.rgb(243, 246, 248)
        window.navigationBarColor = getColor(R.color.white)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        if (Build.VERSION.SDK_INT >= 30) window.setDecorFitsSystemWindows(false)
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
            settings.blockNetworkLoads = true
            addJavascriptInterface(AndroidBridge(), "Android")
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val root = FrameLayout(this)
        contentRoot = root
        root.setBackgroundColor(Color.rgb(243, 246, 248))
        root.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout() or android.view.WindowInsets.Type.ime())
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                android.view.WindowInsets.CONSUMED
            } else insets
        }
        root.addView(webView, FrameLayout.LayoutParams(-1, -1))
        appLock = AppLock(this, root, webView) { nativeChanged() }
        setContentView(root)
        root.requestApplyInsets()
        ReminderReceiver.deliverDue(this)
        ReminderReceiver.schedule(this)
        webView.loadUrl("file:///android_asset/index.html")
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        navigateBack()
    }
    private var backPending = false
    private fun navigateBack() {
        if (!::webView.isInitialized || webView.visibility != View.VISIBLE || backPending) return
        backPending = true
        webView.evaluateJavascript("window.handleAppBack ? window.handleAppBack() : true") { handled ->
            backPending = false
            if (handled == "false") moveTaskToBack(true)
        }
    }

    override fun onResume() { super.onResume(); if (::appLock.isInitialized) appLock.resume(); ReminderReceiver.deliverDue(this); ReminderReceiver.schedule(this); ShiftReminders.reconcile(this); if (::webView.isInitialized) nativeChanged() }
    override fun onPause() { if (::appLock.isInitialized) appLock.pause(); super.onPause() }
    override fun onDestroy() { googleAccount.destroy(); appLock.destroy(); webView.destroy(); super.onDestroy() }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) { super.onRequestPermissionsResult(requestCode, permissions, grantResults); ShiftReminders.reconcile(this); nativeChanged() }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (appLock.result(requestCode, resultCode)) return
        if (requestCode == 903 || requestCode == 904) {
            if (resultCode != RESULT_OK || resultData?.data == null) { pendingBackup = null; return }
            val uri = resultData.data!!
            val bytes = pendingBackup
            pendingBackup = null
            Thread {
                try {
                    if (requestCode == 903) {
                        requireNotNull(bytes)
                        requireNotNull(contentResolver.openOutputStream(uri, "wt")).use { it.write(bytes) }
                        runOnUiThread { webView.evaluateJavascript("window.backupResult(true)", null) }
                    } else {
                        val input = requireNotNull(contentResolver.openInputStream(uri))
                        val payload = input.use { stream ->
                            val output = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            while (true) {
                                val count = stream.read(buffer)
                                if (count < 0) break
                                require(output.size() + count <= 10 * 1024 * 1024)
                                output.write(buffer, 0, count)
                            }
                            output.toByteArray()
                        }
                        require(payload.size <= 10 * 1024 * 1024)
                        val json = JSONObject.quote(String(payload, Charsets.UTF_8))
                        runOnUiThread { webView.evaluateJavascript("window.receiveBackup($json)", null) }
                    }
                } catch (_: Exception) { runOnUiThread { webView.evaluateJavascript("window.backupResult(false)", null) } }
            }.start()
            return
        }
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
