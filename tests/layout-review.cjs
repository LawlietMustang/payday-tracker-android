const assert=require('node:assert/strict');
const fs=require('node:fs');
module.exports=async(browser,url)=>{
 fs.mkdirSync('test-results',{recursive:true});
 const page=await browser.newPage({viewport:{width:390,height:844}}),errors=[],failed=[];
 page.on('pageerror',e=>errors.push(e.message));page.on('response',r=>{if(r.status()>=400)failed.push(r.url())});
 await page.goto(url);
 await page.evaluate(()=>{data.onboardingCompleted=true;delete data.onboardingDraft;data.settings.theme='light';save()});await page.reload();
 assert.deepEqual(await page.locator('link[rel=stylesheet]').evaluateAll(els=>els.map(e=>e.getAttribute('href'))),['./design.css'],'One static stylesheet owns the UI');
 for(const language of ['en','de'])for(const theme of ['light','dark'])for(const width of [320,390,768]){
  await page.setViewportSize({width,height:844});
  await page.evaluate(({language,theme})=>{q('#language').value=language;q('#language').dispatchEvent(new Event('change'));data.settings.theme=theme;applyTheme();show('dashboard')},{language,theme});
  assert.equal(await page.evaluate(()=>q('#wageOverview').nextElementSibling.id),'designShortcuts');
  await page.locator('#designShortcuts [data-route=reminders]').click();
  assert.equal(await page.evaluate(()=>q('.view.active').id+':'+deviceSection),'device:reminders');
  await page.evaluate(()=>handleAppBack());assert.equal(await page.evaluate(()=>q('.view.active').id),'dashboard');
  if(width<751){await page.click('#menuOpen');await page.locator('#drawer [data-route=appSettings]').click()}
  else await page.locator('.shell>aside [data-route=appSettings]').click();
  assert.deepEqual(await page.locator('#drawer [data-route]').evaluateAll(els=>els.map(e=>e.dataset.route)),['dashboard','shifts','expenses','history','appSettings']);
  assert.equal(await page.locator('#settingsMenu [data-route=pay]').isVisible(),true,'Pay & tax is directly accessible');
  for(const [id,view,panel] of [['profile','profile'],['pay','settings'],['workplaces','workplaces'],['planning','planning'],['reminders','device','reminders'],['lock','device','lock'],['recovery','recovery']]){
   const text=await page.locator('#settingsMenu [data-route="'+id+'"] [data-route-label]').innerText();
   assert.ok(text.length>0);await page.locator('#settingsMenu [data-route="'+id+'"]').click();
   assert.equal(await page.evaluate(()=>q('.view.active').id),view);
   if(panel)assert.equal(await page.evaluate(()=>deviceSection),panel);
   assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1),false,`${id} ${language} ${theme} ${width}`);
   await page.evaluate(()=>handleAppBack());assert.equal(await page.evaluate(()=>q('.view.active').id),'appSettings');
  }
  for(const [id,file] of [['workplaces','workplaces.svg'],['planning','budgets-goals.svg'],['reminders','reminders-widget.svg']]){
   const icon=page.locator('#settingsMenu [data-icon="'+id+'"]');
   assert.equal(await icon.evaluate((el,file)=>getComputedStyle(el).maskImage.includes(file),file),true);
   assert.equal(await icon.evaluate(el=>el.getBoundingClientRect().width),24);
  }
  await page.locator('#appearanceSection summary').click();assert.equal(await page.locator('#language').isVisible(),true);assert.equal(await page.locator('#theme').isVisible(),true);await page.locator('#appearanceSection summary').click();
  assert.equal(await page.locator('#settingsDelete').count(),1);assert.equal(await page.locator('#openRecovery').count(),1);
  if(width===390){await page.screenshot({path:`test-results/settings-hub-${language}-${theme}.png`,fullPage:true});await page.evaluate(()=>show('dashboard'));await page.screenshot({path:`test-results/shortcuts-${language}-${theme}.png`,fullPage:true})}
 }
 assert.deepEqual(errors,[]);assert.deepEqual(failed,[]);await page.close();
};
