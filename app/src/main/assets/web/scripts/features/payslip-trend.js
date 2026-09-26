// Monthly pairs use exactly the workplaces with recorded payslips in each month.
// Gaps remain gaps, rather than implying that missing payslips were zero.
function payslipTrendData(list){
 const valid=list.filter(p=>/^\d{4}-(0[1-9]|1[0-2])$/.test(p.month));
 if(!valid.length)return [];
 const latest=valid.map(p=>p.month).sort().at(-1),[year,month]=latest.split('-').map(Number);
 return Array.from({length:12},(_,i)=>{
  const date=new Date(year,month-12+i,1,12),key=monthKey(date),rows=valid.filter(p=>p.month===key);
  return{month:key,date,estimate:rows.length?rows.reduce((n,p)=>n+summary(key,p.workplaceId).est.net,0):null,
   actual:rows.length?rows.reduce((n,p)=>n+Number(p.actualNet||0),0):null};
 });
}
function payslipTrendChart(list){
 const points=payslipTrendData(list);if(!points.length)return '';
 const ceiling=Math.max(1,...points.flatMap(p=>[p.estimate||0,p.actual||0])),left=48,right=396,top=20,bottom=148;
 const x=i=>left+i*(right-left)/11,y=n=>bottom-n/ceiling*(bottom-top);
 function line(field){let path='',connected=false;
  points.forEach((p,i)=>{if(p[field]===null){connected=false;return}path+=(connected?' L':' M')+x(i).toFixed(1)+' '+y(p[field]).toFixed(1);connected=true});
  return '<path class="trend-'+field+'" d="'+path+'"/>'+points.map((p,i)=>p[field]===null?'':'<circle class="trend-dot-'+field+'" cx="'+x(i)+'" cy="'+y(p[field])+'" r="3"><title>'+p.month+' · '+(field==='actual'?msg('Netto laut Abrechnung','Payslip net'):msg('Geschätztes Netto','Estimated net'))+': '+money(p[field])+'</title></circle>').join('');
 }
 const title=msg('Netto im Vergleich · 12 Monate','Net pay comparison · 12 months');
 return '<figure class="payslip-trend" data-localized><figcaption>'+title+'</figcaption><svg viewBox="0 0 416 180" role="img" aria-label="'+title+'"><desc>'+msg('Gestrichelt: Schätzung. Durchgezogen: Abrechnung. Fehlende Monate bleiben leer. Die Einzelwerte stehen darunter.','Dashed: estimate. Solid: payslip. Missing months stay blank. Exact values are listed below.')+'</desc>'+[0,.5,1].map(r=>'<path d="M'+left+' '+y(ceiling*r)+' H'+right+'" stroke="currentColor" opacity=".15"/><text x="42" y="'+(y(ceiling*r)+4)+'" text-anchor="end">'+new Intl.NumberFormat(language()==='en'?'en-GB':'de-DE',{notation:'compact',maximumFractionDigits:1}).format(ceiling*r)+'</text>').join('')+line('estimate')+line('actual')+[0,3,6,9,11].map(i=>'<text x="'+x(i)+'" y="169" text-anchor="middle">'+points[i].date.toLocaleDateString(language()==='en'?'en-GB':'de-DE',{month:'short'}).replace('.','')+'</text>').join('')+'</svg><div class="trend-legend"><span><i></i>'+msg('Geschätztes Netto','Estimated net')+'</span><span><i class="actual-key"></i>'+msg('Netto laut Abrechnung','Payslip net')+'</span></div><p>'+points[0].month+' – '+points[11].month+' · '+currencyCode()+' · '+msg('Nur Arbeitsplätze mit Abrechnung','Only workplaces with a payslip')+'</p></figure>';
}
