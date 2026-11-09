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
        // MENY, SPAR (same NorgesGruppen range as KIWI and Joker: First Price, Jacobs …) and Oda, in parallel.
        fun ng(chain: Int, label: String) = "https://platform-rest-prod.ngdata.no/api/episearch/$chain/products?search=${Http.enc(q)}&page_size=40"
        val (meny, spar, oda) = coroutineScope {
            val m = async { runCatching { Parse.menyProducts(Http.get(ng(1300, "MENY"))) } }
            val sp = async { runCatching { Parse.menyProducts(Http.get(ng(1210, "SPAR"))).map { it.copy(source = "SPAR", image = it.image?.replace("/meny/", "/kmh/"), imageLarge = it.imageLarge?.replace("/meny/", "/kmh/")) } } }
            val o = async { runCatching { Parse.odaProducts(Http.get("https://oda.com/api/v1/search/?q=${Http.enc(q)}")) } }
            Triple(m.await(), sp.await(), o.await())
        }
        if (meny.isFailure && spar.isFailure && oda.isFailure) throw (meny.exceptionOrNull() ?: Exception("offline"))
        val all = meny.getOrDefault(emptyList()) + spar.getOrDefault(emptyList()) + oda.getOrDefault(emptyList())
        // Ranked here, so "first price egg" puts First Price eggs first, not "Truseinnlegg" or "Duopålegg".
        val res = Parse.rankProducts(q, all)
            .distinctBy { Parse.keywords(it.name).sorted().joinToString(" ") + it.extra.lowercase().replace(" ", "") }
        cache[q] = res
        return res
    }

    private val recipePics = HashMap<String, String?>()

    /**
     * A photo for a dish name, from Oda's recipes (an illustration, not the exact dish). Long names
     * rarely match, so it tries shorter versions: "Kremet pesto-pasta med kylling" → "pesto pasta kylling" → "pesto pasta" …
     */
    suspend fun dishPicture(name: String, imageQuery: String = ""): String? {
        val key = name.lowercase().trim()
        if (recipePics.containsKey(key)) return recipePics[key]
        val words = Parse.keywords(name).filter { it !in setOf("kremet", "enkel", "rask", "hjemmelaget", "ovnsbakt", "stekt", "god", "deilig", "klassisk") }
        val tries = listOf(name) + (words.size downTo 1).map { n -> words.take(n).joinToString(" ") } + words.reversed().take(2)
        var found: String? = null
        for (q in tries.distinct().filter { it.isNotBlank() }) {
            found = runCatching { Parse.odaRecipePicture(Http.get("https://oda.com/api/v1/search/mixed/?q=${Http.enc(q)}&type=recipe")) }.getOrNull()
            if (found != null) break
        }
        // Nothing at Oda: a free photo from Wikimedia Commons, searched with ChatGPT's English words for the dish.
        if (found == null) {
            val q = imageQuery.ifBlank { words.take(3).joinToString(" ") }
            val shorter = q.split(" ").filter { it.isNotBlank() }
            for (attempt in listOf(q, shorter.take(2).joinToString(" "), shorter.lastOrNull().orEmpty() + " dish").distinct().filter { it.isNotBlank() }) {
                found = runCatching {
                    Parse.commonsPicture(Http.get("https://commons.wikimedia.org/w/api.php?action=query&format=json&generator=search" +
                        "&gsrnamespace=6&gsrlimit=6&gsrsearch=${Http.enc("filetype:bitmap $attempt food")}&prop=imageinfo&iiprop=url|mime&iiurlwidth=600"))
                }.getOrNull()
                if (found != null) break
            }
        }
        recipePics[key] = found
        return found
    }
}
