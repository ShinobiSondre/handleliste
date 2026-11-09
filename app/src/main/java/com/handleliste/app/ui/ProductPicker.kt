package com.handleliste.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.handleliste.app.data.Catalog
import com.handleliste.app.data.Alerts
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import com.handleliste.app.data.Flyers
import com.handleliste.app.data.ListRepo
import com.handleliste.app.data.Offer
import com.handleliste.app.data.Parse
import com.handleliste.app.data.Pin
import com.handleliste.app.data.Product
import com.handleliste.app.util.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Which list item is having its product chosen (the picker covers the app while set). */
object PickerUi { var itemId by mutableStateOf<String?>(null) }

/**
 * "Which chicken?": this week's deals and the full grocery range (with pictures) for an item.
 * Pick a product, or just a type ("pesto rosso"), and deals for the item follow your choice.
 */
@Composable
fun ProductPicker() {
    val id = PickerUi.itemId ?: return
    val item = ListRepo.item(id) ?: run { PickerUi.itemId = null; return }
    val ctx = LocalContext.current
    var query by remember(id) { mutableStateOf(Parse.searchTerm(item.name)) }
    var products by remember(id) { mutableStateOf<List<Product>>(emptyList()) }
    var loading by remember(id) { mutableStateOf(false) }
    var failed by remember(id) { mutableStateOf(false) }
    var offers by remember(id) { mutableStateOf<List<Offer>>(emptyList()) }
    LaunchedEffect(id, query) {
        // Matching against this week's ~2000 offers runs off the main thread.
        offers = withContext(Dispatchers.Default) {
            val all = Flyers.cachedAsync(ctx)
            Parse.matchOffers(Parse.searchTerm(query), all, limit = 30)
        }
    }
    LaunchedEffect(id, query) {
        delay(350)
        loading = true; failed = false
        products = runCatching { Catalog.search(query) }.onFailure { failed = true }.getOrDefault(emptyList())
        loading = false
    }
    fun choose(p: Pin?) { ListRepo.setPin(id, p); PickerUi.itemId = null }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).pointerInput(Unit) { detectTapGestures { } }) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Which one?", "Hvilken?"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(item.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FollowButton(item.name, item.pin)
                IconButton(onClick = { PickerUi.itemId = null }) { Icon(Icons.Filled.Close, tr("Close", "Lukk")) }
            }
            OutlinedTextField(
                query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true, shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                placeholder = { Text(tr("Search, e.g. pesto rosso", "Søk, f.eks. pesto rosso")) }
            )
            LazyVerticalGrid(
                GridCells.Adaptive(150.dp), Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        item.pin?.let { cur ->
                            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Thumb(cur.image, 44)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(tr("Chosen now", "Valgt nå"), fontSize = 12.sp)
                                        Text(cur.name, fontWeight = FontWeight.SemiBold, maxLines = 2)
                                    }
                                    TextButton(onClick = { choose(null) }) { Text(tr("Any kind", "Hvilken som helst")) }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                        if (query.isNotBlank()) {
                            OutlinedButton(onClick = { choose(Parse.pinFromType(query)) }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.AutoMirrored.Filled.Label, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                                Text(tr("Just this type: \"${query.trim()}\" (any brand)", "Bare denne typen: «${query.trim()}» (alle merker)"),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                if (offers.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) { Header(tr("🏷️ On sale this week", "🏷️ På tilbud denne uken")) }
                    items(offers, key = { "o" + it.id }) { o -> OfferTile(o) { choose(Parse.pinFromOffer(o)) } }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Header(tr("All products", "Alle varer"))
                        Spacer(Modifier.width(8.dp))
                        if (loading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    }
                }
                if (failed) item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(tr("Couldn't load products. Check your internet.", "Fikk ikke hentet varer. Sjekk internett."), color = MaterialTheme.colorScheme.error)
                } else if (!loading && products.isEmpty() && query.length >= 2) item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(tr("No products found. Try another word.", "Fant ingen varer. Prøv et annet ord."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(products, key = { "p" + it.name + it.extra }) { p -> ProductTile(p) { choose(Parse.pinFromProduct(p)) } }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(tr("Product range and pictures from MENY and Oda. Deals from eTilbudsavis.", "Sortiment og bilder fra MENY og Oda. Tilbud fra eTilbudsavis."),
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** 🔔 "Tell me when it's on sale": follows the item (and the product you picked for it). */
@Composable
fun FollowButton(name: String, pin: Pin?, withLabel: Boolean = true) {
    val ctx = LocalContext.current
    val on = Alerts.follows.any { it.name.equals(name, true) }
    val perm = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }
    val bg by androidx.compose.animation.animateColorAsState(
        if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, label = "bell")
    Surface(
        onClick = {
            if (on) Alerts.unfollow(ctx, name) else {
                if (android.os.Build.VERSION.SDK_INT >= 33) perm.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                Alerts.follow(ctx, name, pin)
            }
        },
        shape = RoundedCornerShape(50), color = bg,
        contentColor = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (on) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsNone, null, Modifier.size(18.dp))
            if (withLabel) { Spacer(Modifier.width(6.dp)); Text(if (on) tr("Following", "Følger") else tr("Alert me", "Varsle meg"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun Header(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun Thumb(url: String?, sizeDp: Int) {
    Box(Modifier.size(sizeDp.dp).clip(RoundedCornerShape(8.dp)).background(Color.White), contentAlignment = Alignment.Center) {
        if (url != null) AsyncImage(url, null, Modifier.fillMaxSize().padding(2.dp), contentScale = ContentScale.Fit)
        else Icon(Icons.AutoMirrored.Filled.Label, null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun Tile(image: String?, title: String, sub: String, price: String, priceColor: Color, badge: String?, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp,
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(1.2f).background(Color.White), contentAlignment = Alignment.Center) {
                if (image != null) AsyncImage(image, title, Modifier.fillMaxSize().padding(8.dp), contentScale = ContentScale.Fit)
                else Text("🛒", fontSize = 30.sp)
                if (badge != null) Text(badge, Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.secondary).padding(horizontal = 6.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.onSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.padding(10.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                if (sub.isNotBlank()) Text(sub, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (price.isNotBlank()) Text(price, fontWeight = FontWeight.Bold, color = priceColor, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun OfferTile(o: Offer, onClick: () -> Unit) {
    val brand = Parse.brandOf(o)
    Tile(o.image, o.heading.lowercase().replaceFirstChar { it.uppercase() }, listOf(o.store, brand).filter { it.isNotBlank() }.joinToString(" · "),
        kr(o.price), MaterialTheme.colorScheme.secondary, o.savingsPct?.takeIf { it in 1..95 }?.let { "−$it %" } ?: tr("Deal", "Tilbud"), onClick)
}

@Composable
private fun ProductTile(p: Product, onClick: () -> Unit) {
    Tile(p.image, p.name, listOf(p.extra, p.source).filter { it.isNotBlank() }.joinToString(" · "),
        p.price?.let { "≈ " + kr(it) }.orEmpty(), MaterialTheme.colorScheme.onSurface, null, onClick)
}
