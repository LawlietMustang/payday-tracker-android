// Optional account connection never gates offline tracking or uploads financial data.
function renderGoogleAccount(code=''){
 const native=typeof Android!=='undefined'&&typeof Android.googleAccountState==='function',s=native?JSON.parse(Android.googleAccountState()):{configured:false};
 const card=q('.account-unavailable');
 card.innerHTML='<strong>'+msg('Google-Konto','Google account')+'</strong><p id="googleAccountInfo"></p><button id="googleSignIn" type="button" class="secondary"></button><p id="googleAccountStatus" role="status"></p><small>'+msg('Anmelden ist freiwillig. Arbeitszeiten und Finanzdaten bleiben auf diesem Gerät. Google-Drive-Synchronisierung ist nicht enthalten.','Sign-in is optional. Work hours and financial data stay on this device. Google Drive sync is not included.')+'</small>';
 q('#googleAccountInfo').textContent=s.email?s.email:s.configured?msg('Verbinde dein Google-Konto.','Connect your Google account.'):msg('Google-Anmeldung wartet auf die Einrichtung durch den App-Entwickler.','Google sign-in is awaiting setup by the app developer.');
 const button=q('#googleSignIn');button.textContent=s.email?msg('Abmelden','Sign out'):msg('Mit Google anmelden','Sign in with Google');button.disabled=!s.configured||!!s.busy;
 button.onclick=()=>s.email?Android.googleSignOut():Android.googleSignIn();
 const texts={busy:['Anmeldung läuft …','Signing in…'],signedIn:['Angemeldet','Signed in'],signedOut:['Abgemeldet. Deine lokalen Daten bleiben erhalten.','Signed out. Your local data is kept.'],cancelled:['Anmeldung abgebrochen','Sign-in cancelled'],setup:['Google-Anmeldung ist noch nicht eingerichtet.','Google sign-in is not configured yet.'],error:['Anmeldung fehlgeschlagen. Prüfe Internet und Google Play-Dienste und versuche es erneut.','Sign-in failed. Check your internet connection and Google Play services, then try again.'],verifyError:['Google-Anmeldung konnte nicht bestätigt werden. Prüfe die Firebase-Einrichtung und versuche es erneut.','Google sign-in could not be verified. Check the Firebase setup and try again.']};
 q('#googleAccountStatus').textContent=texts[code]?msg(...texts[code]):'';
}
// Replace the older setup-only renderer so language changes keep the real account controls.
explainSignIn=renderGoogleAccount;
window.googleAccountChanged=renderGoogleAccount;
renderGoogleAccount();
