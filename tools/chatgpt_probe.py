"""Runs the app's ChatGPT flow in headless Chromium (same engine as Android WebView) and reports what happens."""
import json, sys, time, urllib.parse
from playwright.sync_api import sync_playwright

src = open("app/src/main/java/com/handleliste/app/data/ChatGptBridge.kt", encoding="utf-8").read()

def kt_const(name):
    # Rebuild a Kotlin "a" + "b" string constant into JavaScript.
    start = src.index(f"private const val {name} =")
    body = src[start:src.index("\n\n", start)]
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

READ, SEND = kt_const("READ"), kt_const("SEND")
prompt = "Hva kan jeg lage til middag? Foreslå 3 middager. Handlelisten min: Pasta, Pesto, Kylling, Ost, Mais. Svar kort på norsk.\nAvslutt med én linje som starter med «HANDLE:»."
url = "https://chatgpt.com/?" + urllib.parse.urlencode({"q": prompt})

def note(title, msg): print(f"::notice title={title}::{str(msg).replace(chr(10),' ')[:1500]}")

with sync_playwright() as p:
    b = p.chromium.launch()
    for run in (1, 2):
        ctx = b.new_context(**p.devices["Pixel 7"], locale="nb-NO")
        page = ctx.new_page()
        page.goto(url, wait_until="domcontentloaded", timeout=60000)
        t0 = time.time(); nudged = False; last = None
        while time.time() - t0 < 60:
            time.sleep(1.5)
            try: o = json.loads(page.evaluate(READ))
            except Exception as e: note(f"run{run} read error", e); continue
            last = o
            if o["a"] and not o["busy"] and len(o["a"][0]) > 40:
                note(f"run{run} ANSWER after {time.time()-t0:.0f}s", o["a"][0][:600]); break
            if o["pending"] > 0 and time.time() - t0 > 6 and not nudged:
                nudged = True; note(f"run{run} pending after 6s, pressing send", page.evaluate(SEND))
        else:
            note(f"run{run} NO ANSWER", {k: last.get(k) for k in ("users", "pending", "input", "busy", "url", "title", "dialog", "body", "tail")} if last else None)
        page.screenshot(path=f"run{run}.png")
        note(f"run{run} cookies", [c["name"] for c in ctx.cookies()][:20])
        ctx.close()
    b.close()
