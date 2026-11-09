package com.handleliste.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.handleliste.app.util.Http
import com.handleliste.app.util.HttpException
import com.handleliste.app.util.tr
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Deals for one list item: flyer offers near you, plus (with a Kassalapp key) today's cheapest shelf price per chain. */
data class ItemDeals(val item: String, val term: String, val offers: List<Offer>, val prices: List<ChainPrice>)

/**
 * Finds discounts for what's on your list.
 *  - Flyer deals ("tilbud") from eTilbudsavis/Tjek, the service behind etilbudsavis.no and mattilbud.no. Free, no key.
 *  - Optional: Kassalapp (kassal.app) shelf prices per chain. Free key, entered in Settings.
 */
object Deals {
    var results by mutableStateOf<List<ItemDeals>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var fetchedAt by mutableLongStateOf(0L); private set
    var usedFallbackLocation by mutableStateOf(false); private set

    private val OFFER_HOSTS = listOf(
        "https://squid-api.tjek.com/v2/offers/search",
        "https://api.etilbudsavis.dk/v2/offers/search",
    )

    /** Oslo sentrum, used only when the phone's location isn't available. */
    const val FALLBACK_LAT = 59.9133
    const val FALLBACK_LNG = 10.7389

    suspend fun check(ctx: Context, items: List<String>, lat: Double?, lng: Double?) {
        if (loading) return
        if (items.isEmpty()) { results = emptyList(); error = null; return }
        loading = true
        error = null
        usedFallbackLocation = lat == null
        val la = lat ?: FALLBACK_LAT
        val ln = lng ?: FALLBACK_LNG
        val radius = Settings.radiusKm(ctx) * 1000
        val kassal = Settings.kassalKey(ctx)
        val gate = Semaphore(4)
        val failures = java.util.concurrent.atomic.AtomicInteger()
        try {
            results = coroutineScope {
                items.distinct().map { item ->
                    async {
                        val term = Parse.searchTerm(item)
                        val offers = gate.withPermit {
                            runCatching { offers(term, la, ln, radius) }
                                .onFailure { failures.incrementAndGet() }.getOrDefault(emptyList())
                        }
                        val prices = if (kassal.isNotBlank()) gate.withPermit {
                            runCatching { kassalPrices(term, kassal) }.getOrDefault(emptyList())
                        } else emptyList()
                        ItemDeals(item, term, offers, prices)
                    }
                }.awaitAll()
            }
            if (failures.get() == items.distinct().size) {
                error = tr("Couldn't reach the deals service. Check your internet and try again.",
                    "Fikk ikke kontakt med tilbudstjenesten. Sjekk internett og prøv igjen.")
            }
            fetchedAt = System.currentTimeMillis()
        } finally {
            loading = false
        }
    }

    private suspend fun offers(term: String, lat: Double, lng: Double, radius: Int): List<Offer> {
        val q = "?r_lat=$lat&r_lng=$lng&r_radius=$radius&r_locale=nb_NO&api_av=0.3.0" +
            "&query=${Http.enc(term)}&offset=0&limit=40"
        var last: Exception? = null
        for (host in OFFER_HOSTS) {
            try {
                val all = Parse.tjekOffers(Http.get(host + q))
                val matching = all.filter { Parse.offerMatches(term, it) }
                return matching.sortedBy { it.price }.distinctBy { it.store + it.heading + it.price }.take(12)
            } catch (e: Exception) { last = e }
        }
        throw last ?: Exception("no host")
    }

    private suspend fun kassalPrices(term: String, key: String): List<ChainPrice> {
        val url = "https://kassal.app/api/v1/products?search=${Http.enc(term)}&size=40"
        repeat(2) { attempt ->
            try {
                return Parse.kassalCheapestPerChain(Http.get(url, mapOf("Authorization" to "Bearer $key")))
            } catch (e: HttpException) {
                if (e.code == 429 && attempt == 0) delay(3000) else throw e
            }
        }
        return emptyList()
    }

    /** Deals at one chain for the items still on the list (for the in-store screen). */
    fun atStore(storeName: String): List<Pair<String, Offer>> =
        results.flatMap { d -> d.offers.filter { Parse.sameChain(it.store, storeName) }.map { d.item to it } }

    /** Which stores have the most of your list on sale right now. */
    fun bestStores(): List<Pair<String, Int>> =
        results.flatMap { d -> d.offers.map { it.store }.distinct().map { it to d.item } }
            .groupBy { it.first }.map { (s, l) -> s to l.size }
            .sortedByDescending { it.second }
}
