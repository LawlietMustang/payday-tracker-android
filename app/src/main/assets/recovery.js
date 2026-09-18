function backupDocument(){const copy=JSON.parse(JSON.stringify(data));copy.activeTimer=null;if(copy.shiftReminders)copy.shiftReminders.enabled=false;return JSON.stringify({app:'PaydayTracker',version:1,created:new Date().toISOString(),language:language(),data:copy})}
function validateBackup(text){
 if(typeof text!=='string'||text.length>10485760)throw Error('size');
 const doc=JSON.parse(text,(key,value)=>{if(['__proto__','constructor','prototype'].includes(key))throw Error('key');return value});
 const x=doc.data;
 if(doc.app!=='PaydayTracker'||doc.version!==1||!x||typeof x!=='object'||!x.settings||!Array.isArray(x.shifts))throw Error('format');
 for(const [key,defaultValue] of Object.entries(DEFAULTS.settings)){
  const v=x.settings[key];if(v===undefined)continue;
  if(Array.isArray(defaultValue)){if(!Array.isArray(v)||v.some(d=>!Number.isInteger(d)||d<0||d>6))throw Error('days')}
  else if(typeof v!==typeof defaultValue||(typeof v==='number'&&!Number.isFinite(v)))throw Error('setting');
 }
 for(const key of ['shifts','expenses','recurringExpenses','templates','workplaces','payslips','savingsGoals']){
  if(x[key]===undefined)continue;
  if(!Array.isArray(x[key])||x[key].length>50000)throw Error('records');
  for(const row of x[key]){if(!row||typeof row!=='object'||Array.isArray(row))throw Error('record');for(const [k,v] of Object.entries(row)){if(/^(id|workplaceId|recurringId)$/.test(k)&&v!=null&&(typeof v!=='string'||! /^[a-zA-Z0-9_-]{1,100}$/.test(v)))throw Error('id');if(typeof v==='number'&&!Number.isFinite(v))throw Error('number')}}
 }
 for(const row of x.shifts)if(!/^\d{4}-\d{2}-\d{2}$/.test(row.date)||!/^\d{2}:\d{2}$/.test(row.start)||!/^\d{2}:\d{2}$/.test(row.end)||!Number.isFinite(row.minutes))throw Error('shift');
 for(const key of ['shifts','expenses','recurringExpenses','templates','workplaces','payslips','savingsGoals'])for(const row of x[key]||[]){
  if(typeof row.id!=='string')throw Error('id');
  for(const [k,v] of Object.entries(row)){
   if(v!==null&&typeof v==='object')throw Error('nested record');
   if(['minutes','breakMin','wage','amount','target','saved','actualGross','actualNet','created','day'].includes(k)&&typeof v!=='number')throw Error('number');
   if(['name','note','date','start','end','status','category','due','month'].includes(k)&&typeof v!=='string')throw Error('text');
  }
 }
 if(x.profile!==undefined&&(!x.profile||typeof x.profile!=='object'||Array.isArray(x.profile)||Object.values(x.profile).some(v=>typeof v!=='string')))throw Error('profile');
 if(x.budgets!==undefined&&(!x.budgets||typeof x.budgets!=='object'||Array.isArray(x.budgets)))throw Error('budgets');
 for(const [month,budget] of Object.entries(x.budgets||{}))if(!/^\d{4}-\d{2}$/.test(month)||!budget||typeof budget!=='object'||Array.isArray(budget)||Object.values(budget).some(v=>typeof v!=='number'||!Number.isFinite(v)||v<0))throw Error('budget');
 x.activeTimer=null;return doc;
}
window.backupResult=ok=>toast(ok?msg('Sicherung gespeichert','Backup saved'):msg('Datei konnte nicht verarbeitet werden. Bitte erneut versuchen.','Could not process the file. Please try again.'));
window.receiveBackup=async text=>{
 let doc;try{doc=validateBackup(text)}catch(e){toast(msg('Ungültige oder nicht unterstützte Sicherung. Deine Daten bleiben unverändert.','Invalid or unsupported backup. Your data is unchanged.'));return false}
 if(!await askConfirm(msg('Diese Sicherung ersetzt die aktuellen App-Daten. Fortfahren?','This backup will replace your current app data. Continue?'),msg('Sicherung wiederherstellen','Restore backup')))return false;
 if(doc.data.shiftReminders)doc.data.shiftReminders.enabled=false;
 const previous=localStorage.getItem(KEY),oldLanguage=localStorage.getItem('lohnzeit-language');
 try{localStorage.setItem(KEY,JSON.stringify(doc.data));localStorage.setItem('lohnzeit-language',doc.language==='en'?'en':'de')}
 catch(e){if(previous!==null)localStorage.setItem(KEY,previous);else localStorage.removeItem(KEY);if(oldLanguage!==null)localStorage.setItem('lohnzeit-language',oldLanguage);window.backupResult(false);return false}
 if(deviceAvailable()){Android.stopTimerNotification();Android.saveReminder(false,20,0,62,doc.language==='en'?'en':'de')}
 location.reload();return true;
};
function renderRecovery(){
 q('#recovery').innerHTML='<div class="account-stack"><article class="panel"><h2>'+msg('Sichern & wiederherstellen','Backup & restore')+'</h2><p>'+msg('Speichere deine Daten vor einer Deinstallation in Dokumente oder in Google Drive über die Dateiauswahl. Wähle nach der Neuinstallation hier die gespeicherte Datei aus.','Before uninstalling, save your data to Documents or Google Drive through the file picker. After reinstalling, select that saved file here.')+'</p><p class="muted">'+msg('Enthält gespeicherte Schichten, Ausgaben, Arbeitsplätze, Ziele, Profil und Einstellungen. Laufende Timer, App-Sperre und Erinnerungen werden nicht übernommen. Die Datei ist unverschlüsselt; bewahre sie privat auf.','Includes saved shifts, expenses, workplaces, goals, profile and settings. Running timers, app lock and reminders are not transferred. The file is unencrypted; keep it private.')+'</p><div class="device-settings"><button id="saveBackup" class="primary">'+msg('Sicherungsdatei speichern','Save backup file')+'</button><button id="restoreBackup" class="secondary">'+msg('Aus Datei / Drive wiederherstellen','Restore from file / Drive')+'</button></div><p class="muted">'+msg('Drive erscheint, wenn es als Dateianbieter verfügbar ist. Eine Internetverbindung kann erforderlich sein. Dies ist keine automatische Synchronisierung und keine Anmeldung in Payday Tracker.','Drive appears when available as a file provider. An internet connection may be required. This is not automatic sync or a Payday Tracker account sign-in.')+'</p></article></div>';
 q('#saveBackup').onclick=()=>{if(typeof Android!=='undefined'&&typeof Android.exportBackup==='function')Android.exportBackup(backupDocument());else toast(msg('Bitte die Android-App verwenden.','Please use the Android app.'))};
 q('#restoreBackup').onclick=()=>{if(typeof Android!=='undefined'&&typeof Android.importBackup==='function')Android.importBackup();else toast(msg('Bitte die Android-App verwenden.','Please use the Android app.'))};
}
const recovery=document.createElement('section');recovery.id='recovery';recovery.className='view';recovery.dataset.localized='true';q('main').append(recovery);
const recoveryButton=document.createElement('button');recoveryButton.className='account-row';recoveryButton.type='button';recoveryButton.id='openRecovery';recoveryButton.onclick=()=>show('recovery');q('#settingsDelete').before(recoveryButton);
function labelRecovery(){recoveryButton.textContent=msg('Sichern & wiederherstellen','Backup & restore')+' ›'}
const showBeforeRecovery=show;show=function(view){showBeforeRecovery(view);q('main').classList.toggle('expense-view',view==='expenses');if(view==='recovery'){q('main').classList.add('personal-view');q('#title').textContent=msg('Sichern & wiederherstellen','Backup & restore');renderRecovery()}labelRecovery()};
q('#language').addEventListener('change',()=>{labelRecovery();if(q('#recovery').classList.contains('active'))show('recovery')});labelRecovery();
window.widgetPinResult=accepted=>{
 let help=q('#widgetHelp');if(!help){help=document.createElement('div');help.id='widgetHelp';help.setAttribute('role','status');q('#pinWidget').after(help)}
 help.innerHTML='<p>'+ (accepted?msg('Bestätige das Hinzufügen im Startbildschirm-Dialog. Wenn kein Dialog erscheint:','Confirm in your launcher’s add-widget dialog. If no dialog appears:'):msg('Dein Startbildschirm unterstützt das direkte Hinzufügen nicht.','Your launcher does not support adding directly.'))+'</p><p>'+msg('Halte eine freie Stelle auf dem Startbildschirm gedrückt → Widgets → Payday Tracker. Wähle bei Xiaomi gegebenenfalls Android-Widgets / Alle Widgets.','Long-press an empty area on your home screen → Widgets → Payday Tracker. On Xiaomi, look for Android widgets / All widgets if needed.')+'</p><button id="widgetHome" class="secondary">'+msg('Startbildschirm öffnen','Open home screen')+'</button>';
 q('#widgetHome').onclick=()=>Android.openHome();
};
