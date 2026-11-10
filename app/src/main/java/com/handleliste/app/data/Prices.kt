package com.handleliste.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** One line of a price check: what it costs right now and where that price comes from. */
data class PriceLine(val item: String, val price: Double?, val where: String, val product: String, val onSale: Boolean)

/**
 * Today's prices for food, for the budget dinner:
 *  - this week's flyer deals near you (eTilbudsavis), which are the cheapest when they exist
 *  - otherwise the normal price from MENY or Oda, as a guide to what it costs in a shop
 */
object Prices {
    /** Dinner ingredients that make sense to tell the AI about. */
    private val DINNER = listOf(
        "kjøttdeig", "kylling", "kyllingfilet", "kyllinglår", "laks", "torsk", "sei", "fisk", "fiskekaker", "fiskeboller", "reker",
        "svin", "kotelett", "koteletter", "karbonade", "pølse", "pølser", "bacon", "skinke", "biff", "entrecote", "lam", "kjøtt", "kalkun",
        "pasta", "spaghetti", "makaroni", "ris", "nudler", "potet", "poteter", "tortilla", "lefse", "lomper", "taco", "pizza",
        "egg", "ost", "rømme", "fløte", "matfløte", "melk", "smør", "løk", "tomat", "tomater", "hermetisk", "paprika", "brokkoli",
        "gulrot", "gulrøtter", "agurk", "salat", "mais", "bønner", "linser", "kikerter", "sopp", "squash", "kål", "frossen", "wok",
        "suppe", "saus", "pesto", "brød", "hvitløk", "grønnsaker",
    )

    private fun isDinnerFood(o: Offer): Boolean {
        val h = (o.heading + " " + o.description.substringBefore("\n")).lowercase()
        // Whole words or the start/end of a compound: "hakkede tomater", "kyllingfilet", but not "lakRISstang".
        val words = h.split(Regex("[^\\p{L}]+"))
        return words.any { w -> DINNER.any { w == it || w.startsWith(it) || w.endsWith(it) } } && !Regex("(?i)(sjampo|vaskemiddel|bleier|kattemat|hundemat|godteri|sjokolade|brus|øl)").containsMatchIn(h)
    }

    /** The cheapest dinner-type deals near you this week, for the AI to plan with. */
    fun dinnerDeals(ctx: Context, limit: Int = 45): List<Offer> =
        Flyers.cached(ctx).filter { isDinnerFood(it) && it.price < 250 }
            .sortedBy { it.price }.distinctBy { it.heading.lowercase() + it.store }.take(limit)

    fun dealLine(o: Offer): String {
        val pack = o.description.lines().firstOrNull { it.contains(",") }?.take(50).orEmpty()
        return "- ${o.heading.lowercase()}${if (pack.isNotBlank()) " ($pack)" else ""}: ${"%.2f".format(o.price)} kr hos ${o.store}"
    }

    /** What each grocery costs now: cheapest deal near you, else the normal store price (MENY or Oda). */
    suspend fun check(ctx: Context, items: List<String>, pins: Map<String, Pin> = emptyMap()): List<PriceLine> = withContext(Dispatchers.Default) {
        // Only deals from the store you chose on the list screen (if you chose one).
        val flyers = Flyers.cached(ctx).let { all -> Deals.store?.let { st -> all.filter { Parse.sameChain(it.store, st) } } ?: all }
        coroutineScope {
            items.map { item ->
                async {
                    // An item where you picked a product is priced as that product.
                    val pin = pins[item]
                    val term = Parse.searchTerm(item)
                    val deal = Parse.matchOffers(term, flyers, limit = 40)
                        .filter { (pin == null || Parse.pinMatches(pin, it)) && Parse.plainProduct(term, it) }.minByOrNull { it.price }
                    if (deal != null) PriceLine(item, deal.price, deal.store, deal.heading.lowercase(), true)
                    else {
                        // The best-matching real product (ranked by the search), not just the cheapest hit:
                        // cheapest among the top few that really are the thing.
                        val p = runCatching { Catalog.search(pin?.name?.let { Parse.keywords(it).take(4).joinToString(" ") } ?: term) }.getOrDefault(emptyList())
                            .filter { it.price != null && (pin != null || Parse.productIs(term, it)) }.take(3).minByOrNull { it.price!! }
                        if (p != null) PriceLine(item, p.price, p.source, p.name, false)
                        else PriceLine(item, null, "", "", false)
                    }
                }
            }.awaitAll()
        }
    }
}
