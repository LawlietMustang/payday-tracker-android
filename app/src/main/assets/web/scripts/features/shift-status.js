// Local-time shift lifecycle. Automatic completion runs on open/resume and while visible.
let shiftStatusManual=false;
function suggestedShiftStatus(date,start,now=Date.now()){
 return new Date(date+'T'+start).getTime()>now?'planned':'completed';
}
function shiftStatusLabel(status){return status==='cancelled'?msg('Abgesagt','Cancelled'):status==='planned'?msg('Geplant','Planned'):msg('Erledigt','Completed')}
function cancellationLabel(by){return by==='employer'?msg('Vom Arbeitgeber abgesagt','Cancelled by employer'):by==='me'?msg('Von mir abgesagt','Cancelled by me'):msg('Abgesagt · nicht angegeben','Cancelled · not specified')}
function updateAutoShiftStatus(){
 if(!shiftStatusManual)q('#status').value=suggestedShiftStatus(q('#date').value,q('#start').value);
 updateCancellationFields();
}
function updateCancellationFields(){
 if(q('#cancelledByField'))q('#cancelledByField').hidden=q('#status').value!=='cancelled';
 if(q('#statusModeHint'))q('#statusModeHint').textContent=shiftStatusManual?msg('Manuell gewählt','Manually selected'):msg('Automatisch nach Beginn','Based on start date and time');
}
function clockShiftCandidates(t){
 return data.shifts.filter(s=>{if(s.status!=='planned'||s.workplaceId!==t.workplaceId)return false;const r=shiftRange(s);return r.start.getTime()-30*60000<=t.startedAt&&r.end.getTime()>t.startedAt});
}
function timerShift(t){
 if(!t)return null;
 if(t.shiftId)return data.shifts.find(s=>s.id===t.shiftId&&s.status==='planned')||null;
 const candidates=clockShiftCandidates(t);return candidates.length===1?candidates[0]:null;
}
function reconcileShiftStatuses(now=Date.now()){
 if(data.settings.autoCompletePlanned!==true)return false;
 const timer=data.activeTimer,linked=timerShift(timer);let changed=false;
 for(const s of data.shifts){
  if(s.status!=='planned'||s.id===linked?.id)continue;
  const r=shiftRange(s),end=r.end.getTime();
  // An unlinked running clock still protects overlapping scheduled work.
  if(timer&&s.workplaceId===timer.workplaceId&&r.start.getTime()<now&&end>timer.startedAt)continue;
  if(Number.isFinite(end)&&end<=now){s.status='completed';s.completedAutomatically=true;s.completedAt=now;changed=true}
 }
 if(changed)save();return changed;
}
function renderStatusSummary(){
 const box=q('#cancellationSummary');if(!box)return;
 const list=entries(selected),cancelled=list.filter(s=>s.status==='cancelled'),employer=cancelled.filter(s=>s.cancelledBy==='employer').length,me=cancelled.filter(s=>s.cancelledBy==='me').length;
 box.hidden=!cancelled.length;box.textContent=msg('Abgesagt: ','Cancelled: ')+cancelled.length+' / '+list.length+' · '+msg('Arbeitgeber: ','Employer: ')+employer+' · '+msg('Von mir: ','By me: ')+me+(cancelled.length-employer-me?' · '+msg('Nicht angegeben: ','Not specified: ')+(cancelled.length-employer-me):'');
}
function installShiftStatusUI(){
 for(const id of ['status','bulkStatus'])q('#'+id).insertAdjacentHTML('beforeend','<option value="cancelled"></option>');
 q('#status').closest('label').insertAdjacentHTML('afterend','<label id="cancelledByField" class="wide" data-localized hidden><span id="cancelledByLabel"></span><select id="cancelledBy"><option value="unspecified"></option><option value="employer"></option><option value="me"></option></select></label><div class="status-mode wide" data-localized><small id="statusModeHint"></small><button type="button" class="secondary" id="statusAutomatic"></button></div>');
 q('#bulkStatus').closest('label').insertAdjacentHTML('afterend','<label id="bulkCancelledByField" class="wide" data-localized hidden><span id="bulkCancelledByLabel"></span><select id="bulkCancelledBy"><option value="unspecified"></option><option value="employer"></option><option value="me"></option></select></label>');
 q('#status').addEventListener('change',()=>{shiftStatusManual=true;updateCancellationFields();preview()});
 q('#statusAutomatic').onclick=()=>{shiftStatusManual=false;updateAutoShiftStatus();preview()};
 const oldPreview=preview;preview=function(){oldPreview();if(q('#status').value==='cancelled'){q('#previewTime').textContent='0 h';q('#previewMoney').textContent=money(0)+' · '+msg('Abgesagt','Cancelled')}};
 ['date','start','end','break'].forEach(id=>q('#'+id).addEventListener('input',()=>preview()));
 ['date','start'].forEach(id=>{for(const event of ['input','change'])q('#'+id).addEventListener(event,updateAutoShiftStatus)});
 q('#bulkStatus').addEventListener('change',()=>{q('#bulkCancelledByField').hidden=q('#bulkStatus').value!=='cancelled'});
 const setting=document.createElement('article');setting.className='panel shift-status-settings';setting.dataset.localized='true';setting.innerHTML='<label class="check"><span><b id="autoCompleteTitle"></b><small id="autoCompleteDescription"></small></span><input id="autoCompletePlanned" type="checkbox" role="switch"></label>';q('#appSettings .account-stack').append(setting);
 const summary=document.createElement('p');summary.id='cancellationSummary';summary.className='cancellation-summary';summary.dataset.localized='true';q('#shifts .sectionhead').before(summary);
 q('#autoCompletePlanned').onchange=e=>{data.settings.autoCompletePlanned=e.target.checked;save();if(reconcileShiftStatuses())render()};
 function labels(){
  for(const id of ['status','bulkStatus']){q('#'+id+' option[value=cancelled]').textContent=shiftStatusLabel('cancelled');q('#'+id+' option[value=cancelled]').dataset.localized='true'}
  for(const prefix of ['','bulk']){const id=prefix?'bulkCancelledBy':'cancelledBy';q('#'+id+'Label').textContent=msg('Wer hat abgesagt?','Who cancelled?');for(const option of q('#'+id).options)option.textContent=option.value==='employer'?msg('Arbeitgeber','Employer'):option.value==='me'?msg('Ich','Me'):msg('Nicht angegeben','Not specified')}
  q('#statusAutomatic').textContent=msg('Automatisch wählen','Use automatic status');q('#autoCompleteTitle').textContent=msg('Geplante Schichten automatisch abschließen','Auto-complete planned shifts');q('#autoCompleteDescription').textContent=msg('Nach dem geplanten Ende als gearbeitet zählen. Laufende Timer warten auf dein Beenden.','Count as worked after the scheduled end. Running timers wait until you finish.');q('#autoCompletePlanned').checked=data.settings.autoCompletePlanned===true;updateCancellationFields();renderStatusSummary();renderClock();
 }
 q('#language').addEventListener('change',labels);labels();
 const oldRender=render;render=function(){oldRender();renderStatusSummary()};
 const oldShow=show;show=function(view){if(reconcileShiftStatuses())render();oldShow(view);if(view==='appSettings')q('#autoCompletePlanned').checked=data.settings.autoCompletePlanned===true};
 const check=()=>{if(document.hidden||q('dialog[open]')||selectionMode)return;if(reconcileShiftStatuses())render()};
 document.addEventListener('visibilitychange',check);window.addEventListener('pageshow',check);window.addEventListener('focus',check);setInterval(check,15000);check();
}
