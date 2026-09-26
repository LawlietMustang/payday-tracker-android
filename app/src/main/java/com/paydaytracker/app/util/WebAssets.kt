package com.paydaytracker.app.util

import android.content.Context
import android.webkit.WebView
import com.paydaytracker.app.BuildConfig

object WebAssets {
    fun prepare(context: Context, web: WebView) {
        val prefs = context.getSharedPreferences("app_meta", Context.MODE_PRIVATE)
        if (prefs.getInt("last_version_code", -1) != BuildConfig.VERSION_CODE) {
            web.clearCache(true)
            // Cache only: localStorage, preferences, PIN and backup permissions are retained.
            prefs.edit().putInt("last_version_code", BuildConfig.VERSION_CODE).apply()
        }
    }
}
