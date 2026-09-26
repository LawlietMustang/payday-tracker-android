package com.paydaytracker.app.bridge

import android.webkit.JavascriptInterface
import com.paydaytracker.app.MainActivity
import com.paydaytracker.app.util.BridgeErrors
import org.json.JSONArray
import org.json.JSONObject

// Only this facade is exposed to JavaScript. UI work has a second guard on the UI thread.
class AndroidBridge(private val actions: MainActivity.NativeActions, private val failed: (String) -> Unit) {
    @JavascriptInterface
    fun googleAccountState(): String = BridgeErrors.call("googleAccountState", "{\"error\":\"native_failure\"}", failed) { actions.googleAccountState() }

    @JavascriptInterface
    fun googleSignIn() = BridgeErrors.call("googleSignIn", Unit, failed) { actions.googleSignIn() }

    @JavascriptInterface
    fun googleSignOut() = BridgeErrors.call("googleSignOut", Unit, failed) { actions.googleSignOut() }

    @JavascriptInterface
    fun shiftReminderStatus(): String = BridgeErrors.call("shiftReminderStatus", "{\"error\":\"native_failure\"}", failed) { actions.shiftReminderStatus() }

    @JavascriptInterface
    fun syncShiftReminders(json: String) = BridgeErrors.call("syncShiftReminders", Unit, failed) { actions.syncShiftReminders(json) }

    @JavascriptInterface
    fun retryShiftReminders() = BridgeErrors.call("retryShiftReminders", Unit, failed) { actions.retryShiftReminders() }

    @JavascriptInterface
    fun testShiftReminder() = BridgeErrors.call("testShiftReminder", Unit, failed) { actions.testShiftReminder() }

    @JavascriptInterface
    fun requestShiftNotifications() = BridgeErrors.call("requestShiftNotifications", Unit, failed) { actions.requestShiftNotifications() }

    @JavascriptInterface
    fun syncPayslipReminder(json: String) = BridgeErrors.call("syncPayslipReminder", Unit, failed) { actions.syncPayslipReminder(json) }

    @JavascriptInterface
    fun consumePayslipReminder(): String = BridgeErrors.call("consumePayslipReminder", "", failed) { actions.consumePayslipReminder() }

    @JavascriptInterface
    fun syncBackupStatus(dirty: Boolean, language: String) = BridgeErrors.call("syncBackupStatus", Unit, failed) { actions.syncBackupStatus(dirty, language) }

    @JavascriptInterface
    fun autoBackupState(): String = BridgeErrors.call("autoBackupState", "{\"error\":\"native_failure\"}", failed) { actions.autoBackupState() }

    @JavascriptInterface
    fun queueAutoBackup(document: String, hash: String) = BridgeErrors.call("queueAutoBackup", Unit, failed) { actions.queueAutoBackup(document, hash) }

    @JavascriptInterface
    fun retryAutoBackup() = BridgeErrors.call("retryAutoBackup", Unit, failed) { actions.retryAutoBackup() }

    @JavascriptInterface
    fun disableAutoBackup() = BridgeErrors.call("disableAutoBackup", Unit, failed) { actions.disableAutoBackup() }

    @JavascriptInterface
    fun chooseBackupFolder() = BridgeErrors.call("chooseBackupFolder", Unit, failed) { actions.chooseBackupFolder() }

    @JavascriptInterface
    fun exportBackup(json: String) = BridgeErrors.call("exportBackup", Unit, failed) { actions.exportBackup(json) }

    @JavascriptInterface
    fun importBackup() = BridgeErrors.call("importBackup", Unit, failed) { actions.importBackup() }

    @JavascriptInterface
    fun openHome() = BridgeErrors.call("openHome", Unit, failed) { actions.openHome() }

    @JavascriptInterface
    fun clearAppData() = BridgeErrors.call("clearAppData", Unit, failed) { actions.clearAppData() }

    @JavascriptInterface
    fun deviceSettings(): String = BridgeErrors.call("deviceSettings", "{\"error\":\"native_failure\"}", failed) { actions.deviceSettings() }

    @JavascriptInterface
    fun setAppLock(enabled: Boolean) = BridgeErrors.call("setAppLock", Unit, failed) { actions.setAppLock(enabled) }

    @JavascriptInterface
    fun saveReminder(enabled: Boolean, hour: Int, minute: Int, days: Int, language: String) = BridgeErrors.call("saveReminder", Unit, failed) { actions.saveReminder(enabled, hour, minute, days, language) }

    @JavascriptInterface
    fun notificationSettings() = BridgeErrors.call("notificationSettings", Unit, failed) { actions.notificationSettings() }

    @JavascriptInterface
    fun testReminder() = BridgeErrors.call("testReminder", Unit, failed) { actions.testReminder() }

    @JavascriptInterface
    fun exactReminderSettings() = BridgeErrors.call("exactReminderSettings", Unit, failed) { actions.exactReminderSettings() }

    @JavascriptInterface
    fun backgroundSettings() = BridgeErrors.call("backgroundSettings", Unit, failed) { actions.backgroundSettings() }

    @JavascriptInterface
    fun addWidget() = BridgeErrors.call("addWidget", Unit, failed) { actions.addWidget() }

    @JavascriptInterface
    fun syncWidget(state: String, elapsedMs: Double, language: String, progress: String) = BridgeErrors.call("syncWidget", Unit, failed) { actions.syncWidget(state, elapsedMs, language, progress) }

    @JavascriptInterface
    fun setDarkMode(dark: Boolean) = BridgeErrors.call("setDarkMode", Unit, failed) { actions.setDarkMode(dark) }

    @JavascriptInterface
    fun updateTimerNotification(state: String, elapsedMs: Double, language: String) = BridgeErrors.call("updateTimerNotification", Unit, failed) { actions.updateTimerNotification(state, elapsedMs, language) }

    @JavascriptInterface
    fun stopTimerNotification() = BridgeErrors.call("stopTimerNotification", Unit, failed) { actions.stopTimerNotification() }

    @JavascriptInterface
    fun exportCsv(base64Data: String, fileName: String) = BridgeErrors.call("exportCsv", Unit, failed) { actions.exportCsv(base64Data, fileName) }

    // Read-only contract inspection: never call mutating methods with dummy data.
    @JavascriptInterface
    fun bridgeContract(): String = JSONArray(javaClass.declaredMethods.filter {
        it.isAnnotationPresent(JavascriptInterface::class.java)
    }.sortedBy { it.name }.map { method ->
        JSONObject().put("name", method.name).put("arguments", JSONArray(method.parameterTypes.map { it.simpleName }))
            .put("returns", method.returnType.simpleName)
    }).toString()
}
