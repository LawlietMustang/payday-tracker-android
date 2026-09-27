package com.paydaytracker.app

import android.app.Dialog
import android.view.View
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paydaytracker.app.data.db.WageTrackDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Native Compose smoke coverage. Run on a fresh debug emulator, not a user's data. */
@RunWith(AndroidJUnit4::class)
class DeviceSmoke {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun navigate(label: String) {
        compose.onNode(hasText(label) and isSelectable()).performClick()
    }

    @Test
    fun mainNavigationAndProfileBackWork() {
        navigate("Schichten")
        compose.onNodeWithText("Kalender").assertIsDisplayed()
        navigate("Ausgaben")
        navigate("Planung")
        navigate("Einstellungen")
        compose.onNodeWithText("Persönliches Profil").performClick()
        compose.onNodeWithContentDescription("Zurück").performClick()
        compose.onNodeWithText("Arbeitsplätze verwalten").assertIsDisplayed()
        navigate("Übersicht")
        compose.onNodeWithText("GESCHÄTZTES NETTO").assertIsDisplayed()
    }

    @Test
    fun shiftEnteredInUiSurvivesActivityRecreation() {
        val note = "DeviceSmoke-${UUID.randomUUID()}"
        val dao = WageTrackDatabase.getInstance(compose.activity).shiftDao()
        fun saved() = runBlocking(Dispatchers.IO) { dao.getAll().firstOrNull { it.note == note } }
        try {
            navigate("Schichten")
            compose.onNodeWithContentDescription("Schicht hinzufügen").performClick()
            compose.onNode(hasText("Beginn (HH:mm)") and hasSetTextAction())
                .performScrollTo().performTextReplacement("09:00")
            compose.onNode(hasText("Ende (HH:mm)") and hasSetTextAction())
                .performScrollTo().performTextReplacement("17:00")
            compose.onNode(hasText("Pause (Minuten)") and hasSetTextAction())
                .performScrollTo().performTextReplacement("30")
            compose.onNode(hasText("Notiz (optional)") and hasSetTextAction())
                .performScrollTo().performTextReplacement(note)
            compose.onNode(hasText("Speichern") and hasClickAction()).performClick()
            compose.waitUntil(timeoutMillis = 10_000) { saved() != null }
            val before = saved()!!
            assertEquals(450, before.minutes)
            assertEquals(30, before.breakMin)
            compose.activityRule.scenario.recreate()
            compose.waitForIdle()
            assertEquals(before, saved())
            navigate("Schichten")
            compose.onNodeWithText("Kalender").assertIsDisplayed()
        } finally {
            // Remove only the record created by this test.
            runBlocking(Dispatchers.IO) { saved()?.let { dao.deleteById(it.id) } }
        }
    }

    @Test
    fun cancellingPinSetupDoesNotEnableLock() {
        navigate("Einstellungen")
        compose.onNodeWithText("App-Sperre & PIN").performClick()
        compose.runOnIdle {
            val lock = MainActivity.appLockInstance
            assertNotNull(lock)
            val field = AppLock::class.java.getDeclaredField("pinDialog").apply { isAccessible = true }
            val dialog = field.get(lock) as? Dialog
            assertNotNull("PIN setup did not open", dialog)
            val back = dialog!!.window!!.decorView.findViewWithTag<View>("pin-back")
            assertNotNull(back)
            back.performClick()
            assertFalse("Cancelling setup enabled the lock", lock!!.enabled)
        }
    }
}
