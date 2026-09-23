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
    private val appPin = AppPin(activity)
    private var pinDialog: android.app.Dialog? = null
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
        scroll.addView(cover, FrameLayout.LayoutParams(-1, -1))
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
    fun pause() { pinDialog?.dismiss(); if (enabled) { authenticated = false; refresh() } }
    fun change(value: Boolean) {
        if (busy) return
        pendingChange = value; refresh()
        if (value && !enabled && !appPin.configured) setupPin() else authenticate()
    }
    private fun setupPin() = showPinScreen(true)
    private fun enterPin() { if (!busy) showPinScreen(false) }
    private fun showPinScreen(creating: Boolean) {
        busy = true
        var first = ""
        var digits = ""
        var confirming = false
        var finished = false
        val dialog = android.app.Dialog(activity, android.R.style.Theme_Material_NoActionBar)
        pinDialog = dialog
        val page = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(20), dp(24), dp(20))
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Color.parseColor("#241452"),Color.parseColor("#180E33")))
        }
        val screen = ScrollView(activity).apply { isFillViewport = true; addView(page, FrameLayout.LayoutParams(-1,-2)) }
        fun label(size: Float, color: String = "#F3ECFF", bold: Boolean = false) = TextView(activity).apply {
            textSize=size; setTextColor(Color.parseColor(color)); gravity=Gravity.CENTER
            if (bold) setTypeface(null,Typeface.BOLD)
        }
        val top = LinearLayout(activity).apply { gravity=Gravity.CENTER_VERTICAL }
        val back = Button(activity).apply {
            tag="pin-back"; text="‹"; textSize=28f; isAllCaps=false; setTextColor(Color.parseColor("#F3ECFF")); background=shape("#2E1A63",12,"#59437C"); minWidth=0; minimumWidth=0; setPadding(0,0,0,0)
            contentDescription=text("Zurück","Back")
        }
        top.addView(back,spacing(44,44))
        val heading=label(17f,bold=true).apply { text=text(if(creating) "PIN einrichten" else "WageTrack entsperren",if(creating) "Set up a PIN" else "Unlock WageTrack"); gravity=Gravity.START; setPadding(dp(14),0,0,0) }
        top.addView(heading,LinearLayout.LayoutParams(0,-2,1f));page.addView(top,spacing(-1,-2))
        val mark=ImageView(activity).apply { setImageResource(R.drawable.pin_lock); background=shape("#CFFF3D",16);setPadding(dp(14),dp(14),dp(14),dp(14));importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO }
        page.addView(mark,spacing(56,56,24))
        val step=label(11f,"#B7A6DE",true).apply { tag="pin-step" };page.addView(step,spacing(-1,-2,16))
        val pinTitle=label(23f,bold=true).apply { tag="pin-title"; accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE };page.addView(pinTitle,spacing(-1,-2,8))
        val subtitle=label(13f,"#B7A6DE").apply { setLineSpacing(dp(3).toFloat(),1f) };page.addView(subtitle,spacing(-1,-2,10))
        val dots=LinearLayout(activity).apply { gravity=Gravity.CENTER;tag="pin-dots";importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_YES }
        val dotViews=(0..5).map { View(activity).also { v -> dots.addView(v,spacing(16,16).apply { leftMargin=dp(7);rightMargin=dp(7) }) } }
        page.addView(dots,spacing(-1,24,24))
        val hint=label(12f,"#B7A6DE",true).apply { tag="pin-hint";accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE;minHeight=dp(40) };page.addView(hint,spacing(-1,-2,4))
        // Flexible space shrinks before the keypad; compact phones can still scroll safely.
        page.addView(View(activity),LinearLayout.LayoutParams(1,0,1f))
        fun draw(error: Boolean=false) {
            step.text=if(creating) text(if(confirming) "SCHRITT 2 VON 2" else "SCHRITT 1 VON 2",if(confirming) "STEP 2 OF 2" else "STEP 1 OF 2") else ""
            pinTitle.text=if(!creating) text("App-PIN eingeben","Enter your app PIN") else if(confirming) text("PIN bestätigen","Confirm your PIN") else text("6-stellige PIN erstellen","Create a 6-digit PIN")
            subtitle.text=if(!creating) text("Entsperre WageTrack mit deiner 6-stelligen PIN.","Unlock WageTrack with your 6-digit PIN.") else if(confirming) text("Gib dieselbe PIN noch einmal ein.","Enter the same PIN again.") else text("Nutze diese PIN, wenn Fingerabdruck oder Gesichtserkennung nicht verfügbar sind.","Use this PIN when fingerprint or face unlock isn't available.")
            hint.setTextColor(Color.parseColor(if(error) "#FF4F8B" else "#B7A6DE"))
            if(!error) hint.text=text(if(confirming) "PIN BESTÄTIGEN" else "PIN EINGEBEN",if(confirming) "CONFIRM PIN" else "ENTER PIN")
            dotViews.forEachIndexed { i,v -> v.background=shape(if(i<digits.length) "#CFFF3D" else "#241452",8,if(i<digits.length) "#CFFF3D" else "#B7A6DE") }
            dots.contentDescription="${digits.length} "+text("von 6 Ziffern eingegeben","of 6 digits entered")
        }
        fun cancel() { digits="";first="";finished=true;dialog.dismiss();pinDialog=null;failure() }
        fun goBack() { if(creating&&confirming){first="";digits="";confirming=false;draw()}else cancel() }
        fun digit(value: String) {
            if(digits.length>=6)return
            if(!creating&&appPin.waitSeconds>0){hint.text=text("Bitte warten: ","Please wait: ")+appPin.waitSeconds+" s";draw(true);return}
            digits+=value;draw()
            if(digits.length!=6)return
            if(creating&&!confirming){first=digits;digits="";confirming=true;draw();return}
            if(creating&&digits!=first){digits="";hint.text=text("PINs stimmen nicht überein. Erneut versuchen.","PINs don't match. Try again.");draw(true);return}
            if(!creating&&!appPin.verify(digits)){digits="";hint.text=text("Falsche PIN. Erneut versuchen.","Incorrect PIN. Try again.");draw(true);return}
            try {
                if(creating)appPin.create(digits)
                digits="";first="";finished=true;dialog.dismiss();pinDialog=null;success()
            } catch (_: Exception) { digits="";first="";confirming=false;hint.text=text("PIN konnte nicht gespeichert werden. Erneut versuchen.","Could not save PIN. Try again.");draw(true) }
        }
        val keypad=LinearLayout(activity).apply { orientation=LinearLayout.VERTICAL }
        for(rowIndex in 0..3){
            val row=LinearLayout(activity)
            for(column in 0..2){
                val value=if(rowIndex<3)(rowIndex*3+column+1).toString() else listOf("","0","⌫")[column]
                val key=Button(activity).apply {
                    tag=if(value=="⌫") "pin-delete" else "pin-key-$value";text=value;textSize=24f;isAllCaps=false;setTypeface(null,Typeface.BOLD);setTextColor(Color.parseColor("#F3ECFF"));setPadding(0,0,0,0);minWidth=0;minimumWidth=0
                    background=if(value=="⌫"||value.isEmpty())shape("#180E33",18) else shape("#2E1A63",18,"#59437C")
                    if(value.isEmpty()){visibility=View.INVISIBLE;isEnabled=false}
                    if(value=="⌫"){contentDescription=text("Letzte Ziffer löschen","Delete last digit");setOnClickListener { digits=digits.dropLast(1);draw() }}
                    else setOnClickListener { digit(value) }
                }
                row.addView(key,LinearLayout.LayoutParams(0,dp(58),1f).apply {setMargins(dp(5),dp(5),dp(5),dp(5))})
            }
            keypad.addView(row,spacing(-1,-2))
        }
        page.addView(keypad,spacing(-1,-2,8))
        if(!creating){
            val biometric=Button(activity).apply {text=text("Fingerabdruck verwenden","Use fingerprint instead");isAllCaps=false;setTextColor(Color.parseColor("#CFFF3D"));background=shape("#180E33",12);setOnClickListener {digits="";first="";finished=true;dialog.dismiss();pinDialog=null;busy=false;authenticate()}}
            page.addView(biometric,spacing(-1,48,10))
        }
        back.setOnClickListener { goBack() }
        dialog.setContentView(screen)
        dialog.setOnCancelListener { cancel() }
        dialog.setOnDismissListener {digits="";first="";if(!finished){pinDialog=null;failure()}}
        dialog.setOnKeyListener { _,code,event ->
            if(code==android.view.KeyEvent.KEYCODE_BACK){if(event.action==android.view.KeyEvent.ACTION_UP)goBack();true}
            else if(event.action==android.view.KeyEvent.ACTION_UP&&code in android.view.KeyEvent.KEYCODE_0..android.view.KeyEvent.KEYCODE_9){digit((code-android.view.KeyEvent.KEYCODE_0).toString());true}
            else false
        }
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        dialog.show()
        dialog.window?.apply {setLayout(-1,-1);statusBarColor=Color.parseColor("#241452");navigationBarColor=Color.parseColor("#180E33");decorView.systemUiVisibility=0}
        draw()
    }
    private fun success() {
        busy = false
        if (!appPin.configured && pendingChange != false && (enabled || pendingChange == true)) { setupPin(); return }
        authenticated = true
        pendingChange?.let { prefs.edit().putBoolean("lock", it).commit() }
        pendingChange = null; refresh(); PaydayWidget.update(activity); changed()
    }
    private fun failure() { busy = false; pendingChange = null; refresh(); changed() }
    private fun authenticate() {
        if (busy) return
        if (Build.VERSION.SDK_INT < 28) { credential(); return }
        busy = true; signal = CancellationSignal()
        val builder = BiometricPrompt.Builder(activity).setTitle(text("WageTrack entsperren", "Unlock WageTrack"))
        if (Build.VERSION.SDK_INT >= 30) builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        builder.setNegativeButton(text("PIN verwenden", "Use PIN"), activity.mainExecutor) { _, _ -> busy = false; credential() }
        builder.build().authenticate(signal!!, activity.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = success()
            override fun onAuthenticationError(code: Int, message: CharSequence) {
                if (pinDialog?.isShowing == true) return
                // On older devices without enrolled biometrics, use the existing screen credential.
                if (code == BiometricPrompt.BIOMETRIC_ERROR_NO_BIOMETRICS || code == BiometricPrompt.BIOMETRIC_ERROR_HW_NOT_PRESENT) { busy = false; credential(); return }
                failure(); status.text = message
            }
        })
    }
    @Suppress("DEPRECATION")
    private fun credential() {
        if (busy) return
        if (appPin.configured) { enterPin(); return }
        val intent = activity.getSystemService(KeyguardManager::class.java).createConfirmDeviceCredentialIntent("WageTrack", text("Zum Fortfahren entsperren", "Unlock to continue"))
        if (intent == null) { failure(); return }
        busy = true; activity.startActivityForResult(intent, 223)
    }
    fun result(code: Int, result: Int): Boolean {
        if (code != 223) return false
        if (result == Activity.RESULT_OK) success() else failure()
        return true
    }
    fun destroy() { signal?.cancel(); pinDialog?.dismiss() }
}
