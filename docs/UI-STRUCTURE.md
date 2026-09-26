# WageTrack UI structure

## Styling

`app/src/main/assets/web/styles/design.css` is the only stylesheet loaded by `index.html`.
It loads before the page renders; feature scripts must not append stylesheet links.
The old base and feature stylesheets have been retired. Their required layout rules
were consolidated, superseded declarations removed, and the old blue theme removed.

Edit the tokens at the top for shared colors, spacing and corner radii. The default
appearance uses cream cards on the purple canvas; `[data-theme="dark"]` overrides
card and field tokens. Card-scoped `--text`, `--muted` and `--line` keep nested forms
readable. Check both appearances when editing these tokens.

Find the existing selector and edit it instead of adding another override at the
end. Keep breakpoint rules next to the relevant component when adding new styles.
`workplaces.svg`, `budgets-goals.svg` and `reminders-widget.svg` are referenced as
source inputs for embedded CSS masks, so shapes inherit the surrounding text color.
Run `python3 scripts/embed-icon-masks.py` after editing SVG source artwork. External
file masks are blocked in the offline WebView.

## Navigation and screen order

`menu.js` owns `APP_ROUTES`, the ordered `DRAWER_ROUTES`, `SETTINGS_ROUTES` and
`SHORTCUT_ROUTES`. Labels and destinations come from the same catalog, including
English/German variants and device sub-panels. Feature files own their screen
contents; they do not append navigation buttons.

The drawer contains the four main screens and Settings. Settings provides direct
rows for Profile, Pay & tax, Workplaces, Budgets & goals, Reminders & widget, App
lock, Backup & restore, Appearance and Delete data. Optional Google account
controls remain beneath the settings rows. Pay & tax is no longer nested in Profile.

`navigation.js` owns Back history; `menu.js` calls the same `show()` entry point and
sets the device sub-panel before navigation. Dashboard shortcuts use these routes
too. `design.js` places those shortcuts immediately after the overview hero,
before the notice and time clock. Earnings stays before Payslip Check.

## Verification

Run `node tests/planning.cjs` with the workflow's Playwright version and Chromium
installed. `tests/layout-review.cjs` covers the single stylesheet, shared menu,
settings destinations, Back history, asset icons, languages and responsive widths.
The existing suite covers forms, onboarding, reminders, backups and card alignment.
The Android device workflow also checks actual painted icon pixels in the offline WebView.
See REPOSITORY-GUIDE.md for the full folder map and build commands.
