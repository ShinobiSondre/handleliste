package com.handleliste.app.data

import com.handleliste.app.util.Http
import org.json.JSONObject

/**
 * What product a barcode (EAN) is: first NorgesGruppen's range (MENY, then SPAR: name, picture
 * and price), then Open Food Facts (free, community-run, many Norwegian products).
 */
object Barcode {
    suspend fun lookup(ean: String): Product? {
        val code = ean.filter { it.isDigit() }
        if (code.length < 8) return null
        for ((chain, label) in listOf(1300 to "MENY", 1210 to "SPAR")) {
            val hit = runCatching {
                Parse.menyProducts(Http.get("https://platform-rest-prod.ngdata.no/api/episearch/$chain/products?search=$code&page_size=3"))
            }.getOrDefault(emptyList()).firstOrNull()
            if (hit != null) return if (label == "SPAR") hit.copy(source = "SPAR", image = hit.image?.replace("/meny/", "/kmh/"),
                imageLarge = hit.imageLarge?.replace("/meny/", "/kmh/")) else hit
        }
        return runCatching { offProduct(Http.get("https://world.openfoodfacts.org/api/v2/product/$code.json?fields=product_name,product_name_nb,brands,quantity,image_front_small_url,image_front_url")) }.getOrNull()
    }

    fun offProduct(json: String): Product? {
        val o = JSONObject(json)
        if (o.optInt("status") != 1) return null
        val p = o.optJSONObject("product") ?: return null
        val name = p.optString("product_name_nb").ifBlank { p.optString("product_name") }.trim()
        if (name.isBlank()) return null
        val brand = p.optString("brands").split(",").firstOrNull()?.trim().orEmpty()
        return Product(if (brand.isNotBlank() && !name.contains(brand, true)) "$brand $name" else name, brand, p.optString("quantity"),
            null, p.optString("image_front_small_url").takeIf { it.startsWith("http") }, p.optString("image_front_url").takeIf { it.startsWith("http") },
            "Open Food Facts")
    }

    /** A short list name for a product: "Yoghurt Skogsbær Tine" → "Yoghurt Skogsbær". */
    fun itemName(p: Product): String {
        val n = p.name.replace(Regex("(?i)\\b" + Regex.escape(p.brand) + "\\b"), "").replace(Regex("\\s+"), " ").trim()
        return n.ifBlank { p.name }.split(" ").take(4).joinToString(" ").replaceFirstChar { it.uppercase() }
    }
}
