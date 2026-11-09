"""Maps where ChatGPT's answer sits in the page now (headless Chromium = Android WebView engine)."""
import json, time, urllib.parse
from playwright.sync_api import sync_playwright

prompt = "Hva kan jeg lage til middag? Foreslå 3 middager. Handlelisten min: Pasta, Pesto, Kylling, Ost, Mais. Svar kort på norsk.\nAvslutt med én linje som starter med «HANDLE:»."
url = "https://chatgpt.com/?" + urllib.parse.urlencode({"q": prompt})
def note(t, m): print(f"::notice title={t}::{str(m).replace(chr(10),' ')[:1800]}")

MAP = """(()=>{
const out={};
const copy=[...document.querySelectorAll('button')].find(b=>/Kopier svar|Copy response|Copy/i.test(b.getAttribute('aria-label')||''));
const chain=[];let e=copy;for(let i=0;e&&i<12;i++,e=e.parentElement){const d={};for(const a of e.attributes){if(a.name.startsWith('data-')||a.name==='role'||a.name==='id')d[a.name]=a.value.slice(0,40)}chain.push(e.tagName+' '+JSON.stringify(d)+' cls='+(e.className||'').toString().slice(0,60)+' len='+(e.innerText||'').length)}
out.copyChain=chain;
const ids={};document.querySelectorAll('[data-testid]').forEach(x=>{const k=x.getAttribute('data-testid').replace(/[0-9]+/g,'#');ids[k]=(ids[k]||0)+1});out.testids=ids;
const attrs={};document.querySelectorAll('*').forEach(x=>{for(const a of x.attributes){if(a.name.startsWith('data-')&&/(message|turn|author|role|conversation)/.test(a.name)){attrs[a.name+'='+a.value.slice(0,25)]=(attrs[a.name+'='+a.value.slice(0,25)]||0)+1}}});out.attrs=attrs;
out.md=document.querySelectorAll('.markdown').length;out.prose=document.querySelectorAll('.prose').length;out.article=document.querySelectorAll('article').length;
const m=document.querySelector('.markdown');out.mdText=m?m.innerText.slice(0,300):null;
const c=document.querySelector('#prompt-textarea')||document.querySelector('textarea')||document.querySelector('[contenteditable="true"]');
out.composer=c?(c.tagName+'#'+c.id+' '+(c.innerText||c.value||'').length):null;
out.stop=[...document.querySelectorAll('button')].filter(b=>/stopp|stop/i.test(b.getAttribute('aria-label')||'')).map(b=>b.getAttribute('aria-label')+'|'+b.getAttribute('data-testid'));
out.body=document.body.innerText.slice(0,700);
return out})()"""

with sync_playwright() as p:
    br = p.chromium.launch()
    ctx = br.new_context(**p.devices["Pixel 7"], locale="nb-NO")
    page = ctx.new_page()
    page.goto(url, wait_until="domcontentloaded", timeout=60000)
    for t in (4, 12, 30):
        time.sleep(t - (0 if t == 4 else (4 if t == 12 else 12)))
        try: note(f"at {t}s", page.evaluate(MAP))
        except Exception as e: note(f"at {t}s error", e)
    page.screenshot(path="answer.png", full_page=True)
    br.close()
