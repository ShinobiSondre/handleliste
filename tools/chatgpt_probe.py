"""Replays the app's follow-up flow in one browser (headless Chromium = Android WebView engine)."""
import json, time, urllib.parse
from playwright.sync_api import sync_playwright
def note(t, m): print(f"::notice title={t}::{str(m).replace(chr(10),' ')[:900]}")
STATE = """(()=>{const a=[...document.querySelectorAll('[data-message-role="assistant"],[data-message-author-role="assistant"]')].map(e=>e.innerText);
const c=document.querySelector('#prompt-textarea')||document.querySelector('textarea');
const d=[...document.querySelectorAll('[role="dialog"],dialog[open]')].map(e=>(e.innerText||'').slice(0,120)).join(' | ');
return {a:a, users:document.querySelectorAll('[data-message-role="user"],[data-message-author-role="user"]').length, pending:c?(c.innerText||c.value||'').trim().length:-1,
busy:!!document.querySelector('[data-message-streaming],[data-testid="stop-button"]'), path:location.pathname, title:document.title, dialog:d, body:(document.body?document.body.innerText:'').slice(0,300)}})()"""
def ask(page, q, label):
    page.goto("https://chatgpt.com/?" + urllib.parse.urlencode({"q": q}), wait_until="domcontentloaded", timeout=60000)
    t0 = time.time(); st = None; last = ""; stable = time.time()
    while time.time() - t0 < 60:
        time.sleep(1)
        try: st = page.evaluate(STATE)
        except Exception: continue
        txt = st["a"][0] if st["a"] else ""
        if txt != last: last = txt; stable = time.time()
        if txt and not st["busy"] and time.time() - stable > 2.5:
            note(f"{label} ANSWERED in {time.time()-t0:.0f}s", txt[:300]); return txt
    note(f"{label} NO ANSWER", st); return ""
first = "Hva kan jeg lage til middag? Foreslå 3 middager. Handlelisten min: Pasta, Pesto, Kylling, Ost, Mais. Svar kort på norsk.\nAvslutt med én linje som starter med «HANDLE:»."
with sync_playwright() as p:
    br = p.chromium.launch()
    for mode in ("wipe cookies", "keep cookies"):
        ctx = br.new_context(**p.devices["Pixel 7"], locale="nb-NO"); page = ctx.new_page()
        a1 = ask(page, first, f"[{mode}] first")
        for k, follow in enumerate(("Gjør det vegetar", "Noe raskere")):
            fq = first + "\n\nFortsett samtalen under og svar på norsk.\n\nDitt svar: " + a1[:1500] + "\n\nNytt spørsmål: " + follow
            page.goto("about:blank")
            if mode == "wipe cookies": ctx.clear_cookies()
            a1 = ask(page, fq, f"[{mode}] follow-up {k+1}") or a1
        ctx.close()
    br.close()
