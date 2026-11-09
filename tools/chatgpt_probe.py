"""Finds which way of sending works on ChatGPT (headless Chromium = same engine as Android WebView)."""
import json, time, urllib.parse
from playwright.sync_api import sync_playwright

prompt = "Hva kan jeg lage til middag? Foreslå 3 middager. Handlelisten min: Pasta, Pesto, Kylling, Ost, Mais. Svar kort på norsk.\nAvslutt med én linje som starter med «HANDLE:»."
url = "https://chatgpt.com/?" + urllib.parse.urlencode({"q": prompt})
def note(t, m): print(f"::notice title={t}::{str(m).replace(chr(10),' ')[:1400]}")

STATE = """(()=>{const c=document.querySelector('#prompt-textarea')||document.querySelector('textarea');
const a=[...document.querySelectorAll('[data-message-author-role="assistant"]')].map(e=>e.innerText);
return {users:document.querySelectorAll('[data-message-author-role="user"]').length, a:a, pending:c?(c.innerText||c.value||'').trim().length:-1,
 busy:!!document.querySelector('[data-testid="stop-button"]'), url:location.href}})()"""
BUTTONS = """(()=>{const c=document.querySelector('#prompt-textarea');const f=c&&c.closest('form');
return [...(f||document).querySelectorAll('button')].map(b=>({t:b.getAttribute('data-testid'),id:b.id,a:b.getAttribute('aria-label'),type:b.type,dis:b.disabled||b.getAttribute('aria-disabled'),txt:(b.innerText||'').slice(0,20)}))})()"""

WAYS = {
  "none (wait for auto-send)": None,
  "click send-button testid": "(()=>{const b=document.querySelector('[data-testid=\"send-button\"]');if(b){b.click();return 'ok'}return 'missing'})()",
  "click #composer-submit-button": "(()=>{const b=document.querySelector('#composer-submit-button');if(b){b.click();return 'ok:'+b.getAttribute('aria-label')}return 'missing'})()",
  "enter key in composer": "(()=>{const e=document.querySelector('#prompt-textarea');e.focus();for(const t of ['keydown','keypress','keyup'])e.dispatchEvent(new KeyboardEvent(t,{key:'Enter',code:'Enter',keyCode:13,which:13,bubbles:true,cancelable:true}));return 'ok'})()",
  "input event then send button": "(()=>{const e=document.querySelector('#prompt-textarea');e.focus();e.dispatchEvent(new InputEvent('input',{bubbles:true}));const b=document.querySelector('[data-testid=\"send-button\"]')||document.querySelector('#composer-submit-button');if(b){b.click();return 'ok'}return 'missing'})()",
}

with sync_playwright() as p:
    br = p.chromium.launch()
    for name, js in WAYS.items():
        ctx = br.new_context(**p.devices["Pixel 7"], locale="nb-NO")
        page = ctx.new_page()
        try:
            page.goto(url, wait_until="domcontentloaded", timeout=60000)
            time.sleep(6)
            if name.startswith("click send"): note("buttons", page.evaluate(BUTTONS))
            st = page.evaluate(STATE)
            r = None
            if js:
                try: r = page.evaluate(js)
                except Exception as e: r = "navigated: " + str(e)[:80]
            res = None
            for _ in range(40):
                time.sleep(1.5)
                try: res = page.evaluate(STATE)
                except Exception: continue
                if res["a"] and not res["busy"] and len(res["a"][0]) > 40: break
            ok = bool(res and res["a"] and len(res["a"][0]) > 40)
            note(("WORKS " if ok else "fails ") + name, {"before": st, "result": r, "after_users": res and res["users"], "pending": res and res["pending"], "url": res and res["url"], "answer": res and (res["a"][0][:300] if res["a"] else "")})
        except Exception as e:
            note("error " + name, e)
        ctx.close()
    br.close()
