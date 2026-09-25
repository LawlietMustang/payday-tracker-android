const assert=require('node:assert/strict');
module.exports=async(browser,url)=>{
 const page=await browser.newPage({viewport:{width:390,height:844}}),errors=[];page.on('pageerror',e=>errors.push(e.message));await page.goto(url);
 await page.evaluate(()=>{
  data.settings.autoCompletePlanned=false;data.onboardingCompleted=true;delete data.onboardingDraft;data.profile={name:'Alex'};data.settings.theme='light';data.settings.taxMode='manual';data.settings.deductionPercent=20;data.settings.overtimeAfter=160;data.settings.sundayRate=0;data.settings.holidayRate=0;data.settings.nightRate=0;
  data.shifts=[{id:'done',date:'2026-09-07',start:'08:00',end:'16:00',minutes:450,breakMin:30,wage:20,status:'completed',workplaceId:'default',created:1},{id:'overlap-a',date:'2026-09-15',start:'09:00',end:'17:00',minutes:480,breakMin:0,wage:20,status:'planned',workplaceId:'default'},{id:'overlap-b',date:'2026-09-15',start:'16:00',end:'20:00',minutes:240,breakMin:0,wage:20,status:'planned',workplaceId:'default'}];
  data.expenses=[{id:'food',date:'2026-09-01',amount:40,category:'groceries'}];data.recurringExpenses=[];data.savingsGoals=[{id:'laptop',name:'Laptop',target:1000,saved:680,due:'2026-12-01'},{id:'travel',name:'Travel fund',target:2000,saved:680,due:''}];data.budgets={'2026-09':{total:300,groceries:50}};data.payslips=[{id:'pay',month:'2026-09',workplaceId:'default',actualNet:115,actualGross:150}];data.templates=[{id:'early',name:'Early shift',start:'06:00',end:'14:00',breakMin:30,status:'planned',note:''}];localStorage.setItem('lohnzeit-language','en');save();
 });await page.reload();await page.evaluate(()=>{selected='2026-09';q('#month').value=selected;render();show('dashboard')});
 assert.equal(await page.evaluate(()=>!!(q('#earningsDetails').compareDocumentPosition(q('#payReceipt'))&Node.DOCUMENT_POSITION_FOLLOWING)),true,'Earnings before payslip');
 assert.equal(await page.locator('#designAvailable').innerText(),await page.evaluate(()=>money(272)),'Hero includes completed and planned net less expenses');
 assert.equal(await page.locator('.receipt-stamp').innerText(),await page.evaluate(()=>money(-197)),'Receipt compares recorded payslips with inclusive estimates');
 await page.evaluate(()=>show('shifts'));assert.equal(await page.locator('[data-calendar-date="2026-09-07"]').getAttribute('class').then(x=>x.includes('completed')),true);assert.equal(await page.locator('[data-calendar-date="2026-09-15"]').getAttribute('class').then(x=>x.includes('conflict')),true);
 await page.click('[data-calendar-date="2026-09-15"]');assert.equal(await page.locator('#calendarDay .day-shift').count(),2);await page.locator('#calendarDay .day-shift').first().click();assert.equal(await page.inputValue('#editId'),'overlap-a');await page.click('#cancel');
 await page.click('[data-calendar-date="2026-09-10"]');await page.click('#addCalendarShift');assert.equal(await page.inputValue('#date'),'2026-09-10');await page.click('#cancel');
 await page.click('[data-quick-template="early"]');assert.equal(await page.inputValue('#start'),'06:00');assert.equal(await page.inputValue('#date'),'2026-09-10');await page.click('#cancel');
 // Selected days keep their shift status color, with an additional ring/check and accessible state.
 await page.click('#calendarNewShift');await page.check('#bulk');
 await page.evaluate(()=>{calendarMonth=new Date('2026-09-01T12:00');bulkDates=new Set();renderCalendar()});
 for(const date of ['2026-09-07','2026-09-15']){const cell=page.locator('#calendar [data-date="'+date+'"]');const color=await cell.evaluate(el=>getComputedStyle(el).backgroundColor);await cell.click();assert.equal(await cell.getAttribute('aria-pressed'),'true');assert.equal(await cell.locator('.selection-check').count(),1);assert.equal(await cell.evaluate(el=>getComputedStyle(el).backgroundColor),color);assert.equal(await cell.evaluate(el=>getComputedStyle(el).outlineWidth),'3px')}
 await page.screenshot({path:'test-results/calendar-multi-select.png'});await page.click('#calendar [data-date="2026-09-07"]');assert.equal(await page.locator('#calendar [data-date="2026-09-07"]').getAttribute('aria-pressed'),'false');assert.equal(await page.evaluate(()=>bulkDates.size),1);await page.click('#cancel');
 await page.click('#shiftMonthPrevious');assert.equal(await page.evaluate(()=>selected),'2026-08');await page.click('#shiftMonthNext');
 await page.evaluate(()=>show('planning'));assert.equal(await page.locator('.workplace-picker').isVisible(),false);assert.equal(await page.locator('.goal-ring').count(),2);assert.equal(await page.locator('.goal-ring').first().getAttribute('aria-label'),'68%');
 for(const lang of ['en','de'])for(const theme of ['light','dark'])for(const width of [320,390,768]){
  await page.setViewportSize({width,height:844});await page.evaluate(({lang,theme})=>{q('#language').value=lang;q('#language').dispatchEvent(new Event('change'));data.settings.theme=theme;applyTheme()},{lang,theme});
  for(const view of ['dashboard','shifts','planning','expenses']){
   await page.evaluate(v=>show(v),view);assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1),false,`${view} ${lang} ${theme} ${width}`);
   if(width===390)await page.screenshot({path:`test-results/design-${view}-${lang}-${theme}.png`,fullPage:true});
  }
 }

 // Regression: the reported English labels and the compact shift/card layout.
 await page.setViewportSize({width:390,height:844});
 await page.evaluate(()=>{localStorage.setItem('lohnzeit-language','en');q('#language').value='en';data.workplaces[0].name='Burger King';data.savingsGoals=[{id:'default-goal',name:'Mein Sparziel',target:data.settings.savingsTarget,saved:0,due:''}];render();show('dashboard');q('#earningsDetails').open=true;translatePage('en')});
 assert.match(await page.locator('#recent .shift-time').first().innerText(),/min break/);
 assert.doesNotMatch(await page.locator('#recent').innerText(),/Pause/);
 assert.match(await page.locator('#confidence').innerText(),/^(HIGH|MEDIUM|LOW|NO DATA)$/);
 await page.evaluate(()=>show('history'));assert.match(await page.locator('#historyCards').innerText(),/Shifts/);assert.doesNotMatch(await page.locator('#historyCards').innerText(),/Schichten/);
 await page.evaluate(()=>show('planning'));assert.equal(await page.locator('.goal-grid h3').innerText(),'My savings goal');
 await page.evaluate(()=>{data.savingsGoals[0].name='Mein eigener Urlaub';renderPlanning()});assert.equal(await page.locator('.goal-grid h3').innerText(),'Mein eigener Urlaub','Custom goal names stay unchanged');
 for(const width of [320,390,430])for(const lang of ['en','de']){
  await page.setViewportSize({width,height:844});await page.evaluate(lang=>{q('#language').value=lang;q('#language').dispatchEvent(new Event('change'));show('dashboard');q('#earningsDetails').open=true},lang);
  const metrics=await page.evaluate(()=>{const row=q('#recent .shift'),t=row.querySelector('.shift-time'),parts=[...t.children].map(n=>n.getBoundingClientRect()),values=[...q('.kpis').querySelectorAll(':scope>.kpi>strong')].map(n=>n.getBoundingClientRect());return {fits:t.scrollWidth<=t.clientWidth+1,oneLine:Math.abs(parts[0].top-parts[1].top)<1,aligned:innerWidth<=360||Math.abs(values[2].top-values[3].top)<1,overflow:document.documentElement.scrollWidth>innerWidth+1}});
  assert.equal(metrics.fits,true,'Shift time and break fit '+width+' '+lang);assert.equal(metrics.oneLine,true);assert.equal(metrics.aligned,true,'Forecast and comparison baselines align');assert.equal(metrics.overflow,false);
  if(width===390)for(const view of ['dashboard','planning','history']){await page.evaluate(v=>show(v),view);await page.screenshot({path:`test-results/fixes-${view}-${lang}.png`,fullPage:true})}
 }
 await page.evaluate(()=>{data.shifts=[];data.expenses=[];data.payslips=[];data.savingsGoals=[];render();show('dashboard');q('#earningsDetails').open=false});await page.click('#earningsDetails>summary');assert.equal(await page.locator('#gross').isVisible(),true);assert.equal(await page.locator('#payReceipt .receipt-stamp').count(),0);assert.equal(await page.locator('#designAvailable').innerText(),await page.evaluate(()=>money(0)));
 assert.deepEqual(errors,[]);await page.close();
};
