function payslipReminderPayload(){
 const months=new Map();
 for(const shift of data.shifts){
  if(shift.status==='cancelled')continue;
  const month=shift.date.slice(0,7);if(!months.has(month))months.set(month,new Set());
  months.get(month).add(shift.workplaceId||'default');
 }
 return{enabled:data.settings.payslipReminder===true,language:language(),months:[...months].map(([month,places])=>({month,missing:[...places].some(id=>!data.payslips.some(p=>p.month===month&&p.workplaceId===id))}))};
}
function syncPayslipReminder(){
 if(typeof Android!=='undefined'&&typeof Android.syncPayslipReminder==='function')Android.syncPayslipReminder(JSON.stringify(payslipReminderPayload()));
}
function addPayslipReminderControl(){
 const home=q('#device .reminders-home');if(!home||q('#payslipReminderEnabled'))return;
 const native=typeof Android!=='undefined'&&typeof Android.syncPayslipReminder==='function';
 home.insertAdjacentHTML('beforeend','<h2 class="reminder-heading">'+msg('Lohnabrechnung','Payslip')+'</h2><article class="panel reminder-group">'+reminderSwitch('payslipReminderEnabled',msg('Abrechnung eintragen','Enter payslip'),msg('Fehlender Vormonat · ab dem 7., höchstens wöchentlich','Missing previous month · from the 7th, at most weekly'),data.settings.payslipReminder===true,!native)+'</article>');
 q('#payslipReminderEnabled').onchange=e=>{data.settings.payslipReminder=e.target.checked;save();if(e.target.checked&&typeof Android.requestShiftNotifications==='function')Android.requestShiftNotifications()};
}
const beforePayslipHome=renderRemindersHome;
renderRemindersHome=function(){beforePayslipHome();addPayslipReminderControl()};
const beforePayslipSave=save;
save=function(){beforePayslipSave();syncPayslipReminder()};
window.openPayslipReminder=()=>{
 if(typeof Android==='undefined'||typeof Android.consumePayslipReminder!=='function')return;
 const month=Android.consumePayslipReminder();if(!/^\d{4}-(0[1-9]|1[0-2])$/.test(month))return;
 selected=month;q('#month').value=month;show('history');render();q('#payslipMonth').value=month;
 const missing=data.shifts.find(e=>e.date.startsWith(month)&&e.status!=='cancelled'&&!data.payslips.some(p=>p.month===month&&p.workplaceId===e.workplaceId));
 if(missing)q('#payslipWorkplace').value=missing.workplaceId;
 q('#payslipForm').scrollIntoView({block:'start'});
};
q('#language').addEventListener('change',syncPayslipReminder);
window.addEventListener('load',window.openPayslipReminder);
addPayslipReminderControl();syncPayslipReminder();
