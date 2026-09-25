const assert=require('node:assert/strict');
const fs=require('node:fs');
module.exports=async function(browser,url){
 const page=await browser.newPage({viewport:{width:360,height:800}}),errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.goto(url);await page.waitForSelector('#setupNext');
 await page.selectOption('#setupLanguage','en');
 assert.equal(await page.evaluate(()=>data.onboardingCompleted),false);assert.match(await page.locator('.setup-brand').innerText(),/WageTrack/);
 assert.equal(await page.evaluate(()=>window.handleAppBack()),false);
 await page.fill('#setupPersonalName','Alex');await page.selectOption('#setupBasicsCountry','GB');await page.click('#setupNext');assert.equal(await page.inputValue('#setupWage'),'');assert.equal(await page.inputValue('#setupTarget'),'');await page.fill('#setupName','Evening café');await page.fill('#setupWage','20');await page.fill('#setupTarget','80');
 assert.equal(await page.locator('#setupTaxClasses').isVisible(),false);assert.equal(await page.locator('#setupDeduction').isVisible(),true);
 await page.selectOption('#setupCountry','DE');assert.equal(await page.locator('[data-tax-class]').count(),6);await page.click('[data-tax-class=III]');assert.equal(await page.getAttribute('[data-tax-class=III]','aria-pressed'),'true');
 await page.fill('#setupDeduction','17');assert.equal(await page.inputValue('#setupTax'),'manual');await page.selectOption('#setupTax','germany');assert.equal(await page.locator('#setupDeduction').isVisible(),true,'Manual fallback always available');
 await page.selectOption('#setupCountry','BD');assert.equal(await page.locator('#setupTaxClasses').isVisible(),false);
 await page.click('#setupCurrency');await page.fill('#currencySearch','bangladeshi');await page.click('[data-currency="BDT"]');
 assert.equal(await page.inputValue('#setupTax'),'manual');await page.click('#setupCurrency');await page.fill('#currencySearch','USD');await page.click('[data-currency="USD"]');await page.fill('#setupDeduction','20');
 await page.reload();await page.waitForSelector('#setupName');assert.equal(await page.inputValue('#setupName'),'Evening café');assert.equal(await page.inputValue('#setupWage'),'20');assert.equal(await page.inputValue('#setupCountry'),'BD');assert.equal(await page.evaluate(()=>setupDraft.taxclass),'III');assert.match(await page.locator('#setupCurrency').innerText(),/USD/);
 fs.mkdirSync('test-results',{recursive:true});
 for(const width of [320,360,412,768])for(const lang of ['en','de'])for(const theme of ['light','dark']){
  await page.setViewportSize({width,height:800});await page.evaluate(({lang,theme})=>{localStorage.setItem('lohnzeit-language',lang);data.settings.theme=theme;applyTheme();drawSetup()},{lang,theme});
  for(const step of [0,1,2,3]){await page.evaluate(step=>{setupStep=step;drawSetup()},step);assert.equal(await page.evaluate(()=>q('#onboarding').scrollWidth>innerWidth+1),false,`setup overflow ${width} ${lang} ${theme} ${step}`);assert.equal(await page.evaluate(()=>q('#setupNext').getBoundingClientRect().bottom<=innerHeight),true,'Next remains visible');if(step===0)assert.equal(await page.evaluate(()=>{const r=q('#setupRestore').getBoundingClientRect();return r.top>=0&&r.bottom<=innerHeight&&document.elementFromPoint(r.x+r.width/2,r.y+r.height/2)===q('#setupRestore')}),true,'Restore is fully visible without scrolling');if(step===3){assert.equal(await page.evaluate(()=>{const b=q('#setupNext'),r=document.createRange();r.selectNodeContents(b);return r.getBoundingClientRect().height<30&&b.scrollWidth<=b.clientWidth}),true,'Continue label stays on one line');assert.equal(await page.locator('.setup-reminder-icon svg').count(),1)}if(step===0)assert.equal(await page.evaluate(()=>getComputedStyle(q('#setupNext')).justifyContent),'center');if(step===1)assert.equal(await page.evaluate(()=>Math.abs(q('#setupNext').getBoundingClientRect().width-q('#setupBack').getBoundingClientRect().width)<2),true,'Balanced Back and Next widths')}
  if(width===360&&lang==='en'){for(const step of [0,1,2,3]){await page.evaluate(step=>{setupStep=step;drawSetup()},step);await page.screenshot({path:`test-results/setup-${step}-${theme}.png`})}}
 }
 await page.evaluate(()=>{localStorage.setItem('lohnzeit-language','en');setupStep=1;drawSetup()});await page.click('#setupNext');assert.equal(await page.evaluate(()=>window.handleAppBack()),true);assert.equal(await page.evaluate(()=>setupStep),1);await page.click('#setupNext');
 await page.fill('#setupDate','2026-01-05');await page.fill('#setupStart','09:00');await page.fill('#setupEnd','17:00');await page.click('[data-break="30"]');
 assert.match(await page.locator('#setupPreview').innerText(),/150\.00/);
 await page.fill('#setupBreak','600');await page.click('#setupNext');assert.equal(await page.evaluate(()=>setupStep),2);assert.match(await page.locator('#setupError').innerText(),/End must follow start/);
 await page.fill('#setupBreak','30');await page.click('#setupNext');await page.click('#setupNext');
 assert.equal(await page.evaluate(()=>setupActive),false);assert.equal(await page.evaluate(()=>data.shifts.length),1);
 assert.equal(await page.evaluate(()=>summary('2026-01').gross),150);assert.equal(await page.evaluate(()=>summary('2026-01').est.net),120);
 assert.equal(await page.evaluate(()=>currencyCode()),'USD');assert.equal(await page.evaluate(()=>data.settings.holidayRate),0);
 await page.reload();assert.equal(await page.evaluate(()=>setupActive),false);assert.equal(await page.evaluate(()=>data.shifts.length),1);
 await page.evaluate(()=>show('settings'));assert.equal(await page.locator('#settingsCurrency').isDisabled(),true);assert.match(await page.locator('#wage').locator('..').innerText(),/USD/);
 assert.equal(await page.locator('#taxclass').isVisible(),false);await page.click('#settingsForm button[type="submit"]');assert.equal(await page.evaluate(()=>data.settings.currency),'USD');
 const backup=await page.evaluate(()=>validateBackup(backupDocument()));assert.equal(backup.data.settings.currency,'USD');assert.equal(backup.data.settings.country,'BD');assert.equal(backup.data.settings.taxclass,'III');
 // Reopening the guide neither erases records nor creates duplicate shifts.
 await page.evaluate(()=>show('appSettings'));assert.equal(await page.locator('#openSetup').count(),0);await page.evaluate(()=>openSetup());await page.click('#setupNext');assert.equal(await page.locator('#setupCurrency').isDisabled(),true);await page.click('#setupNext');await page.click('#setupSkip');await page.click('#setupNext');assert.equal(await page.evaluate(()=>data.shifts.length),1);
 // Fresh setup: skip is valid; German estimates remain the existing EUR calculation.
 await page.evaluate(()=>localStorage.clear());await page.reload();await page.waitForSelector('#setupNext');await page.fill('#setupPersonalName','Sam');await page.selectOption('#setupBasicsCountry','DE');await page.click('#setupNext');await page.fill('#setupName','First job');await page.fill('#setupWage','18');await page.fill('#setupTarget','80');await page.selectOption('#setupCountry','DE');await page.click('[data-tax-class=IV]');assert.equal(await page.inputValue('#setupDeduction'),'0','A zero deduction must not become an empty required field');await page.screenshot({path:'test-results/setup-tax-germany.png'});await page.click('#setupNext');await page.click('#setupSkip');await page.click('#setupNext');assert.equal(await page.evaluate(()=>data.shifts.length),0);assert.equal(await page.evaluate(()=>data.settings.taxMode),'germany');assert.equal(await page.evaluate(()=>data.settings.taxclass),'IV');assert.equal(await page.evaluate(()=>data.settings.country),'DE');
 await page.reload();assert.equal(await page.evaluate(()=>data.settings.taxclass),'IV');
 await page.evaluate(()=>{void askConfirm('Restore this backup?','Restore backup')});assert.equal(await page.locator('#confirmDialog .brand-info [data-icon=alert]').count(),1);await page.screenshot({path:'test-results/restore-info.png'});await page.click('#confirmCancel');
 assert.deepEqual(errors,[]);await page.close();console.log('PASS: onboarding resume, currencies, manual estimate, back, skip, saved records and responsive light/dark layouts');
};
