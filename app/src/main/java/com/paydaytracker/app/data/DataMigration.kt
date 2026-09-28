package com.paydaytracker.app.data

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import com.paydaytracker.app.data.db.WageTrackDatabase
import kotlinx.coroutines.*
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** One-time localStorage reader. No web screen or legacy application script is executed. */
object DataMigration {
    suspend fun checkAndMigrate(context: Context, repository: WageRepository): Boolean {
        val prefs = context.getSharedPreferences("migration", 0)
        if (prefs.getBoolean("migrated_to_native", false) && repository.document().length() > 0) return false
        val hasData = repository.hasRecords()
        val raw = withContext(Dispatchers.Main) {
            withTimeout(15000) {
                suspendCancellableCoroutine<String?> { continuation ->
                    val web = WebView(context)
                    web.settings.javaScriptEnabled = true
                    web.settings.domStorageEnabled = true
                    web.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
                            view.evaluateJavascript("JSON.stringify({data:localStorage.getItem('lohnzeit-v1'),language:localStorage.getItem('lohnzeit-language')})") { value ->
                                if (continuation.isActive) {
                                    try {
                                        val payload = JSONObject(JSONTokener(value).nextValue() as String)
                                        val content = payload.optString("data")
                                        if (content.isNotBlank() && content != "null") {
                                            val doc = JSONObject(content)
                                            doc.put("language", payload.optString("language", "de").takeIf { it == "en" } ?: "de")
                                            continuation.resume(doc.toString())
                                        } else continuation.resume(null)
                                    } catch (e: Exception) { continuation.resumeWithException(e) }
                                }
                                web.destroy()
                            }
                        }
                    }
                    continuation.invokeOnCancellation { android.os.Handler(android.os.Looper.getMainLooper()).post { web.destroy() } }
                    web.loadDataWithBaseURL("file:///android_asset/index.html", "<html><body></body></html>", "text/html", "UTF-8", null)
                }
            }
        }
        val fallback = withContext(Dispatchers.IO) {
            runCatching { JSONObject(File(context.filesDir, "backup-pending.json").readText()).optString("document") }.getOrNull()
        }
        val source = raw ?: fallback?.takeIf { it.isNotBlank() }
        if (source != null) {
            withContext(Dispatchers.IO) { File(context.filesDir, "pre-native-migration.json").writeText(source) }
            if (hasData) repository.recoverLegacyExtras(source) else check(repository.importBackupJson(source)) { "Saved data could not be imported. Your original data is retained." }
            repository.updateDocument { if (!it.has("onboardingCompleted")) it.put("onboardingCompleted", true) }
        }
        if (source == null) repository.updateDocument { it.put("nativeInitialized", true) }
        prefs.edit().putBoolean("migrated_to_native", true).apply()
        return source != null
    }
}
