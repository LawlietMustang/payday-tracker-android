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
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.ScrollView
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable

// A native gate covers the WebView before loading and whenever the app loses focus.
class AppLock(private val activity: Activity, root: FrameLayout, private val content: View, private val changed: () -> Unit) {
    private val prefs = activity.getSharedPreferences("device", Context.MODE_PRIVATE)
    private val cover = LinearLayout(activity)
    private val title = TextView(activity)
    private val unlock = ImageButton(activity)
    private val maskedName = TextView(activity)
    private val status = TextView(activity)
    private val scroll = ScrollView(activity)
    private val pin = Button(activity)
    private var signal: CancellationSignal? = null
    private var busy = false
    private var authenticated = false
    private var pendingChange: Boolean? = null
    private val enabled get() = prefs.getBoolean("lock", false)
    private fun text(de: String, en: String) = if (prefs.getString("language", "de") == "en") en else de

    private fun dp(value: Int) = (value * activity.resources.displayMetrics.density).toInt()
    private fun shape(color: String, radius: Int, border: String? = null) = GradientDrawable().apply {
        setColor(Color.parseColor(color)); cornerRadius = dp(radius).toFloat()
        border?.let { setStroke(dp(1), Color.parseColor(it)) }
    }
    private fun spacing(width: Int, height: Int, top: Int = 0) = LinearLayout.LayoutParams(
        if (width < 0) width else dp(width), if (height < 0) height else dp(height)
    ).apply { topMargin = dp(top); gravity = Gravity.CENTER_HORIZONTAL }
    init {
        scroll.tag = "wagetrack-lock"; scroll.isFillViewport = true
        scroll.setBackgroundColor(Color.parseColor("#180E33"))
        cover.orientation = LinearLayout.VERTICAL; cover.gravity = Gravity.CENTER
        cover.setPadding(dp(24), dp(32), dp(24), dp(32))
        val logo = ImageView(activity).apply {
            setImageResource(R.drawable.app_icon); contentDescription = "WageTrack"
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        cover.addView(logo, spacing(96, 96))
        title.tag = "lock-greeting"; title.setTextColor(Color.parseColor("#F3ECFF"))
        title.textSize = 26f; title.gravity = Gravity.CENTER; title.setTypeface(null, Typeface.BOLD)
        cover.addView(title, spacing(-1, -2, 24))
        // A constant mask reveals neither the user's name nor its length before authentication.
        maskedName.tag = "lock-masked-name"; maskedName.text = "••••••"
        maskedName.setTextColor(Color.parseColor("#B7A6DE")); maskedName.textSize = 23f
        maskedName.gravity = Gravity.CENTER; maskedName.letterSpacing = .2f
        cover.addView(maskedName, spacing(-1, -2, 8))
        unlock.tag = "lock-fingerprint"; unlock.setImageResource(R.drawable.lock_fingerprint)
        unlock.background = shape("#CFFF3D", 26); unlock.setPadding(dp(19), dp(19), dp(19), dp(19))
        unlock.setOnClickListener { authenticate() }; cover.addView(unlock, spacing(80, 80, 36))
        pin.tag = "lock-pin"; pin.isAllCaps = false; pin.textSize = 15f
        pin.setTextColor(Color.parseColor("#F3ECFF")); pin.background = shape("#2E1A63", 16, "#59437C")
        pin.setPadding(dp(20), dp(12), dp(20), dp(12))
        pin.setOnClickListener { credential() }; cover.addView(pin, spacing(-2, -2, 20))
        status.setTextColor(Color.parseColor("#B7A6DE")); status.textSize = 13f
        status.gravity = Gravity.CENTER; status.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        cover.addView(status, spacing(-1, -2, 16))
        scroll.addView(cover, ScrollView.LayoutParams(-1, -1))
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        refresh()
    }
    private fun refresh() {
        val blocked = (enabled && !authenticated) || pendingChange != null
        scroll.visibility = if (blocked) View.VISIBLE else View.GONE
        content.visibility = if (blocked) View.INVISIBLE else View.VISIBLE
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        title.text = when {
            hour < 12 -> text("Guten Morgen", "Good Morning")
            hour < 18 -> text("Guten Tag", "Good Afternoon")
            else -> text("Guten Abend", "Good Evening")
        }
        maskedName.contentDescription = text("Name bis zum Entsperren verborgen", "Name hidden until unlocked")
        unlock.contentDescription = text("Mit Fingerabdruck entsperren", "Unlock with fingerprint")
        pin.text = text("Stattdessen PIN verwenden", "Use PIN instead")
        status.text = text("Entsperren, um fortzufahren", "Unlock to continue")
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
        val builder = BiometricPrompt.Builder(activity).setTitle(text("WageTrack entsperren", "Unlock WageTrack"))
        if (Build.VERSION.SDK_INT >= 30) builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        else if (Build.VERSION.SDK_INT >= 29) builder.setDeviceCredentialAllowed(true)
        else builder.setNegativeButton(text("PIN verwenden", "Use PIN"), activity.mainExecutor) { _, _ -> busy = false; credential() }
        builder.build().authenticate(signal!!, activity.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = success()
            override fun onAuthenticationError(code: Int, message: CharSequence) {
                // On older devices without enrolled biometrics, use the existing screen credential.
                if (code == BiometricPrompt.BIOMETRIC_ERROR_NO_BIOMETRICS || code == BiometricPrompt.BIOMETRIC_ERROR_HW_NOT_PRESENT) { busy = false; credential(); return }
                failure(); status.text = message
            }
        })
    }
    @Suppress("DEPRECATION")
    private fun credential() {
        if (busy) return
        val intent = activity.getSystemService(KeyguardManager::class.java).createConfirmDeviceCredentialIntent("WageTrack", text("Zum Fortfahren entsperren", "Unlock to continue"))
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
