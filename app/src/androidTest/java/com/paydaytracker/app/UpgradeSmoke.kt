package com.paydaytracker.app

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.paydaytracker.app.data.DataMigration
import com.paydaytracker.app.data.WageRepository
import com.paydaytracker.app.data.db.WageTrackDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.coroutines.resume

/** Exercises the real file:// localStorage origin used by the v2.4.10 APK. */
@RunWith(AndroidJUnit4::class)
class UpgradeSmoke {
    @Test fun legacyOriginMigratesWithoutRunningTheOldApp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, WageTrackDatabase::class.java).build()
        val repo = WageRepository(db)
        val fixture = """{"settings":{"currency":"EUR","wage":19,"country":"DE"},"workplaces":[{"id":"legacy","name":"Legacy job","wage":19}],"shifts":[{"id":"legacy-shift","date":"2026-09-01","start":"10:00","end":"18:00","minutes":450,"breakMin":30,"status":"completed","workplaceId":"legacy"}],"templates":[{"id":"legacy-template","name":"Day","start":"10:00","end":"18:00","breakMin":30}],"profile":{"name":"Migration test","postcode":"36037","email":"test@example.invalid"},"onboardingCompleted":true}"""
        var old: String? = null
        withContext(Dispatchers.Main) {
            withTimeout(10000) { suspendCancellableCoroutine<Unit> { continuation ->
                val web=WebView(context)
                web.settings.javaScriptEnabled=true;web.settings.domStorageEnabled=true
                web.webViewClient=object:WebViewClient(){override fun onPageFinished(view:WebView,url:String?){
                    view.evaluateJavascript("localStorage.getItem('lohnzeit-v1')") { saved ->
                        old=saved
                        view.evaluateJavascript("localStorage.setItem('lohnzeit-v1',${JSONObject.quote(fixture)});localStorage.setItem('lohnzeit-language','en')") {
                            if(continuation.isActive)continuation.resume(Unit);web.destroy()
                        }
                    }
                }}
                web.loadDataWithBaseURL("file:///android_asset/index.html","<html></html>","text/html","UTF-8",null)
            } }
        }
        context.getSharedPreferences("migration",0).edit().putBoolean("migrated_to_native",false).commit()
        try {
            assertTrue(DataMigration.checkAndMigrate(context,repo))
            assertEquals(450,repo.shifts.first().single().minutes)
            assertEquals("Day",repo.shiftTemplates.first().single().name)
            val doc=JSONObject(repo.exportBackupJson()).getJSONObject("data")
            assertEquals("en",doc.getString("language"))
            assertEquals("36037",doc.getJSONObject("profile").getString("postcode"))
            assertEquals("DE",doc.getJSONObject("settings").getString("country"))
        } finally { db.close() }
    }
}
