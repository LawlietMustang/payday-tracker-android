# Native parity with v2.4.10

Reference: original signed v2.4.10 APK assets (commit 9097473).

## Implementation
- Shared purple canvas, light/dark cards, lime actions, supplied SVG vector icons, floating bottom navigation, drawer and safe-area header.
- Overview: available after expenses, next shift, shortcuts, work/break timer, pay breakdown, payslip check and expandable earnings/progress/recent shifts.
- Hours: calendar, day chooser, templates, multi-date entry, auto/manual statuses, cancellation reason and batch selection.
- Expenses: all workplaces combined, edit/delete, recurring rules and custom categories.
- History: monthly summaries and workplace payslip comparisons.
- Planning: savings rings, goal editing, contributions, recurrence and monthly budget.
- Settings: profile, workplaces, pay/tax/bonuses, appearance, language/currency, reminders/widget/lock, backup and deletion.
- First launch: compact four-step native setup with fixed restore action.
- Persistence: additive Room migration; transactional backup import; original data and unknown fields preserved.

## Verification
Native compile, calculation/CSV unit tests and resource checks run in `Validate Android and UI`.
`Android device smoke test` runs five assertion-based instrumentation tests and captures real native screens. The suite covers navigation, saving, activity recreation, populated earnings/goals, an active timer, backup round-tripping, invalid import rejection and legacy localStorage migration. See `DEVICE-TESTS.md` for the command and scope.

Emulator validation passed on the restoration branch on 28 September 2026. Every subsequent code revision must pass the same checks before release. PR #7 records the per-commit results and screenshot artifacts.

Physical Xiaomi notification delivery, biometric hardware, widget pinning and Google sign-in require device/account checks. Google sign-in needs valid owner configuration; choosing Drive in the Android file picker remains available independently. The migration fixture does not substitute for an APK-over-APK upgrade test on a real user's installation.
