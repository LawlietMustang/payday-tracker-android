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
        val image=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val dir=File(compose.activity.getExternalFilesDir(null), "native-screens").apply { mkdirs() }
        File(dir,"$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) }
        image.recycle()
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
    @Test fun oldBackupPreservesTemplatesAndSettings() = runBlocking {
        val fixture="""{"settings":{"currency":"EUR","wage":15,"theme":"dark","country":"DE"},"shifts":[],"templates":[{"id":"parity-template","name":"Late","start":"18:00","end":"23:00","breakMin":15}],"shiftReminders":{"enabled":true,"amount":2,"unit":"hours","scope":"all"},"budgets":{"2026-09":{"total":500,"rent":200}},"language":"en","onboardingCompleted":true}"""
        Assert.assertTrue(repository.importBackupJson(fixture))
        val exported=JSONObject(repository.exportBackupJson()).getJSONObject("data")
        Assert.assertEquals("DE",exported.getJSONObject("settings").getString("country"))
        Assert.assertEquals(500,exported.getJSONObject("budgets").getJSONObject("2026-09").getInt("total"))
        Assert.assertEquals(2,exported.getJSONObject("shiftReminders").getInt("amount"))
        Assert.assertTrue(repository.shiftTemplates.first().any{it.id=="parity-template"})
        Assert.assertFalse(repository.importBackupJson("{\"notABackup\":true}"))
        repository.deleteShiftTemplate("parity-template")
    }
}
