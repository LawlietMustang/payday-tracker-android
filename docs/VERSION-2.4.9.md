# WageTrack 2.4.9

## Expected income and actual work

`summary()` includes completed and planned shifts, excluding cancelled shifts.
The overview's available balance and Expenses use this expected net income.
`summary(month, filter, cutoff, false)` explicitly uses completed shifts only:
earned-to-date cards, worked hours, progress bars, monthly work history, and
automatic savings contributions use this mode. Planned work cannot fund savings.

The forecast starts with exact recorded wages and bonuses. It adds an average
only for remaining preferred weekdays with no shift record. Planned, completed,
and cancelled dates all prevent inferred work on that date. No scheduled shift
is added twice, and different recorded hourly rates are preserved.

## Payslips and widget

- History includes a 12-month net estimate/actual chart, ending at the most recent
  recorded payslip. Missing months stay blank. Both lines include the same
  workplaces for each month; exact values remain in the comparison rows.
- The idle widget shows completed hours against the monthly target. Hours refresh
  when data is saved; stale prior-month hours are hidden on month rollover.
- A next-shift line appears for a planned shift within 48 hours, independently
  of notification settings. Android's periodic widget refresh updates relative
  dates even while the app is closed. App lock hides all time/shift details.
- Reminders has an optional payslip-entry switch, off by default. Missing previous
  month payslips are checked from the 7th at 10:00 local time. First activation or
  a newly missing record gets at least two days' grace. Retries are at most weekly.
  No notification is sent when every relevant workplace has a payslip, no shift
  exists for that month, notifications are blocked, or the switch is off.
- Tapping the reminder opens History with the missing month selected. Saving the
  payslip cancels the reminder. Reboot, app update and month rollover re-evaluate
  the saved local configuration without needing the WebView to stay alive.

## Icons and theme

Supplied profile, lock, cloud-sync, cancel, alert and info assets use the existing
CSS mask pattern. Appearance keeps the System/Light/Dark selector and shows the
supplied sun/moon icon. Refund artwork is retained for the future refund feature.
The static theme-color now matches the purple canvas. All styling stays in
`design.css`; there are no new runtime stylesheet injections.

## Checks

`node tests/planning.cjs` includes the new calculation, chart, icon, compact
reminder control and navigation regressions. `DeviceSmoke` exercises actual
RemoteViews, next-shift privacy, month rollover, notification permission denial,
payslip reminder cadence, and cancellation after saving a payslip.
