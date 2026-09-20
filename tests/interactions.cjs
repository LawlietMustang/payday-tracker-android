const assert=require('node:assert/strict');
module.exports=async function(browser,url){
 const context=await browser.newContext({viewport:{width:360,height:800},isMobile:true,hasTouch:true});
 const page=await context.newPage(),errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.goto(url);await page.waitForSelector('#setupNext');
 await page.evaluate(()=>{data.onboardingCompleted=true;data.settings.theme='light';localStorage.setItem('lohnzeit-language','en');save()});await page.reload();
 // The faded toast used to sit invisibly on top of the bottom navigation.
 for(let i=0;i<3;i++){
  await page.locator('.mobile [data-view="expenses"]').tap();await page.locator('#addExpense').tap();
  await page.evaluate(i=>{q('#expenseAmount').value=String(i+1);q('#expenseForm').requestSubmit()},i);
  await page.waitForTimeout(2800);
  await page.locator('.mobile [data-view="dashboard"]').tap({timeout:2000});
  assert.equal(await page.evaluate(()=>q('.view.active').id),'dashboard','Navigation works after save toast fades');
 }
 await page.evaluate(()=>{
  selected='2026-09';data.workplaceFilter=workplaceFilter='all';
  data.shifts=Array.from({length:100},(_,i)=>({id:'hold-'+i,date:'2026-09-'+String(i%28+1).padStart(2,'0'),start:'09:00',end:'17:00',breakMin:30,minutes:450,wage:15,status:'completed',workplaceId:'default',created:i}));
  save();render();show('shifts');
  window.interactionCounts={render:0,summary:0,translations:0};
  const oldRender=render,oldSummary=summary,oldTranslate=translatePage;
  render=function(...a){interactionCounts.render++;return oldRender(...a)};
  summary=function(...a){interactionCounts.summary++;return oldSummary(...a)};
  translatePage=function(...a){interactionCounts.translations++;return oldTranslate(...a)};
 });
 const client=await context.newCDPSession(page);
 async function point(locator){await locator.scrollIntoViewIfNeeded();const r=await locator.boundingBox();return{x:r.x+r.width/2,y:r.y+r.height/2}}
 const first=page.locator('#all .shift').first();
 const firstId=await first.getAttribute('data-shift-id');
 const p=await point(first.locator('.shiftmain'));
 await page.evaluate(()=>window.originalRow=q('#all .shift'));
 await client.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[p]});
 await page.waitForTimeout(430);
 assert.equal(await page.evaluate(()=>selectionMode),true,'Hold selects within 430ms');
 await client.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]});
 await page.waitForTimeout(50);
 assert.deepEqual(await page.evaluate(()=>Array.from(selectedShiftIds)),[firstId],'Releasing a hold keeps the selection');
 assert.equal(await page.evaluate(()=>originalRow===q('#all .shift')),true,'Selection preserves row DOM');
 assert.equal(await page.evaluate(()=>interactionCounts.render),0);
 assert.equal(await page.evaluate(()=>interactionCounts.summary),0,'Selection does not recalculate earnings');
 const second=page.locator('#all .shift').nth(1),secondId=await second.getAttribute('data-shift-id');
 await second.locator('.shiftmain').tap();assert.deepEqual(new Set(await page.evaluate(()=>Array.from(selectedShiftIds))),new Set([firstId,secondId]));
 await second.locator('.shiftmain').tap();assert.deepEqual(await page.evaluate(()=>Array.from(selectedShiftIds)),[firstId]);
 await second.locator('.shift-select').tap();assert.equal(await page.evaluate(()=>selectedShiftIds.size),2);
 await page.locator('#selectAll').tap();assert.equal(await page.evaluate(()=>selectedShiftIds.size),0);
 await page.locator('#selectAll').tap();assert.equal(await page.evaluate(()=>selectedShiftIds.size),100);
 await page.locator('#doneSelection').tap();assert.equal(await page.evaluate(()=>selectionMode),false);
 assert.equal(await page.evaluate(()=>interactionCounts.render),0);
 assert.equal(await page.evaluate(()=>interactionCounts.summary),0);
 // A scroll gesture or cancellation must not turn into a delayed selection.
 const s=await point(first.locator('.shiftmain'));
 await client.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[s]});
 await client.send('Input.dispatchTouchEvent',{type:'touchMove',touchPoints:[{x:s.x,y:s.y-65}]});
 await page.waitForTimeout(430);await client.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]});
 assert.equal(await page.evaluate(()=>selectionMode),false,'Scrolling cancels the hold');
 const c=await point(first.locator('.shiftmain'));
 await client.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[c]});await client.send('Input.dispatchTouchEvent',{type:'touchCancel',touchPoints:[]});
 await page.waitForTimeout(430);assert.equal(await page.evaluate(()=>selectionMode),false,'Cancelled touches cannot select');
 // Repeat selection after exiting, then delete through the real confirmation.
 const d=await point(first.locator('.shiftmain'));
 await client.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[d]});await page.waitForTimeout(430);await client.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]});
 await page.locator('#deleteSelected').tap();await page.locator('#confirmOk').tap();assert.equal(await page.evaluate(()=>data.shifts.length),99);
 await page.locator('.mobile [data-view="dashboard"]').tap();
 // Both idle and active timer ticks avoid whole-page translation work.
 await page.waitForTimeout(100);await page.evaluate(()=>interactionCounts.translations=0);await page.waitForTimeout(1200);
 assert.equal(await page.evaluate(()=>interactionCounts.translations),0);
 await page.locator('#clockStart').tap();await page.waitForTimeout(100);
 await page.evaluate(()=>{interactionCounts.translations=0;window.clockBefore=q('#clockTime').textContent});await page.waitForTimeout(1200);
 assert.notEqual(await page.locator('#clockTime').innerText(),await page.evaluate(()=>clockBefore));
 assert.equal(await page.evaluate(()=>interactionCounts.translations),0,'Active clock does not scan all page text');
 assert.deepEqual(errors,[]);await context.close();console.log('PASS: navigation after repeated saves, 350ms touch hold, immediate batch selection without payroll work, scroll/cancel, deletion, timer translation isolation');
};
