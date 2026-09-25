// Personal details stay in the existing offline data store.
function accountField(key,de,en,type='text') {
 return '<label>'+msg(de,en)+'<input id="profile_'+key+'" type="'+type+'" maxlength="160" value="'+safe(data.profile?.[key]||'')+'"></label>';
}
function renderProfile(){
 q('#profile').innerHTML='<form id="profileForm" class="account-stack"><article class="panel"><h2>'+msg('Persönliche Angaben','Personal details')+'</h2><p class="muted">'+msg('Alle Angaben sind freiwillig und bleiben auf diesem Gerät.','All details are optional and stay on this device.')+'</p>'+accountField('name','Name','Name')+'</article><article class="panel"><h2>'+msg('Adresse','Address')+'</h2>'+accountField('street','Straße und Hausnummer','Street and house number')+'<div class="account-grid">'+accountField('postcode','Postleitzahl','Postcode')+accountField('city','Stadt','City')+'</div>'+accountField('country','Land','Country')+'</article><article class="panel"><h2>'+msg('Weitere Angaben','Relevant information')+'</h2>'+accountField('email','E-Mail','Email','email')+accountField('phone','Telefon','Phone','tel')+'</article><button class="primary" type="submit">'+msg('Profil speichern','Save profile')+'</button><p id="profileSaved" role="status"></p></form>';
 q('#profileForm').onsubmit=e=>{e.preventDefault();data.profile=Object.fromEntries(['name','street','postcode','city','country','email','phone'].map(k=>[k,q('#profile_'+k).value.trim()]));save();q('#profileSaved').textContent=msg('Profil gespeichert','Profile saved')};
}
function labelAccount(){
 for(const id of ['close','expenseClose','bulkEditClose','menuClose'])q('#'+id).setAttribute('aria-label',msg('Schließen','Close')); 
 qa('[data-account-label]').forEach(el=>{const labels={profile:['Profil','Profile'],appSettings:['Einstellungen','Settings'],appearance:['App-Darstellung','App appearance'],lock:['App-Sperre','App lock'],delete:['Daten löschen','Delete data'],signin:['Anmelden','Sign In'],unavailable:['Noch nicht verfügbar · Offline-Nutzung ohne Konto','Not available yet · Use offline without an account']};const a=labels[el.dataset.accountLabel];if(el.dataset.accountLabel==='appearance')el.innerHTML='<span class="route-icon" data-icon="'+(document.documentElement.dataset.theme==='dark'?'moon':'sun')+'" aria-hidden="true"></span><span>'+msg(...a)+'</span>';else el.textContent=msg(...a)});
 q('#appearanceLanguage').textContent=msg('Sprache','Language');q('#appearanceTheme').textContent=msg('Darstellung','Appearance');q('#language').setAttribute('aria-label',msg('Sprache','Language'));for(const o of q('#theme').options)o.textContent=({system:msg('Systemeinstellung','System default'),light:msg('Hell','Light'),dark:msg('Dunkel','Dark')})[o.value];
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
accountSettings.innerHTML='<div class="account-stack"><article class="panel account-menu"><div id="settingsMenu"></div><div class="account-unavailable"><strong data-account-label="signin"></strong><small data-account-label="unavailable"></small></div></article></div>';
q('main').append(accountSettings);buildSettingsMenu();
const languageLabel=q('#language').closest('label'),themeLabel=q('#theme').closest('label');
languageLabel.querySelector('span').id='appearanceLanguage';themeLabel.querySelector('span').id='appearanceTheme';q('#appearanceControls').append(languageLabel,themeLabel);q('.drawer-settings').remove();
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

const themeBeforeAppearance=applyTheme;applyTheme=function(){themeBeforeAppearance();labelAccount()};
