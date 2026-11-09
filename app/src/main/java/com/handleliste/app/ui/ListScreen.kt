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
import com.handleliste.app.data.ShopList
import com.handleliste.app.data.History
import com.handleliste.app.data.Suggestion
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.remember
import com.handleliste.app.util.tr

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListScreen(onOpenDeals: () -> Unit, onDinner: () -> Unit, onSettings: () -> Unit) {
    val ctx = LocalContext.current
    var text by rememberSaveable { mutableStateOf("") }
    var showIntro by rememberSaveable { mutableStateOf(!Settings.onboarded(ctx)) }
    var editing by remember { mutableStateOf<ShopList?>(null) }
    var creating by remember { mutableStateOf(false) }
    val enableWatch = rememberEnableWatch()
    val open = ListRepo.items.filter { !it.checked }
    val done = ListRepo.items.filter { it.checked }
    val suggestions = remember(History.version, ListRepo.open.size) { History.suggestions(ctx) }

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
            Spacer(Modifier.height(12.dp))
            // One tab per list; hold a tab to rename or delete it.
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ListRepo.lists, key = { it.id }) { l ->
                    val selected = l.id == ListRepo.currentId
                    val count = ListRepo.itemsOf(l.id).count { !it.checked }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.clip(RoundedCornerShape(50))
                            .combinedClickable(onClick = { ListRepo.select(l.id) }, onLongClick = { editing = l })
                    ) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(l.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            if (count > 0) { Spacer(Modifier.width(6.dp)); Text("$count", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
                item {
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant, onClick = { creating = true }) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp))
                            Text(tr("New list", "Ny liste"), fontSize = 14.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
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
            DealsSummary(onOpenDeals)
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = onDinner, modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)) {
                Icon(Icons.Filled.Restaurant, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text(tr("What can I make for dinner?", "Hva kan jeg lage til middag?"), maxLines = 1)
            }
            if (suggestions.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Suggestions(suggestions)
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

    if (creating) NameDialog(tr("New list", "Ny liste"), "", onDismiss = { creating = false }, onSave = { ListRepo.newList(it); creating = false })
    editing?.let { l ->
        NameDialog(tr("Edit list", "Endre liste"), l.name,
            onDismiss = { editing = null },
            onSave = { ListRepo.renameList(l.id, it); editing = null },
            onDelete = if (ListRepo.lists.size > 1) ({ ListRepo.deleteList(l.id); editing = null }) else null)
    }
}

@Composable
private fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit, onDelete: (() -> Unit)? = null) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(tr("e.g. Cabin trip, Taco Friday", "f.eks. Hytta, Tacofredag")) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
                if (onDelete != null) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDelete) { Text(tr("Delete this list", "Slett denne listen"), color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text(tr("Save", "Lagre")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel", "Avbryt")) } }
    )
}

/** Deals are checked by themselves; this shows the result and opens the Deals tab. */
@Composable
private fun DealsSummary(onOpen: () -> Unit) {
    val onSale = Deals.results.filter { it.offers.isNotEmpty() }
    val best = Deals.bestStores().firstOrNull()
    Surface(onClick = onOpen, shape = RoundedCornerShape(14.dp),
        color = if (onSale.isNotEmpty()) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocalOffer, null, Modifier.size(20.dp),
                tint = if (onSale.isNotEmpty()) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        Deals.loading && onSale.isEmpty() -> tr("Checking this week's deals…", "Sjekker ukens tilbud …")
                        ListRepo.open.isEmpty() -> tr("Deals are checked as you add things", "Tilbud sjekkes når du legger til varer")
                        onSale.isEmpty() && Deals.fetchedAt > 0 -> tr("No deals on your list this week", "Ingen tilbud på listen din denne uken")
                        onSale.isEmpty() -> tr("Deals", "Tilbud")
                        else -> tr("${onSale.size} of your items are on sale", "${onSale.size} av varene dine er på tilbud")
                    },
                    fontWeight = FontWeight.SemiBold
                )
                if (best != null) Text(tr("Most at ${best.first} (${best.second})", "Flest hos ${best.first} (${best.second})"), fontSize = 13.sp)
            }
            if (Deals.loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
        }
    }
}

/** "You usually buy…": learned from what you tick off. */
@Composable
private fun Suggestions(list: List<Suggestion>) {
    val due = list.filter { it.due }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AutoAwesome, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text(tr("Suggested for you", "Forslag til deg"), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            if (due.size >= 2) TextButton(onClick = { ListRepo.add(due.joinToString(",") { it.name }) }) {
                Text(tr("Add ${due.size} due", "Legg til ${due.size} som trengs"))
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list, key = { it.name }) { s ->
                Surface(onClick = { ListRepo.add(s.name) }, shape = RoundedCornerShape(14.dp),
                    color = if (s.due) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text("+ ${s.name}", fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(
                            when {
                                s.everyDays == null -> tr("You've bought it before", "Kjøpt før")
                                s.everyDays >= 14 -> tr("About every ${s.everyDays / 7} weeks", "Ca. hver ${s.everyDays / 7}. uke")
                                s.everyDays >= 7 -> tr("About weekly", "Ca. hver uke")
                                else -> tr("About every ${s.everyDays} days", "Ca. hver ${s.everyDays}. dag")
                            },
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
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
