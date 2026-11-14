package com.handleliste.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/** What your whole list costs at one chain: deals where there are any, the normal price for the rest. */
data class StoreTotal(val chain: String, val total: Double, val onSale: Int, val priced: Int, val items: Int)

/**
 * "Which store is cheapest for my whole list?" Each item counts at its cheapest deal in that chain
 * this week, or at its normal price (MENY/SPAR/Oda, or Kassalapp's shelf price for the chain) when
 * it isn't on sale there. Items without any price are left out of every store alike.
 */
object StoreTotals {
    var totals by mutableStateOf<List<StoreTotal>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    private val normalCache = HashMap<String, Double?>()

    private val GROCERS = setOf("kiwi", "rema1000", "meny", "spar", "joker", "bunnpris", "coopextra", "coopobs", "coopprix", "coopmega", "coopmarked", "coop")

    /** Pure calculation, so it can be tested. */
    fun compute(results: List<ItemDeals>, normal: Map<String, Double?>, chains: List<String>): List<StoreTotal> {
        val names = chains.filter { Parse.chainKey(it) in GROCERS }.distinctBy { Parse.chainKey(it) }
        val counted = results.filter { d -> normal[d.item] != null || d.offers.isNotEmpty() }
        return names.map { chain ->
            var total = 0.0; var sale = 0; var priced = 0
            counted.forEach { d ->
                val deal = d.offers.filter { Parse.sameChain(it.store, chain) }.minOfOrNull { it.price }
                val shelf = d.prices.firstOrNull { Parse.sameChain(it.chain, chain) }?.price
                val base = shelf ?: normal[d.item] ?: d.offers.maxOfOrNull { it.prePrice ?: it.price }
                val p = listOfNotNull(deal, base).minOrNull()
                if (p != null) { total += p; priced++ }
                if (deal != null && (base == null || deal <= base)) sale++
            }
            StoreTotal(chain, total, sale, priced, counted.size)
        }.filter { it.priced > 0 }.sortedBy { it.total }
    }

    suspend fun refresh(ctx: Context) {
        val res = Deals.results
        if (res.size < 2) { totals = emptyList(); return }
        loading = true
        try {
            val normal = withContext(Dispatchers.IO) {
                coroutineScope {
                    res.map { d ->
                        async {
                            val key = d.item.lowercase() + "|" + d.pin?.name.orEmpty()
                            if (normalCache.containsKey(key)) return@async d.item to normalCache[key]
                            val q = d.pin?.name?.let { Parse.keywords(it).take(4).joinToString(" ") } ?: d.term
                            val p = runCatching { Catalog.search(q) }.getOrDefault(emptyList())
                                .filter { it.price != null && (d.pin != null || Parse.productIs(d.term, it)) }
                                .take(3).minOfOrNull { it.price!! }
                            normalCache[key] = p
                            d.item to p
                        }
                    }.awaitAll().toMap()
                }
            }
            val chains = (Deals.chains + res.flatMap { d -> d.offers.map { it.store } }).distinct()
            totals = withContext(Dispatchers.Default) { compute(res, normal, chains) }
        } finally { loading = false }
    }
}
