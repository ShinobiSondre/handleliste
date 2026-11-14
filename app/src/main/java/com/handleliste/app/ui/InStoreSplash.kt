package com.handleliste.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.window.Dialog
import com.handleliste.app.data.Offer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.handleliste.app.data.Deals
import com.handleliste.app.data.ListRepo
import com.handleliste.app.util.tr
import com.handleliste.app.watch.StoreAlert

/** Full-screen "you're in the store" list: big tick boxes, and what's on sale in this store. */
@Composable
fun InStoreSplash() {
    val ctx = LocalContext.current
    val store = StoreAlert.splash
    // Fresh deals for this trip, if the last check is old.
    LaunchedEffect(store) {
        if (store != null && ListRepo.open.isNotEmpty() && (Deals.results.isEmpty() || System.currentTimeMillis() - Deals.fetchedAt > 6 * 3600_000L)) {
            runCatching { runDealsCheck(ctx) }
        }
    }
    AnimatedVisibility(store != null, enter = fadeIn() + slideInVertically { it / 6 }, exit = fadeOut() + slideOutVertically { it / 6 }) {
        val name = store.orEmpty()
        // One tab per list that has something on it; the first with things left is opened.
        val tabs = ListRepo.lists.filter { ListRepo.itemsOf(it.id).isNotEmpty() }
        val open = tabs.flatMap { ListRepo.itemsOf(it.id) }.filter { !it.checked }
        val done = tabs.flatMap { ListRepo.itemsOf(it.id) }.filter { it.checked }
        val here = Deals.atStore(name)
        var sel by remember(store) { mutableStateOf(tabs.firstOrNull { l -> ListRepo.itemsOf(l.id).any { !it.checked } }?.id ?: tabs.firstOrNull()?.id) }
        if (tabs.none { it.id == sel }) sel = tabs.firstOrNull()?.id
        // Opens on the big deals page when something from your lists is on sale in this store; the list is one tap away.
        var chose by remember(store) { mutableStateOf<Int?>(null) }
        val page = chose ?: if (here.isNotEmpty() || Deals.loading) 0 else 1
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Compact header: store name and how much is left, all in one band.
            Box(
                Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Color(0xFF2E9E66), Color(0xFF145C39))))
                    .statusBarsPadding().padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.ShoppingCart, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr("You're in the store", "Du er i butikken"), color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (open.isEmpty()) tr("All done. Nice!", "Alt er handlet. Bra!")
                            else tr("${open.size} left to pick up", "${open.size} igjen å plukke"),
                            color = Color(0xFFFFD54F), fontSize = 14.sp, fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = { StoreAlert.splash = null }) { Icon(Icons.Filled.Close, tr("Close", "Lukk"), tint = Color.White) }
                }
            }
            AnimatedContent(page, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) }, label = "page",
                modifier = Modifier.weight(1f)) { pg ->
            Column(Modifier.fillMaxSize()) {
            if (pg == 0) {
                StoreDealsPage(name, here, left = open.size, onList = { chose = 1 })
            } else {
            if (here.isNotEmpty()) {
                Surface(onClick = { chose = 0 }, shape = RoundedCornerShape(50), color = Color(0xFFFFE082),
                    modifier = Modifier.padding(start = 16.dp, top = 10.dp)) {
                    Text(tr("🏷️ ${here.map { it.first }.distinct().size} deals here · see all", "🏷️ ${here.map { it.first }.distinct().size} tilbud her · se alle"),
                        Modifier.padding(horizontal = 14.dp, vertical = 7.dp), color = Color(0xFF4A3300), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            if (tabs.size > 1) {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tabs, key = { it.id }) { l ->
                        val left = ListRepo.itemsOf(l.id).count { !it.checked }
                        val on = l.id == sel
                        val bg by animateColorAsState(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, label = "tab")
                        val fg by animateColorAsState(if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, label = "tabText")
                        Surface(onClick = { sel = l.id }, shape = RoundedCornerShape(50), color = bg, contentColor = fg, tonalElevation = 1.dp) {
                            Row(Modifier.padding(start = if (l.image != null) 6.dp else 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                if (l.image != null) {
                                    AsyncImage(l.image, null, Modifier.size(26.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(l.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1)
                                Spacer(Modifier.width(6.dp))
                                Text(if (left == 0) "✓" else "$left", fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
            // The chosen list, laid out in columns when the screen is wide (e.g. the Fold opened).
            AnimatedContent(sel, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) }, label = "list", modifier = Modifier.weight(1f)) { id ->
                val its = id?.let { ListRepo.itemsOf(it) }.orEmpty()
                val left = its.filter { !it.checked }
                val inCart = its.filter { it.checked }
                val names = its.map { it.name }.toSet()
                val sale = here.filter { it.first in names }
                LazyVerticalGrid(GridCells.Adaptive(320.dp), Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = if (tabs.size > 1) 0.dp else 12.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (sale.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                        LazyRow(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(sale.take(10)) { (item, o) ->
                                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Text("🏷️ $item", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        Text("${kr(o.price)} · ${o.heading.lowercase()}", fontSize = 12.sp, maxLines = 1,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }
                            }
                        }
                    }
                    items(left, key = { it.id }) { Box(Modifier.animateItem()) { ItemRow(it, big = true) } }
                    if (left.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(tr("✓ Everything on this list is in the cart", "✓ Alt på denne listen er i kurven"),
                            Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                    if (inCart.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(tr("In the cart (${inCart.size})", "I kurven (${inCart.size})"), Modifier.padding(top = 12.dp, bottom = 4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        }
                        items(inCart, key = { it.id }) { Box(Modifier.animateItem()) { ItemRow(it, big = true) } }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                Button(
                    onClick = { ListRepo.clearChecked(all = true); StoreAlert.splash = null },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (done.isEmpty()) tr("Close", "Lukk") else tr("Done shopping · clear ${done.size} ticked", "Ferdig handlet · fjern ${done.size} avkryssede"), fontSize = 16.sp)
                }
            }
            }
            }
            }
        }
    }
}

/**
 * The big in-store deals page: everything from all your lists that's on sale in this very store,
 * one large card per item with picture, price and how much you save. Tap ✓ to put it in the cart.
 */
@Composable
private fun StoreDealsPage(store: String, here: List<Pair<String, Offer>>, left: Int, onList: () -> Unit) {
    var zoom by remember { mutableStateOf<Offer?>(null) }
    val clock = rememberSparkleClock()
    val allItems = ListRepo.lists.flatMap { l -> ListRepo.itemsOf(l.id).map { it to l } }
    val byItem = here.groupBy({ it.first }, { it.second }).mapValues { (_, l) -> l.distinctBy { it.id }.sortedBy { it.price } }
        .toList().sortedByDescending { (_, l) -> l.first().prePrice?.let { it - l.first().price } ?: 0.0 }
    val save = byItem.sumOf { (_, l) -> l.first().prePrice?.let { (it - l.first().price).coerceAtLeast(0.0) } ?: 0.0 }
    Column(Modifier.fillMaxSize()) {
        when {
            byItem.isEmpty() && Deals.loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(14.dp))
                    Text(tr("Finding deals at $store …", "Finner tilbud hos $store …"), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            byItem.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(tr("Nothing from your lists is on sale at $store this week.", "Ingenting fra listene dine er på tilbud hos $store denne uken."),
                    fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            else -> LazyVerticalGrid(GridCells.Adaptive(165.dp), Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    // Gold banner, Holmstead style.
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(GoldBrush).shimmer({ clock.value }, alpha = 0.35f)
                        .padding(horizontal = 18.dp, vertical = 14.dp)) {
                        Column {
                            Text(tr("🏷️ ${byItem.size} of your items are on sale here", "🏷️ ${byItem.size} av varene dine er på tilbud her"),
                                fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF4A3300))
                            if (save >= 1) Text(tr("You save about ${kr(save)}", "Du sparer ca. ${kr(save)}"),
                                fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B4A00))
                        }
                    }
                }
                items(byItem, key = { it.first }) { (itemName, offers) ->
                    val pair = allItems.firstOrNull { it.first.name == itemName && !it.first.checked } ?: allItems.firstOrNull { it.first.name == itemName }
                    BigDealCard(itemName, pair?.second?.name?.takeIf { ListRepo.lists.size > 1 }, offers, inCart = pair?.first?.checked == true,
                        onImage = { zoom = offers.first() }, onCart = { pair?.first?.let { ListRepo.toggle(it.id) } })
                }
            }
        }
        Button(onClick = onList, modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(54.dp)) {
            Text(tr("See shopping list · $left left", "Se handlelisten · $left igjen"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
    zoom?.let { o ->
        Dialog(onDismissRequest = { zoom = null }) {
            Surface(onClick = { zoom = null }, shape = RoundedCornerShape(20.dp), color = Color.White) {
                AsyncImage(o.imageLarge ?: o.image, o.heading, Modifier.fillMaxWidth().height(420.dp).padding(12.dp), contentScale = ContentScale.Fit)
            }
        }
    }
}

@Composable
private fun BigDealCard(item: String, list: String?, offers: List<Offer>, inCart: Boolean, onImage: () -> Unit, onCart: () -> Unit) {
    val o = offers.first()
    val fade by animateFloatAsState(if (inCart) 0.45f else 1f, label = "cart")
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, shadowElevation = 2.dp,
        modifier = Modifier.graphicsLayer { alpha = fade }) {
        Column {
            Box(Modifier.fillMaxWidth().height(150.dp).background(Color.White).clickable(onClick = onImage), contentAlignment = Alignment.Center) {
                if (o.image != null) AsyncImage(o.image, o.heading, Modifier.fillMaxSize().padding(10.dp), contentScale = ContentScale.Fit)
                else Text("🏷️", fontSize = 40.sp)
                o.savingsPct?.takeIf { it in 1..95 }?.let {
                    Text("−$it %", Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondary).padding(horizontal = 8.dp, vertical = 3.dp),
                        color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
            }
            Column(Modifier.padding(12.dp)) {
                Text(item.uppercase() + (list?.let { " · $it" } ?: ""), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(o.heading.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 19.sp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(kr(o.price), fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                        o.prePrice?.takeIf { it > o.price }?.let {
                            Text(kr(it), fontSize = 12.sp, textDecoration = TextDecoration.LineThrough, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (offers.size > 1) Text(tr("+${offers.size - 1} more deals", "+${offers.size - 1} tilbud til"), fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(onClick = onCart, shape = CircleShape, modifier = Modifier.size(44.dp),
                        color = if (inCart) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Check, if (inCart) tr("In the cart", "I kurven") else tr("Put in cart", "Legg i kurven"),
                                tint = if (inCart) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
