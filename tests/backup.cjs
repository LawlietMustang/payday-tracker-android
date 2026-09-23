const assert=require('node:assert/strict');
module.exports=async(browser,url)=>{
 const page=await browser.newPage({viewport:{width:390,height:844}}),errors=[];page.on('pageerror',e=>errors.push(e.message));await page.goto(url);
 await page.evaluate(()=>{data.onboardingCompleted=true;data.profile={name:'Alex'};localStorage.setItem('lohnzeit-language','en');save()});await page.reload();
 await page.evaluate(()=>{
  window.backupTest={enabled:true,folder:'WageTrack',status:'pending',hash:'',queuedHash:'',savedAt:0};window.queued=[];
  window.Android={autoBackupState:()=>JSON.stringify(backupTest),queueAutoBackup:(doc,hash)=>{queued.push({doc,hash});backupTest.queuedHash=hash;backupTest.status='pending'},chooseBackupFolder:()=>{},disableAutoBackup:()=>{backupTest.enabled=false;backupTest.status='off';autoBackupChanged()},retryAutoBackup:()=>{},syncBackupStatus:()=>{},deviceSettings:()=>JSON.stringify({lock:false,reminder:false,hour:20,minute:0,days:62,notifications:true}),syncWidget:()=>{},saveReminder:()=>{},setAppLock:()=>{},addWidget:()=>{},notificationSettings:()=>{}};
  show('recovery');
 });
 await page.waitForFunction(()=>queued.length===1);
 assert.match(await page.locator('#autoBackupStatus').innerText(),/Saving/);
 const initial=await page.evaluate(async()=>{const job=queued[0],doc=JSON.parse(job.doc);delete doc.data.onboardingDraft;return {hash:job.hash,actual:await backupHash(JSON.stringify({language:doc.language,data:doc.data}))}});
 assert.equal(initial.hash,initial.actual);
 await page.evaluate(()=>{Object.assign(backupTest,{status:'saved',hash:queued.at(-1).hash,queuedHash:'',savedAt:Date.now()});autoBackupChanged()});await page.waitForFunction(()=>q('.saved.backed-up'));
 await page.evaluate(()=>{data.profile.name='One';save();data.profile.name='Latest';save()});await page.waitForFunction(()=>queued.length===2);
 assert.equal(await page.evaluate(()=>JSON.parse(queued.at(-1).doc).data.profile.name),'Latest');
 assert.equal(await page.locator('.saved i').count(),0,'Old red/green lamp removed');
 await page.evaluate(()=>{backupTest.status='error';autoBackupChanged()});await page.waitForFunction(()=>q('#autoBackupStatus').dataset.state==='error');assert.equal(await page.locator('#retryAutoBackup').isVisible(),true);
 await page.evaluate(()=>{Object.assign(backupTest,{status:'saved',hash:queued.at(-1).hash,queuedHash:'',savedAt:Date.now()});autoBackupChanged()});await page.waitForFunction(()=>q('.saved.backed-up'));
 // Returning to the saved value must supersede a different pending snapshot.
 await page.evaluate(()=>{data.profile.name='Temporary';save()});await page.waitForFunction(()=>queued.length===3);
 await page.evaluate(()=>{data.profile.name='Latest';save()});await page.waitForFunction(()=>queued.length===4);
 assert.equal(await page.evaluate(()=>JSON.parse(queued.at(-1).doc).data.profile.name),'Latest');
 await page.evaluate(()=>{Object.assign(backupTest,{status:'saved',hash:queued.at(-1).hash,queuedHash:'',savedAt:Date.now()});autoBackupChanged()});await page.waitForFunction(()=>q('.saved.backed-up'));
 const count=await page.evaluate(()=>queued.length);await page.evaluate(async()=>{await refreshBackupStatus();await refreshBackupStatus()});assert.equal(await page.evaluate(()=>queued.length),count,'No writes for unchanged data');
 for(const width of [320,390])for(const lang of ['en','de']){
  await page.setViewportSize({width,height:844});await page.evaluate(lang=>{q('#language').value=lang;q('#language').dispatchEvent(new Event('change'));deviceSection='reminders';show('device')},lang);
  for(const section of ['lock','reminders','widget','reminders']){
   assert.equal(await page.locator('#device [data-device-section]').count(),3);
   await page.click('#device [data-device-section="'+section+'"]');
   assert.equal(await page.locator('#device [data-device-section="'+section+'"]').getAttribute('aria-current'),'page');
   assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1),false);
  }
  if(width===390&&lang==='en'){await page.screenshot({path:'test-results/backup-reminder-tabs.png',fullPage:true});await page.evaluate(()=>show('recovery'));await page.screenshot({path:'test-results/automatic-backup.png',fullPage:true})}
 }
 assert.equal(await page.evaluate(()=>infoIcon.includes('cy="8.2"')),true,'Supplied info dot retained');
 assert.deepEqual(errors,[]);await page.close();console.log('PASS: backup state, matching snapshots, coalesced saves, and persistent device tabs');
};
