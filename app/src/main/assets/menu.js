// One route catalog owns labels, destinations and supplied icons in every menu.
const APP_ROUTES = Object.freeze({
 dashboard:{view:'dashboard',label:['Übersicht','Overview'],glyph:'⌂'},
 shifts:{view:'shifts',label:['Arbeitszeiten','Work hours'],short:['Zeiten','Hours'],glyph:'◷'},
 expenses:{view:'expenses',label:['Ausgaben','Expenses'],glyph:'€'},
 history:{view:'history',label:['Verlauf','History'],glyph:'▥'},
 appSettings:{view:'appSettings',label:['Einstellungen','Settings'],glyph:'⚙'},
 profile:{view:'profile',label:['Profil','Profile'],glyph:'○'},
 pay:{view:'settings',label:['Lohn & Steuern','Pay & tax'],glyph:'€'},
 workplaces:{view:'workplaces',label:['Arbeitsplätze','Workplaces'],icon:'workplaces.svg'},
 planning:{view:'planning',label:['Budgets & Sparziele','Budgets & goals'],icon:'budgets-goals.svg'},
 reminders:{view:'device',panel:'reminders',label:['Erinnerungen & Widget','Reminders & widget'],short:['Erinnerungen','Reminders'],icon:'reminders-widget.svg'},
 lock:{view:'device',panel:'lock',label:['App-Sperre','App lock'],glyph:'▣'},
 recovery:{view:'recovery',label:['Sichern & wiederherstellen','Backup & restore'],glyph:'↥'},
 delete:{label:['Daten löschen','Delete data'],glyph:'×',action:()=>deleteAccountData()}
});
const DRAWER_ROUTES=['dashboard','shifts','expenses','history','appSettings'];
const SETTINGS_ROUTES=['profile','pay','workplaces','planning','reminders','lock','recovery','appearance','delete'];
const SHORTCUT_ROUTES=['planning','reminders'];
function routeLabel(id,short=false){const route=APP_ROUTES[id];return msg(...(short&&route.short||route.label))}
function routeIcon(id){const route=APP_ROUTES[id];return route.icon?'<span class="route-icon" data-icon="'+id+'" aria-hidden="true"></span>':'<span class="route-glyph" aria-hidden="true">'+route.glyph+'</span>'}
function openRoute(id){const route=APP_ROUTES[id];if(route.action){route.action();return}if(route.panel)deviceSection=route.panel;show(route.view)}
function routeButton(id,className='',buttonId=''){
 const route=APP_ROUTES[id];return '<button type="button" class="route-link '+className+'" data-route="'+id+'"'+(route.view?' data-view="'+route.view+'"':'')+(route.panel?' data-panel="'+route.panel+'"':'')+(buttonId?' id="'+buttonId+'"':'')+'>'+routeIcon(id)+'<span data-route-label="'+id+'">'+routeLabel(id)+'</span><span class="route-chevron" aria-hidden="true">›</span></button>';
}
function bindRoutes(root){root.querySelectorAll('[data-route]').forEach(button=>button.onclick=()=>openRoute(button.dataset.route))}
function buildSettingsMenu(){
 const ids={lock:'settingsLock',recovery:'openRecovery',delete:'settingsDelete'};
 q('#settingsMenu').innerHTML=SETTINGS_ROUTES.map(id=>id==='appearance'?'<details id="appearanceSection"><summary data-account-label="appearance"></summary><div id="appearanceControls"></div></details>':routeButton(id,'account-row'+(id==='delete'?' account-danger':''),ids[id]||'')).join('');
 bindRoutes(q('#settingsMenu'));
}
function labelRoutes(){
 qa('[data-route-label]').forEach(el=>{const value=routeLabel(el.dataset.routeLabel,el.hasAttribute('data-short-label'));if(el.textContent!==value)el.textContent=value});
 qa('.mobile [data-view]').forEach(button=>{const value=routeLabel(button.dataset.view,true),label=button.querySelector('small');if(label&&label.textContent!==value)label.textContent=value});
}
function updateNavigationState(){
 const view=q('.view.active')?.id,mainRoute=DRAWER_ROUTES.includes(view)?view:'appSettings';
 qa('[data-menu] [data-route]').forEach(button=>{const active=button.dataset.route===mainRoute;button.classList.toggle('active',active);if(active)button.setAttribute('aria-current','page');else button.removeAttribute('aria-current')});
}
function installNavigation(){
 for(const nav of [q('aside:not(.drawer) nav'),q('#drawer nav')]){nav.dataset.menu='main';nav.dataset.localized='true';nav.setAttribute('aria-label',msg('Hauptmenü','Main menu'));nav.innerHTML=DRAWER_ROUTES.map(id=>routeButton(id,'drawer-link')).join('');bindRoutes(nav)}
 const previousShow=show;show=function(view){previousShow(view);updateNavigationState()};
 q('#language').addEventListener('change',()=>{labelRoutes();qa('[data-menu]').forEach(nav=>nav.setAttribute('aria-label',msg('Hauptmenü','Main menu')))});
 labelRoutes();updateNavigationState();
}
