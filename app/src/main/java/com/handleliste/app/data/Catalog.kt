package com.handleliste.app.data

import com.handleliste.app.util.Http

/** Norway's grocery range with pictures, from Oda's public product search (free, no key). */
object Catalog {
    private val cache = HashMap<String, List<Product>>()

    suspend fun search(query: String): List<Product> {
        val q = query.trim().lowercase()
        if (q.length < 2) return emptyList()
        cache[q]?.let { return it }
        val res = Parse.odaProducts(Http.get("https://oda.com/api/v1/search/?q=${Http.enc(q)}"))
        cache[q] = res
        return res
    }
}
