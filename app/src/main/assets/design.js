// WageTrack 2.4: presentation only; existing records and payroll calculations stay authoritative.
(()=>{
 const css=document.createElement('link');css.rel='stylesheet';css.href='./design.css';document.head.append(css);
 const block=(tag,id,parent)=>{const el=document.createElement(tag);el.id=id;el.dataset.localized='true';parent.append(el);return el};
 const hero=block('section','wageOverview',q('#dashboard'));q('#dashboard').prepend(hero);
 const receipt=block('button','payReceipt',q('#dashboard'));receipt.type='button';receipt.className='pay-receipt';receipt.onclick=()=>show('history');
 const composition=block('div','payComposition',q('.bonus-panel'));q('.bonus-panel .panelhead').after(composition);
 q('#timeclock').after(q('.bonus-panel'));q('.bonus-panel').after(receipt);
 const shortcuts=block('div','designShortcuts',q('#dashboard'));receipt.after(shortcuts);
 const detail=document.createElement('details');detail.id='earningsDetails';const detailLabel=document.createElement('summary');detailLabel.dataset.localized='true';detail.append(detailLabel);receipt.before(detail);for(const selector of ['.kpis','.twocol','.recent'])detail.append(q('#dashboard '+selector));
 const calendar=block('article','shiftCalendar',q('#shifts'));calendar.className='shift-calendar';q('#shifts').prepend(calendar);
 const dayDialog=block('dialog','calendarDay',document.body);dayDialog.className='calendar-day-dialog';
 let pickedDay='';
 const locale=()=>language()==='en'?'en-GB':'de-DE';
 const localDate=(date)=>new Date(date+'T12:00:00');
 const shiftRange=s=>{const start=new Date(s.date+'T'+s.start),end=new Date(s.date+'T'+s.end);if(end<=start)end.setDate(end.getDate()+1);return [start.getTime(),end.getTime()]};
 function dayEntries(date){return data.shifts.filter(s=>s.date===date&&(workplaceFilter==='all'||s.workplaceId===workplaceFilter)).sort((a,b)=>a.start.localeCompare(b.start))}
 function addOnDate(date,template){
  dayDialog.close();openDialog();q('#date').value=date;bulkDates=new Set([date]);calendarMonth=localDate(date);
  updateAutoShiftStatus();
  if(template){q('#templateSelect').value=template;applyTemplateToShiftForm()}
  preview();
 }
 function openDay(date){
  pickedDay=date;const list=dayEntries(date);
  dayDialog.innerHTML='<div class="app-dialog-body"><div class="dialoghead"><h2>'+safe(localDate(date).toLocaleDateString(locale(),{weekday:'long',day:'numeric',month:'long'}))+'</h2><button type="button" id="closeCalendarDay" aria-label="'+msg('Schließen','Close')+'">×</button></div><div class="day-shift-list">'+list.map(s=>'<button type="button" class="day-shift" data-edit-day-shift="'+safe(s.id)+'"><span><b>'+safe(s.start+'–'+s.end)+'</b><small>'+safe(workplaceName(s.workplaceId)+(s.status==='cancelled'?' · '+cancellationLabel(s.cancelledBy):''))+'</small></span><span class="tag '+safe(s.status)+'">'+shiftStatusLabel(s.status)+'</span></button>').join('')+(list.length?'':'<p class="muted">'+msg('Noch keine Schichten an diesem Tag.','No shifts on this day yet.')+'</p>')+'</div><button type="button" class="primary" id="addCalendarShift">+ '+msg('Schicht hinzufügen','Add shift')+'</button></div>';
  q('#closeCalendarDay').onclick=()=>dayDialog.close();q('#addCalendarShift').onclick=()=>addOnDate(date);
  qa('[data-edit-day-shift]').forEach(b=>b.onclick=()=>{dayDialog.close();openDialog(b.dataset.editDayShift)});dayDialog.showModal();
 }
 function renderShiftCalendar(){
  const first=localDate(selected+'-01'),start=new Date(first),today=isoDate(new Date());start.setDate(1-(first.getDay()+6)%7);
  const days=new Date(first.getFullYear(),first.getMonth()+1,0).getDate(),count=Math.ceil(((first.getDay()+6)%7+days)/7)*7;
  const all=data.shifts.filter(s=>s.status!=='cancelled'&&(workplaceFilter==='all'||s.workplaceId===workplaceFilter)).map(s=>({s,range:shiftRange(s)})).sort((a,b)=>a.range[0]-b.range[0]),conflicts=new Set();
  for(let i=0;i<all.length;i++)for(let j=i+1;j<all.length&&all[j].range[0]<all[i].range[1];j++){conflicts.add(all[i].s.id);conflicts.add(all[j].s.id)}
  const stateLabel={planned:msg('Geplant','Planned'),completed:msg('Erledigt','Done'),active:msg('Aktiv','Active'),conflict:msg('Überschneidung','Overlap'),cancelled:msg('Abgesagt','Cancelled')};
  let cells='';for(let i=0;i<count;i++){
   const dt=new Date(start);dt.setDate(start.getDate()+i);const key=isoDate(dt),items=dayEntries(key),active=data.activeTimer&&isoDate(new Date(data.activeTimer.startedAt))===key&&(workplaceFilter==='all'||data.activeTimer.workplaceId===workplaceFilter);
   const status=items.some(s=>conflicts.has(s.id))?'conflict':active?'active':items.some(s=>s.status==='planned')?'planned':items.some(s=>s.status==='completed')?'completed':items.length?'cancelled':'';
   cells+='<button type="button" class="shift-day '+status+(key.slice(0,7)!==selected?' other':'')+(key===today?' today':'')+'" data-calendar-date="'+key+'" aria-label="'+safe(dt.toLocaleDateString(locale(),{day:'numeric',month:'long',year:'numeric'})+(status?' · '+stateLabel[status]:'')+' · '+items.length+' '+msg('Schichten','shifts'))+'"'+(key===today?' aria-current="date"':'')+'><span>'+dt.getDate()+'</span><small>'+('•'.repeat(Math.min(3,items.length)))+'</small></button>';
  }
  calendar.innerHTML='<div class="calendar-heading"><div><p class="eyebrow">'+msg('DEINE SCHICHTEN','YOUR SHIFTS')+'</p><h2>'+safe(first.toLocaleDateString(locale(),{month:'long',year:'numeric'}))+'</h2></div><div><button type="button" id="shiftMonthPrevious" aria-label="'+msg('Vorheriger Monat','Previous month')+'">‹</button><button type="button" id="shiftMonthNext" aria-label="'+msg('Nächster Monat','Next month')+'">›</button></div></div><div class="shift-weekdays">'+(language()==='en'?['Mon','Tue','Wed','Thu','Fri','Sat','Sun']:['Mo','Di','Mi','Do','Fr','Sa','So']).map(d=>'<span>'+d+'</span>').join('')+'</div><div class="shift-days">'+cells+'</div><div class="calendar-legend">'+Object.entries(stateLabel).map(([key,label])=>'<span><i class="'+key+'"></i>'+label+'</span>').join('')+'</div><div class="quick-templates"><p class="eyebrow">'+msg('SCHNELLE SCHICHTVORLAGEN','QUICK SHIFT TEMPLATES')+'</p><div>'+data.templates.map(t=>'<button type="button" data-quick-template="'+safe(t.id)+'"><b>'+safe(t.name)+'</b><small>'+safe(t.start+'–'+t.end)+'</small></button>').join('')+'<button type="button" id="calendarNewShift">+ '+msg('Schicht','Shift')+'</button></div></div>';
  const move=n=>{const d=localDate(selected+'-01');d.setMonth(d.getMonth()+n);selected=monthKey(d);q('#month').value=selected;finishSelection();render()};
  q('#shiftMonthPrevious').onclick=()=>move(-1);q('#shiftMonthNext').onclick=()=>move(1);
  qa('[data-calendar-date]').forEach(b=>b.onclick=()=>openDay(b.dataset.calendarDate));
  const defaultDate=()=>pickedDay.startsWith(selected)?pickedDay:today.startsWith(selected)?today:selected+'-01';
  qa('[data-quick-template]').forEach(b=>b.onclick=()=>addOnDate(defaultDate(),b.dataset.quickTemplate));q('#calendarNewShift').onclick=()=>addOnDate(defaultDate());
 }
 function renderOverview(){
  detailLabel.textContent=msg('Verdienst, Fortschritt & letzte Schichten','Earnings, progress & recent shifts');
  const s=summary(selected),all=summary(selected,'all'),expenses=expenseEntries(selected).reduce((a,e)=>a+Number(e.amount||0),0),available=all.est.net-expenses;
  const next=data.shifts.filter(e=>e.status==='planned'&&(workplaceFilter==='all'||e.workplaceId===workplaceFilter)&&new Date(e.date+'T'+e.start)>new Date()).sort((a,b)=>(a.date+a.start).localeCompare(b.date+b.start))[0];
  hero.innerHTML='<div class="available-card"><div class="available-top"><span>'+msg('NACH AUSGABEN VERFÜGBAR','AVAILABLE AFTER EXPENSES')+'</span><span class="estimate-pill">'+msg('GESCHÄTZT','ESTIMATE')+'</span></div><strong id="designAvailable">'+money(available)+'</strong><p>'+msg('Netto aller Arbeitsplätze minus Monatsausgaben','Net from all workplaces minus monthly expenses')+'</p></div><div class="overview-pills"><div><small>'+msg('STUNDEN DIESES MONATS','HOURS THIS MONTH')+'</small><b>'+duration(s.minutes)+'</b></div><button type="button" id="designNextShift"><small>'+msg('NÄCHSTE SCHICHT','NEXT SHIFT')+'</small><b>'+ (next?safe(localDate(next.date).toLocaleDateString(locale(),{day:'numeric',month:'short'})+' · '+next.start):msg('Schicht planen →','Plan a shift →'))+'</b></button></div>';
  q('#designNextShift').onclick=()=>{if(next)openDialog(next.id);else{show('shifts');addOnDate(isoDate(new Date()))}};
  const base=s.gross?Math.min(100,s.baseGross/s.gross*100):0;
  composition.innerHTML='<h3>'+msg('Grundlohn & Zuschläge','Base pay & bonuses')+'</h3><div class="pay-stack" role="img" aria-label="'+safe(msg('Grundlohn ','Base pay ')+money(s.baseGross)+', '+msg('Zuschläge ','bonuses ')+money(s.bonus.total))+'"><i style="width:'+base+'%"></i><i style="width:'+(s.gross?100-base:0)+'%"></i></div><div class="pay-legend"><span><i></i>'+msg('Grundlohn','Base pay')+' <b>'+money(s.baseGross)+'</b></span><span><i></i>'+msg('Zuschläge','Bonuses')+' <b>'+money(s.bonus.total)+'</b></span></div>';
  const pays=data.payslips.filter(p=>p.month===selected&&(workplaceFilter==='all'||p.workplaceId===workplaceFilter));
  // Only compare workplaces that actually have a saved payslip for this month.
  const expected=pays.reduce((a,p)=>a+summary(selected,p.workplaceId).est.net,0),actual=pays.reduce((a,p)=>a+Number(p.actualNet),0),diff=actual-expected;
  receipt.innerHTML='<div><span class="eyebrow">'+msg('LOHNABRECHNUNG','PAYSLIP CHECK')+'</span><h3>'+msg('Deinen Lohn vergleichen','Compare your pay')+'</h3></div>'+(pays.length?'<div class="receipt-values"><span>'+msg('Netto geschätzt','Estimated net')+'<b>'+money(expected)+'</b></span><span>'+msg('Netto laut Abrechnung','Payslip net')+'<b>'+money(actual)+'</b></span></div><span class="receipt-stamp">'+(diff>=0?'+ ':'')+money(diff)+'</span>':'<p>'+msg('Abrechnung hinzufügen und Unterschiede sehen.','Add a payslip to see the difference.')+'</p><span class="receipt-link">'+msg('Abrechnung hinzufügen →','Add payslip →')+'</span>');
  shortcuts.innerHTML='<button class="secondary" type="button" data-design-go="planning">'+msg('Budgets & Sparziele','Budgets & goals')+' ↗</button><button class="secondary" type="button" data-design-go="device">'+msg('Erinnerungen','Reminders')+' ↗</button>';
  qa('[data-design-go]').forEach(b=>b.onclick=()=>{if(b.dataset.designGo==='device')deviceSection='reminders';show(b.dataset.designGo)});
 }
 function decorateGoals(){
  qa('.goal-grid article').forEach(article=>{
   const id=article.querySelector('[data-goal-edit]')?.dataset.goalEdit,g=data.savingsGoals.find(x=>x.id===id);if(!g)return;
   const pct=Math.round(Math.max(0,Math.min(100,g.saved/g.target*100))),ring=document.createElement('div');ring.className='goal-ring';ring.style.setProperty('--goal-progress',pct+'%');ring.setAttribute('role','img');ring.setAttribute('aria-label',pct+'%');ring.innerHTML='<span>'+pct+'<small>%</small></span>';article.prepend(ring);article.classList.add('design-goal');
  });
 }
 function arrangePlanning(){const grid=q('.goal-grid'),head=grid?.previousElementSibling,stack=q('.planning-stack');if(grid&&head&&stack){stack.prepend(head,grid)}}
 const renderPlanningBeforeDesign=renderPlanning;renderPlanning=function(){renderPlanningBeforeDesign();decorateGoals();arrangePlanning()};
 const renderBeforeDesign=render;render=function(){renderBeforeDesign();renderOverview();renderShiftCalendar()};
 const showBeforeDesign=show;show=function(view){showBeforeDesign(view);if(view==='dashboard')renderOverview();if(view==='shifts')renderShiftCalendar();updateDesignTitle(view)};
 function updateDesignTitle(view){q('main').classList.toggle('goals-view',view==='planning');if(view==='dashboard')q('#title').textContent=data.profile?.name?msg('Hey ','Hey ')+data.profile.name.split(' ')[0]+' 👋':msg('Deine Übersicht','Your overview');if(view==='shifts')q('#title').textContent=msg('Schichtkalender','Shift calendar')}
 q('#language').addEventListener('change',()=>{renderOverview();renderShiftCalendar();updateDesignTitle(q('.view.active')?.id)});
 renderOverview();renderShiftCalendar();decorateGoals();arrangePlanning();updateDesignTitle(q('.view.active')?.id);
})();
