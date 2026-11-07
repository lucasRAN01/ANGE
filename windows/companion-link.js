'use strict';
function openCompanion(){window.open('companion/index.html','_blank','noopener')}
document.querySelector('#open-companion').addEventListener('click',openCompanion);
const portrait=document.querySelector('.companion');portrait.setAttribute('role','button');portrait.tabIndex=0;portrait.setAttribute('aria-label','Converser avec la compagne ANGE');portrait.style.cursor='pointer';portrait.addEventListener('click',openCompanion);portrait.addEventListener('keydown',e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();openCompanion()}});
const phone=document.querySelector('#phone-content');
function animatedPhone(){phone.innerHTML='<iframe title="Compagne ANGE animée, aperçu mobile web" src="companion/index.html?compact" style="width:100%;height:100%;border:0;border-radius:8px" allow="microphone"></iframe>'}
animatedPhone();document.addEventListener('click',e=>{if(e.target.closest('[data-phone-go="home"]'))setTimeout(animatedPhone,0)});
