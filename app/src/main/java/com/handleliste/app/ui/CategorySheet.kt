package com.handleliste.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.handleliste.app.data.Alerts
import com.handleliste.app.data.Categories
import com.handleliste.app.data.Category
import com.handleliste.app.data.Deals
import com.handleliste.app.data.Flyers
import com.handleliste.app.data.Offer
import com.handleliste.app.data.Parse
import com.handleliste.app.data.Product
import com.handleliste.app.data.Settings
import com.handleliste.app.util.Lang
import com.handleliste.app.util.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun Category.label() = if (Lang.no) no else en

/** "🍕 Pizza  ☕ Kaffe …" on the deals page. A bell shows the ones you subscribe to. */
@Composable
fun CategoryRow(onOpen: (Category) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(Categories.all, key = { it.id }) { c ->
            val sub = Alerts.category(c.id) != null
            Surface(onClick = { onOpen(c) }, shape = RoundedCornerShape(50),
                color = if (sub) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(c.emoji, fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(c.label(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = if (sub) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                    if (sub) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.NotificationsActive, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

/**
 * One category: subscribe to it, then tick the exact products you want alerts for.
 * Nothing ticked = every deal in the category alerts you.
 */
@Composable
fun CategorySheet(c: Category, onClose: () -> Unit, onImage: (Offer) -> Unit) {
    val ctx = LocalContext.current
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun askPermission() { if (Build.VERSION.SDK_INT >= 33) perm.launch(Manifest.permission.POST_NOTIFICATIONS) }
    var deals by remember(c.id) { mutableStateOf<List<Offer>?>(null) }
    var products by remember(c.id) { mutableStateOf<List<Product>?>(null) }
    var filter by remember(c.id) { mutableStateOf("") }
    LaunchedEffect(c.id) {
        deals = withContext(Dispatchers.Default) {
            var all = Flyers.cachedAsync(ctx)
            if (all.isEmpty()) all = runCatching {
                val loc = currentLocation(ctx)
                Flyers.near(ctx, loc?.first ?: Deals.FALLBACK_LAT, loc?.second ?: Deals.FALLBACK_LNG, Settings.radiusKm(ctx) * 1000)
            }.getOrDefault(emptyList())
            Categories.offers(c, all, nowIso())
        }
        products = withContext(Dispatchers.IO) { Categories.products(c) }
    }
    val sub = Alerts.category(c.id)
    val picks = sub?.picks.orEmpty()
    fun matches(text: String) = filter.isBlank() || Parse.keywords(filter).all { text.lowercase().contains(it) }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(c.emoji, fontSize = 30.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(c.label(), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, tr("Close", "Lukk")) }
                }
                // The subscribe switch for the whole category.
                SubscribeBar(c, subscribed = sub != null, picked = picks.size,
                    onToggle = { if (sub == null) { askPermission(); Alerts.subscribe(ctx, c) } else Alerts.unsubscribe(ctx, c) })
                OutlinedTextField(filter, { filter = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true, shape = RoundedCornerShape(16.dp), leadingIcon = { Icon(Icons.Filled.Search, null) },
                    placeholder = { Text(tr("Find a ${c.en.lowercase()} …", "Finn ${c.no.lowercase()} …")) })

                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (picks.isNotEmpty()) {
                        item { Section(tr("You get alerts for", "Du varsles for")) }
                        items(picks, key = { "p:" + it.name }) { p ->
                            PickRow(p.name, p.detail, p.image, null, true) { Alerts.togglePick(ctx, c, p) }
                        }
                    }
                    val d = deals
                    item { Section(tr("On sale this week", "På tilbud denne uken") + (d?.let { " · ${it.size}" } ?: "")) }
                    when {
                        d == null -> item { Loading() }
                        d.isEmpty() -> item { Hint(tr("No ${c.en.lowercase()} deals near you right now.", "Ingen tilbud på ${c.no.lowercase()} nær deg akkurat nå.")) }
                        else -> items(d.filter { matches(it.heading + " " + it.description) }, key = { "o:" + it.id }) { o ->
                            val pin = Parse.pinFromOffer(o)
                            val on = picks.any { p -> Parse.pinMatches(p, o) }
                            PickRow(o.heading.lowercase().replaceFirstChar { it.uppercase() }, o.store, o.image, o, on, onImage = { onImage(o) }) {
                                if (sub == null) askPermission()
                                val existing = picks.firstOrNull { p -> Parse.pinMatches(p, o) }
                                Alerts.togglePick(ctx, c, existing ?: pin)
                            }
                        }
                    }
                    val p = products
                    item { Section(tr("Pick the ones you want alerts for", "Velg de du vil varsles om")) }
                    when {
                        p == null -> item { Loading() }
                        else -> items(p.filter { matches(it.name + " " + it.brand) }.take(80), key = { "c:" + it.source + it.name + it.extra }) { prod ->
                            val pin = Parse.pinFromProduct(prod)
                            val on = Alerts.isPicked(c, pin)
                            PickRow(prod.name, listOf(prod.brand, prod.extra).filter { it.isNotBlank() }.joinToString(" · "), prod.image, null, on) {
                                if (sub == null) askPermission()
                                Alerts.togglePick(ctx, c, pin)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscribeBar(c: Category, subscribed: Boolean, picked: Int, onToggle: () -> Unit) {
    val bg by animateColorAsState(if (subscribed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer, label = "subBg")
    val fg = if (subscribed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
    Surface(onClick = onToggle, shape = RoundedCornerShape(18.dp), color = bg,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (subscribed) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsNone, null, tint = fg, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (subscribed) tr("Subscribed", "Abonnerer") else tr("Subscribe to ${c.en.lowercase()}", "Abonner på ${c.no.lowercase()}"),
                    fontWeight = FontWeight.Bold, fontSize = 17.sp, color = fg)
                Text(
                    when {
                        !subscribed -> tr("Get a notification when it's on sale near you", "Få varsel når det er på tilbud nær deg")
                        picked == 0 -> tr("Alerts for every ${c.en.lowercase()} deal · tick below to narrow it down",
                            "Varsler for alle tilbud på ${c.no.lowercase()} · huk av under for å velge")
                        else -> tr("Alerts for the $picked you picked", "Varsler for de $picked du har valgt")
                    }, fontSize = 13.sp, color = fg.copy(alpha = 0.85f))
            }
            if (subscribed) Text(tr("Stop", "Stopp"), color = fg, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

/** A product or deal with a bell you tick to get alerts for exactly that one. */
@Composable
private fun PickRow(name: String, detail: String, image: String?, offer: Offer?, on: Boolean, onImage: (() -> Unit)? = null, onToggle: () -> Unit) {
    val pop by animateFloatAsState(if (on) 1f else 0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "bell")
    Surface(onClick = onToggle, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().border(if (on) 2.dp else 0.dp, if (on) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(16.dp))) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(onClick = { onImage?.invoke() ?: onToggle() }, shape = RoundedCornerShape(12.dp), color = Color.White, modifier = Modifier.size(56.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    if (image != null) AsyncImage(image, null, Modifier.fillMaxSize().padding(3.dp), contentScale = ContentScale.Fit)
                    else Text("🏷️", fontSize = 20.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 15.sp)
                if (detail.isNotBlank()) Text(detail, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (offer != null) Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(kr(offer.price), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary, fontSize = 15.sp)
                    offer.savingsPct?.takeIf { it in 1..95 }?.let {
                        Spacer(Modifier.width(6.dp))
                        Text("−$it %", Modifier.clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.secondary)
                            .padding(horizontal = 5.dp, vertical = 1.dp), fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSecondary)
                    }
                }
            }
            Box(Modifier.size(40.dp).graphicsLayer { val k = 1f + 0.25f * pop * (1f - pop) * 4f; scaleX = k; scaleY = k }
                .clip(RoundedCornerShape(50))
                .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center) {
                Icon(if (on) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsNone,
                    if (on) tr("Stop alerts", "Slutt å varsle") else tr("Alert me", "Varsle meg"),
                    tint = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun Section(text: String) =
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))

@Composable
private fun Hint(text: String) = Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)

@Composable
private fun Loading() = Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
    CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 2.5.dp)
}

private fun nowIso(): String = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.ROOT)
    .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date())

