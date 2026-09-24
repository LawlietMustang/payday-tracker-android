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
 for(const key of ['customCategories','savingsLedger']){
  if(x[key]===undefined)continue;if(!Array.isArray(x[key])||x[key].length>50000)throw Error('records');
  for(const row of x[key]){if(!row||typeof row!=='object'||Array.isArray(row)||Object.values(row).some(v=>v!==null&&typeof v==='object'))throw Error('record');
   if(typeof row.id!=='string'||row.id.length>250||! /^[a-zA-Z0-9_:-]+$/.test(row.id))throw Error('id');
   if(key==='customCategories'&&(!/^[a-zA-Z0-9_-]{1,100}$/.test(row.id)||typeof row.name!=='string'||!row.name.trim()||row.name.length>100))throw Error('category');
   if(key==='savingsLedger'&&(!Number.isFinite(row.amount)||row.amount<=0||typeof row.goalId!=='string'||!/^\d{4}-\d{2}-\d{2}$/.test(row.date)||!/^\d{4}-\d{2}-\d{2}$/.test(row.scheduled)))throw Error('ledger');
  }
  if(new Set(x[key].map(r=>r.id)).size!==x[key].length)throw Error('duplicate');
 }
 for(const g of x.savingsGoals||[]){
  if(g.auto!==undefined&&typeof g.auto!=='boolean')throw Error('auto');
  for(const k of ['autoAmount','autoOverdue'])if(g[k]!==undefined&&(!Number.isFinite(g[k])||g[k]<0))throw Error('auto amount');
  if(g.auto&&(!['weekly','monthly','yearly'].includes(g.autoInterval)||!/^\d{4}-\d{2}-\d{2}$/.test(g.autoNext)||!Number.isInteger(g.autoAnchor)||g.autoAnchor<1||g.autoAnchor>31||typeof g.autoEpoch!=='string'))throw Error('schedule');
 }
 if(x.profile!==undefined&&(!x.profile||typeof x.profile!=='object'||Array.isArray(x.profile)||Object.values(x.profile).some(v=>typeof v!=='string')))throw Error('profile');
 if(x.budgets!==undefined&&(!x.budgets||typeof x.budgets!=='object'||Array.isArray(x.budgets)))throw Error('budgets');
 for(const [month,budget] of Object.entries(x.budgets||{}))if(!/^\d{4}-\d{2}$/.test(month)||!budget||typeof budget!=='object'||Array.isArray(budget)||Object.values(budget).some(v=>typeof v!=='number'||!Number.isFinite(v)||v<0))throw Error('budget');
 x.activeTimer=null;return doc;
}
let pendingBackupHash=null,backupCheckGeneration=0;
const BACKUP_STATUS_KEY='wagetrack-backup-status';
function backupSnapshot(){const doc=JSON.parse(backupDocument());delete doc.data.onboardingDraft;return JSON.stringify({language:doc.language,data:doc.data})}
async function backupHash(snapshot){const bytes=await crypto.subtle.digest('SHA-256',new TextEncoder().encode(snapshot));return [...new Uint8Array(bytes)].map(v=>v.toString(16).padStart(2,'0')).join('')}
function nativeAutoBackup(){try{return typeof Android!=='undefined'&&typeof Android.autoBackupState==='function'?JSON.parse(Android.autoBackupState()):null}catch(e){return null}}
let submittedBackupHash='';
function backupStateLabel(state){return state==='error'?msg('Sicherung prüfen','Backup needs attention'):state==='saving'?msg('Sicherung wird gespeichert…','Saving backup…'):state==='saved'?msg('Gesichert','Backed up'):msg('Sicherung einrichten','Set up backup')}
async function refreshBackupStatus(){
 const generation=++backupCheckGeneration,snapshot=backupSnapshot(),document=backupDocument();let hash='';
 try{hash=await backupHash(snapshot)}catch(e){}
 if(generation!==backupCheckGeneration)return;
 const auto=nativeAutoBackup(),backed=!!hash&&(auto?.enabled?hash===auto.hash:hash===localStorage.getItem(BACKUP_STATUS_KEY));
 const status=auto?.status==='error'?'error':backed?'saved':auto?.enabled?'saving':'off';
 if(auto?.enabled&&hash&&(hash!==auto.hash||(auto.queuedHash&&hash!==auto.queuedHash))&&hash!==auto.queuedHash&&hash!==submittedBackupHash){
  submittedBackupHash=hash;try{Android.queueAutoBackup(document,hash)}catch(e){submittedBackupHash=''}
 }
 const savedAt=auto?.enabled?auto.savedAt:Number(localStorage.getItem('wagetrack-backup-at')||0);
 const stamp=savedAt?new Date(savedAt).toLocaleString(language()==='en'?'en-GB':'de-DE',{day:'numeric',month:'short',hour:'2-digit',minute:'2-digit'}):'';
 const symbol=status==='saved'?'✓':status==='error'?'!':status==='saving'?'↻':'↑';
 qa('.saved').forEach(el=>{el.dataset.localized='true';el.dataset.backupState=status;el.classList.toggle('backed-up',status==='saved');el.innerHTML='<span class="backup-state-icon" aria-hidden="true">'+symbol+'</span><span><b>'+backupStateLabel(status)+'</b><small>'+safe(status==='saved'&&stamp?stamp:msg('Sicherung verwalten','Manage backup'))+'</small></span>';el.setAttribute('role','button');el.tabIndex=0;el.onclick=()=>show('recovery');el.onkeydown=e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();show('recovery')}}});
 const meaningful=!!(data.shifts.length||data.expenses.length||data.profile?.name);
 if(typeof Android!=='undefined'&&typeof Android.syncBackupStatus==='function')Android.syncBackupStatus(status!=='saved'&&meaningful,language());
 updateAutoBackupControls(auto,status,stamp);
}
window.autoBackupChanged=()=>{submittedBackupHash='';void refreshBackupStatus()};
function updateAutoBackupControls(auto,status,stamp){
 const control=q('#autoBackupToggle');if(!control)return;
 control.checked=!!auto?.enabled;
 q('#backupFolder').textContent=auto?.enabled?msg('Ordner ändern','Change folder'):msg('Ordner auswählen','Choose folder');
 q('#backupFolderName').textContent=auto?.enabled?auto.folder:msg('Einmal einen Ordner auf deinem Gerät auswählen.','Choose a folder on your phone once.');
 q('#autoBackupStatus').textContent=backupStateLabel(status)+(status==='saved'&&stamp?' · '+stamp:'');
 q('#autoBackupStatus').dataset.state=status;
 q('#retryAutoBackup').hidden=!(auto?.enabled&&status==='error');
 q('#autoBackupError').hidden=status!=='error';
}
async function startBackupExport(){
 if(pendingBackupHash)return;
 if(typeof Android==='undefined'||typeof Android.exportBackup!=='function'){toast(msg('Bitte die Android-App verwenden.','Please use the Android app.'));return}
 const snapshot=backupSnapshot(),document=backupDocument();pendingBackupHash=backupHash(snapshot);q('#saveBackup')?.setAttribute('disabled','');
 try{await pendingBackupHash;Android.exportBackup(document)}catch(e){window.backupResult(false)}
}
window.backupResult=async ok=>{
 const snapshot=pendingBackupHash;pendingBackupHash=null;q('#saveBackup')?.removeAttribute('disabled');
 if(ok&&snapshot)try{{localStorage.setItem(BACKUP_STATUS_KEY,await snapshot);localStorage.setItem('wagetrack-backup-at',String(Date.now()))}}catch(e){ok=false}
 await refreshBackupStatus();
 if(ok!==null)toast(ok?msg('Sicherung gespeichert','Backup saved'):msg('Datei konnte nicht verarbeitet werden. Bitte erneut versuchen.','Could not process the file. Please try again.'));
};
const saveBeforeBackup=save;save=function(){saveBeforeBackup();void refreshBackupStatus()};
q('#language').addEventListener('change',()=>void refreshBackupStatus());void refreshBackupStatus();
window.receiveBackup=async text=>{
 let doc;try{doc=validateBackup(text)}catch(e){toast(msg('Ungültige oder nicht unterstützte Sicherung. Deine Daten bleiben unverändert.','Invalid or unsupported backup. Your data is unchanged.'));return false}
 if(!await askConfirm(msg('Diese Sicherung ersetzt die aktuellen App-Daten. Fortfahren?','This backup will replace your current app data. Continue?'),msg('Sicherung wiederherstellen','Restore backup')))return false;
 if(doc.data.shiftReminders)doc.data.shiftReminders.enabled=false;
 doc.data.onboardingCompleted=true;delete doc.data.onboardingDraft;
 const previous=localStorage.getItem(KEY),oldLanguage=localStorage.getItem('lohnzeit-language');
 try{localStorage.setItem(KEY,JSON.stringify(doc.data));localStorage.setItem('lohnzeit-language',doc.language==='en'?'en':'de')}
 catch(e){if(previous!==null)localStorage.setItem(KEY,previous);else localStorage.removeItem(KEY);if(oldLanguage!==null)localStorage.setItem('lohnzeit-language',oldLanguage);window.backupResult(false);return false}
 if(deviceAvailable()){Android.stopTimerNotification();Android.saveReminder(false,20,0,62,doc.language==='en'?'en':'de')}
 location.reload();return true;
};
function renderRecovery(){
 const supported=typeof Android!=='undefined'&&typeof Android.chooseBackupFolder==='function';
 q('#recovery').innerHTML='<div class="account-stack"><article class="panel automatic-backup"><h2>'+msg('Automatische Sicherung','Automatic backup')+'</h2><label class="check"><span>'+msg('Auf diesem Gerät sichern','Back up on this phone')+'</span><input id="autoBackupToggle" type="checkbox" role="switch" '+(!supported?'disabled':'')+'></label><p id="autoBackupStatus" role="status"></p><p id="backupFolderName" class="muted"></p><div class="backup-actions"><button id="backupFolder" class="primary" '+(!supported?'disabled':'')+'></button><button id="retryAutoBackup" class="secondary" hidden>'+msg('Erneut versuchen','Retry backup')+'</button></div><p class="muted">'+msg('Nach Änderungen automatisch speichern. Eine aktuelle Datei und eine vorherige Kopie – keine Dateisammlung.','Saved automatically after edits. One current file and one previous copy—no growing pile of files.')+'</p><p id="autoBackupError" role="alert" hidden>'+msg('Der Ordner ist nicht erreichbar oder konnte nicht beschrieben werden. Erneut versuchen oder einen Ordner neu auswählen.','The folder is unavailable or could not be written. Retry or choose a folder again.')+'</p></article><article class="panel"><h2>'+msg('Wiederherstellen & exportieren','Restore & export')+'</h2><div class="backup-actions"><button id="restoreBackup" class="primary">'+msg('Sicherung wiederherstellen','Restore a backup')+'</button><button id="saveBackup" class="secondary">'+msg('Kopie exportieren','Export a copy')+'</button></div><p class="muted">'+msg('Nach einer Neuinstallation die Sicherungsdatei über „Wiederherstellen“ auswählen. Eine lokale Sicherung schützt nicht vor dem Verlust deines Telefons.','After reinstalling, choose your saved file using Restore. A local backup does not protect against losing your phone.')+'</p><details><summary>'+msg('Was wird gesichert?','What is backed up?')+'</summary><p>'+msg('Schichten, Ausgaben, Arbeitsplätze, Ziele, Profil und Einstellungen. Laufende Timer und App-PINs werden nicht übernommen; Erinnerungen bleiben nach dem Wiederherstellen ausgeschaltet. Die Dateien sind unverschlüsselt.','Shifts, expenses, workplaces, goals, profile and settings. Running timers and app PINs are not transferred; reminders stay off after restoring. The files are unencrypted.')+'</p><p>'+msg('Für eine zusätzliche Kopie in Drive „Kopie exportieren“ wählen und Drive in der Dateiauswahl öffnen.','For an additional copy in Drive, choose Export a copy and select Drive in the file picker.')+'</p></details></article></div>';
 q('#backupFolder').onclick=()=>Android.chooseBackupFolder();
 q('#autoBackupToggle').onchange=async e=>{const wanted=e.target.checked;e.target.checked=!!nativeAutoBackup()?.enabled;if(wanted)Android.chooseBackupFolder();else if(await askConfirm(msg('Automatische Sicherung ausschalten? Vorhandene Dateien bleiben erhalten.','Turn off automatic backup? Existing backup files will be kept.')))Android.disableAutoBackup()};
 q('#retryAutoBackup').onclick=()=>{submittedBackupHash='';Android.retryAutoBackup();void refreshBackupStatus()};
 q('#saveBackup').onclick=startBackupExport;
 q('#restoreBackup').onclick=()=>{if(typeof Android!=='undefined'&&typeof Android.importBackup==='function')Android.importBackup();else toast(msg('Bitte die Android-App verwenden.','Please use the Android app.'))};
 void refreshBackupStatus();
}
const recovery=document.createElement('section');recovery.id='recovery';recovery.className='view';recovery.dataset.localized='true';q('main').append(recovery);
const showBeforeRecovery=show;show=function(view){showBeforeRecovery(view);q('main').classList.toggle('expense-view',view==='expenses');if(view==='recovery'){q('main').classList.add('personal-view');q('#title').textContent=msg('Sichern & wiederherstellen','Backup & restore');renderRecovery()}labelRoutes()};
q('#language').addEventListener('change',()=>{labelRoutes();if(q('#recovery').classList.contains('active'))show('recovery')});labelRoutes();
window.widgetPinResult=accepted=>{
 if(!q('#pinWidget'))return; // The user may have left the widget page while the launcher replied.
 let help=q('#widgetHelp');if(!help){help=document.createElement('div');help.id='widgetHelp';help.setAttribute('role','status');q('#pinWidget').after(help)}
 help.innerHTML='<p>'+ (accepted?msg('Bestätige das Hinzufügen im Startbildschirm-Dialog. Wenn kein Dialog erscheint:','Confirm in your launcher’s add-widget dialog. If no dialog appears:'):msg('Dein Startbildschirm unterstützt das direkte Hinzufügen nicht.','Your launcher does not support adding directly.'))+'</p><p>'+msg('Halte eine freie Stelle auf dem Startbildschirm gedrückt → Widgets → WageTrack. Wähle bei Xiaomi gegebenenfalls Android-Widgets / Alle Widgets.','Long-press an empty area on your home screen → Widgets → WageTrack. On Xiaomi, look for Android widgets / All widgets if needed.')+'</p><button id="widgetHome" class="secondary">'+msg('Startbildschirm öffnen','Open home screen')+'</button>';
 q('#widgetHome').onclick=()=>Android.openHome();
};
