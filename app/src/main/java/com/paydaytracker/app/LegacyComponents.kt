package com.paydaytracker.app

// Android persists these names in launcher shortcuts, widgets and PendingIntents.
// Keep the component identities; implementation belongs in feature packages.
class BackupReminder : com.paydaytracker.app.reminders.BackupReminder()
class MainActivity : com.paydaytracker.app.ui.MainActivity()
class PaydayWidget : com.paydaytracker.app.widget.PaydayWidget()
class PayslipReminder : com.paydaytracker.app.reminders.PayslipReminder()
class ReminderReceiver : com.paydaytracker.app.reminders.ReminderReceiver()
class ShiftReminderReceiver : com.paydaytracker.app.reminders.ShiftReminderReceiver()
class TimerNotificationService : com.paydaytracker.app.service.TimerNotificationService()
