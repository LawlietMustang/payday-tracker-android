// UI history is separate from WebView URL history: every screen uses the same URL.
let pageTrail=[],currentRoute={view:q('.view.active')?.id||'dashboard',panel:deviceSection},returning=false;
const showBeforeNavigation=show;
show=function(view){
 const next={view,panel:view==='device'?deviceSection:null};
 if(!returning&&(next.view!==currentRoute.view||next.panel!==currentRoute.panel)){
  if(view==='dashboard')pageTrail=[];else pageTrail.push({...currentRoute,scroll:scrollY});
 }
 showBeforeNavigation(view);currentRoute=next;
};
window.handleAppBack=function(){
 const modal=qa('dialog[open]').at(-1);
 if(modal){if(modal.dispatchEvent(new Event('cancel',{cancelable:true})))modal.close();return true}
 if(q('#drawer').classList.contains('open')){closeDrawer();return true}
 if(selectionMode){finishSelection();return true}
 if(currentRoute.view==='dashboard')return false;
 const previous=pageTrail.pop()||{view:'dashboard'};returning=true;
 try{if(previous.view==='device')deviceSection=previous.panel||'all';show(previous.view)}finally{returning=false}
 requestAnimationFrame(()=>scrollTo({top:previous.scroll||0,behavior:'instant'}));return true;
};
function isoCalendarWeek(date){
 const d=new Date(Date.UTC(date.getFullYear(),date.getMonth(),date.getDate()));
 d.setUTCDate(d.getUTCDate()+4-(d.getUTCDay()||7));
 const year=d.getUTCFullYear(),start=new Date(Date.UTC(year,0,1));
 return {year,week:Math.ceil(((d-start)/86400000+1)/7)};
}
renderBars=function(list){
 const [year,month]=selected.split('-').map(Number),buckets=new Map();
 const count=new Date(year,month,0).getDate();
 for(let day=1;day<=count;day++){const w=isoCalendarWeek(new Date(year,month-1,day)),key=w.year+'-'+w.week;if(!buckets.has(key))buckets.set(key,{...w,minutes:0})}
 for(const e of list){if(!e.date.startsWith(selected+'-'))continue;const [y,m,d]=e.date.split('-').map(Number),w=isoCalendarWeek(new Date(y,m-1,d));buckets.get(w.year+'-'+w.week).minutes+=e.minutes}
 const values=[...buckets.values()],max=Math.max(60,...values.map(v=>v.minutes));
 q('#bars').style.gridTemplateColumns='repeat('+values.length+',minmax(0,1fr))';
 q('#bars').innerHTML=values.map(v=>'<div class="bar"><i style="height:'+Math.max(2,v.minutes/max*100)+'%" title="'+v.year+' · '+duration(v.minutes)+'"></i><small>'+(language()==='en'?'W':'KW')+v.week+'</small></div>').join('');
};
// Make the missing account service explicit instead of presenting an inert sign-in row.
const accountNotice=q('.account-unavailable');accountNotice.setAttribute('role','note');
function explainSignIn(){accountNotice.innerHTML='<strong>'+msg('Google-Anmeldung nicht eingerichtet','Google sign-in not configured')+'</strong><small>'+msg('Kontoverbindung und automatische Drive-Sicherung sind noch nicht verfügbar. Nutze „Sichern & wiederherstellen“ für eine Datei auf dem Gerät oder in Drive.','Account connection and automatic Drive backup are not available yet. Use Backup & restore for a file on your device or in Drive.')+'</small>'}
q('#language').addEventListener('change',()=>{explainSignIn();renderBars(summary(selected).done)});
explainSignIn();renderBars(summary(selected).done);
