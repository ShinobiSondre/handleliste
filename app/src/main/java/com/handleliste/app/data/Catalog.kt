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
