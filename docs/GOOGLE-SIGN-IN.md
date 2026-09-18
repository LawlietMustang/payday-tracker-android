# Enable Google sign-in

Version 2.2.5 includes native Credential Manager and Firebase Authentication. An unconfigured build keeps the sign-in button disabled. It cannot authenticate until the app owner registers the app with Google. Do not use another project's client ID.

## One-time setup by the app owner

1. Open https://console.firebase.google.com/ and create or select your project. Google Analytics is not required.
2. Add an Android app with package name `com.paydaytracker.app`.
3. Register the permanent release certificate SHA-1:
   `4B:EE:4F:5C:62:A1:0E:B8:94:61:18:6D:83:61:FF:CC:3E:C8:7D:DE`
   This is a public certificate fingerprint, not a private signing key. Keep the current signing key.
4. Under Authentication → Sign-in method, enable Google and choose the support email.
5. Complete the Google Auth Platform branding/audience setup. If the project is restricted to test users, add your Google account there.
6. Download the updated **google-services.json** from Firebase project settings after enabling Google. It must contain the Android client and a Web OAuth client (`client_type: 3`).
7. In GitHub → LawlietMustang/payday-tracker-android → Settings → Secrets and variables → Actions, create a repository secret named `GOOGLE_SERVICES_JSON`. Paste the complete file contents as its value. No service-account key or OAuth client secret is needed.
8. Run the **Build Android APK** workflow again. It places the configuration only in the release assets. Download and install the signed APK over the existing app.
9. Open Settings → Google account → Sign in with Google. Choose an account and complete consent. Check the displayed email; sign out and sign in again to confirm.

For local release builds, put the file at `app/src/release/assets/google-services.json`. This file is excluded from Git. A debug build needs its own registered `.debug` package and debug certificate plus configuration at `app/src/debug/assets/google-services.json`; never change the production signing key to make debug authentication work.

## What sign-in does

Firebase verifies Google's ID token before the app reports success. Tokens stay in the native authentication SDK and never enter JavaScript, backups, or logs. The account session is optional; tracking and reminders remain available offline. Sign-out leaves local work and expense records intact.

This is account authentication, **not Google Drive backup or automatic data recovery**. No work hours, expenses, payslips, or profile form data are uploaded by this integration. Those features still use the existing manual file backup/restore flow. Deleting local app data does not delete the remote Firebase account.

## Troubleshooting

- Disabled button: missing/invalid configuration, wrong package, or missing Web OAuth client. Download the updated file after enabling Google.
- Account picker fails: check Google Play services, internet access, consent/test-user settings, package and certificate registration.
- Verification fails: check that Google is enabled in Firebase Authentication and the API key restrictions permit that project's authentication APIs.
- Debug build: register its separate package and certificate. The production fingerprint above only applies to the signed release APK.

References: [Firebase Google authentication](https://firebase.google.com/docs/auth/android/google-signin), [Credential Manager implementation](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation).
