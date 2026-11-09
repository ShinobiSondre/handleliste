package com.handleliste.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
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
            Hero(open.size, done.size, ListRepo.current?.name.orEmpty(), ListRepo.current?.image, onSettings)
            Spacer(Modifier.height(12.dp))
            // One tab per list; hold a tab to rename or delete it.
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ListRepo.lists, key = { it.id }) { l ->
                    val selected = l.id == ListRepo.currentId
                    val count = ListRepo.itemsOf(l.id).count { !it.checked }
                    val bg by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, tween(250), label = "tab")
                    val fg by animateColorAsState(if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, tween(250), label = "tabText")
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = bg,
                        contentColor = fg,
                        tonalElevation = 1.dp,
                        modifier = Modifier.clip(RoundedCornerShape(50))
                            .combinedClickable(onClick = { ListRepo.select(l.id) }, onLongClick = { editing = l })
                    ) {
                        Row(Modifier.padding(start = if (l.image != null) 6.dp else 14.dp, end = 14.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (l.image != null) {
                                AsyncImage(l.image, null, Modifier.size(24.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                                Spacer(Modifier.width(6.dp))
                            }
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

        items(open, key = { it.id }) { Box(Modifier.animateItem()) { ItemRow(it) } }

        if (done.isNotEmpty()) {
            item(key = "done-header") {
                Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("In the cart (${done.size})", "I kurven (${done.size})"), style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    TextButton(onClick = { ListRepo.clearChecked() }) { Text(tr("Clear", "Fjern")) }
                }
            }
            items(done, key = { it.id }) { Box(Modifier.animateItem()) { ItemRow(it) } }
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

/** The top of the list: greeting, list name, a progress ring that fills as you tick things off. */
@Composable
private fun Hero(open: Int, done: Int, listName: String, image: String?, onSettings: () -> Unit) {
    val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greet = when (hour) {
        in 5..9 -> tr("Good morning", "God morgen")
        in 10..16 -> tr("Hi there", "Hei")
        in 17..21 -> tr("Good evening", "God kveld")
        else -> tr("Late-night shopping?", "Sen handletur?")
    }
    val total = open + done
    val progress by animateFloatAsState(if (total == 0) 0f else done.toFloat() / total, tween(700, easing = FastOutSlowInEasing), label = "progress")
    val dark = isSystemInDarkTheme()
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(if (dark) listOf(Color(0xFF1E5A3B), Color(0xFF0F3322)) else listOf(Color(0xFF2E9E66), Color(0xFF145C39))))
    ) {
        // A list made from a dish shows the dish's photo behind a dark-green veil.
        if (image != null) {
            AsyncImage(image, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color(0xF00F3322), Color(0x990F3322)))))
        }
        // Soft decorative circles.
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(alpha = 0.07f), radius = size.height * 0.9f, center = Offset(size.width * 0.95f, -size.height * 0.1f))
            drawCircle(Color.White.copy(alpha = 0.05f), radius = size.height * 0.55f, center = Offset(size.width * 0.12f, size.height * 1.15f))
        }
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(greet, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(listName.ifBlank { tr("Shopping list", "Handleliste") }, color = Color.White, fontSize = 28.sp,
                    fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                AnimatedContent(open, transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) }, label = "count") { n ->
                    Text(
                        when (n) {
                            0 -> if (total > 0) tr("All done 🎉", "Alt er handlet 🎉") else tr("Nothing to buy yet", "Ingenting å kjøpe ennå")
                            1 -> tr("1 thing to buy", "1 ting å kjøpe")
                            else -> tr("$n things to buy", "$n ting å kjøpe")
                        },
                        color = Sun, fontSize = 15.sp, fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(10.dp))
                WatchPill(onClick = onSettings, onDark = true)
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(78.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = 8.dp.toPx()
                    drawArc(Color.White.copy(alpha = 0.18f), 0f, 360f, false, style = Stroke(w, cap = StrokeCap.Round),
                        topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w))
                    drawArc(Sun, -90f, 360f * progress, false, style = Stroke(w, cap = StrokeCap.Round),
                        topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$done/$total", color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
                    Text(tr("in cart", "i kurven"), color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun WatchPill(onClick: () -> Unit, onDark: Boolean = false) {
    val on = WatchState.on
    Surface(
        onClick = onClick, shape = RoundedCornerShape(50),
        color = when {
            onDark -> Color.White.copy(alpha = if (on) 0.22f else 0.12f)
            on -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (onDark) Color.White else MaterialTheme.colorScheme.onSurface
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (on) Icons.Filled.LocationOn else Icons.Outlined.LocationOff, null, Modifier.size(16.dp),
                tint = if (onDark) (if (on) Sun else Color.White.copy(alpha = 0.7f)) else if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            Text(if (on) tr("Store alert on", "Butikkvarsel på") else tr("Store alert off", "Butikkvarsel av"),
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmptyList() {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val float = rememberInfiniteTransition(label = "float")
        val dy by float.animateFloat(-5f, 5f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "dy")
        Box(Modifier.offset(y = dy.dp).size(84.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
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
    val pin = item.pin
    // Ticking an item: a quick pop, then it settles slightly faded.
    val fade by animateFloatAsState(if (item.checked) 0.62f else 1f, tween(300), label = "fade")
    var popped by remember(item.id) { mutableStateOf(item.checked) }
    val pop by animateFloatAsState(if (popped != item.checked) 1.04f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "pop",
        finishedListener = { popped = item.checked })
    Surface(
        Modifier.fillMaxWidth().padding(vertical = 3.dp).graphicsLayer { alpha = fade; scaleX = pop; scaleY = pop }.animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        color = if (item.checked) Color.Transparent else MaterialTheme.colorScheme.surface,
        tonalElevation = if (item.checked) 0.dp else 1.dp,
        shadowElevation = if (item.checked || big) 0.dp else 1.dp,
    ) {
        Row(
            // In the list, tapping an item lets you choose exactly which product; in the store, it ticks it off.
            Modifier.clickable { if (big) ListRepo.toggle(item.id) else PickerUi.itemId = item.id }
                .padding(start = 4.dp, end = 4.dp, top = if (big) 6.dp else 0.dp, bottom = if (big) 6.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = item.checked, onCheckedChange = { ListRepo.toggle(item.id) })
            if (pin != null) {
                Box(
                    Modifier.size(if (big) 56.dp else 46.dp).clip(RoundedCornerShape(10.dp)).background(Color.White)
                        .clickable { PickerUi.itemId = item.id },
                    contentAlignment = Alignment.Center
                ) {
                    if (pin.image != null) AsyncImage(pin.image, pin.name, Modifier.fillMaxSize().padding(3.dp), contentScale = ContentScale.Fit)
                    else Icon(Icons.AutoMirrored.Filled.Label, null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.name, fontSize = if (big) 22.sp else 17.sp,
                    fontWeight = if (big) FontWeight.SemiBold else FontWeight.Normal,
                    textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                    color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                )
                if (com.handleliste.app.data.Alerts.isFollowed(item.name)) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.NotificationsActive, tr("Following deals", "Følger tilbud"), Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                }
                }
                if (pin != null) {
                    Text(pin.name + if (pin.detail.isNotBlank()) " · ${pin.detail}" else "", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (deal != null && !item.checked) {
                    Text(
                        "🏷️ ${kr(deal.price)} · ${deal.store}",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold, maxLines = 1
                    )
                }
            }
            if (!big) {
                if (pin == null && !item.checked) IconButton(onClick = { PickerUi.itemId = item.id }) {
                    Icon(Icons.Outlined.ImageSearch, tr("Choose product", "Velg vare"), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { ListRepo.remove(item.id) }) {
                    Icon(Icons.Filled.Close, tr("Remove", "Fjern"), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
