package com.handleliste.app.ui

import androidx.compose.foundation.background
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
suspend fun runDealsCheck(ctx: android.content.Context) {
    val loc = currentLocation(ctx)
    Deals.check(ctx, ListRepo.open.map { it.name }, loc?.first, loc?.second)
}

@Composable
fun DealsScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val results = Deals.results

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Deals", "Tilbud"), style = MaterialTheme.typography.headlineMedium)
                    val sub = when {
                        Deals.loading -> tr("Looking through this week's flyers…", "Ser gjennom ukens tilbudsaviser …")
                        Deals.fetchedAt > 0 -> tr("Within ${Settings.radiusKm(ctx)} km · updated ", "Innen ${Settings.radiusKm(ctx)} km · oppdatert ") +
                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(Deals.fetchedAt))
                        else -> tr("For what's on your list", "For det som står på listen")
                    }
                    Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { scope.launch { runDealsCheck(ctx) } }, enabled = !Deals.loading) {
                    Icon(Icons.Filled.Refresh, tr("Check again", "Sjekk igjen"))
                }
            }
            if (Deals.loading) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)))
            }
        }

        Deals.error?.let { e -> item { Note(e) } }
        if (Deals.usedFallbackLocation && Deals.fetchedAt > 0) {
            item { Note(tr("Location is off, so these are deals around Oslo. Turn on location for stores near you.",
                "Posisjon er av, så dette er tilbud rundt Oslo. Slå på posisjon for butikker nær deg.")) }
        }

        if (!Deals.loading && Deals.fetchedAt == 0L && results.isEmpty()) {
            item {
                Text(
                    if (ListRepo.open.isEmpty()) tr("Add something to your list first, then check for deals.", "Legg til noe på listen først, så sjekker vi tilbud.")
                    else tr("Tap ↻ to check this week's deals for your list.", "Trykk ↻ for å sjekke ukens tilbud for listen din."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        }

        val best = Deals.bestStores().take(3)
        if (best.isNotEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.EmojiEvents, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(tr("Most of your list on sale", "Flest av varene dine på tilbud"), fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text(best.joinToString("  ·  ") { (s, n) -> "$s ($n)" }, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }
            }
        }

        items(results.sortedByDescending { it.offers.size }, key = { it.item }) { ItemDealsCard(it) }

        if (Deals.fetchedAt > 0) {
            item {
                Text(
                    tr("Deals from eTilbudsavis (Tjek). ", "Tilbud fra eTilbudsavis (Tjek). ") +
                        if (Settings.kassalKey(ctx).isNotBlank()) tr("Shelf prices from Kassalapp.", "Hyllepriser fra Kassalapp.")
                        else tr("Add a free Kassalapp key in Settings to also compare everyday prices.", "Legg inn en gratis Kassalapp-nøkkel i Innstillinger for å også sammenligne vanlige priser."),
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
private fun ItemDealsCard(d: ItemDeals) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(vertical = 12.dp)) {
            Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(d.item, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (d.offers.isNotEmpty()) {
                    Box(Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.secondary).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text(tr("${d.offers.size} deals", "${d.offers.size} tilbud"), color = MaterialTheme.colorScheme.onSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (d.offers.isEmpty()) {
                Text(tr("No deals this week", "Ingen tilbud denne uken"), color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp))
            }
            d.offers.take(5).forEachIndexed { i, o ->
                if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
                OfferRow(o)
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
private fun OfferRow(o: Offer) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(Color.White), contentAlignment = Alignment.Center) {
            if (o.image != null) AsyncImage(o.image, null, Modifier.size(48.dp), contentScale = ContentScale.Fit)
            else Text("🏷️", fontSize = 22.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(o.store, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
            o.savingsPct?.let { if (it in 1..95) Text("−$it %", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary) }
        }
    }
}
