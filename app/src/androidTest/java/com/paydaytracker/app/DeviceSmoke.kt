package com.paydaytracker.app

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

// Runs against the real file:// WebView and Java bridge, not a browser mock.
class DeviceSmoke : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    private lateinit var activity: Activity
    private lateinit var web: WebView
    private fun findWeb(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) findWeb(view.getChildAt(i))?.let { return it }
        return null
    }
    private fun js(code: String): String {
        val latch = CountDownLatch(1); var value = ""
        runOnMainSync { web.evaluateJavascript(code) { value = it; latch.countDown() } }
        check(latch.await(10, TimeUnit.SECONDS)) { "JavaScript timeout: $code" }
        return value
    }
    private fun requireJS(code: String) { check(js(code) == "true") { "Failed: $code; result=${js(code)}" } }
    override fun onStart() {
        try {
            activity = startActivitySync(Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            runOnMainSync { web = findWeb(activity.window.decorView) ?: error("Missing WebView") }
            for (i in 0..40) { if (js("!!document.querySelector('#device')") == "true") break; Thread.sleep(250) }
            requireJS("!!document.querySelector('#device')")
            requireJS("deviceAvailable()")
            requireJS("JSON.parse(Android.deviceSettings()).lock === false")
            js("localStorage.setItem('lohnzeit-language','en');q('#language').value='en';q('#language').dispatchEvent(new Event('change'));q('#headerAddPlace').click()")
            requireJS("q('#planningDialog').open")
            js("q('#placeName').value='Device test job';q('#placeWage').value='22';q('#planningForm').requestSubmit()")
            requireJS("data.workplaces.some(w=>w.name==='Device test job')")
            js("q('#headerManagePlaces').click()")
            requireJS("q('#workplaces').classList.contains('active')")
            js("show('device')")
            requireJS("!q('#pinWidget').disabled && !q('#biometricLock').disabled")
            js("q('#reminderTime').value='19:30';q('#reminderForm').requestSubmit()")
            Thread.sleep(500)
            requireJS("JSON.parse(Android.deviceSettings()).hour===19 && JSON.parse(Android.deviceSettings()).minute===30")
            js("startWorkTimer()")
            Thread.sleep(500)
            check(targetContext.getSharedPreferences("device",0).getString("timerState", "") == "working") { "Widget did not receive timer" }
            js("startWorkBreak()")
            Thread.sleep(500)
            check(targetContext.getSharedPreferences("device",0).getString("timerState", "") == "break") { "Widget did not receive break" }
            // The test device initially has no PIN. Enabling must not silently turn the lock on.
            js("Android.setAppLock(true)")
            Thread.sleep(500)
            requireJS("JSON.parse(Android.deviceSettings()).lock===false")
            uiAutomation.takeScreenshot()?.let { bitmap -> java.io.File(targetContext.getExternalFilesDir(null),"device-smoke.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) } }
            finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "PAYDAY_SMOKE_PASS: real WebView startup, workplace creation, native reminder settings, timer/widget sync and lock enrollment guard\n") })
        } catch (e: Throwable) {
            android.util.Log.e("PaydaySmoke", "Failure", e)
            finish(Activity.RESULT_CANCELED, Bundle().apply { putString("stream", "PAYDAY_SMOKE_FAIL: ${e.stackTraceToString()}\n") })
        }
    }
}
