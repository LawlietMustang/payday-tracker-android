package com.paydaytracker.app

import com.paydaytracker.app.reminders.PayslipReminder

import com.paydaytracker.app.reminders.ShiftReminders

import com.paydaytracker.app.reminders.ReminderReceiver

import com.paydaytracker.app.reminders.BackupReminder

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
    private var permissionMode = ""
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); permissionMode = arguments?.getString("mode") ?: ""; start() }
    private lateinit var activity: Activity
    private lateinit var web: WebView
    private fun findWeb(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) findWeb(view.getChildAt(i))?.let { return it }
        return null
    }
    private fun js(code: String, timeout: Long = 10): String {
        val latch = CountDownLatch(1); var value = ""
        runOnMainSync { web.evaluateJavascript(code) { value = it; latch.countDown() } }
        check(latch.await(timeout, TimeUnit.SECONDS)) { "JavaScript timeout: $code" }
        return value
    }
    private fun requireJS(code: String) { check(js(code) == "true") { "Failed: $code; result=${js(code)}" } }
    private fun verifyOfflineIcons() {
        requireJS("location.protocol==='file:'")
        js("q('#language').value='en';q('#language').dispatchEvent(new Event('change'))")
        // Wait for the renderer to commit DOM changes before drawing the WebView.
        // UiAutomation can otherwise capture the previous compositor frame on a busy emulator.
        fun renderedBitmap(): android.graphics.Bitmap {
            val ready = CountDownLatch(1)
            runOnMainSync { web.postVisualStateCallback(0, object : WebView.VisualStateCallback() {
                override fun onComplete(requestId: Long) { ready.countDown() }
            }) }
            check(ready.await(15, TimeUnit.SECONDS)) { "WebView did not commit its visual state" }
            lateinit var bitmap: android.graphics.Bitmap
            runOnMainSync {
                bitmap = android.graphics.Bitmap.createBitmap(web.width, web.height, android.graphics.Bitmap.Config.ARGB_8888)
                web.draw(android.graphics.Canvas(bitmap))
            }
            return bitmap
        }
        for (theme in listOf("light", "dark")) {
            js("data.settings.theme='$theme';applyTheme();show('appSettings');window.scrollTo(0,0)")
            Thread.sleep(500)
            fun screenshot(name: String): android.graphics.Bitmap {
                val bitmap = renderedBitmap()
                java.io.File(targetContext.getExternalFilesDir(null), name).outputStream().use {
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
                return bitmap
            }
            screenshot("settings-icons-$theme.png").recycle()
            // Exercise every production icon rule, including dynamically inserted dialog icons.
            // The last tile deliberately uses the old file mask as a diagnostic control.
            js("""
                (()=>{
                  const names=['profile','workplaces','planning','reminders','lock','recovery','info','alert','cancel','sun','moon'];
                  const gallery=document.createElement('div');gallery.id='nativeIconGallery';gallery.dataset.localized='true';
                  gallery.style.cssText='position:fixed;z-index:99999;top:160px;left:16px;display:grid;grid-template-columns:repeat(6,24px);gap:20px;padding:20px;background:'+('$theme'==='light'?'#fff':'#160e2b')+';color:'+('$theme'==='light'?'#160e2b':'#fff');
                  gallery.innerHTML=names.map(id=>'<span class="route-icon" data-icon="'+id+'"></span>').join('')+'<span class="route-icon" data-icon="profile" style="--route-icon:url(./icons/profile.svg)"></span>';
                  document.body.appendChild(gallery);
                })()
            """.trimIndent())
            Thread.sleep(500)
            val boxes = JSONArray(js("Array.from(document.querySelectorAll('#nativeIconGallery .route-icon'),e=>{const r=e.getBoundingClientRect();return [r.x,r.y,r.width,r.height].map(x=>x*devicePixelRatio)})"))
            val painted = screenshot("icon-gallery-$theme.png")
            js("document.querySelectorAll('#nativeIconGallery .route-icon').forEach(e=>e.style.visibility='hidden')")
            Thread.sleep(200)
            val hidden = renderedBitmap()
            try {
                for (i in 0 until boxes.length()) {
                    val box=boxes.getJSONArray(i)
                    val left=box.getDouble(0).toInt(); val top=box.getDouble(1).toInt()
                    val width=box.getDouble(2).toInt(); val height=box.getDouble(3).toInt()
                    var changed=0
                    for(y in top until top+height) for(x in left until left+width) {
                        val a=painted.getPixel(x,y); val b=hidden.getPixel(x,y)
                        val difference=kotlin.math.abs(android.graphics.Color.red(a)-android.graphics.Color.red(b))+kotlin.math.abs(android.graphics.Color.green(a)-android.graphics.Color.green(b))+kotlin.math.abs(android.graphics.Color.blue(a)-android.graphics.Color.blue(b))
                        if(difference>90) changed++
                    }
                    val fraction=changed.toDouble()/(width*height)
                    if(i<11) check(fraction in 0.04..0.65) { "Icon $i ($theme) is blank or a solid block: $fraction" }
                    else android.util.Log.i("WageTrackIcons", "Old external-file mask painted fraction: $fraction ($theme)")
                }
            } finally { painted.recycle(); hidden.recycle(); js("q('#nativeIconGallery').remove()") }
        }
        js("data.settings.theme='light';applyTheme();show('dashboard')")
    }
    private fun shell(command: String): String = android.os.ParcelFileDescriptor.AutoCloseInputStream(uiAutomation.executeShellCommand(command)).bufferedReader().use { it.readText() }
    private fun tap(selector: String, holdMillis: Long = 50) {
        js("document.querySelector('$selector').scrollIntoView({block:'center'})")
        Thread.sleep(300)
        val point=JSONArray(js("(()=>{let r=document.querySelector('$selector').getBoundingClientRect();return [(r.x+r.width/2)*devicePixelRatio,(r.y+r.height/2)*devicePixelRatio]})()"))
        val offset=IntArray(2); runOnMainSync { web.getLocationOnScreen(offset) }
        val x=point.getDouble(0).toFloat()+offset[0];val y=point.getDouble(1).toFloat()+offset[1];val t=SystemClock.uptimeMillis()
        sendPointerSync(MotionEvent.obtain(t,t,MotionEvent.ACTION_DOWN,x,y,0))
        Thread.sleep(holdMillis)
        sendPointerSync(MotionEvent.obtain(t,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,x,y,0))
        Thread.sleep(400)
    }
    private fun currentPinDialog(): android.app.Dialog {
        val field=MainActivity::class.java.getDeclaredField("appLock").apply { isAccessible=true }
        val lock=field.get(activity)
        return AppLock::class.java.getDeclaredField("pinDialog").apply { isAccessible=true }.get(lock) as android.app.Dialog
    }
    private fun enterAppPin(value: String) { runOnMainSync {
        val dialog=currentPinDialog()
        value.forEach { digit -> dialog.window!!.decorView.findViewWithTag<View>("pin-key-$digit").performClick() }
    };Thread.sleep(150) }
    private fun runAutoBackupTest() {
        runOnMainSync {activity.startActivityForResult(Intent().setComponent(android.content.ComponentName("com.paydaytracker.app.debug.test","com.paydaytracker.app.BackupFolderActivity")),905)}
        Thread.sleep(1000)
        fun awaitSaved() {for(i in 0..80){if(js("JSON.parse(Android.autoBackupState()).status==='saved'")=="true")return;Thread.sleep(100)};error("Automatic backup did not finish: "+js("Android.autoBackupState()"))}
        awaitSaved()
        val tree=android.net.Uri.parse("content://com.paydaytracker.backup.tests/tree/root")
        val resolver=targetContext.contentResolver
        fun read(name:String)=resolver.openInputStream(android.provider.DocumentsContract.buildDocumentUriUsingTree(tree,"root/"+name))!!.bufferedReader().use {it.readText()}
        val first=read(AutoBackup.CURRENT)
        check(org.json.JSONObject(first).getJSONObject("data").has("settings"))
        js("data.profile.name='Backup burst one';save();data.profile.name='Backup burst latest';save()")
        Thread.sleep(4000);awaitSaved()
        check(org.json.JSONObject(read(AutoBackup.CURRENT)).getJSONObject("data").getJSONObject("profile").getString("name")=="Backup burst latest")
        check(read(AutoBackup.PREVIOUS)==first)
        val children=android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(tree,"root")
        resolver.query(children,arrayOf(android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME),null,null,null)!!.use {check(it.count==2){"Backup files accumulated"}}
        resolver.call(tree,"fail-current",null,null)
        js("data.profile.name='Recoverable pending edit';save()")
        Thread.sleep(4000)
        requireJS("JSON.parse(Android.autoBackupState()).status==='error'")
        check(org.json.JSONObject(read(AutoBackup.CURRENT)).getJSONObject("data").getJSONObject("profile").getString("name")=="Backup burst latest")
        check(org.json.JSONObject(read(AutoBackup.PREVIOUS)).getJSONObject("data").getJSONObject("profile").getString("name")=="Backup burst latest")
        resolver.call(tree,"allow-writes",null,null)
        js("Android.retryAutoBackup()")
        Thread.sleep(500);awaitSaved()
        check(org.json.JSONObject(read(AutoBackup.CURRENT)).getJSONObject("data").getJSONObject("profile").getString("name")=="Recoverable pending edit")
        js("Android.disableAutoBackup()")
        Thread.sleep(500)
        requireJS("JSON.parse(Android.autoBackupState()).enabled===false")
        check(read(AutoBackup.CURRENT).isNotEmpty())
    }
    private fun verifyPayslipReminder(blocked: Boolean) {
        val c=targetContext; val now=System.currentTimeMillis()
        val month=java.time.YearMonth.now().minusMonths(1).toString()
        val config=org.json.JSONObject().put("enabled",true).put("language","en").put("months",org.json.JSONArray().put(org.json.JSONObject().put("month",month).put("missing",true)))
        val p=c.getSharedPreferences("payslip_reminder",0)
        p.edit().clear().commit();PayslipReminder.update(c,config.toString(),now)
        val first=p.getLong("due",0);check(first>=now+2L*86400000)
        PayslipReminder.update(c,config.toString(),now+1000);check(first==p.getLong("due",0)) { "Edits postponed payslip reminder" }
        p.edit().putLong("due",now-1000).commit()
        PayslipReminder().onReceive(c,Intent())
        val manager=c.getSystemService(android.app.NotificationManager::class.java)
        if (!blocked) for(i in 0..20){if(manager.activeNotifications.any{it.id==227})break;Thread.sleep(100)}
        check(manager.activeNotifications.any{it.id==227} != blocked) { "Payslip notification permission handling failed" }
        val weekly=p.getLong("due",0);check(weekly>=now+6L*86400000)
        PayslipReminder.update(c,config.toString());check(weekly==p.getLong("due",0))
        config.getJSONArray("months").getJSONObject(0).put("missing",false)
        PayslipReminder.update(c,config.toString());check(!p.contains("due"))
        for(i in 0..20){if(manager.activeNotifications.none{it.id==227})break;Thread.sleep(100)}
        check(manager.activeNotifications.none{it.id==227}) { "Saved payslip did not cancel notification" }
        // Month rollover is evaluated natively, without a WebView save.
        val future=java.time.YearMonth.now().plusMonths(1)
        val futureNow=future.atDay(7).atTime(11,0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        config.getJSONArray("months").put(org.json.JSONObject().put("month",future.minusMonths(1).toString()).put("missing",true))
        PayslipReminder.update(c,config.toString(),now)
        PayslipReminder.reconcile(c,futureNow)
        check(p.getString("month","")==future.minusMonths(1).toString())
        check(p.getLong("due",0)>futureNow)
        config.put("enabled",false);PayslipReminder.update(c,config.toString());check(!p.contains("due"))
    }
    private fun verifyIdleWidget() {
        val c=targetContext;val p=c.getSharedPreferences("device",0);val now=System.currentTimeMillis()
        val at=java.time.LocalDate.now().plusDays(1).atTime(9,0)
        val row=org.json.JSONObject().put("id","glance").put("date",at.toLocalDate().toString()).put("start","09:00").put("workplace","Widget test")
        val config=org.json.JSONObject().put("enabled",false).put("leadMinutes",60).put("shifts",org.json.JSONArray()).put("glanceShifts",org.json.JSONArray().put(row))
        check(ShiftReminders.replace(c,config.toString()))
        val progress=org.json.JSONObject().put("month",java.time.YearMonth.now().toString()).put("minutes",2430).put("target",160)
        p.edit().putString("timerState","idle").putBoolean("lock",false).putString("language","en").putString("widgetProgress",progress.toString()).commit()
        runOnMainSync {
            fun widget()=PaydayWidget.views(c,now).apply(c,android.widget.FrameLayout(c))
            var v=widget();check(v.findViewById<android.widget.TextView>(R.id.widget_status).text.toString()=="40 h 30 min / 160 h")
            check(v.findViewById<android.widget.TextView>(R.id.widget_next).text.toString()=="Next: Tomorrow 09:00")
            p.edit().putString("language","de").commit();v=widget()
            check(v.findViewById<android.widget.TextView>(R.id.widget_next).text.toString()=="Nächste: Morgen 09:00")
            p.edit().putBoolean("lock",true).commit();v=widget()
            check(v.findViewById<android.widget.TextView>(R.id.widget_status).text.toString()=="Zum Entsperren öffnen")
            check(v.findViewById<View>(R.id.widget_next).visibility==View.GONE)
            progress.put("month","2000-01");p.edit().putBoolean("lock",false).putString("widgetProgress",progress.toString()).commit();v=widget()
            check(v.findViewById<android.widget.TextView>(R.id.widget_status).text.toString()=="0 h / 160 h")
        }
        config.put("glanceShifts",org.json.JSONArray());ShiftReminders.replace(c,config.toString())
        check(ShiftReminders.nextGlance(c)==null)
        p.edit().putString("language","en").putString("timerState","break").putBoolean("lock",false).commit()
    }

    override fun onStart() {
        try {
            if (permissionMode == "blocked") {
                val at = java.time.LocalDateTime.now().plusMinutes(30).withSecond(0).withNano(0)
                val row = org.json.JSONObject().put("id", "permission-retry").put("date", at.toLocalDate().toString()).put("start", at.toLocalTime().toString()).put("workplace", "Retry test")
                val config = org.json.JSONObject().put("enabled", true).put("leadMinutes", 60).put("language", "en").put("shifts", org.json.JSONArray().put(row))
                check(!ShiftReminders.status(targetContext).getBoolean("allowed")) { "Permission should be denied before this test" }
                check(ShiftReminders.replace(targetContext, config.toString()))
                check(ShiftReminders.status(targetContext).getInt("blocked") == 1)
                check(ShiftReminders.status(targetContext).getInt("delivered") == 0) { "Blocked reminder was marked delivered" }
                verifyPayslipReminder(true)
                finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "PAYDAY_PERMISSION_BLOCKED_PASS\n") })
                return
            }
            if (permissionMode == "granted") {
                check(ShiftReminders.status(targetContext).getInt("blocked") == 1) { "Pending reminder did not survive process restart" }
                check(ShiftReminders.status(targetContext).getBoolean("allowed"))
                ShiftReminders.reconcile(targetContext)
                val manager = targetContext.getSystemService(android.app.NotificationManager::class.java)
                for (i in 0..20) { if (manager.activeNotifications.any { it.id == 225 }) break; Thread.sleep(100) }
                check(manager.activeNotifications.any { it.id == 225 }) { "Reminder did not retry after permission grant" }
                check(ShiftReminders.status(targetContext).getInt("delivered") == 1)
                ShiftReminders.replace(targetContext, "{\"enabled\":false,\"leadMinutes\":60,\"shifts\":[]}")
                for (i in 0..20) { if (manager.activeNotifications.none { it.id == 225 }) break; Thread.sleep(100) }
            }
            activity = startActivitySync(Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            runOnMainSync { web = findWeb(activity.window.decorView) ?: error("Missing WebView") }
            for (i in 0..40) { if (js("!!document.querySelector('#device')",30) == "true") break; Thread.sleep(250) }
            requireJS("!!document.querySelector('#device')")
            requireJS("deviceAvailable()")
            requireJS("JSON.parse(Android.bridgeContract()).some(m=>m.name==='syncWidget' && m.arguments.length===4)")
            requireJS("Array.from(document.querySelectorAll('script[src],link[rel=stylesheet]')).every(e=>(e.src||e.href).includes('?v='))")
            check(targetContext.getSharedPreferences("app_meta",0).getInt("last_version_code",-1)==BuildConfig.VERSION_CODE)
            var reported=false
            val fallback=com.paydaytracker.app.util.BridgeErrors.call("testFailure", "fallback", { reported=true }) { throw IllegalStateException("test") }
            check(reported && fallback=="fallback")
            // Complete a fresh-install setup through the rendered controls.
            for (i in 0..20) { if (js("typeof setupActive !== 'undefined'") == "true") break; Thread.sleep(100) }
            if (js("setupActive") == "true") {
                js("q('#setupPersonalName').value='Alex';q('#setupPersonalName').dispatchEvent(new Event('input'));q('#setupBasicsCountry').value='DE';q('#setupBasicsCountry').dispatchEvent(new Event('change'));q('#setupNext').click()")
                js("q('#setupCountry').value='DE';q('#setupCountry').dispatchEvent(new Event('change'));q('#setupName').value='Smoke workplace';q('#setupName').dispatchEvent(new Event('input'));q('#setupWage').value='20';q('#setupWage').dispatchEvent(new Event('input'));q('#setupTarget').value='80';q('#setupTarget').dispatchEvent(new Event('input'));q('#setupNext').click()")
                requireJS("setupStep === 2")
                js("q('#setupSkip').click();q('#setupNext').click()")
                requireJS("data.onboardingCompleted && !setupActive")
            }
            runOnMainSync {
                val position = IntArray(2); web.getLocationOnScreen(position)
                val inset = activity.window.decorView.rootWindowInsets.getInsets(android.view.WindowInsets.Type.statusBars()).top
                check(position[1] >= inset) { "WebView overlaps status bar" }
            }
            verifyOfflineIcons()
            requireJS("JSON.parse(Android.deviceSettings()).lock === false")
            requireJS("JSON.parse(Android.googleAccountState()).configured === false")
            js("Android.googleSignIn()")
            Thread.sleep(300)
            requireJS("JSON.parse(Android.googleAccountState()).email === ''")
            // A faded save message must not intercept a real touch on bottom navigation.
            js("show('expenses');toast('Expense saved')")
            Thread.sleep(2800)
            tap(".mobile [data-view=dashboard]")
            requireJS("q('.view.active').id === 'dashboard'")
            // Hold with actual Android MotionEvents, then release without losing selection.
            js("selected='2026-09';data.shifts.push({id:'native-hold',date:'2026-09-01',start:'09:00',end:'17:00',breakMin:30,minutes:450,wage:15,status:'completed',workplaceId:'default'});render();show('shifts');window.heldRow=q('#all .shift')")
            tap("#all .shift .shiftmain", 430)
            requireJS("selectionMode && selectedShiftIds.has('native-hold') && heldRow === q('#all .shift')")
            js("finishSelection();data.shifts=data.shifts.filter(s=>s.id!=='native-hold');render();show('dashboard')")
            // Native alarm delivery while the app is in the background, plus cancellation.
            shell("appops set com.paydaytracker.app.debug SCHEDULE_EXACT_ALARM allow")
            val start = java.time.LocalDateTime.now().plusMinutes(2).withSecond(0).withNano(0)
            val row = org.json.JSONObject().put("id", "native-shift").put("date", start.toLocalDate().toString()).put("start", start.toLocalTime().toString()).put("workplace", "Test workplace")
            val config = org.json.JSONObject().put("enabled", true).put("leadMinutes", 1).put("language", "en").put("shifts", org.json.JSONArray().put(row))
            check(ShiftReminders.replace(targetContext, config.toString()))
            val due = ShiftReminders.status(targetContext).getLong("next")
            check(due > System.currentTimeMillis()) { "Shift reminder not scheduled" }
            // Edited time replaces the old alarm; deleting cancels it entirely.
            val editedStart = start.plusHours(1)
            row.put("date", editedStart.toLocalDate().toString()).put("start", editedStart.toLocalTime().toString())
            check(ShiftReminders.replace(targetContext, config.toString()))
            check(ShiftReminders.status(targetContext).getLong("next") > due)
            config.put("shifts", org.json.JSONArray())
            check(ShiftReminders.replace(targetContext, config.toString()))
            check(ShiftReminders.status(targetContext).getLong("next") == 0L)
            row.put("date", start.toLocalDate().toString()).put("start", start.toLocalTime().toString());config.put("shifts", org.json.JSONArray().put(row))
            check(ShiftReminders.replace(targetContext, config.toString()))
            shell("input keyevent 3")
            val deadline = System.currentTimeMillis() + 75000
            val manager = targetContext.getSystemService(android.app.NotificationManager::class.java)
            while (System.currentTimeMillis() < deadline && manager.activeNotifications.none { it.id == 225 }) Thread.sleep(500)
            val posted = manager.activeNotifications.firstOrNull { it.id == 225 } ?: error("Background shift reminder not delivered")
            ShiftReminders.reconcile(targetContext)
            check(manager.activeNotifications.first { it.id == 225 }.postTime == posted.postTime) { "Duplicate shift reminder" }
            config.put("enabled", false);ShiftReminders.replace(targetContext, config.toString())
            for (i in 0..20) { if (manager.activeNotifications.none { it.id == 225 }) break; Thread.sleep(100) }
            check(manager.activeNotifications.none { it.id == 225 }) { "Disabled shift notification was not cancelled" }
            shell("am start -n com.paydaytracker.app.debug/com.paydaytracker.app.MainActivity");Thread.sleep(700)
            js("show('dashboard');show('shifts');show('expenses')")
            shell("input keyevent 4"); Thread.sleep(500)
            requireJS("q('.view.active').id==='shifts'")
            shell("input keyevent 4"); Thread.sleep(500)
            requireJS("q('.view.active').id==='dashboard'")
            js("openExpenseDialog()")
            shell("input keyevent 4"); Thread.sleep(500)
            requireJS("!q('#expenseDialog').open && q('.view.active').id==='dashboard'")
            shell("input keyevent 4"); Thread.sleep(500)
            check(!activity.hasWindowFocus()) { "Home Back did not background the app" }
            shell("am start -n com.paydaytracker.app.debug/com.paydaytracker.app.MainActivity")
            Thread.sleep(700)
            js("localStorage.setItem('lohnzeit-language','en');q('#language').value='en';q('#language').dispatchEvent(new Event('change'))")
            tap("#headerAddPlace")
            requireJS("q('#planningDialog').open")
            js("q('#placeName').value='Device test job';q('#placeWage').value='22';q('#planningForm').requestSubmit()")
            requireJS("data.workplaces.some(w=>w.name==='Device test job')")
            js("q('#headerManagePlaces').click()")
            requireJS("q('#workplaces').classList.contains('active')")
            js("deviceSection='reminders';show('device')")
            tap("#reminderEnabled")
            Thread.sleep(300)
            requireJS("JSON.parse(Android.deviceSettings()).reminder === true")
            tap("#editLoggingReminder")
            js("q('#reminderTime').value='19:30';q('#reminderForm').requestSubmit()")
            Thread.sleep(500)
            requireJS("JSON.parse(Android.deviceSettings()).hour===19 && JSON.parse(Android.deviceSettings()).minute===30")
            js("startWorkTimer()")
            requireJS("!q('#clockLiveBadge').hidden && q('#clockLiveBadge').textContent.length > 0")
            Thread.sleep(500)
            check(targetContext.getSharedPreferences("device",0).getString("timerState", "") == "working") { "Widget did not receive timer" }
            js("startWorkBreak()")
            Thread.sleep(500)
            check(targetContext.getSharedPreferences("device",0).getString("timerState", "") == "break") { "Widget did not receive break" }
            // Exercise real persisted SAF writes, rotation and write-failure recovery.
            runAutoBackupTest()
            // Cancelled PIN setup cannot enable the lock.
            js("Android.setAppLock(true)");Thread.sleep(500)
            runOnMainSync { currentPinDialog().window!!.decorView.findViewWithTag<View>("pin-back").performClick() }
            requireJS("JSON.parse(Android.deviceSettings()).lock===false")
            // Backup nudges wait 48h and repeat no more than weekly.
            BackupReminder.update(targetContext, true, "en")
            val backupPrefs = targetContext.getSharedPreferences("device",0)
            check(backupPrefs.getLong("backupDue",0) > System.currentTimeMillis()+47L*3600000)
            val originalDue = backupPrefs.getLong("backupDue",0)
            BackupReminder.update(targetContext,true,"en");check(originalDue==backupPrefs.getLong("backupDue",0))
            backupPrefs.edit().putLong("backupDue",System.currentTimeMillis()-1000).commit()
            BackupReminder().onReceive(targetContext,Intent())
            // NotificationManager publishes asynchronously; wait for observable delivery.
            for (i in 0..20) { if (targetContext.getSystemService(android.app.NotificationManager::class.java).activeNotifications.any { it.id==226 }) break; Thread.sleep(100) }
            check(targetContext.getSystemService(android.app.NotificationManager::class.java).activeNotifications.any { it.id==226 }) { "Backup reminder was not posted" }
            check(backupPrefs.getLong("backupDue",0)>System.currentTimeMillis()+6L*86400000)
            BackupReminder.update(targetContext,false,"en")
            // Dispatch a real reminder notification and inspect Android's active notifications.
            targetContext.getSharedPreferences("device",0).edit().putBoolean("reminder",true).putLong("nextReminder",System.currentTimeMillis()-1000).apply()
            runOnMainSync { ReminderReceiver().onReceive(targetContext,Intent(ReminderReceiver.ACTION)) }
            for (i in 0..20) { if (targetContext.getSystemService(android.app.NotificationManager::class.java).activeNotifications.any { it.id==221 }) break; Thread.sleep(100) }
            check(targetContext.getSystemService(android.app.NotificationManager::class.java).activeNotifications.any { it.id==221 }) { "Reminder notification was not posted" }
            // Let Android deliver a scheduled reminder through AlarmManager, without invoking the receiver.
            shell("appops set com.paydaytracker.app.debug SCHEDULE_EXACT_ALARM allow")
            val nextMinute=java.util.Calendar.getInstance().apply { add(java.util.Calendar.MINUTE,1);set(java.util.Calendar.SECOND,0);set(java.util.Calendar.MILLISECOND,0) }
            targetContext.getSystemService(android.app.NotificationManager::class.java).cancel(221)
            js("Android.saveReminder(true,${nextMinute.get(java.util.Calendar.HOUR_OF_DAY)},${nextMinute.get(java.util.Calendar.MINUTE)},127,'en')")
            Thread.sleep(500)
            val expected=targetContext.getSharedPreferences("device",0).getLong("nextReminder",0)
            check(expected==nextMinute.timeInMillis) { "Scheduled time did not match saved reminder" }
            shell("input keyevent 3")
            for (i in 0..300) { if (targetContext.getSystemService(android.app.NotificationManager::class.java).activeNotifications.any { it.id==221 }) break;Thread.sleep(250) }
            check(targetContext.getSystemService(android.app.NotificationManager::class.java).activeNotifications.any { it.id==221 }) { "Scheduled reminder never arrived while backgrounded" }
            check(targetContext.getSharedPreferences("device",0).getLong("lastReminder",0)==expected) { "Wrong reminder delivered" }
            shell("am start -n com.paydaytracker.app.debug/com.paydaytracker.app.MainActivity");Thread.sleep(500)
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
            verifyPayslipReminder(false);verifyIdleWidget()
            // Create and confirm a real app PIN, without a phone PIN dependency.
            js("show('appSettings');q('#settingsLock').click()");requireJS("!q('#appLockCard').hidden")
            tap("#biometricLock");Thread.sleep(500)
            runOnMainSync {
                val screen=currentPinDialog().window!!.decorView
                check(screen.findViewWithTag<android.widget.TextView>("pin-step").text.toString()=="STEP 1 OF 2")
                val bitmap=android.graphics.Bitmap.createBitmap(screen.width,screen.height,android.graphics.Bitmap.Config.ARGB_8888)
                screen.draw(android.graphics.Canvas(bitmap))
                java.io.File(targetContext.getExternalFilesDir(null),"pin-setup.png").outputStream().use {bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
            }
            enterAppPin("246810")
            runOnMainSync {check(currentPinDialog().window!!.decorView.findViewWithTag<android.widget.TextView>("pin-step").text.toString()=="STEP 2 OF 2")}
            enterAppPin("135790") // mismatch must keep the gate closed
            requireJS("JSON.parse(Android.deviceSettings()).lock===false")
            enterAppPin("246810");Thread.sleep(500)
            requireJS("JSON.parse(Android.deviceSettings()).lock===true")
            shell("input keyevent 3");Thread.sleep(500)
            shell("am start -n com.paydaytracker.app.debug/com.paydaytracker.app.MainActivity");Thread.sleep(1500)
            runOnMainSync { check(web.visibility!=View.VISIBLE) { "App contents exposed before authentication" } }
            shell("input keyevent 4");Thread.sleep(500)
            runOnMainSync { check(web.visibility!=View.VISIBLE) { "Cancel bypassed app lock" } }
            runOnMainSync {
                val root = activity.window.decorView
                val gate = root.findViewWithTag<View>("wagetrack-lock")
                check(gate.visibility == View.VISIBLE) { "Missing branded lock screen" }
                check((gate.background as android.graphics.drawable.ColorDrawable).color == android.graphics.Color.parseColor("#180E33"))
                val greeting = root.findViewWithTag<android.widget.TextView>("lock-greeting").text.toString()
                check(greeting in listOf("Good Morning", "Good Afternoon", "Good Evening"))
                check(root.findViewWithTag<android.widget.TextView>("lock-masked-name").text.toString() == "••••••")
                check(root.findViewWithTag<View>("lock-fingerprint").isClickable)
                val bitmap = android.graphics.Bitmap.createBitmap(gate.width,gate.height,android.graphics.Bitmap.Config.ARGB_8888)
                gate.draw(android.graphics.Canvas(bitmap))
                java.io.File(targetContext.getExternalFilesDir(null),"lock-screen.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
                root.findViewWithTag<View>("lock-pin").performClick()
            }
            Thread.sleep(500);enterAppPin("111111")
            runOnMainSync { check(web.visibility != View.VISIBLE) { "Wrong PIN unlocked app" } }
            enterAppPin("246810");Thread.sleep(500)
            runOnMainSync { check(web.visibility == View.VISIBLE) { "PIN fallback did not unlock app" } }
            val verifier=AppPin(targetContext)
            repeat(5) { check(!verifier.verify("000000")) }
            check(verifier.waitSeconds>0);check(!verifier.verify("246810"))
            targetContext.getSharedPreferences("app-pin",0).edit().putLong("until",0).commit()
            check(verifier.verify("246810"))
            check(!targetContext.getSharedPreferences("app-pin",0).all.values.contains("246810"))
            uiAutomation.takeScreenshot()?.let { bitmap -> java.io.File(targetContext.getExternalFilesDir(null),"device-smoke.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) } }
            finish(Activity.RESULT_OK, Bundle().apply { putString("stream", "PAYDAY_SMOKE_PASS: real touch workplace creation, reminder notification delivery, rendered widget, PIN authentication and cancellation/background privacy\n") })
        } catch (e: Throwable) {
            uiAutomation.takeScreenshot()?.let { bitmap -> java.io.File(targetContext.getExternalFilesDir(null),"device-smoke.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) } }
            android.util.Log.e("PaydaySmoke", "Active window: ${uiAutomation.rootInActiveWindow}")
            android.util.Log.e("PaydaySmoke", "Failure", e)
            finish(Activity.RESULT_CANCELED, Bundle().apply { putString("stream", "PAYDAY_SMOKE_FAIL: ${e.stackTraceToString()}\n") })
        }
    }
}
