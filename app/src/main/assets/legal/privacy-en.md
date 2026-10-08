# WageTrack Privacy Policy

Review draft — 8 October 2026. Not ready for public release: the publisher must complete the marked details and the release review. Document version: 2026-10-08-draft1.

## 1. Who is responsible

WageTrack (Android package com.paydaytracker.app) is provided by {{PUBLISHER_LEGAL_NAME}}, {{PUBLISHER_POSTAL_ADDRESS}}, {{PUBLISHER_COUNTRY}}. Privacy and support contact: {{PRIVACY_EMAIL}}. Where applicable: {{REPRESENTATIVE_OR_DATA_PROTECTION_OFFICER_CONTACT}}.

This policy explains the app's local records, optional Google sign-in, backups and support. Distribution platforms and storage providers also process information under their own policies.

## 2. Your records stay on your device by default

WageTrack stores the information you enter in its private Android storage: workplaces, wage rates, shifts, dates, working and break times, shift status, notes, templates, expenses, budgets, savings goals, contributions and payslip comparison amounts. It uses these records to display your work history, calculate estimates and provide the features you select.

Optional profile fields include name, address, email, phone number and tax ID. Payroll settings can include tax class, state, insurance contribution settings, childless status and church-tax preference. Church-tax information may reveal religious affiliation. Leave unnecessary personal fields blank and use manual deductions if you do not want to enter detailed payroll settings. These fields are not sent to Firebase by the app. Do not enter another person's sensitive information without authority.

The app also stores your language, theme, currency, notification preferences, active timer and backup settings. A one-time agreement record stores the document versions, selected language and device timestamp locally. It is not an advertising identifier or an account and is not included in the app's exported backup.

WageTrack has no advertising, analytics or automatic crash-reporting SDK in this implementation. It does not sell your information or upload your shift, expense or profile database to a publisher server. Its calculations are local estimates, not automated decisions determining your employment, credit or legal rights.

## 3. Optional account features

Where Google sign-in is configured, the account controls initialize Firebase Authentication. Connecting an account sends Google credentials to Firebase for verification. The account can contain your Google-linked name, email, identifier and sign-in metadata. Firebase also processes connection information such as IP address and user agent for authentication and abuse prevention. The app does not receive your Google password. Opening account controls in a configured build can initialize this service even before you sign in.

Google/Firebase is the authentication provider; the publisher administers the Firebase project. The relevant contracting entity and processing terms are {{FIREBASE_CONTRACTING_ENTITY_AND_PROCESSOR_DETAILS}}. Authentication is optional and is separate from the records stored on your device. Sign-in does not enable automatic Drive synchronization or upload financial records.

Firebase Authentication operates in the United States. International transfers must use the safeguards applicable to the publisher's agreement, identified here before release: {{INTERNATIONAL_TRANSFER_SAFEGUARDS_AND_HOW_TO_OBTAIN_COPY}}. Google's current documentation describes its security practices, processing locations and applicable transfer arrangements:

https://firebase.google.com/support/privacy

https://policies.google.com/privacy

## 4. Backups, exports and sharing

You choose whether and where to export a JSON backup or CSV data, restore a file, or enable automatic folder backups. The Android file picker may offer a local location or a cloud provider such as Google Drive. That provider receives files you save there and handles them under its own policy. The app does not obtain unrestricted access to your Google Drive account.

Automatic folder backup keeps a current WageTrack-backup.json and a previous WageTrack-previous.json, with temporary files used while safely writing a replacement. Manual exports can create additional files. The selected provider may keep its own versions or trash copies. Folder permission persists until revoked or the app is removed; automatic writes stop when you disable automatic backup.

Backups can contain your financial and optional profile information. JSON and CSV exports are not encrypted by WageTrack. App lock does not protect copies outside the app. Choose a trusted location and share only with intended recipients. Disabling backup, deleting app records or uninstalling does not delete previously exported files. Remove those files, previous copies and provider trash separately. Restoring a backup can restore information you previously deleted.

## 5. Device features and security

Notifications and exact alarms support reminders you select. Android permission requests are separate from the agreement screen. You can change these permissions in Android Settings. Restart and time-change receivers reschedule reminders; a foreground notification supports an active work timer. Notifications and widgets may reveal work information to people who can see your screen, depending on your device settings.

Biometric or device-credential verification is performed by Android. WageTrack receives an authentication result, not a fingerprint or face template. An optional app PIN is stored as a salted verifier protected by an Android Keystore key, not as the entered digits. These controls restrict access; they do not encrypt the Room database or exported files. The app relies on Android's private storage and device protections for local data and uses HTTPS for Firebase communication. No security measure can guarantee protection of a compromised device.

Android automatic app backup is disabled in this implementation. The app does not request contacts, location, microphone or camera access. A hidden, local-only migration component may read records left by older WebView-based versions; it is not a web analytics service.

## 6. Purposes and legal grounds

Where data-protection law applies to the publisher's processing, requested app/account functions and related support use the contractual basis under GDPR Article 6(1)(b). Necessary security and abuse prevention rely on Article 6(1)(f), with protection of the service and users as the interest. Legal duties use Article 6(1)(c). Any optional processing requiring consent must have a separate, specific choice under Article 6(1)(a). Acknowledging this notice is not that consent. The publisher must confirm these grounds and assess sensitive payroll fields before release.

Core tracking needs no account or personal identity. Supplying the fields required for a particular feature is voluntary, but that feature may not work without them. The publisher has no remote access to records kept solely on your device.

## 7. Retention and deletion

Local records remain until you remove them. Settings → Delete data removes the active app records and disables automatic backup. Some device preferences, the agreement record, authentication state and legacy migration storage can remain. Android Settings → Apps → WageTrack → Storage → Clear storage, or uninstalling, removes app-private storage. Neither action deletes an online sign-in account or external files.

Settings → Google account → Delete sign-in account requests deletion of the WageTrack Firebase account. Recent sign-in may be required for security. This does not delete your Google account. Local tracking records are independent of the sign-in account and remain until you delete them separately. An external deletion request is available at {{PUBLIC_ACCOUNT_DELETION_URL}} or through {{PRIVACY_EMAIL}}; the publisher verifies account ownership without requesting your password.

Google reports that Authentication logs IP addresses for a few weeks and removes other authentication information from live and backup systems within 180 days after account deletion is initiated. Independently held Google-account and file-provider information follows those providers' retention rules.

If you contact support, the publisher receives the contact details, message and attachments you choose to send. Avoid sending payslips, tax IDs or backups unless needed. Support provider and retention: {{SUPPORT_PROVIDER_AND_RETENTION_PERIOD}}. Any legally required additional retention must be limited and explained to you.

## 8. Your choices and rights

Depending on applicable law, you may request access, correction, deletion, restriction or portability, object to processing based on legitimate interests, and withdraw consent prospectively. Contact the privacy address above. You may complain to a supervisory authority, including where you live or work; the publisher's competent authority is {{SUPERVISORY_AUTHORITY}}. For device-only records, use the app's edit, export and deletion controls; the publisher cannot retrieve them remotely.

## 9. Children and changes

The intended audience and any age restrictions must be confirmed before distribution: {{INTENDED_AGE_GROUP_AND_MARKETS}}. The app must not be marketed to children or treat a child's agreement as sufficient for optional online processing without the safeguards required in the relevant country.

This notice is available offline in Settings. Material changes will be identified in an updated version; separate consent will be requested if legally necessary. Public policy URL: {{PUBLIC_PRIVACY_POLICY_URL}}. New purposes are not authorized merely because a notice was updated.
