package com.handleliste.app.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Item(val id: String, val name: String, val checked: Boolean = false, val added: Long = System.currentTimeMillis())

/** The shopping list, saved on the phone. Shared by the screens and the store watcher. */
object ListRepo {
    val items = mutableStateListOf<Item>()
    private var loaded = false
    private lateinit var app: Context

    private fun prefs() = app.getSharedPreferences("list", Context.MODE_PRIVATE)

    @Synchronized
    fun init(ctx: Context) {
        if (loaded) return
        app = ctx.applicationContext
        val arr = runCatching { JSONArray(prefs().getString("items", "[]")) }.getOrDefault(JSONArray())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            items += Item(o.optString("id"), o.optString("name"), o.optBoolean("checked"), o.optLong("added"))
        }
        loaded = true
    }

    private fun save() {
        val arr = JSONArray()
        items.forEach { arr.put(JSONObject().put("id", it.id).put("name", it.name).put("checked", it.checked).put("added", it.added)) }
        prefs().edit().putString("items", arr.toString()).apply()
    }

    val open: List<Item> get() = items.filter { !it.checked }

    fun add(text: String) {
        val existing = items.map { it.name.lowercase() }.toSet()
        Parse.splitItems(text).forEach { name ->
            if (name.lowercase() in existing) {
                // Already on the list: just un-tick it.
                val i = items.indexOfFirst { it.name.equals(name, true) }
                if (i >= 0) items[i] = items[i].copy(checked = false)
            } else {
                items.add(0, Item(UUID.randomUUID().toString(), name))
            }
        }
        save()
    }

    fun toggle(id: String) {
        val i = items.indexOfFirst { it.id == id }
        if (i >= 0) { items[i] = items[i].copy(checked = !items[i].checked); save() }
    }

    fun remove(id: String) { items.removeAll { it.id == id }; save() }

    fun rename(id: String, name: String) {
        val i = items.indexOfFirst { it.id == id }
        if (i >= 0 && name.isNotBlank()) { items[i] = items[i].copy(name = name.trim()); save() }
    }

    fun clearChecked() { items.removeAll { it.checked }; save() }

    fun restore(item: Item) { if (items.none { it.id == item.id }) { items.add(0, item); save() } }
}

/** Small settings, saved on the phone. API keys never leave the phone except to their own service. */
object Settings {
    private fun p(ctx: Context) = ctx.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun watchOn(ctx: Context) = p(ctx).getBoolean("watch", false)
    fun setWatch(ctx: Context, on: Boolean) = p(ctx).edit().putBoolean("watch", on).apply()

    fun geminiKey(ctx: Context) = p(ctx).getString("gemini", "").orEmpty().trim()
    fun setGeminiKey(ctx: Context, k: String) = p(ctx).edit().putString("gemini", k.trim()).apply()

    fun kassalKey(ctx: Context) = p(ctx).getString("kassal", "").orEmpty().trim()
    fun setKassalKey(ctx: Context, k: String) = p(ctx).edit().putString("kassal", k.trim()).apply()

    /** Deals radius in km (default 15). */
    fun radiusKm(ctx: Context) = p(ctx).getInt("radius", 15)
    fun setRadiusKm(ctx: Context, km: Int) = p(ctx).edit().putInt("radius", km).apply()

    fun lastLocation(ctx: Context): Pair<Double, Double>? {
        val s = p(ctx).getString("lastLoc", null) ?: return null
        val (a, b) = s.split(",").mapNotNull { it.toDoubleOrNull() }.takeIf { it.size == 2 } ?: return null
        return a to b
    }
    fun setLastLocation(ctx: Context, lat: Double, lng: Double) = p(ctx).edit().putString("lastLoc", "$lat,$lng").apply()

    fun onboarded(ctx: Context) = p(ctx).getBoolean("onboarded", false)
    fun setOnboarded(ctx: Context) = p(ctx).edit().putBoolean("onboarded", true).apply()
}
