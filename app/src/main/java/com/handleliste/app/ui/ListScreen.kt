package com.handleliste.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.handleliste.app.data.Deals
import com.handleliste.app.data.Item
import com.handleliste.app.data.ListRepo
import com.handleliste.app.data.Settings
import com.handleliste.app.util.tr

@Composable
fun ListScreen(onCheckDeals: () -> Unit, onDinner: () -> Unit, onSettings: () -> Unit) {
    val ctx = LocalContext.current
    var text by rememberSaveable { mutableStateOf("") }
    var showIntro by rememberSaveable { mutableStateOf(!Settings.onboarded(ctx)) }
    val enableWatch = rememberEnableWatch()
    val open = ListRepo.items.filter { !it.checked }
    val done = ListRepo.items.filter { it.checked }

    fun add() {
        if (text.isNotBlank()) { ListRepo.add(text); text = "" }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 24.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Shopping list", "Handleliste"), style = MaterialTheme.typography.headlineMedium)
                    Text(
                        when (open.size) {
                            0 -> tr("Nothing to buy yet", "Ingenting å kjøpe ennå")
                            1 -> tr("1 thing to buy", "1 ting å kjøpe")
                            else -> tr("${open.size} things to buy", "${open.size} ting å kjøpe")
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                WatchPill(onClick = onSettings)
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(tr("Add items (milk, bread, eggs)", "Legg til (melk, brød, egg)")) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() }),
                trailingIcon = {
                    IconButton(onClick = { add() }, enabled = text.isNotBlank()) {
                        Icon(Icons.Filled.Add, tr("Add", "Legg til"))
                    }
                }
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onCheckDeals, modifier = Modifier.weight(1f), enabled = open.isNotEmpty(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)) {
                    Icon(Icons.Filled.LocalOffer, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text(tr("Check deals", "Sjekk tilbud"), maxLines = 1)
                }
                FilledTonalButton(onClick = onDinner, modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)) {
                    Icon(Icons.Filled.Restaurant, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text(tr("Dinner ideas", "Middagsforslag"), maxLines = 1)
                }
            }
            if (showIntro && !WatchState.on) {
                Spacer(Modifier.height(14.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(tr("Your list, the moment you walk in", "Listen din idet du går inn"), style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            tr("Handleliste can notice when you're in a grocery store and pop your list up on screen. It uses free map data and stays on your phone.",
                                "Handleliste kan merke når du er i en matbutikk og vise listen på skjermen. Den bruker gratis kartdata, og alt blir på telefonen."),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(10.dp))
                        Row {
                            Button(onClick = { Settings.setOnboarded(ctx); showIntro = false; enableWatch() }) { Text(tr("Turn on", "Slå på")) }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { Settings.setOnboarded(ctx); showIntro = false }) { Text(tr("Not now", "Ikke nå")) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        if (ListRepo.items.isEmpty()) {
            item { EmptyList() }
        }

        items(open, key = { it.id }) { ItemRow(it) }

        if (done.isNotEmpty()) {
            item(key = "done-header") {
                Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("In the cart (${done.size})", "I kurven (${done.size})"), style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    TextButton(onClick = { ListRepo.clearChecked() }) { Text(tr("Clear", "Fjern")) }
                }
            }
            items(done, key = { it.id }) { ItemRow(it) }
        }
    }
}

@Composable
private fun WatchPill(onClick: () -> Unit) {
    val on = WatchState.on
    Surface(
        onClick = onClick, shape = RoundedCornerShape(50),
        color = if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (on) Icons.Filled.LocationOn else Icons.Outlined.LocationOff, null, Modifier.size(16.dp),
                tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            Text(if (on) tr("Store alert on", "Butikkvarsel på") else tr("Store alert off", "Butikkvarsel av"),
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmptyList() {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(84.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.ShoppingCart, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(14.dp))
        Text(tr("Your list is empty", "Listen er tom"), style = MaterialTheme.typography.titleMedium)
        Text(tr("Type a few things above. Commas add several at once.", "Skriv noen varer over. Med komma legger du til flere på en gang."),
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ItemRow(item: Item, big: Boolean = false) {
    val deal = Deals.results.firstOrNull { it.item == item.name }?.offers?.minByOrNull { it.price }
    Surface(
        Modifier.fillMaxWidth().padding(vertical = 3.dp).animateContentSize(),
        shape = RoundedCornerShape(14.dp),
        color = if (item.checked) Color.Transparent else MaterialTheme.colorScheme.surface,
        tonalElevation = if (item.checked) 0.dp else 1.dp,
    ) {
        Row(
            Modifier.clickable { ListRepo.toggle(item.id) }.padding(start = 4.dp, end = 4.dp, top = if (big) 6.dp else 0.dp, bottom = if (big) 6.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = item.checked, onCheckedChange = { ListRepo.toggle(item.id) })
            Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                Text(
                    item.name, fontSize = if (big) 22.sp else 17.sp,
                    fontWeight = if (big) FontWeight.SemiBold else FontWeight.Normal,
                    textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                    color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                if (deal != null && !item.checked) {
                    Text(
                        "🏷️ ${kr(deal.price)} · ${deal.store}",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold, maxLines = 1
                    )
                }
            }
            if (!big) IconButton(onClick = { ListRepo.remove(item.id) }) {
                Icon(Icons.Filled.Close, tr("Remove", "Fjern"), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
