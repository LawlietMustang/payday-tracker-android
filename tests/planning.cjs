const {chromium}=require('playwright');
const http=require('node:http'),fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve('app/src/main/assets');
const server=http.createServer((req,res)=>{const file=path.join(root,req.url==='/'?'index.html':req.url);if(!file.startsWith(root)){res.writeHead(404).end();return}try{res.setHeader('Content-Type',file.endsWith('.js')?'application/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(fs.readFileSync(file))}catch{res.writeHead(404).end()}});
(async()=>{
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));const url='http://127.0.0.1:'+server.address().port;
 const browser=await chromium.launch({headless:true});await require('./onboarding.cjs')(browser,url);const page=await browser.newPage({viewport:{width:360,height:800}});let errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.goto(url);await page.waitForSelector('#planning',{state:'attached'});
 await page.evaluate(()=>{localStorage.setItem('lohnzeit-language','en');delete data.onboardingCompleted;delete data.onboardingDraft;data.settings.theme='dark';data.shifts.push({id:'legacy',date:'2026-09-01',start:'08:00',end:'16:00',minutes:480,breakMin:0,wage:15,status:'completed',created:1});save()});await page.reload();
 await page.click('#headerAddPlace');
 await page.fill('#placeName','Second job');await page.fill('#placeWage','20');await page.click('#planningSave');
 assert.equal(await page.evaluate(()=>data.workplaces.length),2);assert.equal(await page.locator('#placesContent').innerText().then(s=>s.includes('Second job')),true);
 await page.click('#headerManagePlaces');await page.locator('[data-place-edit]').last().click();await page.fill('#placeName','Evening job');await page.click('#planningSave');
 assert.equal(await page.evaluate(()=>data.workplaces[1].name),'Evening job');
 await page.evaluate(()=>openDialog());await page.click('#shiftAddPlace');await page.fill('#placeName','Temporary job');await page.fill('#placeWage','22');await page.click('#planningSave');
 assert.equal(await page.locator('#shiftWorkplace option:checked').innerText(),'Temporary job');
 await page.click('#shiftManagePlaces');await page.locator('[data-place-delete]').last().click();await page.click('#planningSave');assert.equal(await page.evaluate(()=>data.workplaces.length),2);
 await page.click('#menuOpen');await page.click('#drawer [data-view="planning"]');
 await page.click('#editBudget');await page.fill('#budgetTotal','1000');await page.fill('#budget_groceries','200');await page.click('#planningSave');
 await page.click('#addGoal');await page.fill('#goalName','Laptop');await page.fill('#goalTarget','1200');await page.fill('#goalSaved','250');await page.click('#planningSave');
 await page.click('#addGoal');await page.fill('#goalName','Trip');await page.fill('#goalTarget','400');await page.click('#planningSave');
 assert.equal(await page.evaluate(()=>data.savingsGoals.length),3);
 await page.reload();await page.evaluate(()=>show('planning'));assert.equal(await page.evaluate(()=>data.savingsGoals.find(g=>g.name==='Laptop').saved),250);assert.equal(await page.evaluate(()=>data.shifts.find(e=>e.id==='legacy').workplaceId),'default');
 const projection=await page.evaluate(()=>{data.expenses=[{id:'r',date:'2026-09-01',amount:600,category:'rent',recurringId:'r'},{id:'v',date:'2026-09-05',amount:100,category:'groceries'},{id:'s',date:'2026-09-25',amount:50,category:'utilities',recurringId:'s'}];return spendingProjection('2026-09',new Date(2026,8,10))});
 assert.equal(projection.spent,700);assert.equal(projection.scheduled,50);assert.equal(projection.projected,950);
 fs.mkdirSync('test-results',{recursive:true});
 for(const width of [320,360,412,768])for(const lang of ['de','en'])for(const theme of ['light','dark']){
  await page.setViewportSize({width,height:850});await page.evaluate(({lang,theme})=>{localStorage.setItem('lohnzeit-language',lang);q('#language').value=lang;data.settings.theme=theme;applyTheme();q('#language').dispatchEvent(new Event('change'))},{lang,theme});
  for(const view of ['dashboard','workplaces','planning','history','settings','device']){await page.evaluate(v=>show(v),view);await page.waitForTimeout(30);const overflow=await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1);assert.equal(overflow,false,`overflow ${width} ${lang} ${theme} ${view}`);}
  if(width===360&&lang==='en'){await page.evaluate(()=>show('planning'));await page.screenshot({path:`test-results/planning-${theme}.png`,fullPage:true});await page.evaluate(()=>show('workplaces'));await page.screenshot({path:`test-results/workplaces-${theme}.png`,fullPage:true})}
 }
 // Exercise the native bridge UI contract without pretending to authenticate in a browser.
 await page.evaluate(()=>{window.nativeTest={lock:false,reminder:false,hour:20,minute:0,days:62,notifications:false};window.Android={deviceSettings:()=>JSON.stringify(window.nativeTest),saveReminder:(enabled,h,m,days)=>{Object.assign(window.nativeTest,{reminder:enabled,hour:h,minute:m,days});refreshDeviceSettings()},setAppLock:v=>{window.lockRequested=v},addWidget:()=>{window.pinRequested=true},notificationSettings:()=>{},syncWidget:()=>{}};localStorage.setItem('lohnzeit-language','en');show('device')});
 await page.check('#reminderEnabled');for(const box of await page.locator('.reminder-days input').all())await box.uncheck();await page.click('#reminderForm button');assert.match(await page.locator('#reminderError').innerText(),/weekday/);
 await page.check('.reminder-days input[value="1"]');await page.fill('#reminderTime','19:30');await page.click('#reminderForm button');assert.equal(await page.evaluate(()=>window.nativeTest.days),2);assert.equal(await page.evaluate(()=>window.nativeTest.hour),19);
 await page.click('#biometricLock');assert.equal(await page.evaluate(()=>window.lockRequested),true);assert.equal(await page.isChecked('#biometricLock'),false);
 await page.click('#pinWidget');assert.equal(await page.evaluate(()=>window.pinRequested),true);
 await page.evaluate(()=>{show('appSettings');q('#settingsLock').click()});assert.equal(await page.locator('#biometricLock').isVisible(),true);assert.equal(await page.locator('#reminderForm').isVisible(),false);
 await page.evaluate(()=>show('profile'));await page.fill('#profile_name','Test Person');await page.fill('#profile_street','Example Street 1');await page.click('#profileForm button[type=submit]');await page.reload();await page.evaluate(()=>show('profile'));assert.equal(await page.inputValue('#profile_name'),'Test Person');assert.equal(await page.inputValue('#profile_street'),'Example Street 1');
 for(const lang of ['de','en'])for(const theme of ['light','dark']){
  await page.evaluate(({lang,theme})=>{q('#language').value=lang;q('#language').dispatchEvent(new Event('change'));data.settings.theme=theme;applyTheme()},{lang,theme});
  await page.setViewportSize({width:360,height:800});
  for(const view of ['profile','appSettings']){await page.evaluate(v=>show(v),view);assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1),false);await page.screenshot({path:`test-results/${view}-${lang}-${theme}.png`,fullPage:true})}
 }
 await page.click('#settingsDelete');await page.click('#confirmCancel');assert.equal(await page.evaluate(()=>data.profile.name),'Test Person');
 await page.evaluate(()=>{window.Android={clearAppData:()=>window.deleted=true};show('appSettings')});await page.click('#settingsDelete');await page.click('#confirmOk');assert.equal(await page.evaluate(()=>window.deleted),true);
 await page.evaluate(()=>{delete window.Android;show('expenses')});assert.equal(await page.locator('#workplaceFilter').isVisible(),false);await page.screenshot({path:'test-results/expenses-clean.png',fullPage:true});
 await page.evaluate(()=>show('dashboard'));assert.equal(await page.locator('#workplaceFilter').isVisible(),true);
 const snapshot=await page.evaluate(()=>backupDocument());assert.equal(await page.evaluate(s=>validateBackup(s).data.profile.name,snapshot),'Test Person');
 assert.equal(await page.evaluate(()=>{try{validateBackup('{"app":"Wrong"}');return false}catch(e){return true}}),true);
 await page.evaluate(()=>{data.profile.name='Changed';save();show('recovery')});await page.screenshot({path:'test-results/recovery.png',fullPage:true});
 await page.evaluate(s=>{void receiveBackup(s)},snapshot);await page.click('#confirmCancel');assert.equal(await page.evaluate(()=>data.profile.name),'Changed');
 await page.evaluate(s=>{void receiveBackup(s)},snapshot);await page.click('#confirmOk');await page.waitForFunction(()=>typeof data!=='undefined'&&data.profile?.name==='Test Person');
 await page.evaluate(()=>{show('device');widgetPinResult(false)});assert.equal(await page.locator('#widgetHome').isVisible(),true);
 await page.screenshot({path:'test-results/device-settings.png',fullPage:true});
 await page.evaluate(()=>{show('dashboard');show('shifts');show('expenses');show('expenses');handleAppBack()});assert.equal(await page.evaluate(()=>q('.view.active').id),'shifts');
 await page.evaluate(()=>handleAppBack());assert.equal(await page.evaluate(()=>q('.view.active').id),'dashboard');assert.equal(await page.evaluate(()=>handleAppBack()),false);
 await page.evaluate(()=>{show('history');show('dashboard')});assert.equal(await page.evaluate(()=>handleAppBack()),false);
 await page.evaluate(()=>{show('appSettings');deviceSection='lock';show('device');deviceSection='widget';show('device');handleAppBack()});assert.equal(await page.evaluate(()=>deviceSection),'lock');
 await page.evaluate(()=>{show('dashboard');openDrawer();handleAppBack()});assert.equal(await page.evaluate(()=>q('#drawer').classList.contains('open')),false);
 await page.evaluate(()=>{void askConfirm('Test').then(v=>window.backAnswer=v);handleAppBack()});assert.equal(await page.evaluate(()=>window.backAnswer),false);
 await page.evaluate(()=>{show('shifts');selectionMode=true;handleAppBack()});assert.equal(await page.evaluate(()=>selectionMode),false);assert.equal(await page.evaluate(()=>q('.view.active').id),'shifts');
 assert.deepEqual(await page.evaluate(()=>[isoCalendarWeek(new Date(2021,0,1)),isoCalendarWeek(new Date(2024,11,30))]),[{year:2020,week:53},{year:2025,week:1}]);
 await page.evaluate(()=>{q('#language').value='en';q('#language').dispatchEvent(new Event('change'));data.settings.theme='light';applyTheme();show('dashboard');selected='2026-09';renderBars([{date:'2026-09-06',minutes:60},{date:'2026-09-07',minutes:120}])});
 assert.deepEqual(await page.locator('#bars small').allTextContents(),['W36','W37','W38','W39','W40']);
 assert.equal(await page.locator('#bars i').first().getAttribute('style'),'height:50%');
 await page.screenshot({path:'test-results/calendar-weeks.png',fullPage:true});
 await page.evaluate(()=>{selected='2026-08';renderBars([])});assert.deepEqual(await page.locator('#bars small').allTextContents(),['W31','W32','W33','W34','W35','W36']);
 await page.evaluate(()=>{show('expenses');openExpenseDialog()});await page.click('#expenseRecurring');
 assert.equal(await page.locator('#expenseRecurring').evaluate(e=>getComputedStyle(e).boxShadow),'none');
 assert.equal(await page.locator('#expenseRecurring').evaluate(e=>getComputedStyle(e).outlineStyle),'none');
 await page.screenshot({path:'test-results/checkbox-clean.png',fullPage:true});await page.evaluate(()=>handleAppBack());
 for(const size of await page.locator('.mobile button').evaluateAll(es=>es.map(e=>{const s=getComputedStyle(e,'::before');return [s.width,s.height]})))assert.deepEqual(size,['24px','24px']);
 await page.evaluate(()=>{
  const date='2099-10-01';data.shifts.push({id:'remind-a',date,start:'12:00',end:'20:00',minutes:450,wage:15,status:'planned',workplaceId:'default'},{id:'remind-b',date,start:'18:00',end:'22:00',minutes:240,wage:15,status:'planned',workplaceId:'default'});
  window.Android={syncShiftReminders:json=>window.shiftPayload=JSON.parse(json),shiftReminderStatus:()=>JSON.stringify({allowed:true}),requestShiftNotifications:()=>window.requested=true};deviceSection='reminders';show('device');
 });
 await page.check('#shiftReminderEnabled');await page.fill('#shiftReminderAmount','2');await page.selectOption('#shiftReminderUnit','days');await page.click('#shiftReminderForm button[type=submit]');
 assert.equal(await page.evaluate(()=>window.shiftPayload.leadMinutes),2880);assert.equal(await page.evaluate(()=>window.shiftPayload.shifts.length),2);assert.equal(await page.evaluate(()=>window.requested),true);
 await page.evaluate(()=>{data.shifts.push({id:'remind-c',date:'2099-10-02',start:'09:00',end:'17:00',minutes:480,wage:15,status:'planned',workplaceId:'default'});save()});assert.equal(await page.evaluate(()=>window.shiftPayload.shifts.length),3);
 await page.evaluate(()=>renderDeviceSettings());await page.selectOption('#shiftReminderScope','selected');await page.check('#shiftReminderChoices input[value="remind-a"]');await page.click('#shiftReminderForm button[type=submit]');assert.deepEqual(await page.evaluate(()=>window.shiftPayload.shifts.map(x=>x.id)),['remind-a']);
 await page.evaluate(()=>{data.shifts.find(x=>x.id==='remind-a').start='13:00';save()});assert.equal(await page.evaluate(()=>window.shiftPayload.shifts[0].start),'13:00');
 await page.evaluate(()=>{data.shifts.find(x=>x.id==='remind-a').status='completed';save()});assert.equal(await page.evaluate(()=>window.shiftPayload.shifts.length),0);
 await page.evaluate(()=>{data.shifts=data.shifts.filter(x=>!x.id.startsWith('remind-'));save()});assert.equal(await page.evaluate(()=>window.shiftPayload.shifts.length),0);
 await page.selectOption('#shiftReminderScope','all');await page.selectOption('#shiftReminderUnit','hours');await page.fill('#shiftReminderAmount','3');await page.click('#shiftReminderForm button[type=submit]');
 await page.reload();assert.equal(await page.evaluate(()=>shiftReminderConfig().amount),3);assert.equal(await page.evaluate(()=>shiftReminderConfig().enabled),true);
 assert.equal(await page.evaluate(()=>JSON.parse(backupDocument()).data.shiftReminders.enabled),false);
 for(const lang of ['en','de'])for(const theme of ['light','dark']){
  await page.setViewportSize({width:360,height:800});await page.evaluate(({lang,theme})=>{q('#language').value=lang;q('#language').dispatchEvent(new Event('change'));data.settings.theme=theme;applyTheme();deviceSection='reminders';show('device')},{lang,theme});
  assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1),false);await page.screenshot({path:`test-results/shift-reminders-${lang}-${theme}.png`,fullPage:true});
 }
 await page.evaluate(()=>{q('#language').value='en';q('#language').dispatchEvent(new Event('change'));show('appSettings')});assert.equal(await page.isDisabled('#googleSignIn'),true);
 await page.evaluate(()=>{window.accountState={configured:true,email:'',busy:false};window.Android={googleAccountState:()=>JSON.stringify(window.accountState),googleSignIn:()=>window.signInClicked=true,googleSignOut:()=>window.signOutClicked=true};renderGoogleAccount()});await page.click('#googleSignIn');assert.equal(await page.evaluate(()=>window.signInClicked),true);
 await page.evaluate(()=>{window.accountState.email='example@example.com';googleAccountChanged('signedIn')});assert.equal(await page.locator('#googleAccountInfo').innerText(),'example@example.com');await page.click('#googleSignIn');assert.equal(await page.evaluate(()=>window.signOutClicked),true);
 await page.evaluate(()=>{delete window.Android;renderGoogleAccount();show('dashboard');data.settings.theme='light';applyTheme()});await page.screenshot({path:'test-results/euro-icon.png'});
 await page.evaluate(()=>{window.Android={shiftReminderStatus:()=>JSON.stringify({allowed:false,blocked:1,exact:false}),testShiftReminder:()=>shiftReminderTestResult(false),retryShiftReminders:()=>window.retried=true};deviceSection='reminders';show('device')});
 assert.match(await page.locator('#shiftDeliveryStatus').innerText(),/Notifications are blocked/);await page.click('#testShiftDelivery');assert.match(await page.locator('#shiftTestResult').innerText(),/Test blocked/);
 await page.click('#retryShiftDelivery');await page.click('#confirmOk');assert.equal(await page.evaluate(()=>window.retried),true);
 await page.evaluate(()=>{Android.shiftReminderStatus=()=>JSON.stringify({allowed:true,next:Date.now()+3600000,count:2,exact:true});renderShiftDeliveryStatus()});assert.match(await page.locator('#shiftDeliveryStatus').innerText(),/No matching upcoming shifts/);
 assert.deepEqual(errors,[]);console.log('PASS: shift reminder diagnostics, all/selected/edit/delete/persistence, Google UI states, navigation, ISO weeks, icons, responsive views');await browser.close();server.close();
})().catch(e=>{console.error(e);server.close();process.exit(1)});
