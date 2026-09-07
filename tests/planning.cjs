const {chromium}=require('playwright');
const http=require('node:http'),fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve('app/src/main/assets');
const server=http.createServer((req,res)=>{const file=path.join(root,req.url==='/'?'index.html':req.url);if(!file.startsWith(root)){res.writeHead(404).end();return}try{res.setHeader('Content-Type',file.endsWith('.js')?'application/javascript':file.endsWith('.css')?'text/css':'text/html');res.end(fs.readFileSync(file))}catch{res.writeHead(404).end()}});
(async()=>{
 await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));const url='http://127.0.0.1:'+server.address().port;
 const browser=await chromium.launch({headless:true});const page=await browser.newPage({viewport:{width:360,height:800}});let errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.goto(url);await page.waitForSelector('#planning',{state:'attached'});
 await page.evaluate(()=>{localStorage.setItem('lohnzeit-language','en');data.settings.theme='dark';data.shifts.push({id:'legacy',date:'2026-09-01',start:'08:00',end:'16:00',minutes:480,breakMin:0,wage:15,status:'completed',created:1});save()});await page.reload();
 await page.click('#menuOpen');await page.click('#drawer [data-view="workplaces"]');await page.click('#addPlaceNow');
 await page.fill('#placeName','Second job');await page.fill('#placeWage','20');await page.click('#planningSave');
 assert.equal(await page.evaluate(()=>data.workplaces.length),2);assert.equal(await page.locator('#placesContent').innerText().then(s=>s.includes('Second job')),true);
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
  for(const view of ['dashboard','workplaces','planning','history','settings']){await page.evaluate(v=>show(v),view);await page.waitForTimeout(30);const overflow=await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1);assert.equal(overflow,false,`overflow ${width} ${lang} ${theme} ${view}`);}
  if(width===360&&lang==='en'){await page.evaluate(()=>show('planning'));await page.screenshot({path:`test-results/planning-${theme}.png`,fullPage:true});await page.evaluate(()=>show('workplaces'));await page.screenshot({path:`test-results/workplaces-${theme}.png`,fullPage:true})}
 }
 assert.deepEqual(errors,[]);console.log('PASS: startup, workplace creation, migration, budget and goal persistence, forecast, 80 responsive view checks');await browser.close();server.close();
})().catch(e=>{console.error(e);server.close();process.exit(1)});
