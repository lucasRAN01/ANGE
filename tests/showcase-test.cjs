const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
(async()=>{
const root=path.resolve(__dirname,'..'),out=path.join(root,'docs/verification');const b=await chromium.launch({channel:'chrome',headless:true});const p=await b.newPage({viewport:{width:1536,height:1053}}),errors=[];p.on('pageerror',e=>errors.push(e.message));p.on('requestfailed',r=>errors.push(r.url()+': '+r.failure()?.errorText));
await p.goto('file:///'+path.join(root,'windows/ange-ui.html').replace(/\\/g,'/'));await p.waitForTimeout(250);
assert.equal(await p.locator('.module-dock .mini-panel').count(),6);assert.equal(await p.locator('.feature-item').count(),6);assert.equal(await p.locator('#phone-content iframe').count(),1);
const geometry=await p.evaluate(()=>{const stage=document.querySelector('.stage').getBoundingClientRect();return Object.fromEntries(['.pc-window','.handset','.companion','.module-dock'].map(s=>{const r=document.querySelector(s).getBoundingClientRect();return[s,{x:Math.round(r.x-stage.x),y:Math.round(r.y-stage.y),width:Math.round(r.width),height:Math.round(r.height)}]}))});assert.equal(geometry['.pc-window'].x,196);assert.equal(geometry['.pc-window'].y,144);assert.equal(geometry['.pc-window'].width,794);
const images=await p.locator('img').evaluateAll(es=>es.map(e=>({src:e.getAttribute('src'),loaded:e.complete&&e.naturalWidth>0})));assert.equal(images.every(i=>i.loaded),true);
await p.locator('.stage').screenshot({path:path.join(out,'reference-reproduction.png')});
const phone=p.frameLocator('#phone-content iframe');await phone.locator('#settings-toggle').click();await phone.locator('#auto-speak').uncheck();await phone.locator('#settings-toggle').click();await phone.locator('#message').fill('Bonjour');await phone.locator('#send').click();assert.match(await phone.locator('#answer').textContent(),/Bonjour/);await p.locator('.phone-nav [data-phone-go="home"]').click();assert.equal(await p.locator('#phone-content iframe').count(),1);
const toggle=p.locator('.switch').first();await toggle.click();assert.equal(await toggle.getAttribute('aria-checked'),'false');
await p.locator('[data-language="English"]').click();assert.match(await p.locator('#toast').textContent(),/English/);
await p.locator('#dock-message').fill('Bonjour ANGE');await p.locator('#dock-message').press('Enter');assert.equal(await p.locator('.bubble.me').textContent(),'Bonjour ANGE');
await p.locator('#modules').click();await p.locator('nav [data-go="splash"]').click();assert.match(await p.locator('#content').textContent(),/Interface prête/);
await p.locator('#minimize').click();assert.equal(await p.locator('.pc-window').evaluate(e=>e.classList.contains('minimized')),true);await p.locator('#minimize').click();
assert.deepEqual(errors,[]);await b.close();fs.writeFileSync(path.join(out,'showcase-results.json'),JSON.stringify({status:'PASS',geometry,images,checks:['6 panneaux DOM','6 fonctionnalités','6 raccourcis mobile','images chargées','parcours voix mobile','interrupteur','langue','chat du panneau','menu modules','réduction et restauration','absence erreur JavaScript ou fichier manquant'],errors},null,2));console.log('PASS showcase');
})().catch(e=>{console.error(e);process.exit(1)});
