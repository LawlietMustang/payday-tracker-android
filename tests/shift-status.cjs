const assert=require('node:assert/strict');
module.exports=async(browser,url)=>{
 const context=await browser.newContext({viewport:{width:390,height:844},timezoneId:'Europe/Berlin'}),page=await context.newPage(),errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.clock.install({time:new Date('2030-05-15T08:00:00Z')});await page.goto(url);
 await page.evaluate(()=>{data.onboardingCompleted=true;data.shifts=[];data.settings.taxMode='manual';data.settings.deductionPercent=0;data.settings.autoCompletePlanned=true;localStorage.setItem('lohnzeit-language','en');save()});await page.reload();
 await page.evaluate(()=>openDialog());await page.fill('#date','2030-05-16');await page.fill('#start','12:00');assert.equal(await page.inputValue('#status'),'planned');
 await page.selectOption('#status','completed');await page.fill('#date','2030-05-17');await page.fill('#start','13:00');assert.equal(await page.inputValue('#status'),'completed');
 await page.click('#statusAutomatic');assert.equal(await page.inputValue('#status'),'planned');await page.selectOption('#status','completed');await page.locator('#shiftForm').evaluate(f=>f.requestSubmit());
 const manual=await page.evaluate(()=>data.shifts[0].id);await page.evaluate(id=>openDialog(id),manual);await page.fill('#date','2030-05-18');assert.equal(await page.inputValue('#status'),'completed');await page.click('#cancel');
 // Templates follow the date; an explicit user status still wins.
 await page.evaluate(()=>{data.templates=[{id:'morning',name:'Morning',start:'06:00',end:'14:00',breakMin:30,status:'completed',note:''}];openDialog()});await page.fill('#date','2030-05-20');await page.selectOption('#templateSelect','morning');assert.equal(await page.inputValue('#status'),'planned');
 await page.selectOption('#status','cancelled');await page.selectOption('#cancelledBy','employer');await page.fill('#start','07:00');assert.equal(await page.inputValue('#status'),'cancelled');await page.locator('#shiftForm').evaluate(f=>f.requestSubmit());
 const cancelled=await page.evaluate(()=>data.shifts.find(s=>s.status==='cancelled').id);assert.equal(await page.evaluate(()=>data.shifts.find(s=>s.status==='cancelled').cancelledBy),'employer');
 assert.equal(await page.evaluate(id=>grossEntry(data.shifts.find(s=>s.id===id)),cancelled),0);assert.equal(await page.evaluate(id=>bonusForEntry(data.shifts.find(s=>s.id===id)).total,cancelled),0);assert.equal(await page.evaluate(id=>shiftReminderPayload().shifts.some(s=>s.id===id),cancelled),false);
 await page.reload();await page.evaluate(()=>{selected='2030-05';render();show('shifts')});assert.match(await page.locator('#cancellationSummary').innerText(),/Employer: 1/);assert.match(await page.locator('[data-calendar-date="2030-05-20"]').getAttribute('class'),/cancelled/);
 await page.click('[data-calendar-date="2030-05-20"]');assert.match(await page.locator('#calendarDay').innerText(),/Cancelled/);await page.click('#closeCalendarDay');
 const roundtrip=await page.evaluate(()=>validateBackup(backupDocument()).data.shifts.find(s=>s.status==='cancelled'));assert.equal(roundtrip.cancelledBy,'employer');
 const csv=await page.evaluate(()=>{window.Android={exportCsv:(csv)=>window.capturedCsv=csv};exportCsv();delete window.Android;return atob(capturedCsv)});const row=csv.split('\r\n').find(r=>r.includes(',cancelled,'));assert.match(row,/,0,0\.00,0\.00,0\.00,0\.00,0\.00,0\.00,cancelled,employer,/);
 // Mixed date batches infer each date, not just the date shown in the form.
 await page.evaluate(()=>{openDialog();q('#date').value='2030-05-14';q('#start').value='08:00';q('#end').value='16:00';q('#bulk').checked=true;bulkDates=new Set(['2030-05-14','2030-05-21']);updateAutoShiftStatus();submitShift(new Event('submit'))});
 assert.equal(await page.evaluate(()=>data.shifts.find(s=>s.date==='2030-05-14').status),'completed');assert.equal(await page.evaluate(()=>data.shifts.find(s=>s.date==='2030-05-21').status),'planned');
 // Bulk cancellation retains all records and cancels reminders.
 await page.evaluate(()=>{selectedShiftIds=new Set(data.shifts.filter(s=>s.date==='2030-05-21').map(s=>s.id));openBulkEdit()});await page.selectOption('#bulkStatus','cancelled');await page.selectOption('#bulkCancelledBy','me');await page.locator('#bulkEditForm').evaluate(f=>f.requestSubmit());assert.equal(await page.evaluate(()=>data.shifts.find(s=>s.date==='2030-05-21').cancelledBy),'me');
 // Overnight completion at end, cancellation immunity and opt-out.
 await page.evaluate(()=>{data.shifts=[{id:'night',date:'2030-05-14',start:'22:00',end:'06:00',minutes:450,breakMin:30,wage:15,status:'planned',workplaceId:'default'},{id:'cancel',date:'2030-05-14',start:'08:00',end:'16:00',minutes:450,breakMin:30,wage:15,status:'cancelled',cancelledBy:'me',workplaceId:'default'}]});
 assert.equal(await page.evaluate(()=>reconcileShiftStatuses(new Date('2030-05-15T05:59:59').getTime())),false);
 assert.equal(await page.evaluate(()=>reconcileShiftStatuses(new Date('2030-05-15T06:00:00').getTime())),true);assert.equal(await page.evaluate(()=>reconcileShiftStatuses(new Date('2030-05-15T06:01:00').getTime())),false);
 assert.equal(await page.evaluate(()=>data.shifts[1].status),'cancelled');
 await page.evaluate(()=>show('appSettings'));await page.uncheck('#autoCompletePlanned');await page.evaluate(()=>{data.shifts[0].status='planned';save()});await page.reload();assert.equal(await page.evaluate(()=>data.shifts[0].status),'planned');await page.evaluate(()=>show('appSettings'));await page.check('#autoCompletePlanned');assert.equal(await page.evaluate(()=>data.shifts[0].status),'completed');
 // Matching planned work is protected while timing and is updated, never duplicated.
 await page.evaluate(()=>{data.shifts=[{id:'clock-plan',date:'2030-05-15',start:'09:00',end:'17:00',minutes:450,breakMin:30,wage:18,status:'planned',workplaceId:'default',note:'Scheduled work',created:1}];show('dashboard');startWorkTimer()});assert.equal(await page.evaluate(()=>data.activeTimer.shiftId),'clock-plan');assert.match(await page.locator('#clockLiveBadge').innerText(),/Ongoing/);
 await page.evaluate(()=>startWorkBreak());assert.match(await page.locator('#clockLiveBadge').innerText(),/On break/);await page.evaluate(()=>resumeWorkTimer());
 await page.clock.setFixedTime(new Date('2030-05-15T15:01:00Z'));assert.equal(await page.evaluate(()=>reconcileShiftStatuses()),false);assert.equal(await page.evaluate(()=>data.shifts[0].status),'planned');
 await page.evaluate(()=>{void finishWorkTimer()});await page.click('#confirmOk');assert.equal(await page.evaluate(()=>data.shifts.length),1);assert.equal(await page.evaluate(()=>data.shifts[0].status),'completed');assert.equal(await page.evaluate(()=>data.shifts[0].plannedStart),'09:00');assert.equal(await page.locator('#clockLiveBadge').isVisible(),false);
 // Closed-app catch-up and forecast exclusion on a cancelled future workday.
 await page.evaluate(()=>{data.shifts.push({id:'catchup',date:'2030-05-14',start:'08:00',end:'16:00',minutes:480,breakMin:0,wage:15,status:'planned',workplaceId:'default'});save()});await page.reload();assert.equal(await page.evaluate(()=>data.shifts.find(s=>s.id==='catchup').status),'completed');
 const forecastDelta=await page.evaluate(()=>{selected='2030-05';data.settings.days=[0,1,2,3,4,5,6];const before=forecast(summary(selected)).minutes;data.shifts.push({id:'cancel-forecast',date:'2030-05-16',start:'08:00',end:'16:00',minutes:480,breakMin:0,wage:15,status:'cancelled',cancelledBy:'employer',workplaceId:'default'});return before-forecast(summary(selected)).minutes});assert.ok(forecastDelta>0,'Cancelled date is not replaced by inferred forecast work');
 for(const lang of ['en','de'])for(const theme of ['light','dark']){
  await page.evaluate(({lang,theme})=>{q('#language').value=lang;q('#language').dispatchEvent(new Event('change'));data.settings.theme=theme;applyTheme();render();show('shifts')},{lang,theme});await page.screenshot({path:`test-results/status-calendar-${lang}-${theme}.png`,fullPage:true});
  await page.evaluate(()=>openDialog('cancel-forecast'));assert.equal(await page.locator('#cancelledByField').isVisible(),true);assert.equal(await page.evaluate(()=>q('#dialog').scrollWidth>innerWidth),false);await page.screenshot({path:`test-results/status-editor-${lang}-${theme}.png`});await page.click('#cancel');
 }
 assert.deepEqual(errors,[]);await context.close();console.log('PASS: automatic/manual statuses, cancellation reason/batch/CSV/backup/forecast, overnight completion, opt-out, reopening and linked clock completion');
};
