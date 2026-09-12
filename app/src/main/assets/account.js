// Personal details stay in the existing offline data store.
function accountField(key,de,en,type='text') {
 return '<label>'+msg(de,en)+'<input id="profile_'+key+'" type="'+type+'" maxlength="160" value="'+safe(data.profile?.[key]||'')+'"></label>';
}
function renderProfile(){
 q('#profile').innerHTML='<form id="profileForm" class="account-stack"><article class="panel"><h2>'+msg('Persönliche Angaben','Personal details')+'</h2><p class="muted">'+msg('Alle Angaben sind freiwillig und bleiben auf diesem Gerät.','All details are optional and stay on this device.')+'</p>'+accountField('name','Name','Name')+'</article><article class="panel"><h2>'+msg('Adresse','Address')+'</h2>'+accountField('street','Straße und Hausnummer','Street and house number')+'<div class="account-grid">'+accountField('postcode','Postleitzahl','Postcode')+accountField('city','Stadt','City')+'</div>'+accountField('country','Land','Country')+'</article><article class="panel"><h2>'+msg('Weitere Angaben','Relevant information')+'</h2>'+accountField('email','E-Mail','Email','email')+accountField('phone','Telefon','Phone','tel')+'<button id="profilePay" type="button" class="account-row">'+msg('Lohn, Arbeitsziele & Steuerprofil','Pay, work targets & tax profile')+' <span aria-hidden="true">›</span></button></article><button class="primary" type="submit">'+msg('Profil speichern','Save profile')+'</button><p id="profileSaved" role="status"></p></form>';
 q('#profilePay').onclick=()=>show('settings');
 q('#profileForm').onsubmit=e=>{e.preventDefault();data.profile=Object.fromEntries(['name','street','postcode','city','country','email','phone'].map(k=>[k,q('#profile_'+k).value.trim()]));save();q('#profileSaved').textContent=msg('Profil gespeichert','Profile saved')};
}
function labelAccount(){
 qa('[data-account-label]').forEach(el=>{const labels={profile:['Profil','Profile'],appSettings:['Einstellungen','Settings'],appearance:['App-Darstellung','App appearance'],lock:['App-Sperre','App lock'],delete:['Daten löschen','Delete data'],signin:['Anmelden','Sign In'],unavailable:['Noch nicht verfügbar · Offline-Nutzung ohne Konto','Not available yet · Use offline without an account']};const a=labels[el.dataset.accountLabel];el.textContent=msg(...a)});
 q('#appearanceLanguage').textContent=msg('Sprache','Language');q('#appearanceTheme').textContent=msg('Darstellung','Appearance');
}
async function deleteAccountData(){
 const message=msg('Alle Profile, Schichten, Ausgaben, Ziele und App-Einstellungen auf diesem Gerät dauerhaft löschen? Laufende Timer und Erinnerungen werden beendet. Exportierte Dateien bleiben erhalten. Die App wird geschlossen.','Permanently delete all profiles, shifts, expenses, goals and app settings on this device? Active timers and reminders will stop. Exported files remain. The app will close.');
 if(!await askConfirm(message,msg('Alle App-Daten löschen?','Delete all app data?')))return;
 if(typeof Android!=='undefined'){
  if(typeof Android.clearAppData==='function')Android.clearAppData();
  else toast(msg('Bitte aktualisiere die Android-App.','Please update the Android app.'));
 }else{localStorage.removeItem(KEY);localStorage.removeItem('lohnzeit-language');location.reload()}
}
const accountProfile=document.createElement('section');accountProfile.id='profile';accountProfile.className='view';accountProfile.dataset.localized='true';q('main').append(accountProfile);
const accountSettings=document.createElement('section');accountSettings.id='appSettings';accountSettings.className='view';accountSettings.dataset.localized='true';
accountSettings.innerHTML='<div class="account-stack"><article class="panel account-menu"><details id="appearanceSection"><summary data-account-label="appearance"></summary><div id="appearanceControls"></div></details><button id="settingsLock" class="account-row" type="button"><span data-account-label="lock"></span><span aria-hidden="true">›</span></button><button id="settingsDelete" class="account-row account-danger" type="button"><span data-account-label="delete"></span><span aria-hidden="true">›</span></button><div class="account-unavailable"><strong data-account-label="signin"></strong><small data-account-label="unavailable"></small></div></article></div>';
q('main').append(accountSettings);
const languageLabel=q('#language').closest('label'),themeLabel=q('#theme').closest('label');
languageLabel.querySelector('span').id='appearanceLanguage';themeLabel.querySelector('span').id='appearanceTheme';q('#appearanceControls').append(languageLabel,themeLabel);q('.drawer-settings').remove();
q('#settingsLock').onclick=()=>{deviceSection='lock';show('device')};q('#settingsDelete').onclick=deleteAccountData;
// Preserve payroll settings as a Profile subpage, while giving app settings its own entry.
qa('[data-view="settings"]').forEach(b=>{b.dataset.view='appSettings';b.dataset.localized='true';b.classList.remove('nav');b.classList.add('account-nav');b.innerHTML='<span data-account-label="appSettings"></span>';b.onclick=()=>show('appSettings')});
for(const nav of [q('aside:not(.drawer) nav'),q('#drawer nav')]){
 const b=document.createElement('button');b.type='button';b.className='account-nav';b.dataset.view='profile';b.dataset.localized='true';b.innerHTML='<span data-account-label="profile"></span>';b.onclick=()=>show('profile');nav.insertBefore(b,nav.querySelector('[data-view="appSettings"]'));
}
qa('.device-nav[data-panel="lock"]').forEach(b=>b.remove());
const showBeforeAccount=show;show=function(view){
 showBeforeAccount(view);
 const personal=['profile','appSettings','settings','device'].includes(view);
 q('main').classList.toggle('personal-view',personal);
 if(view==='profile'){renderProfile();q('#title').textContent=msg('Profil','Profile')}
 if(view==='appSettings')q('#title').textContent=msg('Einstellungen','Settings');
 if(view==='settings')q('#title').textContent=msg('Lohn & Steuerprofil','Pay & tax profile');
 if(view==='device'&&deviceSection==='lock')q('#title').textContent=msg('App-Sperre','App lock');
 labelAccount();
};
q('#language').addEventListener('change',()=>{labelAccount();const view=q('.view.active')?.id;if(['profile','appSettings','settings','device'].includes(view))show(view)});
labelAccount();renderProfile();
