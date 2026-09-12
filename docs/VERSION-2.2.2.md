# 2.2.2 — Profile and Settings

Profile now contains optional name, address, email and phone fields, plus access to the existing pay, work-target and tax information. Records use the existing offline store without replacing older shifts or settings.

Settings groups App appearance (language and light/dark/system mode), App lock, Delete data and Sign In. App lock uses the existing native authentication. Sign In is explicitly unavailable: no account or cloud service exists in this offline version.

The delete action requires confirmation. On Android it requests the operating system's own clear-application-data operation, which closes the app and resets private data. Exported files outside app storage remain. On browser preview it removes only this app's known storage keys.

Month/workplace controls are hidden on these personal/settings screens. English/German labels and dark mode are supported. Version code 20 retains the permanent release signing configuration and package ID.

Validation includes profile persistence, responsive personal/settings screens, cancellation and confirmation of deletion through a native mock, and the existing native app-lock tests via the new Settings route. Destructive clearing is not exercised on a user's phone. Sign-in implementation remains future work.
