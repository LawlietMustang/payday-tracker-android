package com.paydaytracker.app

import androidx.compose.ui.test.*
import android.graphics.Bitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paydaytracker.app.data.*
import com.paydaytracker.app.data.db.WageTrackDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceSmoke {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val repository get() = WageRepository(WageTrackDatabase.getInstance(compose.activity))
    @Before fun seed() = runBlocking {
        if (repository.workplaces.first().isEmpty()) repository.addWorkplace("Smoke workplace",15.0)
        repository.updateDocument { it.put("language","en").put("onboardingCompleted",true) }
        compose.waitUntil(15000) { compose.onAllNodesWithTag("screen-dashboard").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun screenshot(name: String) {
        compose.activityRule.scenario.onActivity { activity ->
            androidx.core.view.WindowCompat.getInsetsController(activity.window,activity.window.decorView).hide(androidx.core.view.WindowInsetsCompat.Type.ime())
        }
        compose.waitForIdle()
        // Wait for the submitted frame to reach SurfaceFlinger before taking a device screenshot.
        Thread.sleep(250)
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        fun shell(command: String) = automation.executeShellCommand(command).use {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes().decodeToString()
        }
        shell("mkdir -p /sdcard/Download/native-screens")
        shell("screencap -p /sdcard/Download/native-screens/$name.png")
        Assert.assertTrue("Screenshot must be nonempty", shell("wc -c /sdcard/Download/native-screens/$name.png").trim().substringBefore(' ').toLong() > 1000)
    }
    @Test fun originalNavigationIsRestored() {
        for(route in listOf("shifts","expenses","history","dashboard")) {
            compose.onNodeWithTag("nav-$route").performClick()
            compose.onNodeWithTag("screen-$route").assertExists()
            screenshot(route)
        }
        val monthLabel=compose.onNodeWithContentDescription("Select month").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString()
        compose.onNodeWithContentDescription("Select month").performScrollTo().performClick()
        compose.onNodeWithText("Set month").assertIsDisplayed()
        screenshot("month-picker")
        compose.onNodeWithText("Cancel").performClick()
        Assert.assertEquals(monthLabel,compose.onNodeWithContentDescription("Select month").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString())
        compose.onNodeWithContentDescription("Menu").performScrollTo().performClick()
        screenshot("drawer-layout")
        compose.onNodeWithText("Settings",useUnmergedTree=true).performClick()
        screenshot("settings-layout")
        compose.onNodeWithText("Reminders & widget").performScrollTo().performClick()
        compose.onNodeWithText("Upcoming shifts").assertExists()
        screenshot("reminders")
        compose.onNodeWithText("App lock").performClick()
        compose.onNodeWithText("Biometric / PIN lock").assertExists()
        compose.onNodeWithText("Reminders").performClick()
        compose.onNodeWithText("App lock").assertExists()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("screen-settings").assertExists()
    }
    @Test fun shiftEntryPersistsAndEnglishLabelsStayEnglish() {
        val note="Native parity smoke ${System.currentTimeMillis()}"
        compose.onNodeWithContentDescription("Add shift").performClick()
        compose.onNodeWithTag("shift-date").assert(hasSetTextAction().not())
        compose.onNodeWithTag("shift-start-time").performScrollTo().assert(hasSetTextAction().not()).performClick()
        compose.onNodeWithTag("time-picker").assertExists()
        compose.onAllNodes(hasSetTextAction() and hasAnyAncestor(hasTestTag("time-dialog"))).assertCountEquals(2)
        screenshot("shift-time-picker")
        // Tap the 12-hour face and choose the period explicitly.
        compose.onNodeWithTag("time-am").performClick()
        compose.onNodeWithContentDescription("9 hours").performClick()
        compose.onNodeWithContentDescription("0 minutes").performClick()
        compose.onNodeWithTag("confirm-time").performClick()
        compose.onNodeWithTag("shift-start-time").assertTextContains("09:00")
        compose.onNodeWithTag("shift-end-time").performClick()
        compose.onNodeWithTag("time-pm").performClick()
        compose.onNodeWithContentDescription("5 hours").performClick()
        compose.onNodeWithContentDescription("0 minutes").performClick()
        compose.onNodeWithTag("confirm-time").performClick()
        compose.onNodeWithTag("shift-end-time").assertTextContains("17:00")
        // Changes on a dial must not leak into the editor when cancelled.
        compose.onNodeWithTag("shift-start-time").performClick()
        compose.onNodeWithContentDescription("10 hours").performClick()
        compose.onNode(hasText("Cancel") and hasAnyAncestor(hasTestTag("time-dialog"))).performClick()
        compose.onNodeWithTag("shift-start-time").assertTextContains("09:00")
        compose.onNodeWithText("Unpaid break (minutes)").performScrollTo().performTextReplacement("30")
        compose.onNodeWithText("Note (optional)").performScrollTo().performTextReplacement(note)
        compose.onNodeWithText("Save shift").performClick()
        var saved: Shift?=null
        compose.waitUntil(10000) { saved=runBlocking{repository.shifts.first().firstOrNull{it.note==note}};saved!=null }
        Assert.assertEquals(450,saved!!.minutes)
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15000){compose.onAllNodesWithTag("screen-dashboard").fetchSemanticsNodes().isNotEmpty()}
        Assert.assertEquals(saved!!.id,runBlocking{repository.shifts.first().first{it.note==note}.id})
        runBlocking{repository.deleteShift(saved!!.id)}
    }
    @Test fun clockAcceptsTyped24HourAndManualPeriodWithoutSavingInvalidInput() {
        compose.onNodeWithContentDescription("Add shift").performClick()
        compose.onNodeWithTag("shift-start-time").performScrollTo().performClick()
        compose.onNodeWithTag("time-hour-input").performTextReplacement("17")
        compose.onNodeWithTag("time-pm").assertIsSelected()
        compose.onNodeWithTag("time-minute-input").performTextReplacement("30")
        compose.onNodeWithTag("time-hour-input").assertTextEquals("05")
        compose.onNodeWithTag("confirm-time").performClick()
        compose.onNodeWithTag("shift-start-time").assertTextContains("17:30")
        compose.onNodeWithTag("shift-start-time").performClick()
        compose.onNodeWithTag("time-hour-input").performTextReplacement("5")
        compose.onNodeWithTag("time-am").performClick()
        compose.onNodeWithTag("time-pm").performClick()
        compose.onNodeWithTag("confirm-time").performClick()
        compose.onNodeWithTag("shift-start-time").assertTextContains("17:30")
        compose.onNodeWithTag("shift-start-time").performClick()
        compose.onNodeWithTag("time-hour-input").performTextReplacement("00")
        compose.onNodeWithTag("time-am").assertIsSelected()
        compose.onNodeWithTag("time-minute-input").performTextReplacement("00")
        compose.onNodeWithTag("confirm-time").performClick()
        compose.onNodeWithTag("shift-start-time").assertTextContains("00:00")
        compose.onNodeWithTag("shift-start-time").performClick()
        compose.onNodeWithTag("time-hour-input").performTextReplacement("24")
        compose.onNodeWithTag("confirm-time").assertIsNotEnabled()
        compose.onNodeWithTag("time-hour-input").performTextReplacement("23")
        compose.onNodeWithTag("time-minute-input").performTextReplacement("60")
        compose.onNodeWithTag("confirm-time").assertIsNotEnabled()
        compose.onNodeWithTag("time-minute-input").performTextClearance()
        compose.onNodeWithTag("confirm-time").assertIsNotEnabled()
        compose.onNodeWithTag("time-minute-input").performTextReplacement("59")
        compose.onNodeWithTag("confirm-time").performClick()
        compose.onNodeWithTag("shift-start-time").assertTextContains("23:59")
        compose.onNodeWithTag("shift-start-time").performClick()
        screenshot("clock-editable-am-pm")
        // A swipe does not select another hour or stretch the clock hand.
        compose.onNodeWithTag("time-picker").performTouchInput { swipe(centerLeft, centerRight) }
        compose.onNodeWithTag("time-hour-input").assertTextEquals("11")
        compose.onNode(hasText("Cancel") and hasAnyAncestor(hasTestTag("time-dialog"))).performClick()
        compose.onNodeWithTag("shift-start-time").assertTextContains("23:59")
    }
    @Test fun calendarAndMultiDayPickerMatchReference() = runBlocking {
        val wp=repository.workplaces.first().first()
        val month=java.time.YearMonth.now().plusMonths(2)
        val prior=repository.getSettings()
        val records=listOf(
            Shift("calendar-planned",month.atDay(5).toString(),"09:00","17:00",450,30,wp.wage,wp.id,"planned","manual"),
            Shift("calendar-done",month.atDay(6).toString(),"09:00","17:00",450,30,wp.wage,wp.id,"completed","manual"),
            Shift("calendar-overlap-a",month.atDay(7).toString(),"09:00","17:00",450,30,wp.wage,wp.id,"planned","manual"),
            Shift("calendar-overlap-b",month.atDay(7).toString(),"16:00","18:00",120,0,wp.wage,wp.id,"planned","manual"),
            Shift("calendar-cancelled",month.atDay(8).toString(),"09:00","17:00",450,30,wp.wage,wp.id,"cancelled","manual")
        )
        val note="Calendar picker smoke ${System.currentTimeMillis()}"
        repository.saveShifts(records)
        repository.saveSettings(prior.copy(theme="light"))
        try {
            compose.onNodeWithTag("nav-shifts").performClick()
            repeat(2){compose.onNodeWithContentDescription("Next month").performScrollTo().performClick()}
            compose.onNodeWithTag("calendar-day-${month.atDay(7)}").assertContentDescriptionEquals("${month.atDay(7)}, Overlap")
            compose.onNodeWithTag("calendar-day-${month.atDay(8)}").assertContentDescriptionEquals("${month.atDay(8)}, Cancelled")
            compose.onNodeWithTag("shift-calendar").performScrollTo()
            screenshot("hours-calendar-reference")
            compose.onNodeWithTag("calendar-day-${month.atDay(10)}").performClick()
            compose.onNodeWithText("+ Add shift").performClick()
            compose.onNodeWithTag("shift-date").performClick()
            compose.onNodeWithTag("picker-day-${month.atDay(11)}").performClick()
            compose.onNodeWithTag("shift-date").assertTextContains(month.atDay(11).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")))
            compose.onNodeWithTag("multiple-shift-days").performScrollTo().performClick()
            compose.onNodeWithTag("picker-day-${month.atDay(12)}").performScrollTo().performClick()
            compose.onNodeWithTag("picker-day-${month.atDay(13)}").performClick()
            compose.onNodeWithTag("picker-day-${month.atDay(13)}").performClick()
            compose.onNodeWithTag("picker-day-${month.atDay(12)}").assertIsSelected()
            compose.onNodeWithTag("picker-day-${month.atDay(13)}").assertIsNotSelected()
            compose.onNodeWithText("2 days selected").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("save-shift").assertIsDisplayed()
            screenshot("shift-multiple-days-light")
            repository.saveSettings(prior.copy(theme="dark"))
            compose.waitUntil(10000) {
                val image=compose.onNodeWithTag("shift-editor").captureToImage()
                val color=image.toPixelMap()[image.width/2,20]
                color.red < .25f && color.blue < .5f
            }
            screenshot("shift-multiple-days-dark")
            repository.saveSettings(prior.copy(theme="light"))
            compose.waitUntil(10000) {
                val image=compose.onNodeWithTag("shift-editor").captureToImage()
                val color=image.toPixelMap()[image.width/2,20]
                color.red > .8f && color.blue > .8f
            }
            screenshot("shift-multiple-days-light-return")
            compose.onNodeWithText("Note (optional)").performScrollTo().performTextReplacement(note)
            compose.onNodeWithTag("save-shift").performClick()
            compose.waitUntil(10000){runBlocking{repository.shifts.first().count{it.note==note}}==2}
            val saved=repository.shifts.first().filter{it.note==note}
            Assert.assertEquals(setOf(month.atDay(11).toString(),month.atDay(12).toString()),saved.map{it.date}.toSet())
            Assert.assertTrue(saved.all{it.status=="planned" && it.start=="12:00" && it.end=="20:00" && it.minutes==450})
        } finally {
            repository.deleteShifts(records.map{it.id}+repository.shifts.first().filter{it.note==note}.map{it.id})
            repository.saveSettings(prior)
        }
    }
    @Test fun populatedScreensAndTimerSurviveRecreation() = runBlocking {
        val month=java.time.YearMonth.now()
        val wp=repository.workplaces.first().first()
        val records=(1..8).map { day -> Shift("visual-$day",month.atDay(day).toString(),"09:00","17:00",450,30,wp.wage,wp.id,"completed","manual") }
        repository.saveShifts(records)
        val previous=repository.getSettings()
        repository.saveSettings(previous.copy(theme="light"))
        repository.saveSavingsGoal(SavingsGoal("visual-goal","Holiday fund",1000.0,250.0))
        try {
            compose.waitForIdle()
            screenshot("overview-populated")
            compose.onNodeWithText("Earnings, progress & recent shifts",substring=true).performScrollTo().performClick()
            compose.onNodeWithTag("earnings-row").performScrollTo()
            screenshot("earnings-populated")
            compose.onNodeWithTag("bonus-card").performScrollTo();screenshot("bonus-layout")
            compose.onNodeWithTag("progress-card").performScrollTo();screenshot("progress-layout")
            compose.onNodeWithTag("deductions-card").performScrollTo();screenshot("deductions-layout")
            compose.onNodeWithContentDescription("Menu").performScrollTo().performClick()
            compose.onNodeWithText("Settings",useUnmergedTree=true).performClick()
            compose.onNodeWithText("Budgets & goals").performClick()
            compose.waitUntil(10000){compose.onAllNodesWithText("Holiday fund").fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithText("Holiday fund").assertExists()
            compose.onNodeWithTag("goal-visual-goal").performScrollTo()
            screenshot("goals-populated")
            compose.onNodeWithTag("nav-dashboard").performClick()
            compose.onNodeWithText("▶ Start work").performScrollTo().performClick()
            compose.waitUntil(5000){ repositoryDocument().optJSONObject("activeTimer") != null }
            compose.activityRule.scenario.recreate()
            compose.waitUntil(15000){compose.onAllNodesWithTag("screen-dashboard").fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithText("Take a break").performScrollTo().assertExists()
            screenshot("clock-running")
        } finally {
            repository.updateDocument{it.put("activeTimer",JSONObject.NULL)}
            repository.deleteShifts(records.map{it.id})
            repository.deleteSavingsGoal("visual-goal")
            repository.saveSettings(previous)
        }
    }
    @Test fun setupMatchesReferenceAndSavesSelections() = runBlocking {
        val originalSettings=repository.getSettings()
        val oldProfile=repository.profile.first()
        val originalDoc=repository.document()
        val oldPlaces=repository.workplaces.first().map{it.id}.toSet()
        repository.saveSettings(originalSettings.copy(theme="light"))
        repository.updateDocument{it.put("onboardingCompleted",false).put("language","en")}
        try {
            compose.waitUntil(10000){compose.onAllNodesWithTag("setup-step-0").fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithText("Restore a backup").assertIsDisplayed()
            compose.onNodeWithText("Your name").performTextInput("Fahad")
            screenshot("setup-welcome")
            compose.onNodeWithText("Get started →").performClick()
            compose.onNodeWithText("e.g. Burger King").performTextInput("Layout test job")
            compose.onNodeWithText("e.g. 14.50").performScrollTo().performTextInput("14.50")
            compose.onNodeWithText("e.g. 80").performScrollTo().performTextInput("80")
            compose.onNodeWithText("Workplace name").performScrollTo()
            screenshot("setup-workplace")
            compose.onNodeWithText("Choose your country").performScrollTo().performClick()
            compose.onNodeWithText("Search country").performTextInput("Germany")
            compose.onNode(hasText("Germany") and hasSetTextAction().not()).performClick()
            compose.onNodeWithText("III").performScrollTo().performClick()
            screenshot("setup-tax")
            compose.onNodeWithText("Next →").performClick()
            compose.onNodeWithText("30 min").performScrollTo().performClick()
            compose.onNodeWithText("7 h 30 min").performScrollTo().assertExists()
            screenshot("setup-shift")
            compose.onNodeWithText("Skip this step").performScrollTo().performClick()
            screenshot("setup-notifications")
            compose.onNodeWithText("Continue to app →").performClick()
            compose.waitUntil(15000){compose.onAllNodesWithTag("screen-dashboard").fetchSemanticsNodes().isNotEmpty()}
            Assert.assertEquals("III",repository.getSettings().taxclass)
            Assert.assertEquals("DE",repository.document().getJSONObject("settings").getString("country"))
            Assert.assertEquals("Fahad",repository.profile.first().name)
        } finally {
            repository.workplaces.first().filter{it.id !in oldPlaces}.forEach{repository.deleteWorkplace(it.id)}
            repository.saveSettings(originalSettings);repository.saveProfile(oldProfile)
            repository.updateDocument{d->d.put("onboardingCompleted",true);d.put("settings",originalDoc.optJSONObject("settings") ?: JSONObject());d.put("profile",originalDoc.optJSONObject("profile") ?: JSONObject())}
        }
    }
    private fun repositoryDocument() = runBlocking { repository.document() }

    @Test fun oldBackupPreservesTemplatesAndSettings() = runBlocking {
        val fixture="""{"settings":{"currency":"EUR","wage":15,"theme":"dark","country":"DE"},"shifts":[],"templates":[{"id":"parity-template","name":"Late","start":"18:00","end":"23:00","breakMin":15}],"shiftReminders":{"enabled":true,"amount":2,"unit":"hours","scope":"all"},"budgets":{"2026-09":{"total":500,"rent":200}},"language":"en","onboardingCompleted":true}"""
        Assert.assertTrue(repository.importBackupJson(fixture))
        val exported=JSONObject(repository.exportBackupJson()).getJSONObject("data")
        Assert.assertEquals("DE",exported.getJSONObject("settings").getString("country"))
        Assert.assertEquals(500,exported.getJSONObject("budgets").getJSONObject("2026-09").getInt("total"))
        Assert.assertEquals(2,exported.getJSONObject("shiftReminders").getInt("amount"))
        Assert.assertTrue(repository.shiftTemplates.first().any{it.id=="parity-template"})
        Assert.assertFalse(repository.importBackupJson("{\"notABackup\":true}"))
        val invalid=JSONObject(fixture).put("shifts",org.json.JSONArray().put(JSONObject().put("id","invalid-record").put("date","not-a-date").put("start","09:00").put("end","17:00").put("minutes",480)))
        Assert.assertFalse(repository.importBackupJson(invalid.toString()))
        Assert.assertFalse(repository.shifts.first().any{it.id=="invalid-record"})
        repository.deleteShiftTemplate("parity-template")
    }
}
