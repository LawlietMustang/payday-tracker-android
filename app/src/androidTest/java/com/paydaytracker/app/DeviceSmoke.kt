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
import android.os.SystemClock
import android.view.MotionEvent
import org.json.JSONArray

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
    private fun shell(command: String): String = android.os.ParcelFileDescriptor.AutoCloseInputStream(uiAutomation.executeShellCommand(command)).bufferedReader().use { it.readText() }
    private fun tap(selector: String) {
        js("document.querySelector('$selector').scrollIntoView({block:'center'})")
        Thread.sleep(300)
        val point=JSONArray(js("(()=>{let r=document.querySelector('$selector').getBoundingClientRect();return [(r.x+r.width/2)*devicePixelRatio,(r.y+r.height/2)*devicePixelRatio]})()"))
        val offset=IntArray(2); runOnMainSync { web.getLocationOnScreen(offset) }
        val x=point.getDouble(0).toFloat()+offset[0];val y=point.getDouble(1).toFloat()+offset[1];val t=SystemClock.uptimeMillis()
        sendPointerSync(MotionEvent.obtain(t,t,MotionEvent.ACTION_DOWN,x,y,0))
        sendPointerSync(MotionEvent.obtain(t,t+50,MotionEvent.ACTION_UP,x,y,0))
        Thread.sleep(400)
    }
    override fun onStart() {
        try {
            activity = startActivitySync(Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            runOnMainSync { web = findWeb(activity.window.decorView) ?: error("Missing WebView") }
            for (i in 0..40) { if (js("!!document.querySelector('#device')") == "true") break; Thread.sleep(250) }
            requireJS("!!document.querySelector('#device')")
            requireJS("deviceAvailable()")
            requireJS("JSON.parse(Android.deviceSettings()).lock === false")
            js("localStorage.setItem('lohnzeit-language','en');q('#language').value='en';q('#language').dispatchEvent(new Event('change'))")
            tap("#headerAddPlace")
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
            // Dispatch a real reminder notification and inspect Android's active notifications.
            targetContext.getSharedPreferences("device",0).edit().putBoolean("reminder",true).apply()
            runOnMainSync { ReminderReceiver().onReceive(targetContext,Intent(ReminderReceiver.ACTION)) }
            check(targetContext.getSystemService(android.app.NotificationManager::class.java).activeNotifications.any { it.id==221 }) { "Reminder notification was not posted" }
            // Bind a real widget in the emulator's test host and inspect its rendered RemoteViews.
            uiAutomation.adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET")
            lateinit var host: android.appwidget.AppWidgetHost
            lateinit var hostView: android.appwidget.AppWidgetHostView
            var widgetId=0
            runOnMainSync {
                host=android.appwidget.AppWidgetHost(targetContext,991);widgetId=host.allocateAppWidgetId()
                val manager=android.appwidget.AppWidgetManager.getInstance(targetContext)
                check(manager.bindAppWidgetIdIfAllowed(widgetId,android.content.ComponentName(targetContext,PaydayWidget::class.java))) { "Widget binding failed" }
                host.startListening();hostView=host.createView(activity,widgetId,manager.getAppWidgetInfo(widgetId));PaydayWidget.update(targetContext)
            }
            uiAutomation.dropShellPermissionIdentity();Thread.sleep(1000)
            runOnMainSync { check(hostView.findViewById<android.widget.TextView>(R.id.widget_status)?.text.toString()=="On break") { "Widget RemoteViews did not render break status" };host.deleteAppWidgetId(widgetId);host.stopListening() }
            // Configure a test-only emulator PIN, then complete the actual system credential prompt.
            check(shell("locksettings set-pin 2468").contains("success",true)) { "Could not configure emulator PIN" }
            js("Android.setAppLock(true)");Thread.sleep(1500)
            shell("input text 2468");shell("input keyevent 66");Thread.sleep(1500)
            requireJS("JSON.parse(Android.deviceSettings()).lock===true")
            shell("input keyevent 3");Thread.sleep(500)
            shell("am start -n com.paydaytracker.app.debug/com.paydaytracker.app.MainActivity");Thread.sleep(1500)
            runOnMainSync { check(web.visibility!=View.VISIBLE) { "App contents exposed before authentication" } }
            shell("input keyevent 4");Thread.sleep(500)
            runOnMainSync { check(web.visibility!=View.VISIBLE) { "Cancel bypassed app lock" } }
            // Remove test-only PIN after checking cancellation; release users are never affected.
            shell("locksettings clear --old 2468")
            uiAutomation.takeScreenshot()?.let { bitmap -> java.io.File(targetContext.getExternalFilesDir(null),"device-smoke.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) } }
            finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "PAYDAY_SMOKE_PASS: real touch workplace creation, reminder notification delivery, rendered widget, PIN authentication and cancellation/background privacy\n") })
        } catch (e: Throwable) {
            android.util.Log.e("PaydaySmoke", "Failure", e)
            finish(Activity.RESULT_CANCELED, Bundle().apply { putString("stream", "PAYDAY_SMOKE_FAIL: ${e.stackTraceToString()}\n") })
        }
    }
}
