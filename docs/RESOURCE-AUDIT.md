# Resource audit

- Removed unreferenced `navy` and `white` Android colors.
- Moved the supplied future refund icon to `design/unused/refund.svg`; it is retained
  for future work and no longer increases the installed asset set.
- All remaining JavaScript files are loaded by index.html. The shared SVG files are
  source inputs for the embedded CSS masks; absence of a runtime filename is not dead code.
- `app_icon.xml` is the current native lock-screen logo, not an obsolete launcher.
  The adaptive launcher also uses both wagetrack foreground/background drawables.
- No committed build directories, private signing files or Google configuration found.
  Added Android Studio workspace files to .gitignore.
- No reflection resource lookups (`getIdentifier`) found in application sources.

CI runs `gradle -PresourceAudit :app:lintDebug` with UnusedResources as an error.
Keep this audit separate from feature and package moves to make cleanup reversible.
