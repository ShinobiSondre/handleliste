package com.handleliste.app.data

import kotlin.math.max
import kotlin.math.roundToInt

// Pure logic (no Android) so the cloud build can unit-test it.

/** What the app has learned about one product: when it was bought, and how often it was put on a list. */
data class Habit(val name: String, val buys: List<Long>, val adds: Int)

data class Suggestion(val name: String, val score: Double, val everyDays: Int?, val due: Boolean, val times: Int)

object Suggest {
    private const val DAY = 24 * 3600 * 1000L

    fun key(name: String) = name.trim().lowercase()

    /** A tick counts as a purchase, but ticking the same thing twice within 6 hours is one trip. */
    fun bought(h: Habit?, name: String, now: Long): Habit {
        val buys = h?.buys.orEmpty()
        val last = buys.lastOrNull()
        val next = if (last != null && now - last < 6 * 3600 * 1000L) buys else (buys + now).takeLast(30)
        return Habit(name, next, h?.adds ?: 0)
    }

    /** Un-ticked again shortly after: it was a mis-tap, not a purchase. */
    fun unbought(h: Habit, now: Long): Habit {
        val last = h.buys.lastOrNull() ?: return h
        return if (now - last < 6 * 3600 * 1000L) h.copy(buys = h.buys.dropLast(1)) else h
    }

    /**
     * What you probably need: things you buy regularly that are about due (it's been ~their usual
     * gap since last time), then things you often buy. Nothing already on a list.
     */
    fun suggestions(habits: Collection<Habit>, onList: Set<String>, now: Long, limit: Int = 12): List<Suggestion> =
        habits.asSequence()
            .filter { key(it.name) !in onList }
            .mapNotNull { h ->
                val n = h.buys.size
                if (n >= 2) {
                    val gap = max((h.buys.last() - h.buys.first()).toDouble() / (n - 1), DAY.toDouble())
                    val since = (now - h.buys.last()).toDouble()
                    val ratio = since / gap
                    if (since > gap * 6 && since > 60 * DAY) null // stopped buying it
                    else Suggestion(h.name, ratio.coerceAtMost(3.0) + n * 0.05, (gap / DAY).roundToInt().coerceAtLeast(1), ratio >= 0.8, n)
                } else if (n + h.adds >= 2 && now - (h.buys.lastOrNull() ?: 0L) > DAY) {
                    Suggestion(h.name, 0.2 + (n + h.adds) * 0.05, null, false, n)
                } else null
            }
            .sortedByDescending { it.score }
            .take(limit)
            .toList()
}
