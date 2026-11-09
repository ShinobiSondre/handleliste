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
}
