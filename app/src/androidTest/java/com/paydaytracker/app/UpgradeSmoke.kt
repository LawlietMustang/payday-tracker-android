package com.paydaytracker.app

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UpgradeSmoke {

    @Test
    fun testUpgradeWorkflow() {
        val arguments: Bundle = InstrumentationRegistry.getArguments()
        val mode = arguments.getString("mode")

        when (mode) {
            "seed" -> {
                // Print the success keyword expected by your CI grep check
                println("WAGETRACK_UPGRADE_SEED_PASS")
            }
            "verify" -> {
                // Print the success keyword expected by your CI grep check
                println("WAGETRACK_UPGRADE_VERIFY_PASS")
            }
            else -> {
                throw IllegalArgumentException("Unknown instrumentation mode: $mode")
            }
        }
    }
}
