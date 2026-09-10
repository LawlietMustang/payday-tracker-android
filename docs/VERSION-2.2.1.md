# Version 2.2.1

- App lock now has a dedicated drawer entry. Its switch opens immediately, without scrolling past widgets and reminders. A notice explains when the phone needs a screen PIN/password first.
- Reminders show their next scheduled date/time, notification permission (including a disabled reminder channel), and precise-time permission status.
- Send test notification checks delivery without waiting for the next scheduled day. Android permission/settings links are accessible from the reminder page.
- User-granted Alarms & reminders access enables exact alarms. Without this permission the app retains inexact scheduling and explains possible delays.
- A delayed occurrence from the past 24 hours is checked when reopening the app, before the next occurrence replaces it. Each occurrence is claimed once to avoid duplicate alerts.
- Existing weekday and time settings are preserved. Application ID and permanent signing key are unchanged; versionCode increases to 19.

Xiaomi background restrictions can still prevent delivery. The page includes a link to app settings and guidance to inspect background activity, autostart and battery saving. This update cannot change phone permissions without the user.

Validation includes browser navigation and responsive checks, Android PIN authentication, and a real scheduled notification while the test app is backgrounded. Physical Xiaomi verification remains necessary.
