// Savings are recorded allocations, not bank transfers. The ledger prevents reusing income.
function planningData(){if(!Array.isArray(data.customCategories))data.customCategories=[];if(!Array.isArray(data.savingsLedger))data.savingsLedger=[]}
function budgetCategoryKeys(){planningData();return [...planningCategories,...data.customCategories.map(c=>c.id)]}
function nextSavingsDate(date,interval,anchor){const d=new Date(date+'T12:00'),day=anchor||d.getDate();if(interval==='weekly')d.setDate(d.getDate()+7);else{const month=d.getMonth()+(interval==='yearly'?12:1);d.setDate(1);d.setMonth(month);d.setDate(Math.min(day,new Date(d.getFullYear(),d.getMonth()+1,0).getDate()))}return isoDate(d)}
const cents=n=>Math.round((Number(n)||0)*100);
function savingsAvailable(now=new Date()){
 planningData();const today=isoDate(now),months=[...new Set(data.shifts.filter(s=>s.status==='completed'&&shiftRange(s).end<=now).map(s=>s.date.slice(0,7)))];
 const income=months.reduce((sum,m)=>sum+cents(summary(m,'all',now,false).est.net),0);
 // Include unmaterialized recurring costs as well as explicitly entered expenses.
 let expenses=data.expenses.filter(e=>e.date<=today).reduce((s,e)=>s+cents(e.amount),0);
 for(const t of data.recurringExpenses){let key=t.startMonth;for(let i=0;key<=monthKey(now)&&i<1200;i++){const [y,m]=key.split('-').map(Number),day=Math.min(t.day,new Date(y,m,0).getDate()),date=key+'-'+String(day).padStart(2,'0');if(date<=today&&!data.expenses.some(e=>e.recurringId===t.id&&e.date.slice(0,7)===key))expenses+=cents(t.amount);key=monthKey(new Date(y,m,1))}}
 const allocated=data.savingsLedger.reduce((s,e)=>s+cents(e.amount),0);
 const manual=(data.savingsGoals||[]).reduce((s,g)=>s+Math.max(0,cents(g.saved)-data.savingsLedger.filter(e=>e.goalId===g.id).reduce((n,e)=>n+cents(e.amount),0)),0);
 return income-expenses-allocated-manual;
}
function reconcileSavings(now=new Date()){
 planningData();const today=isoDate(now);let available=savingsAvailable(now),changed=false;
 // Deterministic ordering gives the oldest due goal first claim on unallocated funds.
 const goals=[...(data.savingsGoals||[])].filter(g=>g.auto&&g.autoNext&&g.autoNext<=today).sort((a,b)=>a.autoNext.localeCompare(b.autoNext)||a.id.localeCompare(b.id));
 for(const g of goals){for(let i=0;i<1200&&g.autoNext<=today&&cents(g.saved)<cents(g.target);i++){
  const remaining=cents(g.target)-cents(g.saved),amount=g.autoAmount>0?Math.min(cents(g.autoAmount),remaining):Math.min(Math.max(0,available),remaining);
  if(!amount||available<amount){const overdue=g.autoAmount>0?Math.max(0,amount-Math.max(0,available)):Math.min(remaining,Math.max(0,-available));if(g.autoOverdue!==overdue/100){g.autoOverdue=overdue/100;changed=true}break}
  const id=g.id+':'+g.autoEpoch+':'+g.autoNext;
  if(!data.savingsLedger.some(e=>e.id===id)){data.savingsLedger.push({id,goalId:g.id,date:today,scheduled:g.autoNext,amount:amount/100});g.saved=(cents(g.saved)+amount)/100;available-=amount;changed=true}
  g.autoOverdue=0;g.autoNext=nextSavingsDate(g.autoNext,g.autoInterval,g.autoAnchor);changed=true;
 }}return changed;
}
function goalAutoStatus(g){if(!g.auto)return msg('Manuell','Manual');if(cents(g.saved)>=cents(g.target))return msg('Ziel erreicht','Goal reached');if(g.autoOverdue>0)return '−'+money(g.autoOverdue)+' '+msg('ausstehend','overdue');if(g.autoNext<=isoDate(new Date()))return msg('Wartet auf verfügbares Guthaben','Waiting for available balance');return msg('Nächster Beitrag: ','Next contribution: ')+new Date(g.autoNext+'T12:00').toLocaleDateString(language()==='en'?'en-GB':'de-DE')}
planningData();
