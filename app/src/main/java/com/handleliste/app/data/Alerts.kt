package com.handleliste.app.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.mutableStateListOf
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.handleliste.app.MainActivity
import com.handleliste.app.R
import com.handleliste.app.util.Lang
import com.handleliste.app.util.tr
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Something you follow: an item ("Pesto"), optionally a specific product or type you picked.
 * Or a whole category ([cat] = "pizza"): with no [picks] every deal in it alerts you, with picks only those products do.
 */
data class Follow(val id: String, val name: String, val pin: Pin?, val cat: String? = null, val picks: List<Pin> = emptyList())

/**
 * "Tell me when it's on sale": items you follow are checked against this week's flyers near you
 * every 6 hours in the background, and you get one notification per new deal.
 */
object Alerts {
    val follows = mutableStateListOf<Follow>()
    private var loaded = false
    private const val CHANNEL = "deal_alerts"
    const val EXTRA_TAB = "open_tab"

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences("alerts", Context.MODE_PRIVATE)

    @Synchronized
    fun init(ctx: Context) {
        if (loaded) return
        loaded = true
        runCatching {
            val a = JSONArray(prefs(ctx).getString("follows", "[]"))
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                val picks = o.optJSONArray("picks") ?: JSONArray()
                follows += Follow(o.getString("id"), o.getString("name"), o.optJSONObject("pin")?.let { pinFrom(it) },
                    o.optString("cat").ifBlank { null }, (0 until picks.length()).map { pinFrom(picks.getJSONObject(it)) })
            }
        }
    }

    private fun pinJson(p: Pin) = JSONObject().put("name", p.name).put("detail", p.detail).put("image", p.image ?: "")
        .put("source", p.source).put("kw", JSONArray(p.keywords))
    private fun pinFrom(o: JSONObject): Pin {
        val kw = o.optJSONArray("kw") ?: JSONArray()
        return Pin(o.optString("name"), o.optString("detail"), o.optString("image").takeIf { it.startsWith("http") },
            o.optString("source"), (0 until kw.length()).map { kw.optString(it) })
    }

    private fun save(ctx: Context) {
        val a = JSONArray()
        follows.forEach { f ->
            a.put(JSONObject().put("id", f.id).put("name", f.name).put("pin", f.pin?.let { pinJson(it) } ?: JSONObject.NULL)
                .put("cat", f.cat ?: "").put("picks", JSONArray().apply { f.picks.forEach { put(pinJson(it)) } }))
        }
        prefs(ctx).edit().putString("follows", a.toString()).apply()
    }

    fun isFollowed(name: String) = follows.any { it.name.equals(name.trim(), true) }

    /** Follow (or update what exactly to follow), then check straight away. */
    fun follow(ctx: Context, name: String, pin: Pin?) {
        init(ctx)
        val i = follows.indexOfFirst { it.name.equals(name.trim(), true) }
        if (i >= 0) follows[i] = follows[i].copy(pin = pin) else follows += Follow(UUID.randomUUID().toString(), name.trim().replaceFirstChar { it.uppercase() }, pin)
        save(ctx)
        schedule(ctx, checkNow = true)
    }

    /** Your subscription to a category, if any. */
    fun category(id: String) = follows.firstOrNull { it.cat == id }

    /** Subscribe to a category (all of it until you pick specific products). */
    fun subscribe(ctx: Context, c: Category) {
        init(ctx)
        if (category(c.id) == null) follows += Follow(UUID.randomUUID().toString(), c.no.takeIf { Lang.no } ?: c.en, null, c.id)
        save(ctx)
        schedule(ctx, checkNow = true)
    }

    fun unsubscribe(ctx: Context, c: Category) {
        init(ctx)
        follows.removeAll { it.cat == c.id }
        save(ctx)
        if (follows.isEmpty()) WorkManager.getInstance(ctx).cancelUniqueWork("deal-alerts")
    }

    fun isPicked(c: Category, p: Pin) = category(c.id)?.picks?.any { it.name.equals(p.name, true) } == true

    /** Tick or untick one product in a category; ticking subscribes you to the category. */
    fun togglePick(ctx: Context, c: Category, p: Pin) {
        init(ctx)
        if (category(c.id) == null) subscribe(ctx, c)
        val i = follows.indexOfFirst { it.cat == c.id }
        val f = follows[i]
        val picks = if (f.picks.any { it.name.equals(p.name, true) }) f.picks.filterNot { it.name.equals(p.name, true) } else f.picks + p
        follows[i] = f.copy(picks = picks)
        save(ctx)
        schedule(ctx, checkNow = true)
    }

    /** This week's deals that would alert you for a follow. */
    fun dealsFor(f: Follow, flyers: List<Offer>, now: String): List<Offer> {
        val c = Categories.byId(f.cat)
        return when {
            c != null && f.picks.isEmpty() -> Categories.offers(c, flyers, now)
            c != null -> f.picks.flatMap { p -> Parse.dealsFor(Parse.searchTerm(p.name), p, flyers, now).first }.distinctBy { it.id }
            else -> Parse.dealsFor(Parse.searchTerm(f.name), f.pin, flyers, now).first
        }
    }

    fun unfollow(ctx: Context, name: String) {
        init(ctx)
        follows.removeAll { it.name.equals(name.trim(), true) }
        save(ctx)
        if (follows.isEmpty()) WorkManager.getInstance(ctx).cancelUniqueWork("deal-alerts")
    }

    /** Keeps the follow in step when you pick a different product for the item. */
    fun pinChanged(ctx: Context, name: String, pin: Pin?) {
        init(ctx)
        val i = follows.indexOfFirst { it.name.equals(name.trim(), true) }
        if (i >= 0) { follows[i] = follows[i].copy(pin = pin); save(ctx) }
    }

    fun schedule(ctx: Context, checkNow: Boolean = false) {
        init(ctx)
        if (follows.isEmpty()) return
        val wm = WorkManager.getInstance(ctx)
        val net = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        wm.enqueueUniquePeriodicWork("deal-alerts", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<DealAlertWorker>(6, TimeUnit.HOURS).setConstraints(net).build())
        if (checkNow) wm.enqueueUniqueWork("deal-alerts-now", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<DealAlertWorker>().setConstraints(net).build())
    }

    /** Checks every followed item and notifies about deals not told about before. */
    suspend fun check(ctx: Context) {
        init(ctx)
        if (follows.isEmpty()) return
        val loc = Settings.lastLocation(ctx)
        val flyers = Flyers.near(ctx, loc?.first ?: Deals.FALLBACK_LAT, loc?.second ?: Deals.FALLBACK_LNG, Settings.radiusKm(ctx) * 1000)
        if (flyers.isEmpty()) return
        val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.ROOT)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date())
        val p = prefs(ctx)
        val told = p.getStringSet("told", emptySet())!!.toMutableSet()
        follows.toList().forEach { f ->
            val mine = dealsFor(f, flyers, now)
            val fresh = mine.filter { "${f.id}:${it.id}" !in told }
            if (fresh.isNotEmpty()) {
                notify(ctx, f, fresh)
                fresh.forEach { told += "${f.id}:${it.id}" }
            }
        }
        // Forget very old ones so the set doesn't grow forever (offer ids are unique per week).
        p.edit().putStringSet("told", told.toList().takeLast(600).toSet()).apply()
    }

    private fun notify(ctx: Context, f: Follow, offers: List<Offer>) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(
            NotificationChannel(CHANNEL, tr("Deals on things you follow", "Tilbud på det du følger"), NotificationManager.IMPORTANCE_DEFAULT))
        val best = offers.minBy { it.price }
        val what = when {
            f.cat != null && offers.size == 1 -> offers[0].heading.lowercase().replaceFirstChar { it.uppercase() }
            else -> f.pin?.name ?: f.name
        }
        val price = "kr " + String.format(java.util.Locale("nb", "NO"), "%.2f", best.price)
        val pct = best.savingsPct?.takeIf { it in 1..95 }?.let { " (−$it %)" }.orEmpty()
        val open = PendingIntent.getActivity(ctx, f.id.hashCode(),
            Intent(ctx, MainActivity::class.java).putExtra(EXTRA_TAB, 1).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val style = NotificationCompat.InboxStyle()
        offers.sortedBy { it.price }.take(5).forEach {
            style.addLine("${it.store}: ${it.heading.lowercase().replaceFirstChar { c -> c.uppercase() }} · kr " +
                String.format(java.util.Locale("nb", "NO"), "%.2f", it.price))
        }
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notify).setColor(0xFF1F7A4D.toInt())
            .setContentTitle(tr("🏷️ $what is on sale", "🏷️ $what er på tilbud"))
            .setContentText("$price ${tr("at", "hos")} ${best.store}$pct" +
                if (offers.size > 1) tr(" · ${offers.size} deals", " · ${offers.size} tilbud") else "")
            .setStyle(style).setAutoCancel(true).setContentIntent(open).build()
        NotificationManagerCompat.from(ctx).notify(f.id.hashCode(), n)
    }
}

class DealAlertWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        Lang.init(applicationContext)
        return runCatching { Alerts.check(applicationContext) }.fold({ Result.success() }, { Result.retry() })
    }
}
