package com.handleliste.app.data

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Pure parsing and matching code, with no Android in it, so the cloud build can unit-test it.

/** A weekly-flyer deal from eTilbudsavis / Tjek. */
data class Offer(
    val id: String,
    val heading: String,
    val description: String,
    val store: String,
    val price: Double,
    val prePrice: Double?,
    val currency: String,
    val runTill: String,
    val image: String?,
    val runFrom: String = "",
    val imageLarge: String? = null,
) {
    val savingsPct: Int? get() = prePrice?.takeIf { it > price }?.let { (((it - price) / it) * 100).toInt() }
}

/** A grocery shop from OpenStreetMap. */
data class Shop(
    val id: String,
    val name: String,
    val brand: String,
    val kind: String,
    val lat: Double,
    val lng: Double,
    /** Buildings (ways) are measured from their middle, so they get a bit more room. */
    val isArea: Boolean,
)

/**
 * What you specifically want for a list item: a product (with picture) or just a type ("pesto rosso").
 * [keywords] decide which deals count for it.
 */
data class Pin(
    val name: String,
    val detail: String,
    val image: String?,
    val source: String, // "oda", "deal" or "type"
    val keywords: List<String>,
)

/** A product from Oda's range (the picker's "all products"). */
data class Product(val name: String, val brand: String, val extra: String, val price: Double?, val image: String?, val imageLarge: String?, val source: String = "Oda")

/** Today's shelf price for a product at one chain (Kassalapp). */
data class ChainPrice(val chain: String, val product: String, val price: Double, val image: String?)

data class Recipe(
    val title: String,
    val minutes: Int,
    val servings: Int,
    val uses: List<String>,
    val missing: List<String>,
    val ingredients: List<String>,
    val steps: List<String>,
    val why: String,
)

object Parse {

    fun tjekOffers(json: String): List<Offer> {
        val arr = JSONArray(json)
        val out = ArrayList<Offer>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val pricing = o.optJSONObject("pricing") ?: continue
            val price = pricing.optDouble("price", Double.NaN)
            if (price.isNaN() || price <= 0) continue
            val pre = pricing.optDouble("pre_price", Double.NaN).takeIf { !it.isNaN() && it > price }
            val store = o.optJSONObject("branding")?.optString("name").orEmpty()
                .ifBlank { o.optJSONObject("dealer")?.optString("name").orEmpty() }
            val images = o.optJSONObject("images")
            out += Offer(
                id = o.optString("id"),
                heading = o.optString("heading").trim(),
                description = o.optString("description").takeIf { it != "null" }.orEmpty().trim(),
                store = store.trim(),
                price = price,
                prePrice = pre,
                currency = pricing.optString("currency", "NOK").ifBlank { "NOK" },
                runTill = o.optString("run_till"),
                runFrom = o.optString("run_from"),
                imageLarge = images?.optString("zoom")?.takeIf { it.startsWith("http") }
                    ?: images?.optString("view")?.takeIf { it.startsWith("http") },
                image = images?.optString("thumb")?.takeIf { it.startsWith("http") }
                    ?: images?.optString("view")?.takeIf { it.startsWith("http") },
            )
        }
        return out
    }

    fun overpassShops(json: String): List<Shop> {
        val els = JSONObject(json).optJSONArray("elements") ?: return emptyList()
        val out = ArrayList<Shop>()
        for (i in 0 until els.length()) {
            val e = els.optJSONObject(i) ?: continue
            val type = e.optString("type")
            val center = e.optJSONObject("center")
            val lat = if (center != null) center.optDouble("lat") else e.optDouble("lat", Double.NaN)
            val lng = if (center != null) center.optDouble("lon") else e.optDouble("lon", Double.NaN)
            if (lat.isNaN() || lng.isNaN()) continue
            val tags = e.optJSONObject("tags") ?: JSONObject()
            val brand = tags.optString("brand")
            val name = tags.optString("name").ifBlank { brand }.ifBlank { tags.optString("shop") }
            out += Shop(
                id = "$type/${e.optLong("id")}",
                name = name,
                brand = brand.ifBlank { name },
                kind = tags.optString("shop"),
                lat = lat, lng = lng,
                isArea = type != "node",
            )
        }
        return out
    }

    /** Kassalapp /products search → cheapest matching product per chain. */
    fun kassalCheapestPerChain(json: String): List<ChainPrice> {
        val data = JSONObject(json).optJSONArray("data") ?: return emptyList()
        val best = LinkedHashMap<String, ChainPrice>()
        for (i in 0 until data.length()) {
            val p = data.optJSONObject(i) ?: continue
            val price = when (val cp = p.opt("current_price")) {
                is Number -> cp.toDouble()
                is JSONObject -> cp.optDouble("price", Double.NaN)
                else -> Double.NaN
            }
            if (price.isNaN() || price <= 0) continue
            val chain = p.optJSONObject("store")?.optString("name").orEmpty()
            if (chain.isBlank()) continue
            val cur = best[chain]
            if (cur == null || price < cur.price) {
                best[chain] = ChainPrice(chain, p.optString("name"), price, p.optString("image").takeIf { it.startsWith("http") })
            }
        }
        return best.values.sortedBy { it.price }
    }

    /** The AI answer: a JSON array of recipes (or an object holding one). */
    fun recipes(text: String): List<Recipe> {
        val t = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val arr = if (t.startsWith("[")) JSONArray(t) else {
            val o = JSONObject(t)
            o.optJSONArray("recipes") ?: o.optJSONArray("dinners") ?: JSONArray()
        }
        fun JSONObject.list(key: String): List<String> {
            val a = optJSONArray(key) ?: return emptyList()
            return (0 until a.length()).map { a.optString(it).trim() }.filter { it.isNotBlank() }
        }
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            Recipe(
                title = o.optString("title").ifBlank { return@mapNotNull null },
                minutes = o.optInt("minutes", 30),
                servings = o.optInt("servings", 2),
                uses = o.list("uses"),
                missing = o.list("missing"),
                ingredients = o.list("ingredients"),
                steps = o.list("steps"),
                why = o.optString("why"),
            )
        }
    }

    /** Text the Gemini API returns inside its response wrapper. */
    fun geminiText(json: String): String {
        val c = JSONObject(json).optJSONArray("candidates")?.optJSONObject(0) ?: return ""
        val parts = c.optJSONObject("content")?.optJSONArray("parts") ?: return ""
        return (0 until parts.length()).joinToString("") { parts.optJSONObject(it)?.optString("text").orEmpty() }
    }

    // ---- matching ----

    /** "2 l Melk!" → "melk". Drops amounts and units so the search hits the product. */
    fun searchTerm(item: String): String {
        val units = setOf("stk", "pk", "pakke", "pakker", "l", "dl", "cl", "ml", "g", "kg", "hg", "x", "boks", "bokser", "flaske", "flasker", "pcs", "pack")
        return item.lowercase()
            .replace(Regex("[^\\p{L}\\p{N} ]"), " ")
            .split(" ").filter { it.isNotBlank() && it.toDoubleOrNull() == null && it !in units && !it.matches(Regex("\\d+\\p{L}{0,2}")) }
            .joinToString(" ").trim()
            .ifBlank { item.trim().lowercase() }
    }

    /** Flyer catalogs near you → (catalog id, chain) for grocery chains that have offers. */
    fun groceryCatalogs(json: String): List<Pair<String, String>> {
        val arr = JSONArray(json)
        return (0 until arr.length()).mapNotNull { i ->
            val c = arr.optJSONObject(i) ?: return@mapNotNull null
            val chain = c.optJSONObject("branding")?.optString("name").orEmpty()
            if (c.optInt("offer_count", 0) <= 0 || !isGrocery(chain)) null else c.optString("id") to chain
        }.distinctBy { it.first }
    }

    private val GROCERY = setOf(
        "kiwi", "rema1000", "coopextra", "coopprix", "coopmega", "coopobs", "coopmarked", "coop", "meny", "spar",
        "joker", "bunnpris", "holdbart", "europris", "oda", "naerbutikken", "nærbutikken", "matkroken", "matkrogen",
        "lidl", "ccmat", "afoodmarket", "alltimat", "dagsmarked", "7eleven",
    )

    fun isGrocery(chain: String): Boolean {
        val k = chainKey(chain)
        return k in GROCERY
    }

    /**
     * The deals for one list item among all this week's offers, best match first.
     * Norwegian packs words together, so "melk" should find "Lettmelk", but a whole-word
     * match in the product name ranks above a match only in the small print.
     */
    fun matchOffers(term: String, offers: List<Offer>, nowIso: String = "", limit: Int = 10, anyWord: Boolean = false): List<Offer> {
        val words = term.lowercase().split(" ").filter { it.length >= 2 }
        if (words.isEmpty()) return emptyList()
        val whole = words.map { Regex("(^|[^\\p{L}])" + Regex.escape(it) + "($|[^\\p{L}])") }
        val split = Regex("[^\\p{L}]+")
        fun score(o: Offer): Int {
            val head = o.heading.lowercase()
            val desc = o.description.lowercase()
            val headWords = head.split(split)
            var total = 0
            for ((i, w) in words.withIndex()) {
                total += when {
                    whole[i].containsMatchIn(head) -> 6
                    headWords.any { endsAs(it, w) } -> 4 // lettMELK, gårdsEGG (not pålEGG)
                    containsReal(head, w) -> 2 // sjokoMELKdrikk
                    containsReal(desc, w) -> 1
                    anyWord -> 0
                    else -> return 0 // every word must be there
                }
            }
            return total
        }
        // Ready meals and combos ("lapskaus og spaghetti a la capri") rank below the plain product.
        fun combo(o: Offer): Int {
            val hw = o.heading.lowercase().split(split).filter { it.isNotBlank() }
            return (if (hw.any { it in setOf("og", "med", "and", "with") } || o.heading.contains("/")) 3 else 0) + (hw.size - 3).coerceAtLeast(0)
        }
        return offers.asSequence()
            .filter { nowIso.isEmpty() || it.runTill.isEmpty() || it.runTill >= nowIso }
            .map { o -> score(o).let { sc -> o to if (sc > 0) maxOf(1, sc - combo(o)) else 0 } }.filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<Offer, Int>> { it.second }.thenBy { it.first.price })
            .map { it.first }.distinctBy { it.store + "|" + it.heading.lowercase() + "|" + it.price }
            .take(limit).toList()
    }

    fun odaProducts(json: String): List<Product> {
        val arr = JSONObject(json).optJSONArray("products") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val p = arr.optJSONObject(i) ?: return@mapNotNull null
            val img = p.optJSONArray("images")?.optJSONObject(0)
            Product(
                name = p.optString("full_name").ifBlank { p.optString("name") }.trim(),
                brand = p.optString("brand").takeIf { it != "null" }.orEmpty(),
                extra = p.optString("name_extra").takeIf { it != "null" }.orEmpty(),
                price = p.optString("gross_price").toDoubleOrNull(),
                image = img?.optJSONObject("thumbnail")?.optString("url")?.takeIf { it.startsWith("http") },
                imageLarge = img?.optJSONObject("large")?.optString("url")?.takeIf { it.startsWith("http") },
            )
        }.filter { it.name.isNotBlank() }
    }

    /**
     * MENY's range (NorgesGruppen, so also Jacobs Utvalgte, First Price, Eldorado, Prior …).
     * Title "Pesto", description "Alla Genovese 150g Jacobs".
     */
    fun menyProducts(json: String): List<Product> {
        val hits = JSONObject(json).optJSONArray("hits") ?: return emptyList()
        val size = Regex("(?i)(\\d+\\s?x\\s?)?\\d+([,.]\\d+)?\\s?(g|kg|l|dl|cl|ml|stk|pk)\\b")
        return (0 until hits.length()).mapNotNull { i ->
            val h = hits.optJSONObject(i) ?: return@mapNotNull null
            val c = h.optJSONObject("contentData") ?: JSONObject()
            if (!c.optBoolean("isForSale", true)) return@mapNotNull null
            val desc = h.optString("description")
            val pack = size.find(desc)?.value.orEmpty()
            val name = (h.optString("title") + " " + desc.replace(size, " ")).replace(Regex("\\s+"), " ").trim()
            val img = c.optString("imagePath").ifBlank { h.optString("imageId") }.takeIf { it.isNotBlank() && it != "null" }
            Product(
                name = name, brand = c.optString("brand"), extra = pack,
                price = if (c.optBoolean("productSoldByWeight")) null else c.optDouble("pricePerUnit", Double.NaN).takeIf { !it.isNaN() && it > 0 },
                image = img?.let { "https://bilder.ngdata.no/$it/small.jpg" },
                imageLarge = img?.let { "https://bilder.ngdata.no/$it/large.jpg" },
                source = "MENY",
            )
        }.filter { it.name.isNotBlank() }
    }

    /** The first photo in a Wikimedia Commons image search (free pictures). */
    fun commonsPicture(json: String): String? {
        val pages = JSONObject(json).optJSONObject("query")?.optJSONObject("pages") ?: return null
        val list = pages.keys().asSequence().mapNotNull { pages.optJSONObject(it) }
            .sortedBy { it.optInt("index", 999) }.toList()
        for (p in list) {
            val info = p.optJSONArray("imageinfo")?.optJSONObject(0) ?: continue
            val mime = info.optString("mime")
            if (mime.isNotEmpty() && !mime.startsWith("image/jpeg") && !mime.startsWith("image/png") && !mime.startsWith("image/webp")) continue
            val u = info.optString("thumburl").ifBlank { info.optString("url") }
            if (u.startsWith("http")) return u
        }
        return null
    }

    /** The first recipe photo in an Oda recipe search. */
    fun odaRecipePicture(json: String): String? {
        val items = JSONObject(json).optJSONArray("items") ?: return null
        for (i in 0 until items.length()) {
            val it = items.optJSONObject(i) ?: continue
            if (it.optString("type") != "recipe") continue
            val a = it.optJSONObject("attributes") ?: continue
            listOf("feature_image_url", "image_url", "image").forEach { k ->
                // Oda's picture links are signed: changing any part (like the width) makes them fail, so use as-is.
                a.optString(k).takeIf { u -> u.startsWith("http") }?.let { u -> return u }
            }
            a.optJSONArray("images")?.optJSONObject(0)?.let { img ->
                (img.optJSONObject("large")?.optString("url") ?: img.optString("url")).takeIf { u -> u.startsWith("http") }?.let { return it }
            }
        }
        return null
    }

    /**
     * Orders products for a search the way a person would: every searched word must be there, a word
     * that *is* the product ("Egg", "Egg Frittgående") beats one hidden inside another ("Truseinnlegg"),
     * and a brand you typed ("first price") has to match.
     */
    /** "pålegg", "innlegg", "tillegg" end in "egg" but have nothing to do with eggs. */
    private fun fakeEnd(word: String, w: String) = w == "egg" && word.endsWith("legg")

    /** [word] ends in [w] as a real compound ("gårdsegg", "lettmelk"), not by accident ("pålegg"). */
    fun endsAs(word: String, w: String) = word.endsWith(w) && !fakeEnd(word, w)

    /** [text] contains [w] somewhere other than inside "-legg" words. */
    fun containsReal(text: String, w: String): Boolean =
        if (w == "egg") Regex("(?<!l)egg").containsMatchIn(text) else text.contains(w)

    fun rankProducts(query: String, products: List<Product>): List<Product> {
        val words = query.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.length >= 2 }
        if (words.isEmpty()) return products
        val split = Regex("[^\\p{L}\\p{N}]+")
        fun score(p: Product): Int {
            val title = p.name.lowercase()
            val all = (p.name + " " + p.brand + " " + p.extra).lowercase()
            val tw = title.split(split).filter { it.isNotBlank() }
            val aw = all.split(split).filter { it.isNotBlank() }
            var total = 0
            for (w in words) {
                total += when {
                    tw.firstOrNull() == w -> 10                       // "Egg …"
                    tw.any { it == w } -> 8
                    aw.any { it == w } -> 6                           // brand words: first, price
                    tw.any { it.startsWith(w) } -> 5                  // "eggerøre"
                    aw.any { it.startsWith(w) } -> 3
                    tw.firstOrNull()?.let { it.endsWith(w) && !fakeEnd(it, w) } == true -> 2   // "Gårdsegg"
                    aw.any { endsAs(it, w) } -> 1
                    aw.any { it.endsWith(w) } -> 0                    // "Truseinnlegg", "Duopålegg": only if nothing better
                    else -> return -1
                }
            }
            return total
        }
        val scored = products.map { it to score(it) }
        val hits = scored.filter { it.second >= 0 }
        // If nothing has every word, keep the store's own order rather than showing nothing.
        return if (hits.isEmpty()) products else hits.sortedByDescending { it.second }.map { it.first }
    }

    private val STOP = setOf("med", "og", "av", "til", "for", "smak", "uten", "the", "and", "with", "naturell", "pr", "per", "ca", "stk", "pakke", "pk")

    /** The words that identify a product: brand, kind and variant, without sizes and filler. */
    fun keywords(text: String): List<String> =
        text.lowercase().split(Regex("[^\\p{L}]+"))
            .filter { it.length >= 3 && it !in STOP && it !in setOf("stk", "pakke", "gram", "liter") }
            .distinct().take(6)

    fun pinFromProduct(p: Product) = Pin(p.name, p.extra, p.image, p.source.lowercase(), keywords(p.name))

    fun pinFromOffer(o: Offer): Pin {
        // Flyers put the brand first in the small print: "Solvinge, 400 g, 112,25 pr. kg".
        val brand = brandOf(o)
        val name = o.heading.lowercase().replaceFirstChar { it.uppercase() }
        return Pin(if (brand.isNotBlank()) "$brand $name" else name, o.store, o.image, "deal", keywords("$brand $name"))
    }

    /** "3 x 198 g, Eldorado, pr. kg 50,30" → "Eldorado": the first part of the small print that's a name, not an amount. */
    fun brandOf(o: Offer): String =
        o.description.lines().flatMap { it.split(",") }.map { it.trim() }
            .firstOrNull { it.length in 2..30 && it.none { c -> c.isDigit() } && it.any { c -> c.isLetter() } &&
                !it.contains(Regex("(?i)(tilbud|kupp|maks|pr\\.|per |gjelder|velg|eller)")) }.orEmpty()

    /**
     * The word that says what kind of thing you picked: for "Eldorado Maiskorn" and the list item "Mais"
     * it's "maiskorn", for "Ytterøy Kyllingfilet naturell" and "Kylling" it's "kyllingfilet".
     */
    fun coreWord(pin: Pin, term: String): String {
        val t = term.lowercase().split(" ").filter { it.length >= 3 }.maxByOrNull { it.length } ?: term.lowercase()
        return pin.keywords.firstOrNull { it.contains(t) } ?: pin.keywords.firstOrNull { t.contains(it) } ?: t
    }

    /**
     * Other deals of the same kind as what you picked. With "Maiskorn" picked: other corn ("COOP MAIS 3 PK"),
     * but not "MAISKAKE" or "MAISCHIPS", which only start with the same word.
     */
    fun sameKind(pin: Pin, term: String, o: Offer): Boolean {
        val core = coreWord(pin, term)
        val head = o.heading.lowercase()
        val words = head.split(Regex("[^\\p{L}]+")).filter { it.isNotBlank() }
        val t = term.lowercase().trim()
        return words.any { it == core || endsAs(it, core) } ||       // maiskorn, sukkermaiskorn
            words.any { it == t } ||                                   // "MAIS 3 PK"
            (core != t && words.any { endsAs(it, t) && !it.startsWith(t) })  // lettMELK for "melk"
    }

    fun pinFromType(text: String) = Pin(text.trim().replaceFirstChar { it.uppercase() }, "", null, "type", keywords(text))

    /** Does this deal fit what you picked? Most of the picked product's words must be on the deal. */
    fun pinMatches(pin: Pin, o: Offer): Boolean {
        val words = pin.keywords
        if (words.isEmpty()) return true
        val hay = (o.heading + " " + o.description).lowercase()
        val hits = words.count { hay.contains(it) }
        val need = if (pin.source == "type") words.size else maxOf(1, Math.ceil(words.size * 0.6).toInt())
        return hits >= need
    }

    /**
     * Is this deal simply the product itself, good enough to price an ingredient with?
     * "SPAGHETTI" or "BARILLA SPAGHETTI" yes; "LAPSKAUS OG SPAGHETTI A LA CAPRI" no.
     */
    fun plainProduct(term: String, o: Offer): Boolean {
        val words = term.lowercase().split(" ").filter { it.length >= 3 }
        if (words.isEmpty()) return false
        val hw = o.heading.lowercase().split(Regex("[^\\p{L}]+")).filter { it.isNotBlank() }
        if (hw.size > 4 || hw.any { it in setOf("og", "med", "and", "with") }) return false
        return words.all { w -> hw.any { it == w || endsAs(it, w) || (it.startsWith(w) && it.length <= w.length + 2) } }
    }

    /**
     * Deals for one item: (deals on what you want, other deals of the same kind). With a picked product
     * or type, only deals matching it count; without one, every deal on the item.
     */
    fun dealsFor(term: String, pin: Pin?, flyers: List<Offer>, nowIso: String = ""): Pair<List<Offer>, List<Offer>> {
        val all = matchOffers(term, flyers, nowIso, limit = 40) +
            (if (pin != null) matchOffers(pin.keywords.joinToString(" ").ifBlank { term }, flyers, nowIso, limit = 40, anyWord = true) else emptyList())
        val kind = all.distinctBy { it.id }
        val mine = if (pin == null) kind.take(10) else kind.filter { pinMatches(pin, it) }.take(10)
        val others = if (pin == null) emptyList() else kind.filter { it !in mine && sameKind(pin, term, it) }.take(10)
        return mine to others
    }

    /** Does a deal really belong to this list item? (Search results can be loose.) */
    fun offerMatches(term: String, offer: Offer): Boolean {
        val words = term.lowercase().split(" ").filter { it.length >= 3 }
        if (words.isEmpty()) return true
        val hay = (offer.heading + " " + offer.description).lowercase()
        return words.all { containsReal(hay, it) }
    }

    /** "REMA 1000", "Rema1000" and "rema" all become "rema1000"-ish keys that can be compared. */
    fun chainKey(name: String): String {
        val k = name.lowercase().replace(Regex("[^a-z0-9æøå]"), "")
        return when {
            // Hardware/electronics chains with grocery-sounding names: keep them apart.
            k.contains("bygg") || k.contains("elektro") || k.startsWith("sparkj") -> k
            k.startsWith("rema") -> "rema1000"
            k.startsWith("kiwi") -> "kiwi"
            k.startsWith("meny") -> "meny"
            k.startsWith("spar") || k.startsWith("eurospar") -> "spar"
            k.startsWith("joker") -> "joker"
            k.startsWith("bunnpris") -> "bunnpris"
            k.contains("extra") -> "coopextra"
            k.contains("obs") -> "coopobs"
            k.contains("prix") -> "coopprix"
            k.contains("mega") -> "coopmega"
            k.contains("marked") && k.contains("coop") -> "coopmarked"
            k.startsWith("coop") -> "coop"
            k.startsWith("narvesen") -> "narvesen"
            k.startsWith("7eleven") -> "7eleven"
            k.startsWith("oda") -> "oda"
            else -> k
        }
    }

    fun sameChain(a: String, b: String): Boolean {
        val x = chainKey(a); val y = chainKey(b)
        if (x.isBlank() || y.isBlank()) return false
        return x == y || (x.length >= 4 && y.startsWith(x)) || (y.length >= 4 && x.startsWith(y))
    }

    fun distanceM(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * r * atan2(sqrt(a), sqrt(1 - a))
    }

    /** The shop you are standing in, if any: close enough to it, and your position is precise enough. */
    fun shopAt(shops: List<Shop>, lat: Double, lng: Double, accuracyM: Float): Shop? {
        if (accuracyM > 65f) return null
        return shops
            .map { it to distanceM(lat, lng, it.lat, it.lng) }
            .filter { (s, d) -> d <= (if (s.isArea) 55.0 else 40.0) + (accuracyM / 3.0) }
            .minByOrNull { it.second }?.first
    }

    /** Splits "melk, brød og egg" into three items. */
    fun splitItems(input: String): List<String> =
        input.split(Regex(",|;|\\n| og | and ")).map { it.trim() }.filter { it.isNotBlank() }
            .map { it.replaceFirstChar { c -> c.uppercase() } }
}
