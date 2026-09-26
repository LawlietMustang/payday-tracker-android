const infoIcon='<span class="route-icon" data-icon="info" aria-hidden="true"></span>';
// Version 2.1: explicit navigation, non-destructive workplace management, personal planning.
function migratePlanning() {
  if (!data.budgets || typeof data.budgets !== 'object') data.budgets = {};
  if (!Array.isArray(data.savingsGoals)) data.savingsGoals = [];
  if (!data.planningVersion) {
    if (Number(data.settings.savingsTarget) > 0) data.savingsGoals.push({id:crypto.randomUUID(),name:msg('Mein Sparziel','My savings goal'),defaultName:true,target:Number(data.settings.savingsTarget),saved:0,due:''});
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
function planningLabel(text, field){text=text.replace('(€)','('+currencyCode()+')');return '<label>'+text+field+'</label>'}
function setupPlanningUI(){
  migratePlanning();
  const picker=q('.workplace-picker'); picker.classList.add('panel'); q('header').after(picker);
  for(const [container,prefix] of [[picker,'header'],[q('#shiftWorkplace').parentElement,'shift']]){
    const actions=document.createElement('div');actions.className='place-actions';actions.dataset.localized='true';
    actions.innerHTML='<button type="button" class="secondary" id="'+prefix+'AddPlace"></button><button type="button" class="secondary" id="'+prefix+'ManagePlaces"></button>';container.after(actions);
    q('#'+prefix+'AddPlace').onclick=()=>editPlace();
    q('#'+prefix+'ManagePlaces').onclick=()=>{q('#dialog').close();show('workplaces')};
  }
  const manager=q('.workplace-manager');
  const places=document.createElement('section'); places.id='workplaces'; places.className='view'; places.dataset.localized='true';
  q('main').append(places); places.append(manager); manager.innerHTML='<div id="placesContent"></div>';
  const plan=document.createElement('section'); plan.id='planning'; plan.className='view'; plan.dataset.localized='true'; q('main').append(plan);
  const dialog=document.createElement('dialog'); dialog.id='planningDialog'; dialog.dataset.localized='true';
  dialog.innerHTML='<form id="planningForm"><div class="dialoghead"><h2 id="planningTitle"></h2><button type="button" id="planningClose" aria-label="Close">×</button></div><div id="planningFields" class="formgrid"></div><p id="planningError" class="error" role="alert"></p><div class="dialogactions"><button id="planningCancel" class="secondary" type="button"></button><button id="planningSave" class="primary"></button></div></form>';
  document.body.append(dialog); q('#planningClose').onclick=q('#planningCancel').onclick=()=>dialog.close();
  q('#shiftWorkplace').onchange=preview;
  q('#language').addEventListener('change',()=>{renderPlaces();renderPlanning();labelPlanningNav();const active=q('.view.active')?.id;if(['workplaces','planning'].includes(active))show(active)});
  labelPlanningNav(); renderPlaces(); renderPlanning();
}
function labelPlanningNav(){for(const prefix of ['header','shift']){q('#'+prefix+'AddPlace').textContent='+ '+msg('Arbeitsplatz','New workplace');q('#'+prefix+'ManagePlaces').textContent=msg('Verwalten','Manage workplaces')}}
function openPlanningDialog(title,fields,onSave){
  q('#planningTitle').textContent=title; q('#planningFields').innerHTML=fields; q('#planningError').textContent='';
  q('#planningCancel').textContent=msg('Abbrechen','Cancel');q('#planningSave').textContent=msg('Speichern','Save');
  q('#planningForm').onsubmit=e=>{e.preventDefault();try{onSave();save();q('#planningDialog').close();renderWorkplaces();render();renderPlaces();renderPlanning();toast(msg('Gespeichert','Saved'))}catch(err){q('#planningError').textContent=err.message}};
  q('#planningDialog').showModal();
}
function finiteAmount(selector,positive=false){let n=Number(q(selector).value);if(!Number.isFinite(n)||n<0||(positive&&n===0)||n>10000000)throw Error(msg('Bitte einen gültigen Betrag eingeben.','Enter a valid amount.'));return n}
function editPlace(id){
  const w=id?workplace(id):{name:'',wage:''};
  openPlanningDialog(id?msg('Arbeitsplatz bearbeiten','Edit workplace'):msg('Arbeitsplatz hinzufügen','Add workplace'),
    planningLabel(msg('Name','Name'),'<input id="placeName" maxlength="80" required value="'+safe(w.name)+'">')+
    planningLabel(msg('Stundenlohn (€)','Hourly wage (€)'),'<input id="placeWage" type="number" min="0.01" max="10000" step="0.01" required placeholder="'+msg('z. B. 14,50','e.g. 14.50')+'" value="'+w.wage+'">')+
    '<p class="wide muted">'+msg('Neue Löhne gelten für neue Schichten. Bestehende Einträge bleiben unverändert.','New wages apply to new shifts. Existing entries keep their recorded wage.')+'</p>',()=>{
      const name=q('#placeName').value.trim();if(!name)throw Error(msg('Name fehlt.','Enter a name.'));
      const wage=finiteAmount('#placeWage',true);if(id){w.name=name;w.wage=wage}else{const next={id:crypto.randomUUID(),name,wage};data.workplaces.push(next);renderWorkplaces();q('#shiftWorkplace').value=next.id}preview();
    });
}
async function archivePlace(id){const w=workplace(id);if(!w.archived&&data.workplaces.filter(x=>!x.archived).length<2){toast(msg('Ein aktiver Arbeitsplatz muss bleiben.','Keep at least one active workplace.'));return}if(data.activeTimer?.workplaceId===id){toast(msg('Bitte zuerst die laufende Schicht beenden.','Finish the active shift first.'));return}w.archived=!w.archived;save();renderWorkplaces();renderPlaces()}
function renderPlaces(){if(!q('#placesContent'))return;
 q('#placesContent').innerHTML='<div class="panelhead"><h2>'+msg('Arbeitsplätze','Workplaces')+'</h2><button class="primary" id="addPlaceNow">+ '+msg('Arbeitsplatz hinzufügen','Add workplace')+'</button></div><p class="muted">'+msg('Eigene Löhne, zugeordnete Schichten. Archivieren erhält alle Schichten und Abrechnungen.','Separate wages and assigned shifts. Archiving preserves all shifts and payslips.')+'</p>'+data.workplaces.map(w=>'<div class="workplace-row"><span><b>'+safe(w.name)+'</b><small>'+money(w.wage)+' / h'+(w.archived?' · '+msg('Archiviert','Archived'):'')+'</small></span><button class="secondary" data-place-edit="'+w.id+'">'+msg('Bearbeiten','Edit')+'</button><button class="secondary" data-place-archive="'+w.id+'">'+(w.archived?msg('Aktivieren','Restore'):msg('Archivieren','Archive'))+'</button></div>').join('');
 q('#addPlaceNow').onclick=()=>editPlace();qa('[data-place-edit]').forEach(b=>b.onclick=()=>editPlace(b.dataset.placeEdit));qa('[data-place-archive]').forEach(b=>b.onclick=()=>archivePlace(b.dataset.placeArchive));
 for(const row of qa('#placesContent .workplace-row')){const id=row.querySelector('[data-place-edit]').dataset.placeEdit,b=document.createElement('button');b.type='button';b.className='secondary';b.textContent=msg('Löschen','Delete');b.dataset.placeDelete=id;b.onclick=()=>removePlace(id);row.append(b)}
}
function removePlace(id){
 const others=data.workplaces.filter(w=>w.id!==id&&!w.archived);
 if(!others.length){toast(msg('Ein aktiver Arbeitsplatz muss bleiben.','Keep at least one active workplace.'));return}
 if(data.activeTimer?.workplaceId===id){toast(msg('Bitte zuerst die laufende Schicht beenden.','Finish the active shift first.'));return}
 const count=data.shifts.filter(s=>s.workplaceId===id).length,pays=data.payslips.filter(p=>p.workplaceId===id).length;
 openPlanningDialog(msg('Arbeitsplatz löschen','Delete workplace'),'<p class="wide">'+safe(workplace(id).name)+' · '+count+' '+msg('Schichten','shifts')+' · '+pays+' '+msg('Abrechnungen','payslips')+'</p><p class="wide">'+msg('Einträge bleiben erhalten und werden dem gewählten Arbeitsplatz zugeordnet.','Records will be kept and moved to the selected workplace.')+'</p>'+planningLabel(msg('Einträge verschieben nach','Move records to'),'<select id="movePlace">'+others.map(w=>'<option value="'+w.id+'">'+safe(w.name)+'</option>').join('')+'</select>'),()=>{
  if(data.activeTimer?.workplaceId===id)throw Error(msg('Bitte zuerst die laufende Schicht beenden.','Finish the active shift first.'));
  const target=q('#movePlace').value;
  if(data.payslips.some(p=>p.workplaceId===id&&data.payslips.some(x=>x.workplaceId===target&&x.month===p.month)))throw Error(msg('Im Ziel existiert bereits eine Abrechnung für denselben Monat. Wähle einen anderen Arbeitsplatz oder archiviere diesen.','The destination already has a payslip for the same month. Choose another workplace or archive this one.'));
  for(const list of [data.shifts,data.payslips])list.forEach(x=>{if(x.workplaceId===id)x.workplaceId=target});
  data.workplaces=data.workplaces.filter(w=>w.id!==id);if(workplaceFilter===id)workplaceFilter='all';data.workplaceFilter=workplaceFilter;
 });q('#planningSave').textContent=msg('Löschen und verschieben','Delete and move');
}
function editBudget(draft){const b=draft||data.budgets[selected]||{};
 openPlanningDialog(msg('Monatsbudget: ','Monthly budget: ')+selected,planningLabel(msg('Gesamtbudget (€)','Overall budget (€)'),'<input id="budgetTotal" type="number" min="0" step="0.01" value="'+Number(b.total||0)+'">')+budgetCategoryKeys().map(c=>planningLabel(categoryName(c)+' (€)','<input id="budget_'+c+'" type="number" min="0" step="0.01" value="'+Number(b[c]||0)+'">')).join('')+'<button type="button" id="newBudgetCategory" class="secondary wide">+ '+msg('Eigene Kategorie','Custom category')+'</button><p class="wide muted">'+msg('0 = kein Limit. Kategorien sind Teil des Gesamtbudgets, keine zusätzlichen Ausgaben.','0 = no limit. Category limits are part of the overall budget, not extra spending.')+'</p>',()=>{const next={total:finiteAmount('#budgetTotal')};budgetCategoryKeys().forEach(c=>next[c]=finiteAmount('#budget_'+c));data.budgets[selected]=next});
 q('#newBudgetCategory').onclick=async()=>{const draft={total:finiteAmount('#budgetTotal')};budgetCategoryKeys().forEach(c=>draft[c]=finiteAmount('#budget_'+c));if(await createBudgetCategory())editBudget(draft)};
}
function savingsGoalName(g){const legacyDefault=['Mein Sparziel','My savings goal'].includes(g.name)&&Number(g.saved)===0&&!g.due&&Number(g.target)===Number(data.settings.savingsTarget);return g.defaultName||legacyDefault?msg('Mein Sparziel','My savings goal'):g.name}
function editGoal(id){const g=data.savingsGoals.find(x=>x.id===id)||{name:'',target:'',saved:0,due:''};
 openPlanningDialog(id?msg('Sparziel bearbeiten','Edit savings goal'):msg('Neues Sparziel','New savings goal'),planningLabel(msg('Name','Name'),'<input id="goalName" required maxlength="80" value="'+safe(savingsGoalName(g))+'">')+planningLabel(msg('Zielbetrag (€)','Target amount (€)'),'<input id="goalTarget" type="number" required min="0.01" step="0.01" value="'+g.target+'">')+planningLabel(msg('Bereits gespart (€)','Already saved (€)'),'<input id="goalSaved" type="number" required min="0" step="0.01" value="'+g.saved+'">')+planningLabel(msg('Zieldatum (optional)','Target date (optional)'),'<input id="goalDue" type="date" value="'+safe(g.due)+'">')+'<label class="check wide"><span>'+msg('Automatischer Beitrag','Automatic deduction')+'</span><input id="goalAuto" type="checkbox" '+(g.auto?'checked':'')+'></label><div id="goalAutoFields" class="wide account-grid">'+planningLabel(msg('Intervall','Interval'),'<select id="goalInterval"><option value="weekly">'+msg('Wöchentlich','Weekly')+'</option><option value="monthly">'+msg('Monatlich','Monthly')+'</option><option value="yearly">'+msg('Jährlich','Yearly')+'</option></select>')+planningLabel(msg('Betrag (optional)','Amount (optional)'),'<input id="goalAutoAmount" type="number" min="0.01" max="10000000" step="0.01" placeholder="'+msg('Verfügbares Guthaben','Available balance')+'" value="'+(g.autoAmount||'')+'">')+'</div><small class="wide muted">'+msg('Ohne Betrag: verfügbares Netto nach Ausgaben bis zum Zielbetrag. Mit Betrag: fester Beitrag. Wird beim Öffnen nachgetragen; keine Banküberweisung.','Leave amount blank to use available net income after expenses, up to the goal. Enter an amount for a fixed contribution. Due entries catch up when you open the app; no bank transfer.')+'</small>',()=>{
 const name=q('#goalName').value.trim();if(!name)throw Error(msg('Name fehlt.','Enter a name.'));const auto=q('#goalAuto').checked,interval=q('#goalInterval').value,amount=q('#goalAutoAmount').value?finiteAmount('#goalAutoAmount',true):0,today=isoDate(new Date());
 const changed=auto&&(!g.auto||g.autoInterval!==interval||Number(g.autoAmount||0)!==amount);
 const item={...g,id:id||crypto.randomUUID(),name,defaultName:false,target:finiteAmount('#goalTarget',true),saved:finiteAmount('#goalSaved'),due:q('#goalDue').value,auto,autoInterval:interval,autoAmount:amount};
 if(changed){item.autoAnchor=new Date().getDate();item.autoNext=nextSavingsDate(today,interval,item.autoAnchor);item.autoEpoch=crypto.randomUUID();item.autoOverdue=0}
 if(!auto)item.autoOverdue=0;
 if(id)data.savingsGoals[data.savingsGoals.findIndex(x=>x.id===id)]=item;else data.savingsGoals.push(item);
 });q('#goalInterval').value=g.autoInterval||'monthly';const toggle=()=>{q('#goalAutoFields').hidden=!q('#goalAuto').checked;q('#goalAutoAmount').disabled=!q('#goalAuto').checked};q('#goalAuto').onchange=toggle;toggle();
}
function addGoalContribution(id){const g=data.savingsGoals.find(g=>g.id===id);if(!g)return;openPlanningDialog(msg('Beitrag hinzufügen','Add contribution'),planningLabel(msg('Betrag (€)','Amount (€)'),'<input id="goalContribution" type="number" min="0.01" max="10000000" step="0.01" required>'),()=>{g.saved=(cents(g.saved)+cents(finiteAmount('#goalContribution',true)))/100})}
async function createBudgetCategory(){const name=await askPrompt(msg('Name der Kategorie / des Kontos','Category / account name'),msg('Eigene Kategorie','Custom category'));if(!name?.trim())return false;planningData();if(budgetCategoryKeys().some(c=>categoryName(c).toLocaleLowerCase()===safe(name.trim()).toLocaleLowerCase())){toast(msg('Kategorie existiert bereits','Category already exists'));return false}data.customCategories.push({id:'custom_'+crypto.randomUUID(),name:name.trim()});save();refreshExpenseCategories();return true}
function refreshExpenseCategories(){const el=q('#expenseCategory');if(!el)return;const old=el.value;el.innerHTML=budgetCategoryKeys().map(c=>'<option value="'+c+'">'+categoryName(c)+'</option>').join('');el.value=budgetCategoryKeys().includes(old)?old:'rent'}

async function removeGoal(id){if(!await askConfirm(msg('Sparziel und seinen manuellen Sparstand löschen?','Delete this goal and its manually tracked balance?')))return;data.savingsGoals=data.savingsGoals.filter(g=>g.id!==id);save();renderPlanning()}
function renderPlanning(){if(!q('#planning'))return;if(reconcileSavings())save();
 const b=data.budgets[selected]||{},p=spendingProjection(selected),list=expenseEntries(selected),categories=budgetCategoryKeys().filter(c=>b[c]>0),limit=Number(b.total||0),used=p.logged;
 const meter=(value,max)=>'<progress max="100" value="'+Math.min(100,Math.max(0,value/max*100))+'"></progress>';
 const goals=data.savingsGoals.map(g=>'<article class="panel"><div class="panelhead"><h3 data-localized>'+safe(savingsGoalName(g))+'</h3><button class="secondary" data-goal-edit="'+g.id+'">'+msg('Bearbeiten','Edit')+'</button></div><strong>'+money(g.saved)+' / '+money(g.target)+'</strong>'+meter(g.saved,g.target)+'<p class="muted">'+(g.saved>=g.target?msg('Ziel erreicht','Goal reached'):msg('Noch ','Remaining: ')+money(g.target-g.saved))+(g.due?' · '+safe(g.due):'')+'</p><p class="goal-auto-status '+(g.autoOverdue>0?'negative':'muted')+'">'+safe(goalAutoStatus(g))+'</p><div class="goal-actions"><button type="button" class="secondary" data-goal-add="'+g.id+'">+ '+msg('Beitrag','Add money')+'</button><button class="secondary" data-goal-delete="'+g.id+'">'+msg('Ziel löschen','Delete goal')+'</button></div></article>').join('');
 q('#planning').innerHTML='<div class="planning-stack"><article class="panel"><div class="panelhead"><h2>'+msg('Monatsbudget','Monthly budget')+' · '+selected+'</h2><button id="editBudget" class="primary">'+msg('Budget festlegen','Set budget')+'</button></div><p class="muted">'+msg('Persönliche Ausgaben aller Arbeitsplätze. Enthält alle Einträge im gewählten Monat.','Personal expenses across all workplaces. Includes every entry in the selected month.')+'</p><strong>'+money(used)+(limit?' / '+money(limit):'')+'</strong>'+(limit?meter(used,limit)+'<p class="'+(used>limit?'negative':'positive')+'">'+(used>limit?msg('Über Budget: ','Over budget: '):msg('Verfügbar: ','Remaining: '))+money(Math.abs(limit-used))+'</p>':'<p>'+msg('Noch kein Gesamtlimit','No overall limit set')+'</p>')+categories.map(c=>{const v=list.filter(e=>e.category===c).reduce((s,e)=>s+Number(e.amount),0);return '<div class="budget-row"><span>'+categoryName(c)+'</span><b>'+money(v)+' / '+money(b[c])+'</b>'+meter(v,b[c])+(v>b[c]?'<small class="negative">'+msg('Über Budget','Over budget')+'</small>':'')+'</div>'}).join('')+'</article><article class="panel"><h2>'+msg('Ausgabenprognose','Spending forecast')+'</h2><div class="forecast-grid"><span>'+msg('Bis heute','Through today')+'<b>'+money(p.spent)+'</b></span><span>'+msg('Bereits eingeplant','Already scheduled')+'<b>'+money(p.scheduled)+'</b></span><span>'+msg('Monatsende geschätzt','Estimated month-end')+'<b>'+(p.hasData?money(p.projected):'—')+'</b></span></div><p class="muted">'+(p.past?msg('Abgeschlossener Monat: tatsächliche Einträge.','Past month: recorded expenses.'):p.future?msg('Zukünftiger Monat: nur erfasste Ausgaben und wiederkehrende Kosten.','Future month: recorded expenses and recurring costs only.'):msg('Erfasste Kosten + geplante Kosten + durchschnittliche nicht wiederkehrende Tagesausgaben für die restlichen Tage. Einmalige Käufe können die Schätzung erhöhen.','Recorded costs + scheduled costs + average non-recurring daily spending for remaining days. One-off purchases can inflate the estimate.'))+'</p></article><div class="panelhead"><h2>'+msg('Sparziele','Savings goals')+'</h2><button id="addGoal" class="primary">+ '+msg('Sparziel','Savings goal')+'</button></div><div class="goal-grid">'+(goals||'<p>'+msg('Erstelle dein erstes Sparziel.','Create your first savings goal.')+'</p>')+'</div></div>';
 addPlanningInfo();q('#editBudget').onclick=()=>editBudget();qa('[data-goal-add]').forEach(b=>b.onclick=()=>addGoalContribution(b.dataset.goalAdd));q('#addGoal').onclick=()=>editGoal();qa('[data-goal-edit]').forEach(b=>b.onclick=()=>editGoal(b.dataset.goalEdit));qa('[data-goal-delete]').forEach(b=>b.onclick=()=>removeGoal(b.dataset.goalDelete));
}
const originalShow=show;
preview=function(){const min=calcMinutes(q('#date').value,q('#start').value,q('#end').value,q('#break').value),old=data.shifts.find(e=>e.id===q('#editId').value),id=q('#shiftWorkplace').value,wage=old&&old.workplaceId===id?old.wage:workplace(id).wage;q('#previewTime').textContent=min>0?duration(min):'0 h';q('#previewMoney').textContent=min>0?money(min*wage/60)+' '+msg('Grundlohn','base pay'):msg('Bitte Zeiten prüfen','Check working times')};
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

// Custom categories are shared between monthly budgets and expense records.
refreshExpenseCategories();q('#language').addEventListener('change',refreshExpenseCategories);
const categoryButton=document.createElement('button');categoryButton.type='button';categoryButton.id='newExpenseCategory';categoryButton.className='secondary wide';categoryButton.dataset.localized='true';q('#expenseCategory').closest('label').after(categoryButton);
function labelCategoryButton(){categoryButton.textContent='+ '+msg('Eigene Kategorie','Custom category')}labelCategoryButton();q('#language').addEventListener('change',labelCategoryButton);categoryButton.onclick=createBudgetCategory;
const openExpenseBeforeCategories=openExpenseDialog;openExpenseDialog=function(id){refreshExpenseCategories();openExpenseBeforeCategories(id)};
setInterval(()=>{if(document.visibilityState==='visible'&&!q('dialog[open]')&&reconcileSavings()){save();if(q('#planning').classList.contains('active'))renderPlanning()}},60000);

function showPlanningInfo(title,text){let dialog=q('#planningInfo');if(!dialog){dialog=document.createElement('dialog');dialog.id='planningInfo';dialog.className='app-dialog';dialog.dataset.localized='true';document.body.append(dialog)}dialog.innerHTML='<div class="app-dialog-body"><h2>'+safe(title)+'</h2><p>'+safe(text)+'</p><button class="primary" type="button" id="closePlanningInfo">'+msg('Verstanden','Got it')+'</button></div>';q('#closePlanningInfo').onclick=()=>dialog.close();dialog.showModal()}
function addPlanningInfo(){
 for(const article of qa('#planning article')){let text='',title=article.querySelector('h2,h3')?.textContent||'';
 if(article.querySelector('.forecast-grid')){text=article.querySelector(':scope>p.muted')?.textContent||'';article.querySelector(':scope>p.muted')?.remove()}
 else if(article.querySelector('#editBudget')){text=msg('Erfasste Ausgaben aller Arbeitsplätze im gewählten Monat. Kategorien sind Teil des Gesamtbudgets. Verfügbar = Limit minus erfasste Ausgaben; geplante Ausgaben zählen mit.','Recorded expenses from all workplaces for the selected month. Categories are part of the overall budget. Remaining = limit minus recorded expenses, including scheduled expenses.');article.querySelector(':scope>p.muted')?.remove()}
 else if(article.querySelector('[data-goal-edit]'))text=msg('Fortschritt = gespart / Zielbetrag. Automatische Beiträge nutzen noch nicht zugewiesenes, geschätztes Netto nach erfassten und wiederkehrenden Ausgaben. Ziele mit älterem Fälligkeitsdatum kommen zuerst. Ausstehende Beiträge werden bei verfügbarem Guthaben nachgeholt.','Progress = saved / target. Automatic contributions use unallocated estimated net income after recorded and recurring expenses. Goals due earliest are funded first. Overdue contributions retry when a balance becomes available.');
 if(!text)continue;article.classList.add('has-card-info');const button=document.createElement('button');button.type='button';button.className='card-info';button.setAttribute('aria-label',msg('Berechnung erklären','Explain calculation'));button.innerHTML=infoIcon;button.onclick=()=>showPlanningInfo(title,text);article.append(button);
 }
}
