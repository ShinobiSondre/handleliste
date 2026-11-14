package com.handleliste.app.data

import android.content.Context
import org.json.JSONObject
import java.util.Calendar

/**
 * Remembers the deal prices the app has seen for the things you buy, week by week, so it can tell
 * whether this week's deal is actually good ("Lowest in 8 weeks") or one you've seen cheaper.
 * It starts empty and learns as weeks go by; only deals for your lists and follows are kept.
 */
object PriceHistory {
    private const val WEEKS = 26
    private var data: JSONObject? = null
    private lateinit var app: Context

    private fun prefs() = app.getSharedPreferences("pricehistory", Context.MODE_PRIVATE)

    @Synchronized
    fun init(ctx: Context) {
        if (data != null) return
        app = ctx.applicationContext
        data = runCatching { JSONObject(prefs().getString("h", "{}")!!) }.getOrDefault(JSONObject())
    }

    /** The same product in any chain and any week: "GRANDIOSA ORIGINAL" + brand, letters only. */
    fun key(o: Offer): String = (Parse.brandOf(o) + " " + o.heading).lowercase().replace(Regex("[^\\p{L}]+"), " ").trim()

    fun week(ms: Long = System.currentTimeMillis()): Int {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        return c.get(Calendar.YEAR) * 100 + c.get(Calendar.WEEK_OF_YEAR)
    }

    /** Store this week's cheapest price for each of these deals. */
    @Synchronized
    fun record(ctx: Context, offers: List<Offer>) {
        init(ctx)
        val d = data ?: return
        val w = week().toString()
        offers.groupBy { key(it) }.forEach { (k, l) ->
            if (k.isBlank()) return@forEach
            val o = d.optJSONObject(k) ?: JSONObject().also { d.put(k, it) }
            val p = l.minOf { it.price }
            if (!o.has(w) || o.getDouble(w) > p) o.put(w, p)
            // Keep half a year.
            val ks = o.keys().asSequence().toList().sorted()
            ks.dropLast(WEEKS).forEach { o.remove(it) }
        }
        prefs().edit().putString("h", d.toString()).apply()
    }

    /** Earlier weeks' prices (oldest first), not counting this week. */
    fun past(o: Offer): List<Double> {
        val h = data?.optJSONObject(key(o)) ?: return emptyList()
        val now = week().toString()
        return h.keys().asSequence().filter { it != now }.sorted().map { h.getDouble(it) }.toList()
    }

    /** A short verdict about this price, or null when there's not enough history yet. */
    fun verdict(o: Offer): Verdict? = verdictFor(o.price, past(o))

    data class Verdict(val good: Boolean, val en: String, val no: String)

    fun verdictFor(price: Double, past: List<Double>): Verdict? {
        if (past.size < 2) return null
        val low = past.min()
        val n = past.size + 1
        return when {
            price <= low -> Verdict(true, "🔥 Lowest in $n weeks", "🔥 Laveste på $n uker")
            price <= past.sorted()[past.size / 2] -> Verdict(true, "👍 Good price", "👍 God pris")
            else -> Verdict(false, "Has been ${"%.0f".format(low)} kr", "Har vært ${"%.0f".format(low)} kr")
        }
    }
}
