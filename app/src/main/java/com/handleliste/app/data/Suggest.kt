package com.handleliste.app.data

import kotlin.math.max
import kotlin.math.roundToInt

// Pure logic (no Android) so the cloud build can unit-test it.

/**
 * What the app has learned about one product: when it was bought (ticked), and when it was put on a
 * list ([addTimes]). [adds] is an old counter, kept only so old saved data still loads.
 */
data class Habit(val name: String, val buys: List<Long>, val adds: Int, val addTimes: List<Long> = emptyList())

data class Suggestion(val name: String, val score: Double, val everyDays: Int?, val due: Boolean, val times: Int)

object Suggest {
    private const val DAY = 24 * 3600 * 1000L

    fun key(name: String) = name.trim().lowercase()

    /** A tick counts as a purchase, but ticking the same thing twice within 6 hours is one trip. */
    fun bought(h: Habit?, name: String, now: Long): Habit {
        val buys = h?.buys.orEmpty()
        val last = buys.lastOrNull()
        val next = if (last != null && now - last < 6 * 3600 * 1000L) buys else (buys + now).takeLast(30)
        return Habit(name, next, h?.adds ?: 0, h?.addTimes.orEmpty())
    }

    /** Un-ticked again shortly after: it was a mis-tap, not a purchase. */
    fun unbought(h: Habit, now: Long): Habit {
        val last = h.buys.lastOrNull() ?: return h
        return if (now - last < 6 * 3600 * 1000L) h.copy(buys = h.buys.dropLast(1)) else h
    }

    /** Put on a list: one occasion, however many times it's added within 12 hours. */
    fun added(h: Habit?, name: String, now: Long): Habit {
        val t = h?.addTimes.orEmpty()
        val next = if (t.isNotEmpty() && now - t.last() < 12 * 3600 * 1000L) t else (t + now).takeLast(30)
        return Habit(name, h?.buys.orEmpty(), h?.adds ?: 0, next)
    }

    /**
     * What you probably need: only things you've put on a list on at least two different occasions
     * (once doesn't make a habit). Those about due (it's been ~your usual gap since last time) come
     * first. Nothing that's already on a list, and nothing you've stopped buying.
     */
    fun suggestions(habits: Collection<Habit>, onList: Set<String>, now: Long, limit: Int = 12): List<Suggestion> =
        habits.asSequence()
            .filter { key(it.name) !in onList }
            .mapNotNull { h ->
                val occ = h.addTimes.sorted()
                val n = occ.size
                if (n < 2 || now - occ.last() < 12 * 3600 * 1000L) return@mapNotNull null
                val gap = max((occ.last() - occ.first()).toDouble() / (n - 1), DAY.toDouble())
                val since = (now - occ.last()).toDouble()
                val ratio = since / gap
                if (since > gap * 6 && since > 60 * DAY) null // stopped buying it
                else Suggestion(h.name, ratio.coerceAtMost(3.0) + n * 0.05, (gap / DAY).roundToInt().coerceAtLeast(1), ratio >= 0.8, n)
            }
            .sortedByDescending { it.score }
            .take(limit)
            .toList()
}
