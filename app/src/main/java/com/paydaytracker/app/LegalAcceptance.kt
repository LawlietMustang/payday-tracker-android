package com.paydaytracker.app

import android.content.Context

/** Installation-local acknowledgement. Never put this record in the user document or a backup. */
object LegalAcceptance {
    const val PREFS = "legal-acceptance"
    const val TERMS_VERSION = "2026-10-08-draft1"
    const val PRIVACY_VERSION = "2026-10-08-draft1"

    fun isAccepted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString("termsVersion", null) == TERMS_VERSION &&
            prefs.getString("privacyVersion", null) == PRIVACY_VERSION &&
            prefs.getLong("acceptedAt", 0) > 0
    }

    fun accept(context: Context, language: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("termsVersion", TERMS_VERSION)
            .putString("privacyVersion", PRIVACY_VERSION)
            .putString("language", language)
            .putLong("acceptedAt", System.currentTimeMillis())
            .commit()
}
