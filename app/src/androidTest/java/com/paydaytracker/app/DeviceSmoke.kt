package com.paydaytracker.app

import androidx.compose.ui.test.*
import android.graphics.Bitmap
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
        compose.onNodeWithContentDescription("Menu").performClick()
        compose.onNodeWithText("Settings",useUnmergedTree=true).performClick()
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
        compose.onNodeWithText("Start (HH:mm)").performTextReplacement("09:00")
        compose.onNodeWithText("End (HH:mm)").performTextReplacement("17:00")
        compose.onNodeWithText("Unpaid break (minutes)").performTextReplacement("30")
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
            compose.onNodeWithContentDescription("Menu").performClick()
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
