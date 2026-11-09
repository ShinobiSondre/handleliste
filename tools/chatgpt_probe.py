"""How long can a ChatGPT ?q= question be and still be sent? (headless Chromium = Android WebView engine)"""
import json, time, urllib.parse
from playwright.sync_api import sync_playwright
def note(t, m): print(f"::notice title={t}::{str(m).replace(chr(10),' ')[:600]}")
STATE = """(()=>{const a=[...document.querySelectorAll('[data-message-role="assistant"],[data-message-author-role="assistant"]')].map(e=>e.innerText);
const c=document.querySelector('#prompt-textarea')||document.querySelector('textarea');
return {a:a, users:document.querySelectorAll('[data-message-role="user"],[data-message-author-role="user"]').length, pending:c?(c.innerText||c.value||'').trim().length:-1,
busy:!!document.querySelector('[data-message-streaming],[data-testid="stop-button"]'), url:location.pathname}})()"""
filler = "- kyllingfilet (Solvinge, 700 g): 109.00 kr hos REMA 1000\n"
with sync_playwright() as p:
    br = p.chromium.launch()
    for n in (1500, 3000, 4500, 6000, 8000):
        q = "Svar med ett ord: hva er hovedstaden i Norge?\nBakgrunn:\n" + (filler * (n // len(filler)))
        q = q[:n]
        url = "https://chatgpt.com/?" + urllib.parse.urlencode({"q": q})
        ctx = br.new_context(**p.devices["Pixel 7"], locale="nb-NO"); page = ctx.new_page()
        try:
            page.goto(url, wait_until="domcontentloaded", timeout=60000)
            st = None
            for _ in range(30):
                time.sleep(1)
                try: st = page.evaluate(STATE)
                except Exception: continue
                if st["a"] and not st["busy"]: break
            note(f"q={len(q)} chars, url={len(url)}", {"answered": bool(st and st['a']), "users": st and st["users"], "pending": st and st["pending"], "path": st and st["url"], "a": st and (st["a"][0][:80] if st["a"] else "")})
        except Exception as e: note(f"q={n} error", e)
        ctx.close()
    br.close()
