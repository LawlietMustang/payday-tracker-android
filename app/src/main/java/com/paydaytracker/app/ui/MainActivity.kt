package com.paydaytracker.app.ui

import com.paydaytracker.app.R
import com.paydaytracker.app.util.AutoBackup
import com.paydaytracker.app.service.TimerNotificationService
import com.paydaytracker.app.widget.PaydayWidget
import com.paydaytracker.app.auth.GoogleAccount
import com.paydaytracker.app.reminders.PayslipReminder
import com.paydaytracker.app.reminders.ShiftReminders
import com.paydaytracker.app.reminders.ReminderReceiver
import com.paydaytracker.app.reminders.BackupReminder
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
import com.paydaytracker.app.bridge.AndroidBridge
import com.paydaytracker.app.util.BridgeErrors
import com.paydaytracker.app.util.WebAssets
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

open class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var contentRoot: FrameLayout
    private val autoBackup by lazy { AutoBackup.get(this) }
    private var pendingBackup: ByteArray? = null
    private var pendingCsv: ByteArray? = null
    private val createCsvRequest = 901
    private lateinit var appLock: AppLock
    private val googleAccount by lazy { GoogleAccount(this) { code ->
        if (!isDestroyed && ::webView.isInitialized) webView.evaluateJavascript("window.googleAccountChanged && window.googleAccountChanged(" + JSONObject.quote(code) + ")", null)
    } }
    private val devicePrefs by lazy { getSharedPreferences("device", MODE_PRIVATE) }
    private fun nativeChanged() { if (!isDestroyed) webView.evaluateJavascript("window.refreshDeviceSettings && window.refreshDeviceSettings()", null) }

    private fun bridgeFailure(method: String) {
        runOnUiThread { if (!isDestroyed && ::webView.isInitialized) webView.evaluateJavascript(
            "window.nativeBridgeError && window.nativeBridgeError(" + JSONObject.quote(method) + ")", null) }
    }
    private fun runBridgeUi(method: String, action: () -> Unit) {
        runOnUiThread { BridgeErrors.call(method, Unit, ::bridgeFailure, action) }
    }

    inner class NativeActions {
        fun googleAccountState(): String = googleAccount.state()
        fun googleSignIn() { runBridgeUi("googleSignIn") { if (webView.visibility == View.VISIBLE) googleAccount.signIn() } }
        fun googleSignOut() { runBridgeUi("googleSignOut") { if (webView.visibility == View.VISIBLE) googleAccount.signOut() } }
        fun shiftReminderStatus(): String = ShiftReminders.status(this@MainActivity).toString()
        fun syncShiftReminders(json: String) { runBridgeUi("syncShiftReminders") {
            val ok = ShiftReminders.replace(this@MainActivity, json)
            webView.evaluateJavascript("window.shiftReminderSyncResult && window.shiftReminderSyncResult($ok)", null)
        } }
        fun retryShiftReminders() { runBridgeUi("retryShiftReminders") { ShiftReminders.retry(this@MainActivity); nativeChanged() } }
        fun testShiftReminder() { runBridgeUi("testShiftReminder") {
            val ok = ShiftReminders.test(this@MainActivity)
            webView.evaluateJavascript("window.shiftReminderTestResult && window.shiftReminderTestResult($ok)", null)
        } }
        fun requestShiftNotifications() { runBridgeUi("requestShiftNotifications") {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 225)
        } }
        fun syncPayslipReminder(json: String) { runBridgeUi("syncPayslipReminder") { PayslipReminder.update(this@MainActivity, json) } }
        fun consumePayslipReminder(): String {
            val month = intent?.getStringExtra(PayslipReminder.EXTRA_MONTH) ?: ""
            intent?.removeExtra(PayslipReminder.EXTRA_MONTH)
            return month
        }
        fun syncBackupStatus(dirty: Boolean, language: String) { runBridgeUi("syncBackupStatus") { BackupReminder.update(this@MainActivity, dirty, language) } }
        fun autoBackupState(): String = autoBackup.state()
        fun queueAutoBackup(document: String, hash: String) { autoBackup.enqueue(document, hash) }
        fun retryAutoBackup() { autoBackup.retry() }
        fun disableAutoBackup() { runBridgeUi("disableAutoBackup") { if (webView.visibility == View.VISIBLE) autoBackup.disable() } }
        fun chooseBackupFolder() { runBridgeUi("chooseBackupFolder") {
            if (webView.visibility != View.VISIBLE) return@runBridgeUi
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
            }, 905)
        } }
        fun exportBackup(json: String) { runBridgeUi("exportBackup") {
            if (json.toByteArray().size > 10 * 1024 * 1024 || webView.visibility != View.VISIBLE) { webView.evaluateJavascript("window.backupResult(false)", null); return@runBridgeUi }
            pendingBackup = json.toByteArray(Charsets.UTF_8)
            startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); type = "application/json"
                putExtra(Intent.EXTRA_TITLE, "WageTrack-${java.time.LocalDate.now()}.json")
            }, 903)
        } }
        fun importBackup() { runBridgeUi("importBackup") {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"
            }, 904)
        } }
        fun openHome() { runBridgeUi("openHome") { startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)) } }
        fun clearAppData() = runBridgeUi("clearAppData") {
            // Android clears this app's private data and stops its processes/alarms.
            val accepted = getSystemService(android.app.ActivityManager::class.java).clearApplicationUserData()
            if (!accepted) Toast.makeText(this@MainActivity, if (devicePrefs.getString("language", "de") == "en") "Could not delete data. Try again." else "Daten konnten nicht gelöscht werden. Bitte erneut versuchen.", Toast.LENGTH_LONG).show()
        }
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

        fun setAppLock(enabled: Boolean) { runBridgeUi("setAppLock") { appLock.change(enabled) } }

        fun saveReminder(enabled: Boolean, hour: Int, minute: Int, days: Int, language: String) {
            if (hour !in 0..23 || minute !in 0..59 || days !in 0..127 || (enabled && days == 0)) return
            runBridgeUi("saveReminder") {
                devicePrefs.edit().putBoolean("reminder", enabled).putInt("reminderHour", hour).putInt("reminderMinute", minute).putInt("reminderDays", days).putString("language", if (language == "en") "en" else "de").apply()
                ReminderReceiver.schedule(this@MainActivity)
                if (enabled && Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 224)
                nativeChanged()
            }
        }

        fun notificationSettings() { runBridgeUi("notificationSettings") { startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)) } }

        fun testReminder() { runBridgeUi("testReminder") {
            val ok = ReminderReceiver.post(this@MainActivity, true)
            val en = devicePrefs.getString("language", "de") == "en"
            Toast.makeText(this@MainActivity, if (ok) { if (en) "Test sent. Check your notifications." else "Test gesendet. Prüfe deine Benachrichtigungen." } else { if (en) "Notifications are blocked. Allow them in phone settings." else "Benachrichtigungen sind gesperrt. Bitte in den Telefoneinstellungen erlauben." }, Toast.LENGTH_LONG).show()
            if (!ok && Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 224)
            nativeChanged()
        } }

        fun exactReminderSettings() { runBridgeUi("exactReminderSettings") {
            if (Build.VERSION.SDK_INT >= 31) startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, android.net.Uri.parse("package:$packageName")))
        } }

        fun backgroundSettings() { runBridgeUi("backgroundSettings") { startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:$packageName"))) } }

        fun addWidget() { runBridgeUi("addWidget") {
            val manager = AppWidgetManager.getInstance(this@MainActivity)
            val accepted = try { manager.isRequestPinAppWidgetSupported && manager.requestPinAppWidget(ComponentName(this@MainActivity, com.paydaytracker.app.PaydayWidget::class.java), null, null) } catch (e: Exception) { BridgeErrors.report("addWidget", e); false }
            webView.evaluateJavascript("window.widgetPinResult && window.widgetPinResult($accepted)", null)
        } }

        fun syncWidget(state: String, elapsedMs: Double, language: String, progress: String) {
            if (state !in listOf("idle", "working", "break") || !elapsedMs.isFinite() || elapsedMs < 0) return
            devicePrefs.edit().putString("timerState", state).putLong("timerBase", System.currentTimeMillis() - elapsedMs.toLong()).putString("language", if (language == "en") "en" else "de").apply()
            try {
                val doc = JSONObject(progress)
                val minutes = doc.getDouble("minutes"); val target = doc.getDouble("target")
                if (minutes.isFinite() && target.isFinite() && minutes >= 0 && target >= 0 && doc.getString("month").matches(Regex("\\d{4}-\\d{2}")))
                    devicePrefs.edit().putString("widgetProgress", doc.toString()).apply()
            } catch (e: Exception) { BridgeErrors.report("syncWidget", e) }
            PaydayWidget.update(this@MainActivity)
        }
        fun setDarkMode(dark: Boolean) {
            runBridgeUi("setDarkMode") {
                val background = if (dark) Color.rgb(17, 10, 38) else Color.rgb(24, 14, 51)
                window.statusBarColor = background
                window.decorView.setBackgroundColor(background)
                if (::contentRoot.isInitialized) contentRoot.setBackgroundColor(background)
                window.navigationBarColor = background
                window.decorView.systemUiVisibility = 0
            }
        }

        fun updateTimerNotification(state: String, elapsedMs: Double, language: String) {
            runBridgeUi("updateTimerNotification") {
                if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 902)
                }
                val intent = Intent(this@MainActivity, com.paydaytracker.app.TimerNotificationService::class.java).apply {
                    action = TimerNotificationService.ACTION_UPDATE
                    putExtra(TimerNotificationService.EXTRA_STATE, state)
                    putExtra(TimerNotificationService.EXTRA_ELAPSED, elapsedMs.toLong())
                    putExtra(TimerNotificationService.EXTRA_LANGUAGE, language)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
            }
        }

        fun stopTimerNotification() {
            runBridgeUi("stopTimerNotification") {
                startService(Intent(this@MainActivity, com.paydaytracker.app.TimerNotificationService::class.java).apply { action = TimerNotificationService.ACTION_STOP })
            }
        }

        fun exportCsv(base64Data: String, fileName: String) {
            try {
                pendingCsv = Base64.decode(base64Data, Base64.DEFAULT)
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/csv"
                    putExtra(Intent.EXTRA_TITLE, fileName)
                }
                runBridgeUi("exportCsv") { startActivityForResult(intent, createCsvRequest) }
            } catch (e: Exception) {
                BridgeErrors.report("exportCsv", e)
                runBridgeUi("exportCsv") { webView.evaluateJavascript("window.csvResult(false)", null) }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT) { navigateBack() }
        window.statusBarColor = Color.rgb(24, 14, 51)
        window.navigationBarColor = Color.rgb(24, 14, 51)
        window.decorView.systemUiVisibility = 0
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
            addJavascriptInterface(AndroidBridge(NativeActions(), ::bridgeFailure), "Android")
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val root = FrameLayout(this)
        contentRoot = root
        root.setBackgroundColor(Color.rgb(24, 14, 51))
        root.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout() or android.view.WindowInsets.Type.ime())
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                android.view.WindowInsets.CONSUMED
            } else insets
        }
        root.addView(webView, FrameLayout.LayoutParams(-1, -1))
        appLock = AppLock(this, root, webView) { nativeChanged() }
        autoBackup.changed = { if (!isDestroyed && ::webView.isInitialized) webView.evaluateJavascript("window.autoBackupChanged && window.autoBackupChanged()", null) }
        setContentView(root)
        root.requestApplyInsets()
        ReminderReceiver.deliverDue(this)
        ReminderReceiver.schedule(this)
        WebAssets.prepare(this, webView)
        webView.loadUrl("file:///android_asset/web/index.html")
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); setIntent(intent)
        if (::webView.isInitialized) webView.evaluateJavascript("window.openPayslipReminder && window.openPayslipReminder()", null)
    }

    override fun onResume() { super.onResume(); if (::appLock.isInitialized) appLock.resume(); BackupReminder.schedule(this); PayslipReminder.reconcile(this); ReminderReceiver.deliverDue(this); ReminderReceiver.schedule(this); ShiftReminders.reconcile(this); if (::webView.isInitialized) nativeChanged() }
    override fun onPause() { autoBackup.flush(); if (::appLock.isInitialized) appLock.pause(); super.onPause() }
    override fun onDestroy() { autoBackup.changed = null; googleAccount.destroy(); appLock.destroy(); webView.destroy(); super.onDestroy() }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) { super.onRequestPermissionsResult(requestCode, permissions, grantResults); ShiftReminders.reconcile(this); nativeChanged() }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (appLock.result(requestCode, resultCode)) return
        if (requestCode == 905) {
            if (resultCode == RESULT_OK && resultData?.data != null) autoBackup.configure(resultData.data!!, resultData.flags)
            else webView.evaluateJavascript("window.autoBackupChanged && window.autoBackupChanged()", null)
            return
        }
        if (requestCode == 903 || requestCode == 904) {
            if (resultCode != RESULT_OK || resultData?.data == null) { pendingBackup = null; if (requestCode == 903) webView.evaluateJavascript("window.backupResult(null)", null); return }
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
