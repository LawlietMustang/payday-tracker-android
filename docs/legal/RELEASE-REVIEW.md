# Legal integration — review required before public release

Reviewed 8 October 2026 against the native source following v2.5.5. This is a technical/data-flow review and legal drafting aid, not a certification of worldwide compliance. Obtain jurisdiction-specific legal review before launch.

## Supplied file

The uploaded `Privacy_policy.md` is a Play Store pre-launch checklist, not a privacy notice. Its suggestion to select “no data collected” solely because the core app is offline is unsafe when optional Firebase authentication is shipped. Its suggestion to enable Android backup also differs from the manifest, which disables it. No analytics or architecture changes from that checklist were implemented.

## Documents and screens

The canonical documents are packaged in `app/src/main/assets/legal/`:

- [Privacy policy (English)](../../app/src/main/assets/legal/privacy-en.md)
- [Privacy policy (German)](../../app/src/main/assets/legal/privacy-de.md)
- [Terms (English)](../../app/src/main/assets/legal/terms-en.md)
- [Terms (German)](../../app/src/main/assets/legal/terms-de.md)

`ui/parity/LegalPage.kt` renders them natively and offline. Settings has both links. The first-launch screen uses the existing purple, lime, typography, cards and logo. Reading either document, changing language, rotating or pressing Back never accepts it. “Not now” closes without erasing data. Only the acceptance action stores acknowledgement. Normal app-version changes do not reset it.

`LegalAcceptance.kt` stores document versions, language and timestamp in installation-local preferences, outside Room and JSON exports. Restoring user data does not grant acceptance. Clearing Android app storage/reinstalling requires acceptance again. Version constants must change for the final documents and any later revision requiring acknowledgement. This is a local UX record, not tamper-proof server evidence. The button agrees to terms and acknowledges privacy; it is not blanket consent or a replacement for Android permissions or a separate sensitive-data consent where required.

The Google account panel now has explicit account deletion, confirmation of the current email, an error state, and a recent-login recovery action. Account deletion must be tested with the publisher's configured Firebase project before release. Local records are not keyed to Firebase UID and remain independent. Do not describe sign-out as deletion or sign-in as Drive synchronization.

## Information still needed from the publisher

Do not publish the draft documents or distribute this branch as a final legal release. All `{{...}}` fields must be replaced in both languages, and draft labels removed only after the review is complete.

| Missing decision/fact | Why it matters |
| --- | --- |
| Legal publisher/controller name, country and serviceable postal address | Identifies the contracting party and controller; store listing must agree. Do not infer these from a Git commit author. |
| Public privacy/support email and any required representative/DPO | Provides a real rights and support channel; omit an inapplicable DPO/representative field only after checking applicability. |
| Business/legal form, register/VAT details if applicable, consumer-dispute statement | Determine applicable publisher/consumer information duties, including German DDG/VSBG where relevant. |
| Distribution countries, age audience, free/paid model | Determine children's, consumer and regional requirements; the code has no billing flow but a paid store listing remains possible. |
| Firebase contracting entity, signed data-processing terms, transfer mechanism and account-deletion support process | Code alone does not establish the publisher's contractual arrangements or lawful transfer safeguards. |
| Support mailbox/provider and actual retention period | No invented retention promise; include legal-retention exceptions only when they actually apply. |
| Public policy, terms and external account-deletion URLs | In-app text alone does not satisfy Google Play's public URL/account-deletion requirements. URLs must work without login and identify WageTrack. |
| Competent supervisory authority | Depends on the controller's location; users also retain applicable local complaint rights. |

## Actual behavior to preserve in disclosures

- Room/private Android storage is not app-level encrypted. PIN verification and biometrics are access controls, not encryption of backup files.
- JSON/CSV exports can include financial and profile data and are unencrypted by WageTrack. Selected cloud file providers can receive them. Automatic backup uses current/previous files, while a provider may retain versions or trash.
- Optional Firebase Auth initializes when configured account controls are opened, and may process metadata. No financial database is uploaded by this integration. The currently distributed v2.5.5 APK lacked Google configuration; a later configured build requires an updated Data Safety assessment.
- No ad, analytics or Crashlytics SDK is present. The Android manifest disables automatic app backup. Do not declare SDK processing absent without checking the final release artifact and runtime behavior.
- Delete data resets active Room records and disables auto-backup, but preferences/auth/legacy storage can remain. The draft accurately directs a full private-storage erase through Android and distinguishes external files and online account deletion. Consider a future unified deletion UX separately.
- Tax ID is optional. Church-tax and other payroll fields need a specific assessment for special-category data and local-only processing. If explicit consent is necessary, add a separate optional control before that processing; this agreement is insufficient. Confirm the legal basis instead of treating contract as a blanket exception.
- No server processes support messages in app code; the publisher still needs a disclosed mailbox/provider, retention practice and rights-request procedure.

## Release checks

1. Fill publisher facts, settle applicability/market questions, have both language versions reviewed, and finalize the version/date in all four files and `LegalAcceptance.kt`.
2. Host public HTML policy/terms and an external account-deletion resource. This PR does not publish a website or invent live URLs. Verify links and the account-deletion request process outside the app.
3. Review the final APK's SDK behavior, permissions, Data Safety answers, Play financial-features/age declarations where applicable, Firebase processor agreement/transfer configuration and deletion retention. Optional account use does not justify a blanket “no collection” claim.
4. Test configured Google sign-in, deletion, recent-login recovery, offline failure and backend disappearance using a dedicated test account. No production account should be deleted by automated UI tests.
5. Run Android validation and `DeviceSmoke`, `UpgradeSmoke`, `LegalSmoke`. The new suite covers pre-accept reading/back, EN/DE readers, explicit acceptance, reopening, onboarding continuation, decline, Settings access, backup separation and document-version renewal. Test the final signed artifact on a physical device as well.
6. Update release/version notes before distribution. This draft deliberately does not change the production version or merge into main.

## Official references consulted

- GDPR, especially Articles 6, 9, 12–22 and international transfers: https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32016R0679
- Google Play User Data / privacy policy / account deletion: https://support.google.com/googleplay/android-developer/answer/10144311?hl=en
- Firebase processing, locations and retention: https://firebase.google.com/support/privacy
- Firebase Android account deletion and recent authentication: https://firebase.google.com/docs/auth/android/manage-users
- German publisher information, where applicable: https://www.gesetze-im-internet.de/ddg/__5.html
- German standard-terms/consumer rules: https://www.gesetze-im-internet.de/bgb/__307.html and https://www.gesetze-im-internet.de/bgb/__309.html
- Applicability of German digital-product provisions: https://www.gesetze-im-internet.de/bgb/__327.html

The scope review is Germany/EU-oriented because of the existing payroll model, not a statement that every intended market or legal requirement has been established.
