const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright');const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
(async()=>{
const root=path.resolve(__dirname,'..'),out=path.join(root,'docs/verification');const b=await chromium.launch({channel:'chrome',headless:true});const p=await b.newPage({viewport:{width:430,height:1100}});const errors=[];p.on('pageerror',e=>errors.push(e.message));
// Deterministic TTS stand-in verifies lifecycle, not sound quality or actual device voices.
await p.addInitScript(()=>{
const fake=new EventTarget();fake.getVoices=()=>[{name:'Test FR locale',voiceURI:'test-fr',lang:'fr-FR',localService:true}];let current=null;
fake.speak=u=>{current=u;setTimeout(()=>{if(current===u)u.onstart?.()},20)};fake.cancel=()=>{current=null};Object.defineProperty(window,'speechSynthesis',{value:fake});window.SpeechSynthesisUtterance=class{constructor(text){this.text=text}};
});
await p.goto('file:///'+path.join(root,'windows/companion/index.html').replace(/\\/g,'/'));await p.waitForTimeout(100);
await p.locator('#settings-toggle').click();await p.locator('#auto-speak').uncheck();await p.locator('#settings-toggle').click();
await p.locator('#message').fill('Quelle heure est-il ?');await p.locator('#send').click();assert.match(await p.locator('#answer').textContent(),/^Il est /);assert.equal(await p.locator('#count').textContent(),'2');
await p.locator('#avatar').click();await p.waitForTimeout(150);assert.equal(await p.locator('body').evaluate(e=>e.classList.contains('speaking')),true);
const frames=new Set();for(let i=0;i<12;i++){frames.add(await p.locator('#sprite').evaluate(e=>e.style.backgroundPosition));await p.waitForTimeout(75)}assert.ok(frames.size>1,'animated mouth poses');
await p.screenshot({path:path.join(out,'companion-mobile-web.png'),fullPage:true});await p.locator('#stop').click();assert.equal(await p.locator('body').evaluate(e=>e.classList.contains('idle')),true);
await p.locator('#listen').click();assert.match(await p.locator('#status').textContent(),/Autorisez la dictée/);
await p.locator('[data-mode="cloud"]').click();await p.locator('#message').fill('Message privé');p.once('dialog',d=>d.dismiss());await p.locator('#send').click();assert.equal(await p.locator('#count').textContent(),'2');
await p.locator('#settings-toggle').click();await p.locator('#motion').uncheck();await p.waitForTimeout(120);assert.equal(await p.locator('#sprite').evaluate(e=>e.style.backgroundPosition),'0% 0%');
await p.setViewportSize({width:360,height:800});assert.equal(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);
assert.deepEqual(errors,[]);fs.writeFileSync(path.join(out,'companion-results.json'),JSON.stringify({status:'PASS',checks:['réponse limitée heure','historique','salutation','poses de bouche pendant TTS simulé','arrêt','dictée sans consentement refusée','cloud annulé sans envoi','animations désactivées','affichage 360px'],actualAudioTested:false,actualSpeechRecognitionTested:false,errors},null,2));await b.close();console.log('PASS companion browser — speech callbacks simulated');
})().catch(e=>{console.error(e);process.exit(1)});
