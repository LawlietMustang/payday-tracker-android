# 2.2.6 — Shift reminder delivery repair

Fixed a confirmed 2.2.5 bug: reminders due while notifications were blocked were marked sent even though Android never displayed them. They now remain pending and are retried on permission grant or app resume while the shift is still upcoming. No repeating alarm loop is created while blocked.

The Upcoming shifts card now displays the next reminder time and count, disabled state, blocked notification state, no matching planned shifts, or already-delivered state. Native scheduling failures are reported. A dedicated test uses the actual upcoming-shift notification channel, rather than the separate logging reminder channel.

After upgrading, open Menu → Reminders → Upcoming shifts. Check permissions, use Test shift notification, and tap Reschedule upcoming reminders once to repair any delivery flags saved by 2.2.5. This action asks for confirmation and may repeat an already delivered reminder for an upcoming shift within its lead time. Past shifts are never notified.

Android regression tests deny notifications, create a due reminder, verify it is not marked sent, restore permission, and verify delivery. Existing background alarm, cancellation, navigation, reminder and app-lock checks remain enabled. Xiaomi battery restrictions can still delay alarms; use the existing background and precise-time settings controls.

Version code 24, existing signing key. Payroll and Google sign-in configuration unchanged.
