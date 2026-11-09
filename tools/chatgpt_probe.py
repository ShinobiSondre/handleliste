"""Runs the app's own READ script against real ChatGPT in headless Chromium (Android WebView's engine)."""
import json, time, urllib.parse
from playwright.sync_api import sync_playwright
src = open("app/src/main/java/com/handleliste/app/data/ChatGptBridge.kt", encoding="utf-8").read()
def kt_const(name):
    start = src.index(f"private const val {name} =")
    nxt = src.find("private const val", start + 10)
    body = src[start:min(src.index("\n\n", start), nxt if nxt > 0 else len(src))]
    body = "\n".join(l for l in body.split("\n") if not l.strip().startswith("//"))
    out, i = [], body.index("=") + 1
    while True:
        q = body.find('"', i)
        if q < 0: break
        j = q + 1; buf = ""
        while body[j] != '"':
            if body[j] == "\\": buf += body[j:j+2]; j += 2
            else: buf += body[j]; j += 1
        out.append(json.loads('"' + buf + '"')); i = j + 1
    return "".join(out)
READ = kt_const("READ")
prompt = "Hva kan jeg lage til middag? Foreslå 3 middager. Handlelisten min: Pasta, Pesto, Kylling, Ost, Mais. Svar kort på norsk.\nAvslutt med én linje som starter med «HANDLE:»."
url = "https://chatgpt.com/?" + urllib.parse.urlencode({"q": prompt})
def note(t, m): print(f"::notice title={t}::{str(m).replace(chr(10),' ')[:1500]}")
with sync_playwright() as p:
    br = p.chromium.launch()
    for run in (1, 2):
        ctx = br.new_context(**p.devices["Pixel 7"], locale="nb-NO")
        page = ctx.new_page(); page.goto(url, wait_until="domcontentloaded", timeout=60000)
        t0 = time.time(); last = ""; stable = time.time(); done = False
        while time.time() - t0 < 75:
            time.sleep(0.6)
            try: o = json.loads(page.evaluate(READ))
            except Exception as e: continue
            text = o["a"][0].strip() if o["a"] else ""
            if text != last: last = text; stable = time.time()
            if text and not o["busy"] and len(text) >= 20 and time.time() - stable > 2.5:
                note(f"run{run} APP WOULD SHOW ANSWER after {time.time()-t0:.0f}s (users={o['users']}, pending={o['pending']})", text[:700]); done = True; break
        if not done: note(f"run{run} NO ANSWER", {k: o.get(k) for k in ("users","pending","busy","url","dialog")})
        ctx.close()
    br.close()
