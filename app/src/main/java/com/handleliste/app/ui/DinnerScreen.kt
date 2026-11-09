package com.handleliste.app.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.handleliste.app.data.ChatGptBridge
import com.handleliste.app.data.Deals
import com.handleliste.app.data.Dinner
import com.handleliste.app.data.ListRepo
import com.handleliste.app.util.tr

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DinnerScreen() {
    val ctx = LocalContext.current
    var extra by rememberSaveable { mutableStateOf("") }
    var useDeals by rememberSaveable { mutableStateOf(true) }
    val wishes = remember { mutableStateListOf<String>() }
    val wishOptions = listOf(
        tr("Quick (under 30 min)", "Rask (under 30 min)"), tr("Vegetarian", "Vegetar"), tr("Kid-friendly", "Barnevennlig"),
        tr("Cheap", "Billig"), tr("Healthy", "Sunn"), tr("Cosy / comfort food", "Kos / hverdagsfavoritt"),
    )
    val turns by ChatGptBridge.turns.collectAsState()

    fun ask() {
        val list = ListRepo.open.map { it.name }
        val onSale = if (useDeals) Deals.results.filter { it.offers.isNotEmpty() }.map { it.item } else emptyList()
        val prompt = Dinner.prompt(list, extra, onSale, wishes.joinToString(", "))
        val shown = tr("What can I make for dinner?", "Hva kan jeg lage til middag?") +
            (if (list.isNotEmpty()) "\n" + list.joinToString(", ") else "") +
            (if (wishes.isNotEmpty()) "\n" + wishes.joinToString(" · ") else "")
        ChatGptBridge.reset()
        (ctx as? Activity)?.let { ChatGptBridge.ask(it, prompt, shown, prompt) }
        ChatUi.open = true
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(tr("What's for dinner?", "Hva blir det til middag?"), style = MaterialTheme.typography.headlineMedium)
        Text(tr("ChatGPT suggests dinners from your shopping list, right here in the app", "ChatGPT foreslår middager ut fra handlelisten din, rett her i appen"),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Text(tr("From your lists", "Fra listene dine"), fontWeight = FontWeight.SemiBold)
                val open = ListRepo.open
                Text(if (open.isEmpty()) tr("(nothing yet, so ChatGPT will suggest from scratch)", "(ingenting ennå, så ChatGPT foreslår fritt)")
                    else open.joinToString(", ") { it.name }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = extra, onValueChange = { extra = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text(tr("Already at home (optional)", "Har hjemme fra før (valgfritt)")) },
            placeholder = { Text(tr("rice, onions, half a cucumber", "ris, løk, en halv agurk")) },
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            wishOptions.forEach { w ->
                FilterChip(selected = w in wishes, onClick = { if (w in wishes) wishes.remove(w) else wishes.add(w) }, label = { Text(w) })
            }
        }
        if (Deals.results.any { it.offers.isNotEmpty() }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Prefer what's on sale", "Bruk gjerne det som er på tilbud"), Modifier.weight(1f))
                Switch(checked = useDeals, onCheckedChange = { useDeals = it })
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { ask() }, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Icon(Icons.Filled.AutoAwesome, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Text(tr("Ask ChatGPT for dinner ideas", "Spør ChatGPT om middag"), fontSize = 16.sp)
        }
        if (turns.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = { ChatUi.open = true }, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Open the last chat", "Åpne forrige samtale"))
            }
        }
    }
}

/** Whether the dinner chat is on screen (drawn over the whole app by MainActivity). */
object ChatUi { var open by mutableStateOf(false) }

/** The app's own chat with ChatGPT: answers stream in, follow-ups stay in the same conversation. */
@Composable
fun DinnerChat(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val turns by ChatGptBridge.turns.collectAsState()
    val status by ChatGptBridge.status.collectAsState()
    val error by ChatGptBridge.error.collectAsState()
    val pageUrl by ChatGptBridge.pageUrl.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val thinking = status == ChatGptBridge.Status.THINKING
    LaunchedEffect(turns.size, turns.lastOrNull()?.text?.length) {
        if (turns.isNotEmpty()) listState.animateScrollToItem(turns.size)
    }
    fun send(q: String) {
        if (q.isBlank() || thinking) return
        ChatGptBridge.followUp(q.trim()); input = ""
    }

    // Drawn inside the app's own window (not a dialog), so ChatGPT's hidden page keeps writing its answer.
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .pointerInput(Unit) { detectTapGestures { } }) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Restaurant, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr("Dinner ideas", "Middagsforslag"), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(if (thinking) tr("ChatGPT is writing…", "ChatGPT skriver …") else "ChatGPT", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, tr("Close", "Lukk")) }
            }
            LazyColumn(Modifier.weight(1f), state = listState, contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(turns) { i, t ->
                    if (t.user) Bubble(t.text, mine = true)
                    else {
                        Bubble(Dinner.display(t.text), mine = false)
                        val missing = Dinner.missing(t.text)
                        val last = i == turns.lastIndex
                        if (missing.isNotEmpty() && (!last || status == ChatGptBridge.Status.DONE)) AddMissing(missing)
                    }
                }
                if (thinking && turns.lastOrNull()?.user != false) item { Bubble("…", mine = false) }
                error?.let { e ->
                    item {
                        Column {
                            Text(e, color = MaterialTheme.colorScheme.error)
                            pageUrl?.let { u -> TextButton(onClick = { openUrl(ctx, u) }) {
                                Text(tr("Open ChatGPT", "Åpne ChatGPT")); Spacer(Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(16.dp))
                            } }
                        }
                    }
                }
            }
            if (status == ChatGptBridge.Status.DONE) {
                val quick = listOf(
                    tr("Other ideas", "Andre forslag"), tr("Make it vegetarian", "Gjør det vegetar"),
                    tr("Quicker, please", "Noe raskere"), tr("For 4 people", "For 4 personer"), tr("Dessert too?", "Dessert også?"),
                )
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quick) { q ->
                        Surface(onClick = { send(q) }, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(q, Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    input, { input = it }, Modifier.weight(1f),
                    placeholder = { Text(tr("Ask a follow-up…", "Spør videre …")) },
                    shape = RoundedCornerShape(24.dp), maxLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send(input) })
                )
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = { send(input) }, enabled = input.isNotBlank() && !thinking) {
                    Icon(Icons.AutoMirrored.Filled.Send, tr("Send", "Send"), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun AddMissing(missing: List<String>) {
    var added by remember(missing) { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(tr("You'd still need: ", "Du mangler: ") + missing.joinToString(", "), color = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.height(6.dp))
            Button(onClick = { ListRepo.add(missing.joinToString(",")); added = true }, enabled = !added) {
                Icon(Icons.Filled.AddShoppingCart, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                Text(if (added) tr("Added to \"${ListRepo.current?.name.orEmpty()}\"", "Lagt til i «${ListRepo.current?.name.orEmpty()}»")
                    else tr("Add to shopping list", "Legg til i handlelisten"))
            }
        }
    }
}

@Composable
private fun Bubble(text: String, mine: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (mine) 18.dp else 4.dp, bottomEnd = if (mine) 4.dp else 18.dp),
            color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            tonalElevation = if (mine) 0.dp else 1.dp,
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Text(if (mine) AnnotatedString(text) else markdown(text), Modifier.padding(horizontal = 14.dp, vertical = 10.dp), fontSize = 15.sp, lineHeight = 21.sp)
        }
    }
}

/** Just enough Markdown for ChatGPT's answers: **bold**, ### headings, bullet lines. */
private fun markdown(s: String): AnnotatedString = buildAnnotatedString {
    s.lines().forEachIndexed { i, raw ->
        if (i > 0) append("\n")
        var line = raw
        val heading = Regex("^\\s*#{1,6}\\s+").find(line)
        if (heading != null) line = line.substring(heading.range.last + 1)
        line = line.replace(Regex("^(\\s*)[-*]\\s+"), "$1• ")
        if (heading != null) { withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp)) { append(line.replace("**", "")) }; return@forEachIndexed }
        val parts = line.split("**")
        parts.forEachIndexed { j, p -> if (j % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(p) } else append(p) }
    }
}
