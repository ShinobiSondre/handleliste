package com.handleliste.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.handleliste.app.util.Lang
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Item(val id: String, val name: String, val checked: Boolean = false, val added: Long = System.currentTimeMillis(), val pin: Pin? = null)

data class ShopList(val id: String, val name: String, val image: String? = null, val recipe: String? = null)

/**
 * Your shopping lists (Handleliste, Middag, Hytta …), saved on the phone.
 * [items] is the list on screen; the store pop-up and deals use what's open on all lists.
 */
object ListRepo {
    val lists = mutableStateListOf<ShopList>()
    private val byList = HashMap<String, SnapshotStateList<Item>>()
    var currentId by mutableStateOf("")
        private set
    private var loaded = false
    private lateinit var app: Context

    private fun prefs() = app.getSharedPreferences("list", Context.MODE_PRIVATE)

    /** The list shown right now. */
    val items: SnapshotStateList<Item> get() = byList.getOrPut(currentId) { mutableStateListOf() }
    val current: ShopList? get() = lists.firstOrNull { it.id == currentId }

    fun itemsOf(listId: String): List<Item> = byList[listId].orEmpty()

    @Synchronized
    fun init(ctx: Context) {
        if (loaded) return
        app = ctx.applicationContext
        fun items(arr: JSONArray?): SnapshotStateList<Item> {
            val l = mutableStateListOf<Item>()
            if (arr != null) for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                l += Item(o.optString("id"), o.optString("name"), o.optBoolean("checked"), o.optLong("added"), pinFrom(o.optJSONObject("pin")))
            }
            return l
        }
        val saved = prefs().getString("lists", null)
        if (saved != null) runCatching {
            val o = JSONObject(saved)
            val arr = o.getJSONArray("lists")
            for (i in 0 until arr.length()) {
                val l = arr.getJSONObject(i)
                val meta = ShopList(l.getString("id"), l.getString("name"), l.optString("image").takeIf { it.startsWith("http") },
                    l.optString("recipe").takeIf { it.isNotBlank() })
                lists += meta
                byList[meta.id] = items(l.optJSONArray("items"))
            }
            currentId = o.optString("current")
            o.optJSONObject("pins")?.let { p -> p.keys().forEach { k -> pinFrom(p.optJSONObject(k))?.let { lastPins[k] = it } } }
        }
        if (lists.isEmpty()) {
            // First start, or moving from the one-list version: keep what was there.
            val first = ShopList(UUID.randomUUID().toString(), if (Lang.no) "Handleliste" else "Groceries")
            lists += first
            byList[first.id] = items(runCatching { JSONArray(prefs().getString("items", "[]")) }.getOrNull())
        }
        if (lists.none { it.id == currentId }) currentId = lists.first().id
        loaded = true
        save()
    }

    private fun save() {
        val arr = JSONArray()
        lists.forEach { meta ->
            val its = JSONArray()
            byList[meta.id].orEmpty().forEach {
                its.put(JSONObject().put("id", it.id).put("name", it.name).put("checked", it.checked).put("added", it.added)
                    .put("pin", it.pin?.let { p -> pinJson(p) } ?: JSONObject.NULL))
            }
            arr.put(JSONObject().put("id", meta.id).put("name", meta.name).put("image", meta.image ?: "").put("recipe", meta.recipe ?: "").put("items", its))
        }
        val pins = JSONObject(); lastPins.forEach { (k, v) -> pins.put(k, pinJson(v)) }
        prefs().edit().putString("lists", JSONObject().put("lists", arr).put("current", currentId).put("pins", pins).toString()).apply()
    }

    /** Your last pick per item name, so "Pesto" comes back as your pesto next week. */
    private val lastPins = HashMap<String, Pin>()

    private fun pinJson(p: Pin) = JSONObject().put("name", p.name).put("detail", p.detail).put("image", p.image ?: "")
        .put("source", p.source).put("kw", JSONArray(p.keywords))

    private fun pinFrom(o: JSONObject?): Pin? {
        o ?: return null
        val kw = o.optJSONArray("kw") ?: JSONArray()
        return Pin(o.optString("name"), o.optString("detail"), o.optString("image").takeIf { it.startsWith("http") },
            o.optString("source"), (0 until kw.length()).map { kw.optString(it) })
    }

    /** Chooses (or clears, with null) the specific product or type for an item. */
    fun setPin(id: String, pin: Pin?) {
        val (l, i) = find(id) ?: return
        l[i] = l[i].copy(pin = pin)
        if (pin != null) lastPins[l[i].name.lowercase()] = pin else lastPins.remove(l[i].name.lowercase())
        Alerts.pinChanged(app, l[i].name, pin)
        save()
    }

    fun item(id: String): Item? = find(id)?.let { (l, i) -> l[i] }

    // ---- lists ----

    fun select(id: String) { if (lists.any { it.id == id }) { currentId = id; save() } }

    fun newList(name: String): String {
        val meta = ShopList(UUID.randomUUID().toString(), name.trim().ifBlank { if (Lang.no) "Ny liste" else "New list" })
        lists += meta
        byList[meta.id] = mutableStateListOf()
        currentId = meta.id
        save()
        return meta.id
    }

    /** A new list for a dish: its ingredients, with the dish's photo on the list. Opens it. */
    fun newListFrom(name: String, items: List<String>, image: String?, recipe: String? = null): String {
        val meta = ShopList(UUID.randomUUID().toString(), name.trim().ifBlank { if (Lang.no) "Ny liste" else "New list" }, image, recipe?.takeIf { it.isNotBlank() })
        lists += meta
        byList[meta.id] = mutableStateListOf()
        currentId = meta.id
        if (items.isNotEmpty()) add(items.joinToString(","), meta.id) else save()
        return meta.id
    }

    /** Adds, changes or (with blank text) removes a list's recipe. */
    fun setRecipe(id: String, text: String?) {
        val i = lists.indexOfFirst { it.id == id }
        if (i >= 0) { lists[i] = lists[i].copy(recipe = text?.trim()?.takeIf { it.isNotEmpty() }); save() }
    }

    fun renameList(id: String, name: String) {
        val i = lists.indexOfFirst { it.id == id }
        if (i >= 0 && name.isNotBlank()) { lists[i] = lists[i].copy(name = name.trim()); save() }
    }

    fun deleteList(id: String) {
        if (lists.size <= 1) return
        val i = lists.indexOfFirst { it.id == id }
        if (i < 0) return
        lists.removeAt(i); byList.remove(id)
        if (currentId == id) currentId = lists[(i - 1).coerceAtLeast(0)].id
        save()
    }

    // ---- items (on the list shown, unless a list is given) ----

    /** Not yet bought, on every list. */
    val open: List<Item> get() = lists.flatMap { byList[it.id].orEmpty() }.filter { !it.checked }

    /** Open items grouped by list, for the in-store screen. */
    fun openByList(): List<Pair<ShopList, List<Item>>> =
        lists.map { it to byList[it.id].orEmpty().filter { i -> !i.checked } }.filter { it.second.isNotEmpty() }

    fun add(text: String, listId: String = currentId) {
        val target = byList.getOrPut(listId) { mutableStateListOf() }
        Parse.splitItems(text).forEach { name ->
            val i = target.indexOfFirst { it.name.equals(name, true) }
            if (i >= 0) target[i] = target[i].copy(checked = false) // already there: just un-tick it
            else target.add(0, Item(UUID.randomUUID().toString(), name, pin = lastPins[name.lowercase()]))
            History.added(app, name)
        }
        save()
    }

    private fun find(id: String): Pair<SnapshotStateList<Item>, Int>? {
        for (l in byList.values) { val i = l.indexOfFirst { it.id == id }; if (i >= 0) return l to i }
        return null
    }

    fun toggle(id: String) {
        val (l, i) = find(id) ?: return
        val now = !l[i].checked
        l[i] = l[i].copy(checked = now)
        if (now) History.bought(app, l[i].name) else History.unbought(app, l[i].name)
        save()
    }

    fun remove(id: String) { val (l, i) = find(id) ?: return; l.removeAt(i); save() }

    fun rename(id: String, name: String) {
        val (l, i) = find(id) ?: return
        if (name.isNotBlank()) { l[i] = l[i].copy(name = name.trim()); save() }
    }

    /** Clears ticked items from the list shown (or from every list, after shopping). */
    fun clearChecked(all: Boolean = false) {
        if (all) byList.values.forEach { l -> l.removeAll { it.checked } } else items.removeAll { it.checked }
        save()
    }
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

    /** Deals radius in km. The flyer service needs at least ~25 km to find the regional flyers. */
    fun radiusKm(ctx: Context) = p(ctx).getInt("radius2", 30).coerceAtLeast(25)
    fun setRadiusKm(ctx: Context, km: Int) = p(ctx).edit().putInt("radius2", km).apply()

    fun lastLocation(ctx: Context): Pair<Double, Double>? {
        val s = p(ctx).getString("lastLoc", null) ?: return null
        val (a, b) = s.split(",").mapNotNull { it.toDoubleOrNull() }.takeIf { it.size == 2 } ?: return null
        return a to b
    }
    fun setLastLocation(ctx: Context, lat: Double, lng: Double) = p(ctx).edit().putString("lastLoc", "$lat,$lng").apply()

    fun onboarded(ctx: Context) = p(ctx).getBoolean("onboarded", false)
    fun setOnboarded(ctx: Context) = p(ctx).edit().putBoolean("onboarded", true).apply()
}
