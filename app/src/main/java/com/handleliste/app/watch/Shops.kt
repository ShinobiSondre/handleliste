package com.handleliste.app.watch

import android.content.Context
import com.handleliste.app.data.Parse
import com.handleliste.app.data.Shop
import com.handleliste.app.util.Http
import org.json.JSONArray
import org.json.JSONObject

/**
 * Grocery stores near you, from OpenStreetMap (free, no key, via the Overpass API).
 * Fetched once for a 2 km circle and kept on the phone, so walking around costs no network.
 * A new circle is fetched when you move more than 1 km from the last one, or after 3 days.
 */
object Shops {
    private const val RADIUS_M = 2000
    private const val REFRESH_M = 1000.0
    private const val MAX_AGE_MS = 3 * 24 * 3600 * 1000L

    private val SERVERS = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://maps.mail.ru/osm/tools/overpass/api/interpreter",
    )
    private const val KINDS = "supermarket|convenience|grocery|greengrocer|health_food|butcher|deli|frozen_food|general|wholesale"

    private var cache: List<Shop> = emptyList()
    private var centerLat = 0.0
    private var centerLng = 0.0
    private var fetchedAt = 0L
    private var loaded = false

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences("shops", Context.MODE_PRIVATE)

    private fun load(ctx: Context) {
        if (loaded) return
        loaded = true
        val p = prefs(ctx)
        centerLat = p.getFloat("lat", 0f).toDouble()
        centerLng = p.getFloat("lng", 0f).toDouble()
        fetchedAt = p.getLong("at", 0L)
        cache = runCatching {
            val a = JSONArray(p.getString("shops", "[]"))
            (0 until a.length()).map {
                val o = a.getJSONObject(it)
                Shop(o.getString("id"), o.getString("name"), o.getString("brand"), o.getString("kind"),
                    o.getDouble("lat"), o.getDouble("lng"), o.getBoolean("area"))
            }
        }.getOrDefault(emptyList())
    }

    private fun save(ctx: Context) {
        val a = JSONArray()
        cache.forEach {
            a.put(JSONObject().put("id", it.id).put("name", it.name).put("brand", it.brand).put("kind", it.kind)
                .put("lat", it.lat).put("lng", it.lng).put("area", it.isArea))
        }
        prefs(ctx).edit().putString("shops", a.toString()).putFloat("lat", centerLat.toFloat())
            .putFloat("lng", centerLng.toFloat()).putLong("at", fetchedAt).apply()
    }

    /** Stores around this spot (from the phone's cache when it still covers it). */
    suspend fun near(ctx: Context, lat: Double, lng: Double): List<Shop> {
        load(ctx)
        val fresh = System.currentTimeMillis() - fetchedAt < MAX_AGE_MS
        val covered = fetchedAt > 0 && Parse.distanceM(lat, lng, centerLat, centerLng) < REFRESH_M
        if (fresh && covered) return cache
        val q = "[out:json][timeout:25];nwr(around:$RADIUS_M,$lat,$lng)[\"shop\"~\"^($KINDS)$\"];out center tags;"
        for (server in SERVERS) {
            try {
                val shops = Parse.overpassShops(Http.post(server, "data=" + Http.enc(q), "application/x-www-form-urlencoded", timeoutMs = 30000))
                cache = shops; centerLat = lat; centerLng = lng; fetchedAt = System.currentTimeMillis()
                save(ctx)
                return cache
            } catch (_: Exception) { }
        }
        return if (covered) cache else emptyList() // offline: use what we have if it's for this area
    }

    /** Closest known store and its distance, for choosing how often to check. */
    fun nearestDistance(lat: Double, lng: Double): Double =
        cache.minOfOrNull { Parse.distanceM(lat, lng, it.lat, it.lng) } ?: Double.MAX_VALUE
}
