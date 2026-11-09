"""Does ChatGPT end with the app's RETT lines? (headless Chromium = Android WebView engine)"""
import json, re, time, urllib.parse
from playwright.sync_api import sync_playwright
def note(t, m): print(f"::notice title={t}::{str(m).replace(chr(10),' ⏎ ')[:1600]}")
prompt = ("Hva kan jeg lage til middag? Foreslå 3 middager, mest mulig laget av det jeg skal handle.\n"
  "Handlelisten min: Pasta, Pesto, Kylling, Ost, Mais, Lomper\n"
  "Anta at jeg har salt, pepper, olje, smør og vanlige krydder. Svar på norsk, kort og oversiktlig: navn på retten, tid, hvorfor den passer, og en enkel oppskrift med ingredienser og fremgangsmåte for hver.\n"
  "Gi hver rett en kort overskrift med navnet. Avslutt svaret med én linje per rett, nøyaktig slik: «RETT: <navn> | HANDLE: <det jeg må kjøpe, korte varenavn slik de heter i norske butikker, adskilt med komma> | BRUKER: <varer fra handlelisten min som retten bruker>». Skriv «ingenting» der det er tomt.")
STATE = """(()=>{const a=[...document.querySelectorAll('[data-message-role="assistant"]')].map(e=>e.innerText);
return {a:a, busy:!!document.querySelector('[data-message-streaming]'), done:!!document.querySelector('[data-message-role="assistant"][data-message-complete]')}})()"""
with sync_playwright() as p:
    br = p.chromium.launch()
    for run in (1, 2):
        ctx = br.new_context(**p.devices["Pixel 7"], locale="nb-NO"); page = ctx.new_page()
        t0 = time.time()
        page.goto("https://chatgpt.com/?" + urllib.parse.urlencode({"q": prompt}), wait_until="domcontentloaded", timeout=60000)
        st = None
        while time.time() - t0 < 60:
            time.sleep(0.3)
            try: st = page.evaluate(STATE)
            except Exception: continue
            if st["a"] and st["done"] and not st["busy"]: break
        txt = "\n".join(st["a"]) if st else ""
        lines = [l for l in txt.splitlines() if re.match(r"^\W*(RETT|DISH)\s*\d*[*\s]*[:.]", l, re.I) and "|" in l]
        note(f"run{run}: done={st and st['done']} in {time.time()-t0:.1f}s, {len(lines)} RETT lines", lines)
        ctx.close()
    br.close()
