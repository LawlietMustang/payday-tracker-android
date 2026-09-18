# Payday Tracker 2.2.4

- Android Back dismisses a dialog, drawer or shift selection first, then returns to the previous page. Only Back from Overview backgrounds the app. Supports Android 13+ Back callbacks and older devices.
- Monthly chart uses Monday-based ISO calendar weeks, including week-year boundaries and months spanning six weeks. Totals include only dates in the selected month.
- Bottom navigation keeps square, equally sized icons; corrected the euro drawing.
- Checkbox taps no longer show the outer focus rectangle. Keyboard focus remains visible.
- Google sign-in remains unavailable: this repository has no configured Google OAuth integration. The account section now states this clearly. Manual file backup/restore remains available; it is not automatic Google account recovery.

Payroll calculations are unchanged. Release uses the existing signing key and version code 22; install as an update without uninstalling.

Validation: browser regression tests cover navigation, dialog cancellation, calendar boundaries, touch focus and icon dimensions. Android emulator tests exercise actual system Back events alongside reminder and lock checks. Physical Xiaomi testing is still required.
