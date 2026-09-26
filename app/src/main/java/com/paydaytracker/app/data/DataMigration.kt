package com.paydaytracker.app.data

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import com.paydaytracker.app.data.db.WageTrackDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.resume

object DataMigration {

    suspend fun checkAndMigrate(context: Context, repository: WageRepository): Boolean {
        val prefs = context.getSharedPreferences("migration", Context.MODE_PRIVATE)
        if (prefs.getBoolean("migrated_to_native", false)) {
            return false
        }

        // Check if Room database already has records
        val db = WageTrackDatabase.getInstance(context)
        val hasData = withContext(Dispatchers.IO) {
            db.workplaceDao().getAll().isNotEmpty() ||
            db.shiftDao().getAll().isNotEmpty()
        }
        if (hasData) {
            prefs.edit().putBoolean("migrated_to_native", true).apply()
            return false
        }

        // Check for existing pending auto-backup file on disk
        val pendingFile = File(context.filesDir, "backup-pending.json")
        if (pendingFile.exists()) {
            val migrated = withContext(Dispatchers.IO) {
                try {
                    val raw = pendingFile.readText(Charsets.UTF_8)
                    val snapshot = JSONObject(raw)
                    val doc = snapshot.optString("document", "")
                    if (doc.isNotBlank()) {
                        repository.importBackupJson(doc)
                    } else false
                } catch (_: Exception) { false }
            }
            if (migrated) {
                prefs.edit().putBoolean("migrated_to_native", true).apply()
                return true
            }
        }

        // Query WebView localStorage for 'lohnzeit-v1'
        val storageData = withContext(Dispatchers.Main) {
            suspendCancellableCoroutine<String?> { continuation ->
                val webView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            view?.evaluateJavascript("window.localStorage ? window.localStorage.getItem('lohnzeit-v1') : null") { result ->
                                try {
                                    if (result != null && result != "null" && result.length > 5) {
                                        val unquoted = try {
                                            val tokener = org.json.JSONTokener(result).nextValue()
                                            if (tokener is String) tokener else result
                                        } catch (_: Exception) { result }
                                        continuation.resume(unquoted)
                                    } else {
                                        continuation.resume(null)
                                    }
                                } finally {
                                    view?.destroy()
                                }
                            }
                        }
                    }
                }
                continuation.invokeOnCancellation { webView.destroy() }
                webView.loadUrl("file:///android_asset/index.html")
            }
        }

        if (!storageData.isNullOrBlank()) {
            val ok = withContext(Dispatchers.IO) {
                repository.importBackupJson(storageData)
            }
            if (ok) {
                prefs.edit().putBoolean("migrated_to_native", true).apply()
                return true
            }
        }

        prefs.edit().putBoolean("migrated_to_native", true).apply()
        return false
    }
}
