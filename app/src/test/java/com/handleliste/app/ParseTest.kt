package com.handleliste.app

import com.handleliste.app.data.Parse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParseTest {
    @Test fun tjekOffers() {
        val json = """[{"id":"a1","heading":"Tine Lettmelk 1 l","description":null,"branding":{"name":"KIWI"},
            "pricing":{"price":19.9,"pre_price":24.5,"currency":"NOK"},"run_till":"2026-11-15T22:59:59+0000",
            "images":{"thumb":"https://x/t.jpg","view":"https://x/v.jpg"}},
            {"id":"a2","heading":"Kaffe","branding":{"name":"REMA 1000"},"pricing":{"price":null}}]"""
        val o = Parse.tjekOffers(json)
        assertEquals(1, o.size)
        assertEquals("KIWI", o[0].store)
        assertEquals(19.9, o[0].price, 0.001)
        assertEquals(18, o[0].savingsPct)
        assertEquals("", o[0].description)
        assertTrue(Parse.offerMatches("melk", o[0]))
        assertTrue(!Parse.offerMatches("brød", o[0]))
    }

    @Test fun overpass() {
        val json = """{"elements":[{"type":"node","id":1,"lat":59.744,"lon":10.204,"tags":{"shop":"supermarket","name":"KIWI Bragernes","brand":"KIWI"}},
            {"type":"way","id":2,"center":{"lat":59.75,"lon":10.21},"tags":{"shop":"supermarket","brand":"REMA 1000"}}]}"""
        val s = Parse.overpassShops(json)
        assertEquals(2, s.size)
        assertEquals("KIWI Bragernes", s[0].name)
        assertEquals("REMA 1000", s[1].name)
        assertTrue(s[1].isArea)
        // Standing 20 m from the KIWI node with good GPS: in the store.
        assertEquals("node/1", Parse.shopAt(s, 59.74418, 10.204, 10f)?.id)
        // 200 m away: not in a store.
        assertNull(Parse.shopAt(s, 59.7458, 10.204, 10f))
        // Bad accuracy: don't guess.
        assertNull(Parse.shopAt(s, 59.744, 10.204, 120f))
    }

    @Test fun kassal() {
        val json = """{"data":[{"name":"Tine Melk","current_price":24.9,"store":{"name":"Meny"}},
            {"name":"Q Melk","current_price":{"price":21.5},"store":{"name":"Kiwi"}},
            {"name":"Tine Melk","current_price":26.0,"store":{"name":"Meny"}}]}"""
        val p = Parse.kassalCheapestPerChain(json)
        assertEquals(listOf("Kiwi", "Meny"), p.map { it.chain })
        assertEquals(24.9, p[1].price, 0.001)
    }

    @Test fun recipesAndGemini() {
        val inner = """[{"title":"Taco","minutes":25,"servings":4,"uses":["kjøttdeig"],"missing":["lomper"],"ingredients":["400 g kjøttdeig"],"steps":["Stek"],"why":"Fredag!"}]"""
        val wrapper = org.json.JSONObject().put("candidates", org.json.JSONArray().put(org.json.JSONObject()
            .put("content", org.json.JSONObject().put("parts", org.json.JSONArray().put(org.json.JSONObject().put("text", inner)))))).toString()
        val r = Parse.recipes(Parse.geminiText(wrapper))
        assertEquals("Taco", r.single().title)
        assertEquals(listOf("lomper"), r[0].missing)
        assertEquals(1, Parse.recipes("```json\n{\"recipes\":$inner}\n```").size)
    }

    @Test fun terms() {
        assertEquals("melk", Parse.searchTerm("2 l Melk!"))
        assertEquals("kyllingfilet", Parse.searchTerm("Kyllingfilet 400g"))
        assertEquals(listOf("Melk", "Brød", "Egg"), Parse.splitItems("melk, brød og egg"))
        assertTrue(Parse.sameChain("REMA 1000", "Rema1000 Bragernes"))
        assertTrue(Parse.sameChain("KIWI", "Kiwi Strømsø"))
        assertTrue(Parse.sameChain("Coop Extra", "Extra Drammen"))
        assertTrue(!Parse.sameChain("KIWI", "Meny"))
        assertNotNull(Parse.chainKey("Spar"))
    }

    private fun offer(h: String, store: String = "KIWI", p: Double = 20.0, d: String = "") =
        com.handleliste.app.data.Offer(h, h, d, store, p, null, "NOK", "2026-11-15T22:59:59+0000", null)

    @Test fun matching() {
        val all = listOf(offer("SJOKOMELK", p = 69.0), offer("Tine Lettmelk 1 l", p = 22.0), offer("Melk", "REMA 1000", 19.0),
            offer("Kaffe", d = "med melk"), offer("Brød grovt"), offer("Gammel melk").copy(runTill = "2026-11-01T00:00:00+0000"))
        val m = Parse.matchOffers("melk", all, "2026-11-09T10:00:00")
        assertEquals(listOf("Melk", "Tine Lettmelk 1 l", "SJOKOMELK", "Kaffe"), m.map { it.heading })
        assertTrue(Parse.matchOffers("kyllingfilet", all).isEmpty())
    }

    @Test fun catalogs() {
        val json = """[{"id":"a","offer_count":115,"branding":{"name":"KIWI"}},{"id":"b","offer_count":88,"branding":{"name":"Spar Kjøp"}},
            {"id":"c","offer_count":59,"branding":{"name":"Obs! Bygg"}},{"id":"d","offer_count":169,"branding":{"name":"Obs"}},
            {"id":"e","offer_count":0,"branding":{"name":"REMA 1000"}},{"id":"f","offer_count":94,"branding":{"name":"Extra"}},
            {"id":"g","offer_count":18,"branding":{"name":"Coop Byggmix"}},{"id":"h","offer_count":135,"branding":{"name":"Bunnpris"}}]"""
        assertEquals(listOf("KIWI", "Obs", "Extra", "Bunnpris"), Parse.groceryCatalogs(json).map { it.second })
    }

    @Test fun suggestions() {
        val S = com.handleliste.app.data.Suggest
        val day = 24 * 3600 * 1000L
        val now = 100 * day
        fun h(name: String, vararg daysAgo: Int) = com.handleliste.app.data.Habit(name, emptyList(), 0, daysAgo.map { now - it * day }.sorted())
        val milk = h("Melk", 12, 8, 4)          // put on a list every 4 days: due now
        val coffee = h("Kaffe", 15, 1)          // every 14 days: not due yet
        val once = h("Safran", 3)               // only once: no habit, not suggested
        val old = h("Julebrus", 90, 88)         // stopped buying it
        val s = S.suggestions(listOf(milk, coffee, once, old), emptySet(), now)
        assertEquals(listOf("Melk", "Kaffe"), s.map { it.name })
        assertTrue(s[0].due); assertEquals(4, s[0].everyDays); assertTrue(!s[1].due)
        // Already on a list: not suggested.
        assertEquals(listOf("Kaffe"), S.suggestions(listOf(milk, coffee), setOf("melk"), now).map { it.name })
        // Added and ticked in the same trip, even several times that day: still just one occasion.
        var x: com.handleliste.app.data.Habit? = null
        x = S.added(x, "Ost", now - 2 * day); x = S.added(x, "Ost", now - 2 * day + 3600_000)
        x = S.bought(x, "Ost", now - 2 * day + 7200_000)
        assertEquals(1, x.addTimes.size)
        assertTrue(S.suggestions(listOf(x), emptySet(), now).isEmpty())
        // Old saved data that only has the old counter and ticks: not enough on its own.
        assertTrue(S.suggestions(listOf(com.handleliste.app.data.Habit("Brød", listOf(now - 9 * day, now - 2 * day), 5)), emptySet(), now).isEmpty())
        // Put on a list again another day: now it's a habit.
        x = S.added(x, "Ost", now - day)
        assertEquals(listOf("Ost"), S.suggestions(listOf(x), emptySet(), now).map { it.name })
        // Ticking twice in one trip counts once; un-ticking right away undoes it.
        val b = S.bought(milk, "Melk", now)
        assertEquals(1, b.buys.size)
        assertEquals(1, S.bought(b, "Melk", now + 3600_000).buys.size)
        assertEquals(0, S.unbought(b, now + 60_000).buys.size)
    }

    @Test fun dinnerBuyLine() {
        val a = "### 1. Taco\nLaget med **kjøttdeig**.\n\nHANDLE: lomper, rømme, mais.\n"
        assertEquals(listOf("lomper", "rømme", "mais"), com.handleliste.app.data.Dinner.missing(a))
        assertTrue(!com.handleliste.app.data.Dinner.display(a).contains("HANDLE"))
        assertTrue(com.handleliste.app.data.Dinner.missing("**HANDLE:** ingenting").isEmpty())
    }

    @Test fun pins() {
        val fillet = offer("KYLLINGFILET", d = "Ytterøy, 950 g, 164,32 pr. kg", p = 149.0)
        val wings = offer("KYLLINGVINGER/KLUBBER", "REMA 1000", 59.9, "Solvinge, 700-800 g")
        val stekt = offer("KYLLINGFILET STEKT & SKIVET", d = "Solvinge, 200 g", p = 44.9)
        val pesto = offer("BARILLA PESTO", "MENY", 29.9, "Rosso eller Genovese, 190 g")
        val pestoG = offer("PESTO", "KIWI", 24.9, "Jacobs Utvalgte, Genovese, 185 g")

        // Picked from Oda: Ytterøy fillet → only the Ytterøy deal counts, the others are "other chicken".
        val p = Parse.pinFromProduct(com.handleliste.app.data.Product("Ytterøy Kyllingfilet naturell Saktevoksende kylling", "Ytterøy", "950 g", 156.2, null, null))
        assertTrue(Parse.pinMatches(p, fillet))
        assertTrue(!Parse.pinMatches(p, wings))
        assertTrue(!Parse.pinMatches(p, stekt))

        // Picked from a deal: the brand comes from the small print.
        val fromDeal = Parse.pinFromOffer(stekt)
        assertEquals("Solvinge Kyllingfilet stekt & skivet", fromDeal.name)
        assertTrue(Parse.pinMatches(fromDeal, stekt))
        assertTrue(!Parse.pinMatches(fromDeal, fillet))

        // A type: every word must be there.
        val rosso = Parse.pinFromType("pesto rosso")
        assertTrue(Parse.pinMatches(rosso, pesto))
        assertTrue(!Parse.pinMatches(rosso, pestoG))

        val oda = """{"products":[{"full_name":"Barilla Pesto alla Genovese","brand":"Barilla","name_extra":"190 g","gross_price":"21.14",
            "images":[{"large":{"url":"https://img/l.jpg"},"thumbnail":{"url":"https://img/t.jpg"}}]}]}"""
        val prod = Parse.odaProducts(oda).single()
        assertEquals("https://img/t.jpg", prod.image)
        assertEquals(21.14, prod.price!!, 0.001)
        assertEquals(listOf("barilla", "pesto", "alla", "genovese"), Parse.pinFromProduct(prod).keywords)
    }

    @Test fun budgetPrompt() {
        val p = com.handleliste.app.data.Dinner.budgetPrompt(100, 2, listOf("Melk"), "", listOf("- kyllingkjøttdeig (Solvinge, 400 g): 39.90 kr hos REMA 1000"), "")
        assertTrue(p.contains("100"))
        assertTrue(p.contains("REMA 1000"))
        assertTrue(p.contains("BUY:") || p.contains("HANDLE:"))
    }

    @Test fun meny() {
        val json = """{"total":2,"hits":[{"title":"Pesto","description":"Alla Genovese 150g Jacobs","imageId":"7035620025204/meny",
            "contentData":{"pricePerUnit":49.9,"brand":"Jacobs utvalgte","imagePath":"7035620025204/meny","isForSale":true,"productSoldByWeight":false}},
            {"title":"Kyllingfilet","description":"1,5kg First Price","contentData":{"pricePerUnit":187,"imagePath":"x/meny","isForSale":true}}]}"""
        val p = Parse.menyProducts(json)
        assertEquals("Pesto Alla Genovese Jacobs", p[0].name)
        assertEquals("150g", p[0].extra)
        assertEquals("https://bilder.ngdata.no/7035620025204/meny/small.jpg", p[0].image)
        assertEquals("MENY", p[0].source)
        assertEquals("Kyllingfilet First Price", p[1].name)
        assertEquals("1,5kg", p[1].extra)
        // Jacobs pesto from MENY matches the KIWI flyer deal on Jacobs Genovese pesto.
        val pin = Parse.pinFromProduct(p[0])
        assertTrue(Parse.pinMatches(pin, offer("PESTO", "KIWI", 24.9, "Jacobs Utvalgte, Genovese, 185 g")))
        assertTrue(!Parse.pinMatches(pin, offer("BARILLA PESTO", "MENY", 29.9, "Rosso eller Genovese, 190 g")))
    }

    @Test fun sameKindAndBrand() {
        val korn = offer("MAISKORN", "KIWI", 24.9, "3 x 198 g, Eldorado, pr. kg 50,30")
        assertEquals("Eldorado", Parse.brandOf(korn))
        val pin = Parse.pinFromOffer(korn)
        assertEquals("Eldorado Maiskorn", pin.name)
        assertEquals("maiskorn", Parse.coreWord(pin, "mais"))
        assertTrue(Parse.sameKind(pin, "mais", offer("COOP MAIS 3 PK", "Obs", 50.0)))
        assertTrue(Parse.sameKind(pin, "mais", offer("SUKKERMAISKORN", "Extra", 19.9)))
        assertTrue(!Parse.sameKind(pin, "mais", offer("COOP MAISKAKE", "Obs", 44.0)))
        assertTrue(!Parse.sameKind(pin, "mais", offer("MAISCHIPS", "REMA 1000", 29.9)))
        // Kyllingfilet picked: other fillets yes, wings and minced chicken no.
        val fil = Parse.pinFromProduct(com.handleliste.app.data.Product("Ytterøy Kyllingfilet naturell", "Ytterøy", "950 g", 156.2, null, null))
        assertTrue(Parse.sameKind(fil, "kylling", offer("KYLLINGFILET", "REMA 1000", 109.0, "Solvinge, 700 g")))
        assertTrue(!Parse.sameKind(fil, "kylling", offer("KYLLINGVINGER/KLUBBER", "REMA 1000", 59.9)))
        assertTrue(!Parse.sameKind(fil, "kylling", offer("KYLLINGKJØTTDEIG", "REMA 1000", 44.9)))
    }

    @Test fun budgetUsesShoppingList() {
        val list = listOf("Pasta", "Kyllingfilet", "Melk", "Mais")
        val a = "### Kyllingpasta\nPasta 25 kr, kylling 99 kr …\n\nBRUKER: pasta, kyllingfilet\nHANDLE: fløte, løk"
        assertEquals(listOf("Pasta", "Kyllingfilet"), com.handleliste.app.data.Dinner.usedFromList(a, list))
        assertEquals(listOf("fløte", "løk"), com.handleliste.app.data.Dinner.missing(a))
        val shown = com.handleliste.app.data.Dinner.display(a)
        assertTrue(!shown.contains("BRUKER") && !shown.contains("HANDLE"))
        // No BRUKER line: the list items the answer mentions.
        assertEquals(listOf("Pasta", "Mais"), com.handleliste.app.data.Dinner.usedFromList("Pasta med mais og ost.\nHANDLE: ost", list))
        assertTrue(com.handleliste.app.data.Dinner.usedFromList("BRUKER: ingen\nHANDLE: taco", list).isEmpty())
    }

    @Test fun plainProducts() {
        val lapskaus = offer("TRONDHJEMS LYS/BRUN LAPSKAUS OG SPAGHETTI A LA CAPRI", "Bunnpris", 59.0)
        val spag = offer("BARILLA SPAGHETTI", "KIWI", 24.9)
        assertTrue(!Parse.plainProduct("spaghetti", lapskaus))
        assertTrue(Parse.plainProduct("spaghetti", spag))
        assertTrue(Parse.plainProduct("hakkede tomater", offer("TOMATER HAKKEDE", "Bunnpris", 8.9)))
        assertTrue(Parse.plainProduct("gulrøtter", offer("NORSKE GULRØTTER", "Extra", 12.9)))
        // The plain product ranks above the combo even when the combo is cheaper.
        assertEquals("BARILLA SPAGHETTI", Parse.matchOffers("spaghetti", listOf(lapskaus.copy(price = 9.0), spag)).first().heading)
    }

    @Test fun dishLines() {
        val a = """### Kyllingtaco
Rett 1: Kyllingtaco er raskt.
Fyll lomper med kylling.

### Pestopasta
Pasta og pesto.

RETT: Kyllingtaco | HANDLE: lomper, rømme | BRUKER: kylling, mais
**RETT:** Pestopasta | HANDLE: ingenting | BRUKER: pasta, pesto"""
        val d = com.handleliste.app.data.Dinner.dishes(a)
        assertEquals(listOf("Kyllingtaco", "Pestopasta"), d.map { it.name })
        assertEquals(listOf("lomper", "rømme"), d[0].buy)
        assertTrue(d[1].buy.isEmpty())
        assertEquals(listOf("Kylling", "Mais"), com.handleliste.app.data.Dinner.usesFromList(d[0], listOf("Kylling", "Mais", "Melk")))
        val shown = com.handleliste.app.data.Dinner.display(a)
        assertTrue(shown.contains("Rett 1: Kyllingtaco er raskt."))   // headings/text that mention "Rett" stay
        assertTrue(!shown.contains("HANDLE"))
        assertEquals("https://x/y.jpg", Parse.odaRecipePicture("""{"items":[{"type":"recipe","attributes":{"title":"Taco","feature_image_url":"https://x/y.jpg"}}]}"""))
    }

    @Test fun dishImagesAndCommons() {
        val d = com.handleliste.app.data.Dinner.dishes("RETT: Pestopasta | HANDLE: ingenting | BRUKER: pasta | BILDE: pesto pasta chicken")
        assertEquals("pesto pasta chicken", d.single().imageQuery)
        val json = """{"query":{"pages":{"-1":{"index":2,"imageinfo":[{"thumburl":"https://up/b.jpg","mime":"image/jpeg"}]},
            "-2":{"index":1,"imageinfo":[{"thumburl":"https://up/a.jpg","mime":"image/jpeg"}]}}}}"""
        assertEquals("https://up/a.jpg", Parse.commonsPicture(json))
    }
}
