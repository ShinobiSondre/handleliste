"""Times the app's short dinner question and the recipe question on real ChatGPT (headless Chromium)."""
import json, re, time, urllib.parse
from playwright.sync_api import sync_playwright
def note(t, m): print(f"::notice title={t}::{str(m).replace(chr(10),' ⏎ ')[:900]}")
short = ("Hva kan jeg lage til middag? Foreslå 3 middager, mest mulig laget av det jeg skal handle.\n"
  "Handlelisten min: Kjøttdeig, Taco chips, Mais, Ost, Rømme, Pasta, Pesto\n"
  "Anta at jeg har salt, pepper, olje, smør og vanlige krydder. Svar på norsk og KORT: for hver rett bare navnet som overskrift, tid, og én setning om hvorfor den passer. Ikke skriv oppskriften ennå, den ber jeg om etterpå.\n"
  "Gi hver rett en kort overskrift med navnet. Avslutt svaret med én linje per rett, nøyaktig slik: «RETT: <navn> | HANDLE: <det jeg må kjøpe> | BRUKER: <varer fra handlelisten min som retten bruker> | ALT: <alle ingrediensene retten trenger, unntatt salt, pepper, olje og krydder> | BILDE: <2–4 engelske ord>». Skriv «ingenting» der det er tomt.")
recipe = short + "\n\nNytt spørsmål: Gi meg oppskriften på «Tacogryte»: ingredienser med mengder, og fremgangsmåte i korte, nummererte steg. Denne gangen svarer du BARE med oppskriften, uten RETT-linjer til slutt."
STATE = """(()=>{const a=[...document.querySelectorAll('[data-message-role="assistant"]')].map(e=>e.innerText);
return {a:a, first:a.length&&a[0].length>0, done:!!document.querySelector('[data-message-role="assistant"][data-message-complete]')}})()"""
with sync_playwright() as p:
    br = p.chromium.launch()
    ctx = br.new_context(**p.devices["Pixel 7"], locale="nb-NO"); page = ctx.new_page()
    page.goto("https://chatgpt.com/", wait_until="domcontentloaded", timeout=60000); time.sleep(3)  # like the app's warm-up
    for label, q in (("short", short), ("short again", short), ("recipe", recipe)):
        t0 = time.time(); started = None
        page.goto("https://chatgpt.com/?" + urllib.parse.urlencode({"q": q}), wait_until="domcontentloaded", timeout=60000)
        st = None
        while time.time() - t0 < 60:
            time.sleep(0.2)
            try: st = page.evaluate(STATE)
            except Exception: continue
            if st["first"] and started is None: started = time.time() - t0
            if st["done"]: break
        txt = "\n".join(st["a"]) if st else ""
        dishes = len([l for l in txt.splitlines() if re.match(r"^\W*RETT", l) and "|" in l])
        note(f"{label}: first words {started and round(started,1)} s, complete {round(time.time()-t0,1)} s, {len(txt)} chars, {dishes} RETT lines", txt[:300])
        ctx.clear_cookies()
    br.close()
