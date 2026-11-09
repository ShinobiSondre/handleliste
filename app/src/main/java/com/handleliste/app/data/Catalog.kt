package com.handleliste.app.data

import com.handleliste.app.util.Http
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Norway's grocery range with pictures and normal prices, free and without a key:
 *  - MENY (NorgesGruppen: also the brands sold at KIWI, SPAR and Joker, like Jacobs Utvalgte and First Price)
 *  - Oda (other brands, incl. many sold at Coop and REMA)
 */
object Catalog {
    private val cache = HashMap<String, List<Product>>()

    suspend fun search(query: String): List<Product> {
        val q = query.trim().lowercase()
        if (q.length < 2) return emptyList()
        cache[q]?.let { return it }
        val (meny, oda) = coroutineScope {
            val m = async { runCatching { Parse.menyProducts(Http.get(
                "https://platform-rest-prod.ngdata.no/api/episearch/1300/products?search=${Http.enc(q)}&page_size=30")) } }
            val o = async { runCatching { Parse.odaProducts(Http.get("https://oda.com/api/v1/search/?q=${Http.enc(q)}")) } }
            m.await() to o.await()
        }
        if (meny.isFailure && oda.isFailure) throw (meny.exceptionOrNull() ?: Exception("offline"))
        // Alternate between the two so both ranges show near the top; drop duplicates.
        val a = meny.getOrDefault(emptyList()); val b = oda.getOrDefault(emptyList())
        val merged = ArrayList<Product>()
        for (i in 0 until maxOf(a.size, b.size)) { a.getOrNull(i)?.let { merged += it }; b.getOrNull(i)?.let { merged += it } }
        val res = merged.distinctBy { Parse.keywords(it.name).sorted().joinToString(" ") + it.extra.lowercase() }
        cache[q] = res
        return res
    }
}
