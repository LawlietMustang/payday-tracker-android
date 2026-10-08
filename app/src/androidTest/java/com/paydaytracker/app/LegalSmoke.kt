package com.paydaytracker.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.paydaytracker.app.data.WageRepository
import com.paydaytracker.app.data.db.WageTrackDatabase
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LegalSmoke {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val repository get() = WageRepository(WageTrackDatabase.getInstance(context))

    @Before fun freshAgreement() = runBlocking {
        context.getSharedPreferences(LegalAcceptance.PREFS, 0).edit().clear().commit()
        repository.updateDocument { it.put("language", "en").put("onboardingCompleted", true) }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("legal-welcome").fetchSemanticsNodes().isNotEmpty() }
    }

    @After fun leaveOtherTestsIndependent() { LegalAcceptance.accept(context, "en") }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        Thread.sleep(250)
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        fun shell(command: String) = automation.executeShellCommand(command).use {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes()
        }
        shell("mkdir -p /sdcard/Download/native-screens")
        shell("screencap -p /sdcard/Download/native-screens/$name.png")
    }

    @Test fun documentsReadableBeforeAcceptanceAndBackDoesNotAccept() {
        compose.onNodeWithTag("open-privacy").assertIsDisplayed()
        compose.onNodeWithTag("open-terms").assertIsDisplayed()
        compose.onNodeWithTag("legal-draft-notice").assertIsDisplayed()
        compose.onNodeWithTag("accept-legal").assertIsDisplayed()
        screenshot("legal-welcome")
        for (kind in listOf("privacy", "terms")) {
            compose.onNodeWithTag("open-$kind").performScrollTo().performClick()
            compose.onNodeWithTag("legal-$kind").assertIsDisplayed()
            screenshot("legal-$kind")
            Assert.assertFalse(LegalAcceptance.isAccepted(context))
            compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            compose.onNodeWithTag("accept-legal").assertIsDisplayed()
        }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("accept-legal").fetchSemanticsNodes().isNotEmpty() }
        Assert.assertFalse(LegalAcceptance.isAccepted(context))
        compose.onNodeWithTag("legal-language").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Datenschutzerklärung").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("open-privacy").performScrollTo().performClick()
        compose.onNodeWithText("WageTrack Datenschutzerklärung").assertExists()
        compose.onNodeWithTag("legal-back").performClick()
        compose.onNodeWithTag("open-terms").performScrollTo().performClick()
        compose.onNodeWithText("WageTrack Nutzungsbedingungen").assertExists()
    }

    @Test fun acceptanceSurvivesRestartAndSettingsKeepsDocumentsAccessible() = runBlocking {
        compose.onNodeWithTag("accept-legal").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("screen-dashboard").fetchSemanticsNodes().isNotEmpty() }
        Assert.assertTrue(LegalAcceptance.isAccepted(context))
        val acceptedAt = context.getSharedPreferences(LegalAcceptance.PREFS, 0).getLong("acceptedAt", 0)
        Assert.assertFalse(repository.exportBackupJson().contains("acceptedAt"))
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("screen-dashboard").fetchSemanticsNodes().isNotEmpty() }
        Assert.assertEquals(acceptedAt, context.getSharedPreferences(LegalAcceptance.PREFS, 0).getLong("acceptedAt", 0))
        compose.onNodeWithContentDescription("Menu").performScrollTo().performClick()
        compose.onNodeWithText("Settings", useUnmergedTree = true).performClick()
        for ((label, kind) in listOf("Privacy policy" to "privacy", "Terms & conditions" to "terms")) {
            compose.onNodeWithText(label).performScrollTo().performClick()
            compose.onNodeWithTag("legal-$kind").assertIsDisplayed()
            compose.onNodeWithTag("legal-back").performClick()
            compose.onNodeWithTag("screen-settings").assertExists()
        }
    }

    @Test fun firstInstallationContinuesToExistingSetup() = runBlocking {
        repository.updateDocument { it.put("onboardingCompleted", false) }
        compose.onNodeWithTag("accept-legal").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("setup-step-0").fetchSemanticsNodes().isNotEmpty() }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("setup-step-0").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithTag("legal-welcome").assertCountEquals(0)
        repository.updateDocument { it.put("onboardingCompleted", true) }
    }

    @Test fun declineClosesWithoutSavingAcceptance() {
        compose.onNodeWithTag("decline-legal").performClick()
        Assert.assertFalse(LegalAcceptance.isAccepted(context))
        compose.waitUntil(10000) { compose.activityRule.scenario.state == androidx.lifecycle.Lifecycle.State.DESTROYED }
    }

    @Test fun restoredDocumentCannotAcceptAndChangedTermsRequireNewAgreement() = runBlocking {
        // A restored user document may contain old onboarding flags, but cannot write installation preferences.
        val backup = repository.exportBackupJson()
        Assert.assertTrue(repository.importBackupJson(backup))
        Assert.assertFalse(LegalAcceptance.isAccepted(context))
        compose.onNodeWithTag("accept-legal").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("screen-dashboard").fetchSemanticsNodes().isNotEmpty() }
        context.getSharedPreferences(LegalAcceptance.PREFS, 0).edit().putString("termsVersion", "outdated").commit()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15000) { compose.onAllNodesWithTag("legal-welcome").fetchSemanticsNodes().isNotEmpty() }
        Assert.assertFalse(LegalAcceptance.isAccepted(context))
    }
}
