package com.paydaytracker.app

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import com.paydaytracker.app.data.DataMigration
import com.paydaytracker.app.data.WageRepository
import com.paydaytracker.app.data.db.WageTrackDatabase
import com.paydaytracker.app.ui.MainApp
import com.paydaytracker.app.ui.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var contentRoot: FrameLayout
    private lateinit var appLock: AppLock
    private val viewModel: MainViewModel by viewModels()
    private val repository by lazy { WageRepository(WageTrackDatabase.getInstance(this)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Window & status bar styling matching WageTrack theme tokens
        window.statusBarColor = Color.rgb(24, 14, 51)
        window.navigationBarColor = Color.rgb(24, 14, 51)
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
        }

        // Check and migrate data from legacy WebView localStorage if this is an upgrade (Phase 5)
        lifecycleScope.launch {
            DataMigration.checkAndMigrate(this@MainActivity, repository)
        }

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(24, 14, 51))
        }
        contentRoot = root

        val composeView = ComposeView(this).apply {
            setContent {
                MainApp(viewModel = viewModel)
            }
        }
        root.addView(composeView, FrameLayout.LayoutParams(-1, -1))

        // Wire native AppLock security overlay (Phase 4)
        appLock = AppLock(this, root, composeView) {
            // Callback when lock state changes
        }
        appLockInstance = appLock

        setContentView(root)

        // Handle payslip reminder deep-link intent if opened from notification
        handleIncomingIntent(intent)

        // Initial reconciliation of native alarm receivers
        ReminderReceiver.deliverDue(this)
        ReminderReceiver.schedule(this)
        BackupReminder.schedule(this)
        PayslipReminder.reconcile(this)
        ShiftReminders.reconcile(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val payslipMonth = intent?.getStringExtra(PayslipReminder.EXTRA_MONTH)
        if (!payslipMonth.isNullOrBlank()) {
            intent.removeExtra(PayslipReminder.EXTRA_MONTH)
            viewModel.setMonth(payslipMonth)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::appLock.isInitialized) appLock.resume()
        BackupReminder.schedule(this)
        PayslipReminder.reconcile(this)
        ReminderReceiver.deliverDue(this)
        ReminderReceiver.schedule(this)
        ShiftReminders.reconcile(this)
        PaydayWidget.update(this)
    }

    override fun onPause() {
        AutoBackup.get(this).flush()
        if (::appLock.isInitialized) appLock.pause()
        super.onPause()
    }

    override fun onDestroy() {
        if (::appLock.isInitialized) appLock.destroy()
        appLockInstance = null
        super.onDestroy()
    }

    companion object {
        var appLockInstance: AppLock? = null
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (::appLock.isInitialized && appLock.result(requestCode, resultCode)) return

        // AutoBackup SAF folder selection result (905)
        if (requestCode == 905 && resultCode == RESULT_OK && data?.data != null) {
            AutoBackup.get(this).configure(data.data!!, data.flags)
        }
    }
}
