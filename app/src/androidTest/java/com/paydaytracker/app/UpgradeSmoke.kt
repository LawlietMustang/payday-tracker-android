package com.paydaytracker.app

import android.app.Activity
import android.app.Instrumentation
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

// The same test APK seeds the previous app, then verifies an install-over update.
// It references only platform APIs and the stable JavaScript bridge, not moved classes.
class UpgradeSmoke : Instrumentation() {
    private var mode = ""
    private lateinit var web: WebView
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments);mode=arguments?.getString("mode")?:"";start() }
    private fun find(view: View): WebView? {
        if(view is WebView)return view
        if(view is ViewGroup)for(i in 0 until view.childCount)find(view.getChildAt(i))?.let{return it}
        return null
    }
    private fun js(code: String): String {
        val done=CountDownLatch(1);var result=""
        runOnMainSync { web.evaluateJavascript(code) { result=it;done.countDown() } }
        check(done.await(15,TimeUnit.SECONDS));return result
    }
    override fun onStart() {
        try {
            val activity=startActivitySync(Intent().setComponent(ComponentName(targetContext.packageName,"com.paydaytracker.app.MainActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            runOnMainSync { web=find(activity.window.decorView)?:error("No WebView") }
            for(i in 0..80){if(js("typeof setupActive!=='undefined' && typeof save==='function' && typeof data==='object'")=="true")break;Thread.sleep(200)}
            check(js("typeof setupActive!=='undefined' && typeof save==='function' && typeof data==='object'")=="true") { "App did not finish loading" }
            val prefs=targetContext.getSharedPreferences("upgrade_probe",0)
            val manager=AppWidgetManager.getInstance(targetContext)
            val provider=ComponentName(targetContext.packageName,"com.paydaytracker.app.PaydayWidget")
            if(mode=="seed") {
                check(js("location.pathname==='/android_asset/index.html'")=="true")
                check(js("(()=>{data.onboardingCompleted=true;delete data.onboardingDraft;data.profile={...data.profile,name:'Upgrade retained'};data.shifts=[{id:'upgrade-shift',date:'2026-08-01',start:'09:00',end:'17:00',minutes:450,breakMin:30,wage:17,status:'completed',workplaceId:'default'}];save();localStorage.setItem('upgrade-probe','retained');Android.saveReminder(true,20,30,62,'en');return true})()") == "true")
                uiAutomation.adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET")
                runOnMainSync {
                    val host=AppWidgetHost(targetContext,992);val id=host.allocateAppWidgetId()
                    check(manager.bindAppWidgetIdIfAllowed(id,provider));prefs.edit().putInt("widget",id).commit()
                }
                uiAutomation.dropShellPermissionIdentity()
                Thread.sleep(600)
            } else {
                check(js("location.pathname==='/android_asset/web/index.html'")=="true") { "New entry point not loaded" }
                check(js("localStorage.getItem('upgrade-probe')==='retained' && data.profile.name==='Upgrade retained' && data.shifts.some(s=>s.id==='upgrade-shift' && s.minutes===450 && s.wage===17)")=="true") { "Upgrade lost saved app data" }
                check(js("JSON.parse(Android.deviceSettings()).reminder===true")=="true") { "Reminder preference lost" }
                check(js("Array.from(document.querySelectorAll('script[src],link[rel=stylesheet]')).every(e=>(e.src||e.href).includes('?v=2.4.12'))")=="true") { "Old assets loaded" }
                check(targetContext.getSharedPreferences("app_meta",0).getInt("last_version_code",-1)==41)
                val id=prefs.getInt("widget",-1)
                check(manager.getAppWidgetInfo(id)?.provider==provider) { "Existing widget provider lost" }
                runOnMainSync { AppWidgetHost(targetContext,992).deleteAppWidgetId(id) }
            }
            finish(Activity.RESULT_OK,Bundle().apply{putString("stream","WAGETRACK_UPGRADE_${mode.uppercase()}_PASS\n")})
        } catch(e:Throwable) { finish(Activity.RESULT_CANCELED,Bundle().apply{putString("stream","WAGETRACK_UPGRADE_FAIL: ${e.stackTraceToString()}\n")}) }
    }
}
