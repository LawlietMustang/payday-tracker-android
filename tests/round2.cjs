const assert=require('node:assert/strict');
module.exports=async(browser,url)=>{
 const page=await browser.newPage({viewport:{width:390,height:844}}),errors=[];page.on('pageerror',e=>errors.push(e.message));await page.goto(url);
 await page.evaluate(()=>{data.onboardingCompleted=true;data.settings.taxMode='manual';data.settings.deductionPercent=0;data.settings.nightRate=0;data.settings.sundayRate=0;data.settings.holidayRate=0;data.settings.overtimeRate=0;data.shifts=[];data.expenses=[];data.recurringExpenses=[];data.savingsGoals=[];localStorage.setItem('lohnzeit-language','en');save()});await page.reload();
 const schedule=await page.evaluate(()=>[nextSavingsDate('2026-01-31','monthly',31),nextSavingsDate('2026-02-28','monthly',31),nextSavingsDate('2024-02-29','yearly',29),nextSavingsDate('2026-12-29','weekly',29)]);
 assert.deepEqual(schedule,['2026-02-28','2026-03-31','2025-02-28','2027-01-05']);
 const ledger=await page.evaluate(()=>{
  data.shifts=[{id:'pay',workplaceId:'default',date:'2026-01-01',start:'08:00',end:'18:00',minutes:600,breakMin:0,wage:20,status:'completed'}];
  const goal=(id,amount)=>({id,name:id,target:1000,saved:0,auto:true,autoInterval:'monthly',autoAmount:amount,autoAnchor:1,autoNext:'2026-02-01',autoEpoch:'epoch',due:''});
  data.savingsGoals=[goal('a',150),goal('b',100)];data.savingsLedger=[];const now=new Date('2026-02-02T12:00');
  const before=savingsAvailable(now);reconcileSavings(now);const first=JSON.stringify(data.savingsGoals);reconcileSavings(now);
  const result={before,saved:data.savingsGoals.map(g=>g.saved),overdue:data.savingsGoals[1].autoOverdue,count:data.savingsLedger.length,same:JSON.stringify(data.savingsGoals)===first};
  data.expenses=[{id:'bill',date:'2026-01-01',amount:300,category:'rent'}];data.savingsGoals=[goal('c',0)];data.savingsLedger=[];reconcileSavings(now);result.negative=data.savingsGoals[0].autoOverdue;
  data.expenses=[];reconcileSavings(now);result.full=data.savingsGoals[0].saved;result.restored=validateBackup(backupDocument()).data.savingsLedger.length;return result;
 });
 assert.deepEqual(ledger,{before:20000,saved:[150,0],overdue:50,count:1,same:true,negative:100,full:200,restored:1});
 await page.evaluate(()=>{data.shifts=[];data.savingsGoals=[];data.savingsLedger=[];show('planning')});await page.click('#addGoal');assert.equal(await page.isChecked('#goalAuto'),false);await page.fill('#goalName','Holiday');await page.fill('#goalTarget','500');await page.click('#planningSave');await page.click('[data-goal-add]');await page.fill('#goalContribution','25');await page.click('#planningSave');assert.equal(await page.evaluate(()=>data.savingsGoals[0].saved),25);
 await page.click('.goal-grid .card-info');assert.match(await page.locator('#planningInfo').innerText(),/Progress = saved/);await page.click('#closePlanningInfo');
 await page.click('#editBudget');await page.fill('#budgetTotal','500');await page.click('#newBudgetCategory');await page.fill('#promptInput','Pet care');await page.click('#promptOk');assert.equal(await page.inputValue('#budgetTotal'),'500');const custom=await page.evaluate(()=>data.customCategories[0].id);await page.fill('#budget_'+custom,'75');await page.click('#planningSave');await page.evaluate(()=>{show('expenses');openExpenseDialog()});assert.match(await page.locator('#expenseCategory').innerText(),/Pet care/);await page.evaluate(()=>handleAppBack());
 await page.evaluate(()=>{window.Android={exportBackup:()=>{}};show('recovery')});await page.click('#saveBackup');await page.evaluate(()=>window.backupResult(false));assert.equal(await page.locator('.saved.backed-up').count(),0);
 await page.click('#saveBackup');await page.evaluate(()=>window.backupResult(true));assert.equal(await page.locator('.saved.backed-up').count(),2);
 await page.evaluate(()=>{data.profile.name='Edited';save()});await page.waitForFunction(()=>!q('.saved.backed-up'));await page.click('#saveBackup');await page.evaluate(()=>{data.profile.name='Edited while saving';save()});await page.evaluate(()=>window.backupResult(true));assert.equal(await page.locator('.saved.backed-up').count(),0);
 await page.click('#saveBackup');await page.evaluate(()=>window.backupResult(true));await page.reload();await page.waitForFunction(()=>q('.saved.backed-up'));assert.equal(await page.locator('.saved.backed-up').count(),2);
 await page.evaluate(()=>{localStorage.setItem('lohnzeit-language','en');show('appSettings');labelAccount()});assert.equal(await page.locator('#theme option[value=light]').innerText(),'Light');assert.equal(await page.locator('#openSetup').count(),0);assert.equal(await page.locator('#reminderHelp').count(),0);
 await page.evaluate(()=>{openSetup();setupStep=2;setupDraft.date='2099-01-01';setupDraft.start='09:00';setupDraft.end='17:00';setupDraft.breakMin=0;setupDraft.overnight=false;drawSetup()});assert.equal(await page.evaluate(()=>firstShiftValid()),true);await page.fill('#setupEnd','08:00');assert.equal(await page.evaluate(()=>firstShiftValid()),false);await page.check('#setupOvernight');assert.equal(await page.evaluate(()=>firstShiftValid()),true);await page.fill('#setupBreak','1440');assert.equal(await page.evaluate(()=>firstShiftValid()),false);
 assert.deepEqual(errors,[]);await page.close();console.log('PASS: round2 savings ledger, recurrence, shortages, custom categories, backup snapshot status, PIN UI copy and future shifts');
};
