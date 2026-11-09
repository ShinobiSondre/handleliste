package com.handleliste.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/** Learns what you buy and how often (from what you tick off), kept only on the phone. */
object History {
    private val habits = HashMap<String, Habit>()
    private var loaded = false
    /** Bumped on every change, so suggestion lists recompute. */
    var version by mutableIntStateOf(0); private set

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences("history", Context.MODE_PRIVATE)

    @Synchronized
    private fun load(ctx: Context) {
        if (loaded) return
        loaded = true
        runCatching {
            val o = JSONObject(prefs(ctx).getString("habits", "{}")!!)
            o.keys().forEach { k ->
                val h = o.getJSONObject(k)
                val b = h.optJSONArray("b") ?: JSONArray()
                val t = h.optJSONArray("t") ?: JSONArray()
                habits[k] = Habit(h.optString("n"), (0 until b.length()).map { b.getLong(it) }, h.optInt("a"), (0 until t.length()).map { t.getLong(it) })
            }
        }
    }

    private fun save(ctx: Context) {
        val o = JSONObject()
        habits.forEach { (k, h) ->
            o.put(k, JSONObject().put("n", h.name).put("a", h.adds).put("b", JSONArray(h.buys)).put("t", JSONArray(h.addTimes)))
        }
        prefs(ctx).edit().putString("habits", o.toString()).apply()
        version++
    }

    fun bought(ctx: Context, name: String) {
        load(ctx)
        val k = Suggest.key(name)
        habits[k] = Suggest.bought(habits[k], name, System.currentTimeMillis())
        save(ctx)
    }

    fun unbought(ctx: Context, name: String) {
        load(ctx)
        val k = Suggest.key(name)
        habits[k]?.let { habits[k] = Suggest.unbought(it, System.currentTimeMillis()); save(ctx) }
    }

    fun added(ctx: Context, name: String) {
        load(ctx)
        val k = Suggest.key(name)
        habits[k] = Suggest.added(habits[k], name, System.currentTimeMillis())
        save(ctx)
    }

    fun forget(ctx: Context, name: String) {
        load(ctx)
        habits.remove(Suggest.key(name)); save(ctx)
    }

    fun suggestions(ctx: Context): List<Suggestion> {
        load(ctx)
        val onList = ListRepo.open.map { Suggest.key(it.name) }.toSet()
        return Suggest.suggestions(habits.values, onList, System.currentTimeMillis())
    }
}
