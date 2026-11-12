package com.handleliste.app.ui

import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.LaunchedEffect
import com.handleliste.app.data.Flyers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.OutlinedTextField
import com.handleliste.app.data.Alerts
import com.handleliste.app.data.Parse
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.TextButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.handleliste.app.data.Deals
import com.handleliste.app.data.ItemDeals
import com.handleliste.app.data.ListRepo
import com.handleliste.app.data.Offer
import com.handleliste.app.data.Settings
import com.handleliste.app.util.tr
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Starts a deals check for everything not yet ticked off. */
suspend fun runDealsCheck(ctx: android.content.Context, force: Boolean = false) {
    val loc = currentLocation(ctx)
    Deals.check(ctx, ListRepo.open, loc?.first, loc?.second, force)
}

@Composable
fun DealsScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var zoom by remember { mutableStateOf<Offer?>(null) }
    var showFollows by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf<com.handleliste.app.data.Category?>(null) }
    // Search all of this week's deals near you for any food, not just what's on your lists.
    var query by rememberSaveable { mutableStateOf("") }
    var found by remember { mutableStateOf<List<Offer>?>(null) }
    var searching by remember { mutableStateOf(false) }
    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2) { found = null; return@LaunchedEffect }
        delay(250)
        searching = true
        found = withContext(Dispatchers.Default) {
            var all = Flyers.cachedAsync(ctx)
            if (all.isEmpty()) all = runCatching {
                val loc = currentLocation(ctx)
                Flyers.near(ctx, loc?.first ?: Deals.FALLBACK_LAT, loc?.second ?: Deals.FALLBACK_LNG, Settings.radiusKm(ctx) * 1000)
            }.getOrDefault(emptyList())
            val now = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.ROOT)
                .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date())
            Parse.matchOffers(Parse.searchTerm(q), all, now, limit = 60)
        }
        searching = false
    }
    // First row (with several lists): all lists, or one list. Second row: all stores, or one store.
    var listTab by rememberSaveable { mutableStateOf("") }
    if (listTab.isNotEmpty() && ListRepo.lists.none { it.id == listTab }) listTab = ""
    val results = if (listTab.isEmpty()) Deals.results else {
        val names = ListRepo.itemsOf(listTab).filter { !it.checked }.map { it.name.lowercase() }.toSet()
        Deals.results.filter { it.item.lowercase() in names }
    }
    val stores = results.flatMap { d -> d.offers.map { it.store }.distinct() }.groupingBy { it }.eachCount()
        .toList().sortedByDescending { it.second }
    // Opens on the store you chose on the list screen (if it has deals on your list).
    var tab by rememberSaveable { mutableStateOf(Deals.store.orEmpty()) }
    if (tab.isNotEmpty() && stores.none { it.first == tab }) tab = ""

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Deals", "Tilbud"), style = MaterialTheme.typography.headlineMedium)
                    val sub = when {
                        Deals.loading -> tr("Looking through this week's flyers…", "Ser gjennom ukens tilbudsaviser …")
                        Deals.fetchedAt > 0 -> tr("Checked automatically · ", "Sjekkes automatisk · ") +
                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(Deals.fetchedAt))
                        else -> tr("Checked automatically for your lists", "Sjekkes automatisk for listene dine")
                    }
                    Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(onClick = { showFollows = true }, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.NotificationsActive, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text(tr("Alerts", "Varsler") + if (Alerts.follows.isNotEmpty()) " ${Alerts.follows.size}" else "",
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                IconButton(onClick = { scope.launch { runDealsCheck(ctx, force = true) } }, enabled = !Deals.loading) {
                    Icon(Icons.Filled.Refresh, tr("Check again", "Sjekk igjen"))
                }
            }
            if (Deals.loading) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)))
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp),
                placeholder = { Text(tr("Search deals: coffee, salmon, Grandiosa …", "Søk i tilbud: kaffe, laks, Grandiosa …")) },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, tr("Clear", "Tøm")) } }
            )
        }
        if (found == null) {
            // Categories: subscribe to e.g. pizza, then pick the exact pizzas you want alerts for.
            Spacer(Modifier.height(10.dp))
            CategoryRow { category = it }
        }
        if (ListRepo.lists.size > 1 && found == null) {
            Spacer(Modifier.height(8.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { ListTab(tr("All lists", "Alle lister"), listTab == "") { listTab = "" } }
                items(ListRepo.lists, key = { it.id }) { l -> ListTab(l.name, listTab == l.id) { listTab = l.id } }
            }
        }
        if (stores.isNotEmpty() && found == null) {
            Spacer(Modifier.height(8.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { StoreTab(tr("All", "Alle"), null, tab == "") { tab = "" } }
                items(stores, key = { it.first }) { (store, n) -> StoreTab(store, n, tab == store) { tab = store } }
            }
        }

        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val hits = found
            if (hits != null) {
                item {
                    Text(
                        when {
                            searching && hits.isEmpty() -> tr("Searching…", "Søker …")
                            hits.isEmpty() -> tr("No deals on \"${query.trim()}\" this week", "Ingen tilbud på «${query.trim()}» denne uken")
                            else -> tr("${hits.size} deals on \"${query.trim()}\"", "${hits.size} tilbud på «${query.trim()}»")
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                if (hits.isNotEmpty()) item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
                        Column(Modifier.padding(vertical = 6.dp)) {
                            hits.forEachIndexed { i, o ->
                                if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                SearchHit(o, onImage = { zoom = o })
                            }
                        }
                    }
                }
            }
            if (hits == null) Deals.error?.let { e -> item { Note(e) } }
            if (hits == null && Deals.usedFallbackLocation && Deals.fetchedAt > 0) {
                item { Note(tr("Location is off, so these are deals around Oslo. Turn on location for stores near you.",
                    "Posisjon er av, så dette er tilbud rundt Oslo. Slå på posisjon for butikker nær deg.")) }
            }
            if (hits == null && !Deals.loading && Deals.fetchedAt == 0L && results.isEmpty()) {
                item {
                    Text(
                        if (ListRepo.open.isEmpty()) tr("Add something to a list, and deals are checked by themselves.", "Legg til noe på en liste, så sjekkes tilbud av seg selv.")
                        else tr("Checking deals…", "Sjekker tilbud …"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            }

            if (hits != null) Unit
            else if (tab.isEmpty()) {
                items(results.sortedByDescending { it.offers.size }, key = { it.item }) { Box(Modifier.animateItem()) { ItemDealsCard(it) { o -> zoom = o } } }
            } else {
                val here = results.flatMap { d -> d.offers.filter { it.store == tab }.map { d.item to it } }
                item {
                    Text(tr("${here.map { it.first }.distinct().size} of your items on sale at $tab", "${here.map { it.first }.distinct().size} av varene dine på tilbud hos $tab"),
                        style = MaterialTheme.typography.titleMedium)
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
                        Column(Modifier.padding(vertical = 6.dp)) {
                            here.forEachIndexed { i, (item, o) ->
                                if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                OfferRow(o, forItem = item) { zoom = o }
                            }
                        }
                    }
                }
                val total = here.groupBy { it.first }.values.sumOf { l -> l.minOf { it.second.price } }
                item {
                    Text(tr("Cheapest deal per item here adds up to ${kr(total)}", "Billigste tilbud per vare her blir til sammen ${kr(total)}"),
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (hits == null && Deals.fetchedAt > 0) {
                item {
                    Text(
                        (if (Deals.chains.isNotEmpty()) tr("Searched this week's flyers from ", "Søkte i ukens tilbudsaviser fra ") +
                            Deals.chains.joinToString(", ") + ". " else "") +
                            tr("Deals from eTilbudsavis (Tjek). ", "Tilbud fra eTilbudsavis (Tjek). ") +
                            if (Settings.kassalKey(ctx).isNotBlank()) tr("Shelf prices from Kassalapp.", "Hyllepriser fra Kassalapp.")
                            else tr("Add a free Kassalapp key in Settings to also compare everyday prices.", "Legg inn en gratis Kassalapp-nøkkel i Innstillinger for å også sammenligne vanlige priser."),
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    zoom?.let { OfferImageViewer(it) { zoom = null } }
    if (showFollows) FollowsSheet(onOpenCategory = { showFollows = false; category = it }) { showFollows = false }
    category?.let { c -> CategorySheet(c, onClose = { category = null }, onImage = { zoom = it }) }
}

/** Everything you follow for deal alerts: remove, or follow something new by name. */
@Composable
private fun FollowsSheet(onOpenCategory: (com.handleliste.app.data.Category) -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf("") }
    val perm = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }
    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(tr("Deal alerts", "Tilbudsvarsler"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, tr("Close", "Lukk")) }
                }
                Text(tr("You get a notification when something you follow is on sale near you. Checked every 6 hours.",
                    "Du får et varsel når noe du følger er på tilbud nær deg. Sjekkes hver 6. time."),
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(name, { name = it }, Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(14.dp),
                        placeholder = { Text(tr("e.g. coffee, Jacobs pesto", "f.eks. kaffe, Jacobs pesto")) })
                    Spacer(Modifier.width(6.dp))
                    IconButton(onClick = {
                        if (android.os.Build.VERSION.SDK_INT >= 33) perm.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        val words = Parse.keywords(name)
                        Alerts.follow(ctx, name, if (words.size >= 2) Parse.pinFromType(name) else null); name = ""
                    }, enabled = name.isNotBlank()) { Icon(Icons.Filled.Add, tr("Follow", "Følg"), tint = MaterialTheme.colorScheme.primary) }
                }
                Spacer(Modifier.height(8.dp))
                if (Alerts.follows.isEmpty()) {
                    Text(tr("Nothing yet. Tap 🔔 on an item, or add one above.", "Ingenting ennå. Trykk 🔔 på en vare, eller legg til over."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
                }
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    Alerts.follows.forEach { f ->
                        val onSale = Deals.results.firstOrNull { it.item.equals(f.name, true) }?.offers?.minByOrNull { it.price }
                        val cat = com.handleliste.app.data.Categories.byId(f.cat)
                        Row(Modifier.fillMaxWidth().clickable(enabled = cat != null) { cat?.let(onOpenCategory) }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                                if (cat != null) Text(cat.emoji, fontSize = 22.sp)
                                else if (f.pin?.image != null) AsyncImage(f.pin.image, null, Modifier.fillMaxSize().padding(2.dp), contentScale = ContentScale.Fit)
                                else Icon(Icons.Filled.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(f.name, fontWeight = FontWeight.SemiBold)
                                Text(when {
                                    cat != null && f.picks.isEmpty() -> tr("Category · every deal", "Kategori · alle tilbud")
                                    cat != null -> f.picks.joinToString(", ") { it.name }
                                    else -> f.pin?.name ?: tr("Any kind", "Alle typer")
                                }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (onSale != null) Text("🏷️ ${kr(onSale.price)} · ${onSale.store}", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                            }
                            TextButton(onClick = { Alerts.unfollow(ctx, f.name) }) { Text(tr("Stop", "Stopp")) }
                        }
                    }
                }
            }
        }
    }
}

/** A search result: the deal, and "+ Liste" to put it on the list you're on. */
@Composable
private fun SearchHit(o: Offer, onImage: () -> Unit) {
    var added by remember(o.id) { mutableStateOf(false) }
    Column {
        OfferRow(o, onImage = onImage)
        Row(Modifier.fillMaxWidth().padding(start = 90.dp, end = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            val name = o.heading.lowercase().replaceFirstChar { it.uppercase() }
            TextButton(onClick = {
                ListRepo.add(name)
                ListRepo.items.firstOrNull { it.name.equals(name, true) }?.let { ListRepo.setPin(it.id, Parse.pinFromOffer(o)) }
                added = true
            }, enabled = !added) {
                Icon(if (added) Icons.Filled.Check else Icons.Filled.Add, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp))
                Text(if (added) tr("On your list", "På listen") else tr("Add to list", "Legg i listen"), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ListTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick, shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.List, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
        }
    }
}

@Composable
private fun StoreTab(label: String, count: Int?, selected: Boolean, onClick: () -> Unit) {
    val bg by androidx.compose.animation.animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, label = "store")
    val fg by androidx.compose.animation.animateColorAsState(if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, label = "storeText")
    Surface(
        onClick = onClick, shape = RoundedCornerShape(50),
        color = bg,
        contentColor = fg,
        tonalElevation = 1.dp
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontWeight = FontWeight.SemiBold, maxLines = 1)
            if (count != null) { Spacer(Modifier.width(6.dp)); Text("$count", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

/** The offer's picture full screen: pinch to zoom, drag to look around, tap ✕ or back to close. */
@Composable
private fun OfferImageViewer(o: Offer, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val state = rememberTransformableState { z, pan, _ ->
            scale = (scale * z).coerceIn(1f, 5f)
            offset = if (scale == 1f) Offset.Zero else offset + pan
        }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AsyncImage(
                model = o.imageLarge ?: o.image, contentDescription = o.heading,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(bottom = 120.dp, top = 56.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
                    .transformable(state)
                    .pointerInput(Unit) { detectTapGestures(onDoubleTap = { if (scale > 1f) { scale = 1f; offset = Offset.Zero } else scale = 2.5f }) }
            )
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp)) {
                Icon(Icons.Filled.Close, tr("Close", "Lukk"), tint = Color.White)
            }
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color.Black.copy(alpha = 0.6f)).navigationBarsPadding().padding(16.dp)) {
                Text(o.store, color = Sun, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(o.heading, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (o.description.isNotBlank()) Text(o.description, color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(kr(o.price), color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
                    o.prePrice?.let {
                        Spacer(Modifier.width(10.dp))
                        Text(kr(it), color = Color.White.copy(alpha = 0.6f), textDecoration = TextDecoration.LineThrough)
                    }
                    val till = shortDate(o.runTill)
                    if (till.isNotBlank()) { Spacer(Modifier.weight(1f)); Text(tr("Until $till", "Til og med $till"), color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp) }
                }
            }
        }
    }
}

@Composable
private fun Note(text: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Info, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text(text, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ItemDealsCard(d: ItemDeals, onImage: (Offer) -> Unit) {
    var showOthers by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(vertical = 12.dp)) {
            Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                d.pin?.let { p ->
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                        if (p.image != null) AsyncImage(p.image, null, Modifier.fillMaxSize().padding(2.dp), contentScale = ContentScale.Fit)
                        else Icon(Icons.AutoMirrored.Filled.Label, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(10.dp))
                }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(d.item, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(6.dp))
                        FollowButton(d.item, d.pin, withLabel = false)
                    }
                    d.pin?.let { Text(tr("Only: ", "Bare: ") + it.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                if (d.offers.isNotEmpty()) {
                    Box(Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.secondary).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text(tr("${d.offers.size} deals", "${d.offers.size} tilbud"), color = MaterialTheme.colorScheme.onSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (d.offers.isEmpty()) {
                Text(if (d.pin != null) tr("No deals on your choice this week", "Ingen tilbud på det du har valgt denne uken")
                    else tr("No deals this week", "Ingen tilbud denne uken"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp))
            }
            d.offers.take(5).forEachIndexed { i, o ->
                if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                OfferRow(o) { onImage(o) }
            }
            if (d.others.isNotEmpty()) {
                TextButton(onClick = { showOthers = !showOthers }, modifier = Modifier.padding(horizontal = 4.dp)) {
                    Text((if (showOthers) "▾ " else "▸ ") + tr("Other ${d.term} on sale (${d.others.size})", "Andre tilbud på ${d.term} (${d.others.size})"))
                }
                if (showOthers) d.others.take(8).forEachIndexed { i, o ->
                    if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    OfferRow(o) { onImage(o) }
                }
            }
            if (d.prices.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(tr("Cheapest on the shelf today", "Billigst i hyllen i dag"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 14.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    d.prices.take(8).forEach { p ->
                        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(p.chain, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(kr(p.price), fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfferRow(o: Offer, forItem: String? = null, onImage: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(Color.White)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)).clickable(onClick = onImage), contentAlignment = Alignment.Center) {
            if (o.image != null) AsyncImage(o.image, tr("Show larger", "Vis større"), Modifier.size(60.dp), contentScale = ContentScale.Fit)
            else Text("🏷️", fontSize = 22.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if (forItem != null) forItem.uppercase() else o.store, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(o.heading, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 15.sp)
            val till = shortDate(o.runTill)
            if (till.isNotBlank()) Text(tr("Until $till", "Til og med $till"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(kr(o.price), fontWeight = FontWeight.Black, fontSize = 17.sp, color = MaterialTheme.colorScheme.secondary)
            o.prePrice?.let {
                Text(kr(it), fontSize = 12.sp, textDecoration = TextDecoration.LineThrough, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            o.savingsPct?.let {
                if (it in 1..95) Text("−$it %", Modifier.padding(top = 2.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.secondary)
                    .padding(horizontal = 6.dp, vertical = 1.dp), fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSecondary)
            }
        }
    }
}
