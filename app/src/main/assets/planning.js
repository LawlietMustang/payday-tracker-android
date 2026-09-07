// Version 2.1: explicit navigation, non-destructive workplace management, personal planning.
function migratePlanning() {
  if (!data.budgets || typeof data.budgets !== 'object') data.budgets = {};
  if (!Array.isArray(data.savingsGoals)) data.savingsGoals = [];
  if (!data.planningVersion) {
    if (Number(data.settings.savingsTarget) > 0) data.savingsGoals.push({id:crypto.randomUUID(),name:msg('Mein Sparziel','My savings goal'),target:Number(data.settings.savingsTarget),saved:0,due:''});
    data.planningVersion = 1;
  }
  if (!data.workplaces.some(w=>w.id===workplaceFilter)) workplaceFilter='all';
  data.workplaceFilter=workplaceFilter;
  save();
}
const planningCategories=['rent','utilities','health','groceries','transport','other'];
function spendingProjection(key, now=new Date()) {
  const current=monthKey(now), list=expenseEntries(key), days=new Date(Number(key.slice(0,4)),Number(key.slice(5,7)),0).getDate();
  const today=isoDate(now), past=key<current, future=key>current;
  const logged=list.reduce((a,e)=>a+Number(e.amount||0),0);
  const incurred=list.filter(e=>past||(!future&&e.date<=today));
  const spent=incurred.reduce((a,e)=>a+Number(e.amount||0),0);
  const variable=incurred.filter(e=>!e.recurringId).reduce((a,e)=>a+Number(e.amount||0),0);
  const scheduled=logged-spent;
  const projected=past?logged:future?logged:spent+scheduled+variable/now.getDate()*(days-now.getDate());
  return {spent,scheduled,projected,logged,past,future,hasData:list.length>0};
}
function planningLabel(text, field){return '<label>'+text+field+'</label>'}
function setupPlanningUI(){
  migratePlanning();
  const picker=q('.workplace-picker'); picker.classList.add('panel'); q('header').after(picker);
  const manager=q('.workplace-manager');
  const places=document.createElement('section'); places.id='workplaces'; places.className='view'; places.dataset.localized='true';
  q('main').append(places); places.append(manager); manager.innerHTML='<div id="placesContent"></div>';
  const plan=document.createElement('section'); plan.id='planning'; plan.className='view'; plan.dataset.localized='true'; q('main').append(plan);
  for(const nav of [q('aside:not(.drawer) nav'),q('#drawer nav')]){
    for(const [view,icon] of [['workplaces','▣'],['planning','◎']]){
      const b=document.createElement('button'); b.className='drawer-link v21-nav'; b.type='button'; b.dataset.view=view; b.dataset.icon=icon; b.dataset.localized='true'; b.onclick=()=>show(view); nav.append(b);
    }
  }
  const dialog=document.createElement('dialog'); dialog.id='planningDialog'; dialog.dataset.localized='true';
  dialog.innerHTML='<form id="planningForm"><div class="dialoghead"><h2 id="planningTitle"></h2><button type="button" id="planningClose" aria-label="Close">×</button></div><div id="planningFields" class="formgrid"></div><p id="planningError" class="error" role="alert"></p><div class="dialogactions"><button id="planningCancel" class="secondary" type="button"></button><button id="planningSave" class="primary"></button></div></form>';
  document.body.append(dialog); q('#planningClose').onclick=q('#planningCancel').onclick=()=>dialog.close();
  q('#shiftWorkplace').onchange=preview;
  q('#language').addEventListener('change',()=>{renderPlaces();renderPlanning();labelPlanningNav()});
  labelPlanningNav(); renderPlaces(); renderPlanning();
}
function labelPlanningNav(){qa('.v21-nav').forEach(b=>b.textContent=b.dataset.icon+' '+(b.dataset.view==='workplaces'?msg('Arbeitsplätze','Workplaces'):msg('Budgets & Sparziele','Budgets & goals')))}
function openPlanningDialog(title,fields,onSave){
  q('#planningTitle').textContent=title; q('#planningFields').innerHTML=fields; q('#planningError').textContent='';
  q('#planningCancel').textContent=msg('Abbrechen','Cancel');q('#planningSave').textContent=msg('Speichern','Save');
  q('#planningForm').onsubmit=e=>{e.preventDefault();try{onSave();save();q('#planningDialog').close();renderWorkplaces();render();renderPlaces();renderPlanning();toast(msg('Gespeichert','Saved'))}catch(err){q('#planningError').textContent=err.message}};
  q('#planningDialog').showModal();
}
function finiteAmount(selector,positive=false){let n=Number(q(selector).value);if(!Number.isFinite(n)||n<0||(positive&&n===0)||n>10000000)throw Error(msg('Bitte einen gültigen Betrag eingeben.','Enter a valid amount.'));return n}
function editPlace(id){
  const w=id?workplace(id):{name:'',wage:data.settings.wage};
  openPlanningDialog(id?msg('Arbeitsplatz bearbeiten','Edit workplace'):msg('Arbeitsplatz hinzufügen','Add workplace'),
    planningLabel(msg('Name','Name'),'<input id="placeName" maxlength="80" required value="'+safe(w.name)+'">')+
    planningLabel(msg('Stundenlohn (€)','Hourly wage (€)'),'<input id="placeWage" type="number" min="0.01" max="10000" step="0.01" required value="'+w.wage+'">')+
    '<p class="wide muted">'+msg('Neue Löhne gelten für neue Schichten. Bestehende Einträge bleiben unverändert.','New wages apply to new shifts. Existing entries keep their recorded wage.')+'</p>',()=>{
      const name=q('#placeName').value.trim();if(!name)throw Error(msg('Name fehlt.','Enter a name.'));
      const wage=finiteAmount('#placeWage',true);if(id){w.name=name;w.wage=wage}else data.workplaces.push({id:crypto.randomUUID(),name,wage});
    });
}
async function archivePlace(id){const w=workplace(id);if(!w.archived&&data.workplaces.filter(x=>!x.archived).length<2){toast(msg('Ein aktiver Arbeitsplatz muss bleiben.','Keep at least one active workplace.'));return}if(data.activeTimer?.workplaceId===id){toast(msg('Bitte zuerst die laufende Schicht beenden.','Finish the active shift first.'));return}w.archived=!w.archived;save();renderWorkplaces();renderPlaces()}
function renderPlaces(){if(!q('#placesContent'))return;
 q('#placesContent').innerHTML='<div class="panelhead"><h2>'+msg('Arbeitsplätze','Workplaces')+'</h2><button class="primary" id="addPlaceNow">+ '+msg('Arbeitsplatz hinzufügen','Add workplace')+'</button></div><p class="muted">'+msg('Eigene Löhne, zugeordnete Schichten. Archivieren erhält alle Schichten und Abrechnungen.','Separate wages and assigned shifts. Archiving preserves all shifts and payslips.')+'</p>'+data.workplaces.map(w=>'<div class="workplace-row"><span><b>'+safe(w.name)+'</b><small>'+money(w.wage)+' / h'+(w.archived?' · '+msg('Archiviert','Archived'):'')+'</small></span><button class="secondary" data-place-edit="'+w.id+'">'+msg('Bearbeiten','Edit')+'</button><button class="secondary" data-place-archive="'+w.id+'">'+(w.archived?msg('Aktivieren','Restore'):msg('Archivieren','Archive'))+'</button></div>').join('');
 q('#addPlaceNow').onclick=()=>editPlace();qa('[data-place-edit]').forEach(b=>b.onclick=()=>editPlace(b.dataset.placeEdit));qa('[data-place-archive]').forEach(b=>b.onclick=()=>archivePlace(b.dataset.placeArchive));
}
function editBudget(){const b=data.budgets[selected]||{};
 openPlanningDialog(msg('Monatsbudget: ','Monthly budget: ')+selected,planningLabel(msg('Gesamtbudget (€)','Overall budget (€)'),'<input id="budgetTotal" type="number" min="0" step="0.01" value="'+Number(b.total||0)+'">')+planningCategories.map(c=>planningLabel(categoryName(c)+' (€)','<input id="budget_'+c+'" type="number" min="0" step="0.01" value="'+Number(b[c]||0)+'">')).join('')+'<p class="wide muted">'+msg('0 = kein Limit. Kategorien sind Teil des Gesamtbudgets, keine zusätzlichen Ausgaben.','0 = no limit. Category limits are part of the overall budget, not extra spending.')+'</p>',()=>{const next={total:finiteAmount('#budgetTotal')};planningCategories.forEach(c=>next[c]=finiteAmount('#budget_'+c));data.budgets[selected]=next});
}
function editGoal(id){const g=data.savingsGoals.find(x=>x.id===id)||{name:'',target:500,saved:0,due:''};
 openPlanningDialog(id?msg('Sparziel bearbeiten','Edit savings goal'):msg('Neues Sparziel','New savings goal'),planningLabel(msg('Name','Name'),'<input id="goalName" required maxlength="80" value="'+safe(g.name)+'">')+planningLabel(msg('Zielbetrag (€)','Target amount (€)'),'<input id="goalTarget" type="number" required min="0.01" step="0.01" value="'+g.target+'">')+planningLabel(msg('Bereits gespart (€)','Already saved (€)'),'<input id="goalSaved" type="number" required min="0" step="0.01" value="'+g.saved+'">')+planningLabel(msg('Zieldatum (optional)','Target date (optional)'),'<input id="goalDue" type="date" value="'+safe(g.due)+'">')+'<p class="wide muted">'+msg('Manueller Sparstand; keine automatische Buchung oder Bankverbindung.','Manually tracked savings; no automatic transfers or bank connection.')+'</p>',()=>{const name=q('#goalName').value.trim();if(!name)throw Error(msg('Name fehlt.','Enter a name.'));const item={id:id||crypto.randomUUID(),name,target:finiteAmount('#goalTarget',true),saved:finiteAmount('#goalSaved'),due:q('#goalDue').value};if(id)data.savingsGoals[data.savingsGoals.findIndex(x=>x.id===id)]=item;else data.savingsGoals.push(item)});
}
async function removeGoal(id){if(!await askConfirm(msg('Sparziel und seinen manuellen Sparstand löschen?','Delete this goal and its manually tracked balance?')))return;data.savingsGoals=data.savingsGoals.filter(g=>g.id!==id);save();renderPlanning()}
function renderPlanning(){if(!q('#planning'))return;
 const b=data.budgets[selected]||{},p=spendingProjection(selected),list=expenseEntries(selected),categories=planningCategories.filter(c=>b[c]>0),limit=Number(b.total||0),used=p.logged;
 const meter=(value,max)=>'<progress max="100" value="'+Math.min(100,Math.max(0,value/max*100))+'"></progress>';
 const goals=data.savingsGoals.map(g=>'<article class="panel"><div class="panelhead"><h3>'+safe(g.name)+'</h3><button class="secondary" data-goal-edit="'+g.id+'">'+msg('Bearbeiten','Edit')+'</button></div><strong>'+money(g.saved)+' / '+money(g.target)+'</strong>'+meter(g.saved,g.target)+'<p class="muted">'+(g.saved>=g.target?msg('Ziel erreicht','Goal reached'):msg('Noch ','Remaining: ')+money(g.target-g.saved))+(g.due?' · '+safe(g.due):'')+'</p><button class="secondary" data-goal-delete="'+g.id+'">'+msg('Ziel löschen','Delete goal')+'</button></article>').join('');
 q('#planning').innerHTML='<div class="planning-stack"><article class="panel"><div class="panelhead"><h2>'+msg('Monatsbudget','Monthly budget')+' · '+selected+'</h2><button id="editBudget" class="primary">'+msg('Budget festlegen','Set budget')+'</button></div><p class="muted">'+msg('Persönliche Ausgaben aller Arbeitsplätze. Enthält alle Einträge im gewählten Monat.','Personal expenses across all workplaces. Includes every entry in the selected month.')+'</p><strong>'+money(used)+(limit?' / '+money(limit):'')+'</strong>'+(limit?meter(used,limit)+'<p class="'+(used>limit?'negative':'positive')+'">'+(used>limit?msg('Über Budget: ','Over budget: '):msg('Verfügbar: ','Remaining: '))+money(Math.abs(limit-used))+'</p>':'<p>'+msg('Noch kein Gesamtlimit','No overall limit set')+'</p>')+categories.map(c=>{const v=list.filter(e=>e.category===c).reduce((s,e)=>s+Number(e.amount),0);return '<div class="budget-row"><span>'+categoryName(c)+'</span><b>'+money(v)+' / '+money(b[c])+'</b>'+meter(v,b[c])+(v>b[c]?'<small class="negative">'+msg('Über Budget','Over budget')+'</small>':'')+'</div>'}).join('')+'</article><article class="panel"><h2>'+msg('Ausgabenprognose','Spending forecast')+'</h2><div class="forecast-grid"><span>'+msg('Bis heute','Through today')+'<b>'+money(p.spent)+'</b></span><span>'+msg('Bereits eingeplant','Already scheduled')+'<b>'+money(p.scheduled)+'</b></span><span>'+msg('Monatsende geschätzt','Estimated month-end')+'<b>'+(p.hasData?money(p.projected):'—')+'</b></span></div><p class="muted">'+(p.past?msg('Abgeschlossener Monat: tatsächliche Einträge.','Past month: recorded expenses.'):p.future?msg('Zukünftiger Monat: nur erfasste Ausgaben und wiederkehrende Kosten.','Future month: recorded expenses and recurring costs only.'):msg('Erfasste Kosten + geplante Kosten + durchschnittliche nicht wiederkehrende Tagesausgaben für die restlichen Tage. Einmalige Käufe können die Schätzung erhöhen.','Recorded costs + scheduled costs + average non-recurring daily spending for remaining days. One-off purchases can inflate the estimate.'))+'</p></article><div class="panelhead"><h2>'+msg('Sparziele','Savings goals')+'</h2><button id="addGoal" class="primary">+ '+msg('Sparziel','Savings goal')+'</button></div><div class="goal-grid">'+(goals||'<p>'+msg('Erstelle dein erstes Sparziel.','Create your first savings goal.')+'</p>')+'</div></div>';
 q('#editBudget').onclick=editBudget;q('#addGoal').onclick=()=>editGoal();qa('[data-goal-edit]').forEach(b=>b.onclick=()=>editGoal(b.dataset.goalEdit));qa('[data-goal-delete]').forEach(b=>b.onclick=()=>removeGoal(b.dataset.goalDelete));
}
const originalShow=show;
const originalRenderWorkplaces=renderWorkplaces;
renderWorkplaces=function(){
 if(!q('#placesContent'))return originalRenderWorkplaces();
 for(const [selector,all] of [['#workplaceFilter',true],['#shiftWorkplace',false],['#payslipWorkplace',false]]){const el=q(selector),value=el.value;el.innerHTML=workplaceOptions(all);el.value=all?workplaceFilter:value;if(!el.value)el.value=data.workplaces[0].id}
 renderPlaces();renderPayslipComparisons();
};
addWorkplace=()=>editPlace();editWorkplace=id=>editPlace(id);deleteWorkplace=id=>archivePlace(id);
show=function(view){originalShow(view);if(view==='workplaces'){q('#title').textContent=msg('Arbeitsplätze','Workplaces');renderPlaces()}if(view==='planning'){q('#title').textContent=msg('Budgets & Sparziele','Budgets & goals');renderPlanning()}};
const originalRender=render;
render=function(){originalRender();renderPlanning();if(q('#payslipComparisons'))renderPayslipComparisons()};
// Runtime setup follows the original initialization; all new views use explicit localized labels.
installV2UI();init();initV2();setupLanguage();setupPlanningUI();
