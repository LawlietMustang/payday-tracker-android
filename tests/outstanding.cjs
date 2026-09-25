const assert=require('node:assert/strict');
module.exports=async(browser,url)=>{
 const context=await browser.newContext({viewport:{width:390,height:844},timezoneId:'Europe/Berlin'}),page=await context.newPage(),errors=[];
 page.on('pageerror',e=>errors.push(e.message));
 await page.clock.install({time:new Date('2030-01-15T08:00:00Z')});await page.goto(url);
 await page.evaluate(()=>{data.onboardingCompleted=true;data.settings.autoCompletePlanned=false;data.settings.taxMode='manual';data.settings.deductionPercent=0;data.settings.nightRate=0;data.settings.sundayRate=0;data.settings.holidayRate=0;data.settings.overtimeRate=0;data.settings.days=[];data.settings.target=160;data.settings.theme='light';data.workplaces=[{id:'default',name:'First job',wage:10},{id:'second',name:'Second job',wage:20}];
 data.shifts=[{id:'done',date:'2030-01-14',start:'08:00',end:'16:00',minutes:480,wage:10,workplaceId:'default',status:'completed',breakMin:0},{id:'plan',date:'2030-01-16',start:'09:00',end:'13:00',minutes:240,wage:20,workplaceId:'second',status:'planned',breakMin:0},{id:'cancel',date:'2030-01-17',start:'08:00',end:'16:00',minutes:480,wage:10,workplaceId:'default',status:'cancelled',breakMin:0}];data.expenses=[{id:'expense',date:'2030-01-10',amount:30,category:'groceries'}];data.recurringExpenses=[];localStorage.setItem('lohnzeit-language','en');save()});await page.reload();
 const totals=await page.evaluate(()=>{selected='2030-01';workplaceFilter='all';render();const s=summary(selected),f=forecast(s);return{s:s.gross,minutes:s.minutes,worked:s.workedMinutes,count:s.completed.length,forecast:f.gross,forecastMinutes:f.minutes,cutoff:summary(selected,'all',new Date('2030-01-15'),false).gross}});
 assert.deepEqual(totals,{s:160,minutes:720,worked:480,count:1,forecast:160,forecastMinutes:720,cutoff:80});
 assert.match(await page.locator('#gross').innerText(),/80/);assert.match(await page.locator('#worked').innerText(),/8 h/);assert.match(await page.locator('#designAvailable').innerText(),/130/);
 // Expenses are app-wide even when the overview is filtered to one workplace.
 await page.evaluate(()=>{workplaceFilter='default';renderExpenses()});assert.match(await page.locator('#afterExpenses').innerText(),/130/);
 const projected=await page.evaluate(()=>{workplaceFilter='all';data.settings.days=[0,1,2,3,4,5,6];const s=summary(selected),f=forecast(s);data.settings.days=[];return f});
 // Jan16 is scheduled and Jan17 cancelled. Only the 14 remaining empty dates extrapolate.
 assert.equal(projected.minutes,720+14*360);assert.equal(projected.gross,160+14*80);
 const payload=await page.evaluate(()=>{data.settings.payslipReminder=true;data.payslips=[{id:'p',month:'2030-01',workplaceId:'default',actualGross:80,actualNet:75}];return payslipReminderPayload()});
 assert.equal(payload.months[0].missing,true);
 assert.equal(await page.evaluate(()=>{data.payslips.push({id:'p2',month:'2030-01',workplaceId:'second',actualGross:80,actualNet:78});return payslipReminderPayload().months[0].missing}),false);
 // Widget data includes all upcoming shifts even with reminders off/selected.
 const glance=await page.evaluate(()=>{data.shiftReminders={enabled:false,scope:'selected',ids:[]};const p=shiftReminderPayload();return {notification:p.shifts.length,glance:p.glanceShifts.map(s=>s.id)}});
 assert.deepEqual(glance,{notification:0,glance:['plan']});
 const chart=await page.evaluate(()=>{data.payslips.push({id:'old',month:'2028-12',workplaceId:'default',actualNet:50,actualGross:55},{id:'gap',month:'2029-11',workplaceId:'default',actualNet:0,actualGross:0});renderPayslipComparisons();return payslipTrendData(data.payslips).map(p=>({month:p.month,actual:p.actual,estimate:p.estimate}))});
 assert.equal(chart.length,12);assert.equal(chart[0].month,'2029-02');assert.equal(chart[10].actual,null);assert.equal(chart[9].actual,0);assert.equal(chart[11].actual,153);assert.equal(chart[11].estimate,160);
 for(const width of [320,390,768])for(const language of ['en','de'])for(const theme of ['light','dark']){
  await page.setViewportSize({width,height:844});await page.evaluate(({language,theme})=>{q('#language').value=language;q('#language').dispatchEvent(new Event('change'));data.settings.theme=theme;applyTheme();render();show('history')},{language,theme});
  assert.equal(await page.locator('.payslip-trend svg').count(),1);assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false);
  if(width===390)await page.screenshot({path:`test-results/payslip-trend-${language}-${theme}.png`,fullPage:true});
  await page.evaluate(()=>show('appSettings'));
  for(const [id,file]of [['profile','profile'],['lock','lock'],['recovery','cloud-sync']])assert.match(await page.locator(`#settingsMenu [data-icon="${id}"]`).evaluate(el=>getComputedStyle(el).maskImage),new RegExp(file+'\\.svg'));
 }
 // Compact control changes persist and call the native bridge; notification tapping opens the right month.
 await page.evaluate(()=>{window.Android={syncPayslipReminder:p=>window.lastPayslip=JSON.parse(p),requestShiftNotifications:()=>{},consumePayslipReminder:()=> '2029-12'};deviceSection='reminders';show('device')});
 await page.uncheck('#payslipReminderEnabled');assert.equal(await page.evaluate(()=>window.lastPayslip.enabled),false);await page.check('#payslipReminderEnabled');assert.equal(await page.evaluate(()=>window.lastPayslip.enabled),true);
 await page.evaluate(()=>openPayslipReminder());assert.equal(await page.locator('#history').evaluate(e=>e.classList.contains('active')),true);assert.equal(await page.inputValue('#payslipMonth'),'2029-12');
 assert.deepEqual(errors,[]);await context.close();console.log('PASS: inclusive income, completed-only hours, forecast, supplied icons, payslip trend and reminder control');
};
