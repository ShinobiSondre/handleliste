package com.handleliste.app.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject

/** One dinner choice ("Med kjøtt", "Vegansk" …): an id that's saved, and the words for each language. */
data class DinnerChoice(val id: String, val en: String, val no: String)

/**
 * Your dinner choices (remembered between visits) and the dishes ChatGPT suggested lately, so it's asked
 * for something new instead of the same tacos every time.
 */
object DinnerPrefs {
    val CHOICES = listOf(
        DinnerChoice("meat", "With meat", "Med kjøtt"),
        DinnerChoice("chicken", "With chicken", "Med kylling"),
        DinnerChoice("fish", "With fish", "Med fisk"),
        DinnerChoice("vegetarian", "Vegetarian", "Vegetar"),
        DinnerChoice("vegan", "Vegan", "Vegansk"),
        DinnerChoice("glutenfree", "Gluten-free", "Glutenfri"),
        DinnerChoice("lactosefree", "Lactose-free", "Laktosefri"),
        DinnerChoice("quick", "Quick (under 30 min)", "Rask (under 30 min)"),
        DinnerChoice("cheap", "Cheap", "Billig"),
        DinnerChoice("healthy", "Healthy", "Sunn"),
        DinnerChoice("kids", "Kid-friendly", "Barnevennlig"),
        DinnerChoice("cosy", "Cosy / comfort food", "Kos / hverdagsfavoritt"),
        DinnerChoice("spicy", "Spicy", "Sterk"),
        DinnerChoice("oneplot", "One pot / tray bake", "Alt i én gryte/form"),
    )

    /** Choices that can't go together: picking one turns the others off. */
    private val CLASHES = listOf(
        setOf("vegan", "vegetarian", "meat", "chicken", "fish"),
    )

    val chosen = mutableStateListOf<String>()
    private var loaded = false
    private fun p(ctx: Context) = ctx.applicationContext.getSharedPreferences("dinner", Context.MODE_PRIVATE)

    fun init(ctx: Context) {
        if (loaded) return
        loaded = true
        chosen.addAll(p(ctx).getStringSet("choices", emptySet())!!.filter { id -> CHOICES.any { it.id == id } })
    }

    fun toggle(ctx: Context, id: String) {
        init(ctx)
        if (id in chosen) chosen.remove(id) else {
            // Vegan + meat makes no sense: the diet group is either-or (meat, chicken and fish may go together).
            val clash = CLASHES.firstOrNull { id in it }
            if (clash != null) {
                val meaty = setOf("meat", "chicken", "fish")
                chosen.removeAll { it in clash && it != id && !(id in meaty && it in meaty) }
            }
            chosen += id
        }
        p(ctx).edit().putStringSet("choices", chosen.toSet()).apply()
    }

    /** The choices as words for the prompt, in the app's language. */
    fun words(no: Boolean): List<String> = CHOICES.filter { it.id in chosen }.map { if (no) it.no else it.en }

    // ---- recently suggested dishes ----

    private const val KEEP_DAYS = 21L
    private const val MAX = 25

    /** Dish names ChatGPT suggested in the last three weeks, newest first. */
    fun recent(ctx: Context): List<String> {
        val cutoff = System.currentTimeMillis() - KEEP_DAYS * 24 * 3600 * 1000
        return runCatching {
            val a = JSONArray(p(ctx).getString("recent", "[]"))
            (0 until a.length()).map { a.getJSONObject(it) }.filter { it.optLong("t") > cutoff }
                .sortedByDescending { it.optLong("t") }.map { it.optString("n") }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList()).distinctBy { it.lowercase() }.take(MAX)
    }

    fun remember(ctx: Context, dishes: List<String>) {
        if (dishes.isEmpty()) return
        val now = System.currentTimeMillis()
        val keep = recent(ctx).filter { r -> dishes.none { it.equals(r, true) } }
        val a = JSONArray()
        dishes.forEach { a.put(JSONObject().put("n", it).put("t", now)) }
        keep.forEachIndexed { i, n -> a.put(JSONObject().put("n", n).put("t", now - (i + 1) * 60_000L)) }
        p(ctx).edit().putString("recent", a.toString()).apply()
    }
}
