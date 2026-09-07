package com.paydaytracker.app

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

// A native gate covers the WebView before loading and whenever the app loses focus.
class AppLock(private val activity: Activity, root: FrameLayout, private val content: View, private val changed: () -> Unit) {
    private val prefs = activity.getSharedPreferences("device", Context.MODE_PRIVATE)
    private val cover = LinearLayout(activity)
    private val title = TextView(activity)
    private var signal: CancellationSignal? = null
    private var busy = false
    private var authenticated = false
    private var pendingChange: Boolean? = null
    private val enabled get() = prefs.getBoolean("lock", false)
    private fun text(de: String, en: String) = if (prefs.getString("language", "de") == "en") en else de

    init {
        cover.orientation = LinearLayout.VERTICAL; cover.gravity = Gravity.CENTER
        cover.setPadding(32, 32, 32, 32); cover.setBackgroundColor(Color.rgb(13, 30, 43))
        title.setTextColor(Color.WHITE); title.textSize = 22f; cover.addView(title)
        cover.addView(Button(activity).apply { this.text = text("Entsperren", "Unlock"); setOnClickListener { authenticate() } })
        cover.addView(Button(activity).apply { this.text = text("PIN / Passwort verwenden", "Use PIN / password"); setOnClickListener { credential() } })
        root.addView(cover, FrameLayout.LayoutParams(-1, -1))
        refresh()
    }
    private fun refresh() {
        val blocked = (enabled && !authenticated) || pendingChange != null
        cover.visibility = if (blocked) View.VISIBLE else View.GONE
        content.visibility = if (blocked) View.INVISIBLE else View.VISIBLE
        title.text = text("Payday Tracker gesperrt", "Payday Tracker locked")
        if (enabled || pendingChange != null) activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
    fun resume() { refresh(); if (enabled && !authenticated && !busy) authenticate() }
    fun pause() { if (enabled) { authenticated = false; refresh() } }
    fun change(value: Boolean) {
        if (busy) return
        if (!activity.getSystemService(KeyguardManager::class.java).isDeviceSecure) {
            android.widget.Toast.makeText(activity, text("Richte zuerst eine Displaysperre ein.", "Set up a phone screen lock first."), android.widget.Toast.LENGTH_LONG).show(); changed(); return
        }
        pendingChange = value; refresh(); authenticate()
    }
    private fun success() {
        busy = false; authenticated = true
        pendingChange?.let { prefs.edit().putBoolean("lock", it).commit() }
        pendingChange = null; refresh(); PaydayWidget.update(activity); changed()
    }
    private fun failure() { busy = false; pendingChange = null; refresh(); changed() }
    private fun authenticate() {
        if (busy) return
        if (Build.VERSION.SDK_INT < 28) { credential(); return }
        busy = true; signal = CancellationSignal()
        val builder = BiometricPrompt.Builder(activity).setTitle(text("Payday Tracker entsperren", "Unlock Payday Tracker"))
        if (Build.VERSION.SDK_INT >= 30) builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        else if (Build.VERSION.SDK_INT >= 29) builder.setDeviceCredentialAllowed(true)
        else builder.setNegativeButton(text("PIN verwenden", "Use PIN"), activity.mainExecutor) { _, _ -> busy = false; credential() }
        builder.build().authenticate(signal!!, activity.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = success()
            override fun onAuthenticationError(code: Int, message: CharSequence) {
                // On older devices without enrolled biometrics, use the existing screen credential.
                if (code == BiometricPrompt.BIOMETRIC_ERROR_NO_BIOMETRICS || code == BiometricPrompt.BIOMETRIC_ERROR_HW_NOT_PRESENT) { busy = false; credential(); return }
                failure(); title.text = message
            }
        })
    }
    @Suppress("DEPRECATION")
    private fun credential() {
        if (busy) return
        val intent = activity.getSystemService(KeyguardManager::class.java).createConfirmDeviceCredentialIntent("Payday Tracker", text("Zum Fortfahren entsperren", "Unlock to continue"))
        if (intent == null) { failure(); return }
        busy = true; activity.startActivityForResult(intent, 223)
    }
    fun result(code: Int, result: Int): Boolean {
        if (code != 223) return false
        if (result == Activity.RESULT_OK) success() else failure()
        return true
    }
    fun destroy() { signal?.cancel() }
}
