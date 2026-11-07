const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const fs=require('node:fs');const path=require('node:path');const assert=require('node:assert/strict');
(async()=>{
const root=path.resolve(__dirname,'..');const out=path.join(root,'docs','verification');fs.mkdirSync(out,{recursive:true});
const browser=await chromium.launch({headless:true,...(process.env.CHROME_PATH?{executablePath:process.env.CHROME_PATH}:{channel:'chrome'})});
const page=await browser.newPage({viewport:{width:1440,height:1100}});const errors=[];page.on('pageerror',e=>errors.push(e.message));
const network=[];page.on('request',r=>{if(/^https?:/.test(r.url()))network.push(r.url())});
await page.goto('file:///'+path.join(root,'windows','ange-ui.html').replace(/\\/g,'/'));
await page.evaluate(()=>location.hash='__proto__');await page.waitForTimeout(80);assert.equal(await page.locator('nav .active').getAttribute('data-go'),'home');
await page.locator('#view-mode').click();
const ids=['splash','home','mobile','voice','chat','brain','memory','link','control','settings'];
for(const id of ids){await page.locator(`nav [data-go="${id}"]`).click();await page.waitForTimeout(80);assert.equal(await page.locator('#content').textContent().then(t=>t.trim().length>0),true);assert.equal(await page.locator('nav .active').getAttribute('data-go'),id);await page.screenshot({path:path.join(out,id+'.png'),fullPage:true});}
await page.locator('nav [data-go="chat"]').click();await page.locator('#msg').fill('<img src=x onerror=alert(1)>');await page.locator('#msg').press('Enter');assert.equal(await page.locator('.bubble.me').textContent(),'<img src=x onerror=alert(1)>');assert.equal(await page.locator('.bubble img').count(),0);assert.equal(await page.locator('.bubble').count(),2);
await page.locator('nav [data-go="memory"]').click();await page.locator('#note').fill('Note test');await page.locator('#note').press('Enter');assert.match(await page.locator('#content').textContent(),/Note test/);await page.reload();assert.match(await page.locator('#content').textContent(),/Note test/);
page.once('dialog',d=>d.accept('Note modifiée'));await page.locator('[data-action="edit:0"]').click();assert.match(await page.locator('#content').textContent(),/Note modifiée/);
const downloadPromise=page.waitForEvent('download');await page.locator('[data-action="export"]').click();const download=await downloadPromise;const exportPath=path.join(out,'notes-test.json');await download.saveAs(exportPath);assert.equal(JSON.parse(fs.readFileSync(exportPath)).notes[0],'Note modifiée');
page.once('dialog',d=>d.accept());await page.locator('[data-action="delete:0"]').click();assert.equal(await page.locator('[data-action="delete:0"]').count(),0);
page.once('dialog',d=>d.accept());await page.locator('#import-file').setInputFiles(exportPath);await page.waitForTimeout(100);assert.match(await page.locator('#content').textContent(),/Note modifiée/);
await page.locator('#import-file').setInputFiles({name:'invalid.json',mimeType:'application/json',buffer:Buffer.from('{"notes":"invalid"}')});await page.waitForTimeout(100);assert.match(await page.locator('#toast').textContent(),/Import refusé/);assert.match(await page.locator('#content').textContent(),/Note modifiée/);
page.once('dialog',d=>d.accept());await page.locator('[data-action="erase"]').click();
await page.locator('nav [data-go="settings"]').click();await page.locator('#language').selectOption('Malagasy');await page.reload();assert.equal(await page.locator('#language').inputValue(),'Malagasy');await page.locator('#language').selectOption('Français');
await page.locator('nav [data-go="voice"]').click();for(const s of ['Standby','Listening','Processing','Speaking','Error','Offline']){await page.locator(`[data-action="state:${s}"]`).click();assert.equal(await page.locator('#voice-state').textContent(),s)}
await page.locator('nav [data-go="link"]').click();await page.locator('[data-action="pair"]').click();assert.match(await page.locator('#pair-status').textContent(),/Aucun appareil appairé/);
await page.locator('nav [data-go="control"]').click();page.once('dialog',d=>d.dismiss());await page.locator('[data-action="control:0"]').click();assert.match(await page.locator('#control-status').textContent(),/annulée/);
await page.locator('#motion').click();assert.equal(await page.locator('body').evaluate(e=>e.classList.contains('quiet')),true);
await page.locator('nav [data-go="home"]').click();await page.screenshot({path:path.join(out,'windows-desktop.png'),fullPage:true});
await page.setViewportSize({width:390,height:844});for(const id of ids){await page.locator(`nav [data-go="${id}"]`).click();assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true,'Overflow '+id)}
await page.locator('nav [data-go="mobile"]').click();await page.screenshot({path:path.join(out,'windows-responsive.png'),fullPage:true});
assert.deepEqual(errors,[]);assert.deepEqual(network,[]);await browser.close();
fs.writeFileSync(path.join(out,'ui-results.json'),JSON.stringify({status:'PASS',screens:ids,checks:['navigation 10 écrans','captures 10 écrans','chat et échappement HTML','notes ajout modification suppression persistance','export et import JSON','6 états voix','appairage fictif','annulation contrôle','réduction animation','absence débordement 390px sur 10 écrans','absence erreurs JavaScript','absence requêtes réseau'],errors,network},null,2));console.log('PASS — UI, 10 écrans, parcours et responsive');
})().catch(e=>{console.error(e);process.exit(1)});
