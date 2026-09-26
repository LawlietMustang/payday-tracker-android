package com.paydaytracker.app.util

import android.util.Log

object BridgeErrors {
    fun report(method: String, error: Exception) {
        // Do not log arguments, tokens, PINs, backup contents or exception messages.
        Log.e("WageTrackBridge", "$method failed (${error.javaClass.simpleName})")
    }
    fun <T> call(method: String, fallback: T, failed: (String) -> Unit, action: () -> T): T = try {
        action()
    } catch (error: Exception) {
        report(method, error)
        failed(method)
        fallback
    }
}
