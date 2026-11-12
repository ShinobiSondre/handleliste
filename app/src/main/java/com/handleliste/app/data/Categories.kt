package com.handleliste.app.data

/**
 * Deal categories you can browse and subscribe to ("Pizza"), then narrow down to the exact
 * products you care about (Grandiosa Original, Big One Pepperoni …).
 * [queries] find the category in flyers and catalogs (brands too, since "GRANDIOSA ORIGINAL"
 * often has no "pizza" in it); [exclude] keeps out words that only start the same (pizzasaus, pizzabunn).
 */
data class Category(
    val id: String,
    val en: String,
    val no: String,
    val emoji: String,
    val queries: List<String>,
    val exclude: List<String> = emptyList(),
)

object Categories {
    val all = listOf(
        Category("pizza", "Pizza", "Pizza", "🍕",
            listOf("pizza", "grandiosa", "big one", "dolly dimple", "peppes", "pizzabakeren", "ristorante"),
            listOf("pizzasaus", "pizzabunn", "pizzadeig", "pizzamel", "pizzakrydder", "pizzaost", "pizzaskjærer", "pizzastein", "pizzaspade", "pizzagjær", "pizzatopping")),
        Category("kaffe", "Coffee", "Kaffe", "☕",
            listOf("kaffe", "kaffekapsler", "evergood", "friele", "ali kaffe", "nescafe"),
            listOf("kaffefilter", "kaffekopp", "kaffefløte", "kaffekake", "kaffemaskin", "kaffekrem")),
        Category("brus", "Soft drinks", "Brus", "🥤",
            listOf("brus", "cola", "pepsi", "solo", "fanta", "sprite", "urge", "farris"),
            emptyList()),
        Category("sjokolade", "Chocolate", "Sjokolade", "🍫",
            listOf("sjokolade", "freia", "kvikk lunsj", "smash", "nidar"),
            listOf("sjokolademelk", "sjokoladepålegg", "sjokoladesaus", "sjokoladekake", "sjokoladepudding")),
        Category("is", "Ice cream", "Is", "🍦",
            listOf("iskrem", "hennig olsen", "diplom-is", "kroneis", "magnum", "sorbet"),
            listOf("isbit", "issalat")),
        Category("kylling", "Chicken", "Kylling", "🍗",
            listOf("kylling", "kyllingfilet", "kyllinglår", "kyllingvinger"),
            listOf("kyllingpålegg", "kyllingbuljong", "kyllingkrydder", "kyllingsalat")),
        Category("laks", "Salmon", "Laks", "🐟",
            listOf("laks", "laksefilet", "røkelaks"),
            listOf("laksepålegg", "laksesalat")),
        Category("ost", "Cheese", "Ost", "🧀",
            listOf("ost", "norvegia", "jarlsberg", "synnøve", "brunost", "mozzarella"),
            listOf("ostekake", "ostepop", "osteskjærer", "ostesaus", "ostesmør", "ostechips")),
        Category("taco", "Taco", "Taco", "🌮",
            listOf("taco", "tortilla", "tacoskjell", "salsa"),
            emptyList()),
        Category("frokost", "Cereal", "Frokostblanding", "🥣",
            listOf("frokostblanding", "müsli", "granola", "cornflakes", "havregryn"),
            emptyList()),
    )

    fun byId(id: String?) = all.firstOrNull { it.id == id }

    private fun excluded(c: Category, heading: String): Boolean {
        val h = heading.lowercase()
        return h.split(Regex("[^\\p{L}]+")).any { w -> c.exclude.any { w.startsWith(it) } }
    }

    /** This week's deals in the category, cheapest first. */
    fun offers(c: Category, flyers: List<Offer>, nowIso: String = ""): List<Offer> =
        c.queries.flatMap { Parse.matchOffers(Parse.searchTerm(it), flyers, nowIso, limit = 60) }
            .distinctBy { it.id }
            .filter { !excluded(c, it.heading) }
            .sortedBy { it.price }

    /** The everyday range in the category, with pictures (MENY, SPAR, Oda). */
    suspend fun products(c: Category): List<Product> =
        c.queries.take(4).flatMap { q -> runCatching { Catalog.search(q) }.getOrDefault(emptyList()) }
            .filter { !excluded(c, it.name) }
            .distinctBy { Parse.keywords(it.name).sorted().joinToString(" ") }
}
