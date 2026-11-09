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

    /** Oslo sentrum, used only when the phone's location isn't available. */
    const val FALLBACK_LAT = 59.9133
    const val FALLBACK_LNG = 10.7389

    /** Chains whose flyers were searched in the last check. */
    var chains by mutableStateOf<List<String>>(emptyList()); private set

    suspend fun check(ctx: Context, items: List<String>, lat: Double?, lng: Double?, force: Boolean = false) {
        if (loading) return
        if (items.isEmpty()) { results = emptyList(); error = null; return }
        loading = true
        error = null
        usedFallbackLocation = lat == null
        val la = lat ?: FALLBACK_LAT
        val ln = lng ?: FALLBACK_LNG
        val radius = Settings.radiusKm(ctx) * 1000
        val kassal = Settings.kassalKey(ctx)
        try {
            val flyers = runCatching { Flyers.near(ctx, la, ln, radius, force) }
                .onFailure {
                    error = tr("Couldn't reach the deals service. Check your internet and try again.",
                        "Fikk ikke kontakt med tilbudstjenesten. Sjekk internett og prøv igjen.")
                }.getOrDefault(emptyList())
            chains = Flyers.chains
            val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.ROOT)
                .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date())
            val gate = Semaphore(3)
            results = coroutineScope {
                items.distinct().map { item ->
                    async {
                        val term = Parse.searchTerm(item)
                        val offers = Parse.matchOffers(term, flyers, now)
                        val prices = if (kassal.isNotBlank()) gate.withPermit {
                            runCatching { kassalPrices(term, kassal) }.getOrDefault(emptyList())
                        } else emptyList()
                        ItemDeals(item, term, offers, prices)
                    }
                }.awaitAll()
            }
            if (flyers.isNotEmpty() || kassal.isNotBlank()) fetchedAt = System.currentTimeMillis()
        } finally {
            loading = false
        }
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
