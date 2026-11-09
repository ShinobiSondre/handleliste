package com.handleliste.app.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clipToBounds
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
import com.handleliste.app.data.Flyers
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.handleliste.app.data.PriceLine
import com.handleliste.app.data.Prices
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import com.handleliste.app.data.Deals
import com.handleliste.app.data.Dinner
import com.handleliste.app.data.DinnerPrefs
import com.handleliste.app.data.ListRepo
import com.handleliste.app.util.tr

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DinnerScreen() {
    val ctx = LocalContext.current
    var extra by rememberSaveable { mutableStateOf("") }
    var useDeals by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) { DinnerPrefs.init(ctx) }
    val wishes = DinnerPrefs.words(com.handleliste.app.util.Lang.no)
    val turns by ChatGptBridge.turns.collectAsState()
    // Warm ChatGPT up in the background while you choose, so the answer comes faster.
    LaunchedEffect(Unit) { (ctx as? Activity)?.let { ChatGptBridge.prewarm(it) } }
    var budget by rememberSaveable { mutableFloatStateOf(100f) }
    var servings by rememberSaveable { mutableIntStateOf(2) }
    var preparing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun ask() {
        val list = ChatUi.items().map { it.name }
        val onSale = if (useDeals) Deals.results.filter { it.offers.isNotEmpty() }.map { it.item } else emptyList()
        val prompt = Dinner.prompt(list, extra, onSale, "", DinnerPrefs.recent(ctx))
        val shown = tr("What can I make for dinner?", "Hva kan jeg lage til middag?") +
            (if (list.isNotEmpty()) "\n" + list.joinToString(", ") else "") +
            ""
        ChatGptBridge.reset()
        ChatUi.pickedDish.clear(); ChatUi.madeLists.clear(); ChatUi.pendingList = null
        ChatUi.budget = null
        (ctx as? Activity)?.let { ChatGptBridge.ask(it, prompt, shown, prompt) }
        ChatUi.open = true
    }

    fun askBudget() = scope.launch {
        // Make sure this week's prices are on the phone (they usually are, from the automatic deals check).
        if (Flyers.cachedAsync(ctx).isEmpty()) {
            preparing = true
            runCatching {
                val loc = currentLocation(ctx)
                Flyers.near(ctx, loc?.first ?: Deals.FALLBACK_LAT, loc?.second ?: Deals.FALLBACK_LNG,
                    com.handleliste.app.data.Settings.radiusKm(ctx) * 1000)
            }
            preparing = false
        }
        val deals = withContext(Dispatchers.Default) { Prices.dinnerDeals(ctx).map { Prices.dealLine(it) } }
        val prompt = Dinner.budgetPrompt(budget.toInt(), servings, ChatUi.items().map { it.name }, extra, deals, wishes.joinToString(", "), DinnerPrefs.recent(ctx))
        val shown = tr("Make me dinner for ${budget.toInt()} kr · $servings ${if (servings == 1) "person" else "people"}",
            "Lag middag for ${budget.toInt()} kr · $servings ${if (servings == 1) "person" else "personer"}") +
            (if (wishes.isNotEmpty()) "\n" + wishes.joinToString(" · ") else "") +
            (if (deals.isNotEmpty()) tr("\n(with ${deals.size} deals near you this week)", "\n(med ${deals.size} tilbud nær deg denne uken)") else "")
        ChatGptBridge.reset()
        ChatUi.pickedDish.clear(); ChatUi.madeLists.clear(); ChatUi.pendingList = null
        ChatUi.budget = budget.toInt()
        (ctx as? Activity)?.let { ChatGptBridge.ask(it, prompt, shown, prompt) }
        ChatUi.open = true
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        // Warm gradient header with a gently steaming plate.
        val steam = rememberInfiniteTransition(label = "steam")
        val rise by steam.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "rise")
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFE8833A), Color(0xFFB3361F))))) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("What's for dinner?", "Hva blir det til middag?"), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black, lineHeight = 30.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(tr("ChatGPT suggests dinners from your shopping list, right here in the app", "ChatGPT foreslår middager ut fra handlelisten din, rett her i appen"),
                        color = Color.White.copy(alpha = 0.88f), fontSize = 14.sp)
                }
                Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                        for (k in 0..2) {
                            val p = (rise + k / 3f) % 1f
                            val x = size.width * (0.32f + k * 0.18f)
                            drawCircle(Color.White.copy(alpha = 0.35f * (1f - p)), radius = 4.dp.toPx() + p * 4.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(x, size.height * (0.42f - p * 0.38f)))
                        }
                    }
                    Text("🍲", fontSize = 40.sp, modifier = Modifier.offset(y = 10.dp))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Text(tr("Based on", "Basert på"), fontWeight = FontWeight.SemiBold)
                // Pick one or more lists; the dinner is planned from what's on those.
                if (ListRepo.lists.size > 1) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                        val chosen = ChatUi.chosen()
                        ListRepo.lists.forEach { l ->
                            val on = l.id in chosen
                            FilterChip(selected = on, onClick = {
                                val now = chosen.toMutableList()
                                if (on) { if (now.size > 1) now.remove(l.id) } else now += l.id
                                ChatUi.listIds.clear(); ChatUi.listIds.addAll(now)
                            }, label = { Text(l.name + " (" + ListRepo.itemsOf(l.id).count { !it.checked } + ")") },
                                leadingIcon = if (on) ({ Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }) else null)
                        }
                    }
                }
                val open = ChatUi.items()
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
        Spacer(Modifier.height(16.dp))
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Savings, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(Modifier.width(8.dp))
                    Text(tr("Dinner on a budget", "Middag på budsjett"), style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
                Spacer(Modifier.height(4.dp))
                Text(tr("Planned with this week's real deals near you, then price-checked.", "Planlagt med ukens ekte tilbud nær deg, og prissjekket etterpå."),
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${budget.toInt()} kr", fontSize = 30.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(Modifier.weight(1f))
                    // Servings: − 2 +
                    IconButton(onClick = { if (servings > 1) servings-- }) { Text("−", fontSize = 22.sp, color = MaterialTheme.colorScheme.onTertiaryContainer) }
                    Text(tr("$servings ${if (servings == 1) "person" else "people"}", "$servings ${if (servings == 1) "person" else "pers."}"),
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    IconButton(onClick = { if (servings < 8) servings++ }) { Text("+", fontSize = 22.sp, color = MaterialTheme.colorScheme.onTertiaryContainer) }
                }
                Slider(value = budget, onValueChange = { budget = (Math.round(it / 10f) * 10f) }, valueRange = 50f..500f)
                Spacer(Modifier.height(6.dp))
                Text(tr("What should it be? (remembered)", "Hva skal det være? (huskes)"), fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(bottom = 4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DinnerPrefs.CHOICES.forEach { c ->
                        val on = c.id in DinnerPrefs.chosen
                        FilterChip(selected = on, onClick = { DinnerPrefs.toggle(ctx, c.id) }, label = { Text(tr(c.en, c.no)) },
                            leadingIcon = if (on) ({ Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }) else null)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(tr("About ${(budget / servings).toInt()} kr per person", "Ca. ${(budget / servings).toInt()} kr per person"),
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                Spacer(Modifier.height(10.dp))
                Button(onClick = { askBudget() }, modifier = Modifier.fillMaxWidth(), enabled = !preparing) {
                    Icon(Icons.Filled.Restaurant, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text(tr("Make me dinner for ${budget.toInt()} kr", "Lag middag for ${budget.toInt()} kr"))
                }
                if (preparing) {
                    Text(tr("Getting this week's prices near you…", "Henter ukens priser nær deg …"),
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        if (turns.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = { ChatUi.open = true }, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Open the last chat", "Åpne forrige samtale"))
            }
        }
    }
}

/** One thing in the chat list. */
sealed class ChatEntry(val key: String) {
    class Msg(i: Int, val user: Boolean, val text: String) : ChatEntry("m$i")
    class Price(i: Int, val extra: List<String>, val fromList: List<String>) : ChatEntry("p$i")
    class Add(i: Int, val items: List<String>) : ChatEntry("a$i")
    class Dishes(val turn: Int, val dishes: List<Dinner.Dish>, val answer: String) : ChatEntry("d$turn")
}

/** Whether the dinner chat is on screen (drawn over the whole app by MainActivity). */
object ChatUi {
    var open by mutableStateOf(false)
    /** Which lists the dinner is based on (empty = the list you're on). */
    val listIds = mutableStateListOf<String>()
    fun chosen(): List<String> = listIds.filter { id -> ListRepo.lists.any { it.id == id } }.ifEmpty { listOf(ListRepo.currentId) }
    /** A list waiting for its dish's recipe to arrive (dish name), and dishes already made into lists. */
    var pendingList by mutableStateOf<String?>(null)
    val madeLists = mutableStateListOf<String>()
    /** Photos found for dishes (dish name → picture), used for a list made from a dish. */
    val dishPics = mutableStateMapOf<String, String>()
    /** Which dish you picked in each answer (answer index → dish index). */
    val pickedDish = mutableStateMapOf<Int, Int>()
    /** Open items on the chosen lists. */
    fun items() = chosen().flatMap { ListRepo.itemsOf(it) }.filter { !it.checked }
    /** Set when the chat is a budget dinner, so the price check can compare against it. */
    var budget by mutableStateOf<Int?>(null)
}

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
    // While ChatGPT writes, its half-finished answer is hidden behind a typing animation. The chat only
    // moves twice: down to the animation when you ask, and to the top of the answer when it's complete.
    val shown = if (thinking && turns.lastOrNull()?.user == false) turns.dropLast(1) else turns
    val listNames = ChatUi.items().map { it.name }
    // One entry per thing on screen: a bubble, a price check, an "add to list" card.
    val entries = buildList {
        shown.forEachIndexed { i, t ->
            add(ChatEntry.Msg(i, t.user, if (t.user) t.text else Dinner.display(t.text)))
            val dishes = if (t.user) emptyList() else Dinner.dishes(t.text)
            if (!t.user && dishes.isNotEmpty() && (i != shown.lastIndex || status == ChatGptBridge.Status.DONE)) {
                add(ChatEntry.Dishes(i, dishes, t.text))
            } else if (!t.user && (i != shown.lastIndex || status == ChatGptBridge.Status.DONE)) {
                val missing = Dinner.missing(t.text)
                val fromList = if (ChatUi.budget != null) Dinner.usedFromList(t.text, listNames) else emptyList()
                if (ChatUi.budget != null && (missing.isNotEmpty() || fromList.isNotEmpty())) add(ChatEntry.Price(i, missing, fromList))
                if (missing.isNotEmpty()) add(ChatEntry.Add(i, missing))
            }
        }
    }
    // Remember what ChatGPT suggested, so next time it's asked for something different.
    LaunchedEffect(status, turns.size) {
        if (status == ChatGptBridge.Status.DONE) turns.lastOrNull()?.takeIf { !it.user }?.let { t ->
            DinnerPrefs.remember(ctx, Dinner.dishes(t.text).map { it.name })
        }
    }
    LaunchedEffect(shown.size, status) {
        if (entries.isEmpty()) return@LaunchedEffect
        if (thinking) listState.animateScrollToItem(entries.size) // the typing animation
        else if (status == ChatGptBridge.Status.DONE) {
            // The top of the newest answer, so you start reading from its beginning.
            val idx = entries.indexOfLast { it is ChatEntry.Msg && !it.user }
            if (idx >= 0) listState.animateScrollToItem(idx)
        }
    }
    fun send(q: String) {
        if (q.isBlank() || thinking) return
        ChatGptBridge.followUp(q.trim()); input = ""
    }

    // Drawn inside the app's own window (not a dialog), so ChatGPT's hidden page keeps writing its answer.
    val seeThrough by ChatGptBridge.seeThrough.collectAsState()
    // See-through mode (for screen recordings): the ChatGPT page behind shows clearly through the chat.
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = if (seeThrough) 0.25f else 1f))
        .pointerInput(Unit) { detectTapGestures { } }) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Surface(color = MaterialTheme.colorScheme.background.copy(alpha = if (seeThrough) 0.6f else 1f)) {
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
                IconButton(onClick = { ChatGptBridge.setSeeThrough(!seeThrough) }) {
                    Icon(if (seeThrough) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        tr("Show the ChatGPT page behind", "Vis ChatGPT-siden bak"),
                        tint = if (seeThrough) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, tr("Close", "Lukk")) }
            }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            LazyColumn(Modifier.weight(1f).clipToBounds(), state = listState,
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(entries, key = { it.key }) { e ->
                    when (e) {
                        is ChatEntry.Msg -> Bubble(e.text, mine = e.user)
                        is ChatEntry.Price -> PriceCheck(e.extra, e.fromList, ChatUi.budget)
                        is ChatEntry.Add -> AddMissing(e.items)
                        is ChatEntry.Dishes -> DishPicker(e.turn, e.dishes, listNames, e.answer)
                    }
                }
                if (thinking) item(key = "typing") { Typing(writing = turns.lastOrNull()?.user == false) }
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))
            if (status == ChatGptBridge.Status.DONE) {
                val quick = (if (ChatUi.budget != null) listOf(tr("Even cheaper", "Enda billigere"), tr("Without meat", "Uten kjøtt")) else emptyList()) + listOf(
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

/**
 * Checks the plan against today's prices (deals near you first, else the normal store price): what the
 * dinner uses from your shopping lists counts too, so the total is what the whole dinner costs.
 */
@Composable
private fun PriceCheck(extra: List<String>, fromList: List<String>, budget: Int?) {
    val ctx = LocalContext.current
    var lines by remember(extra, fromList) { mutableStateOf<Pair<List<PriceLine>, List<PriceLine>>?>(null) }
    LaunchedEffect(extra, fromList) {
        val pins = ChatUi.items().mapNotNull { i -> i.pin?.let { i.name to it } }.toMap()
        lines = runCatching { Prices.check(ctx, fromList, pins) to Prices.check(ctx, extra) }.getOrDefault(emptyList<PriceLine>() to emptyList())
    }
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(tr("Price check, the app's prices today", "Prissjekk med appens priser i dag"), fontWeight = FontWeight.Bold)
            val l = lines
            if (l == null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp))
                    Text(tr("Looking up prices…", "Henter priser …"), fontSize = 13.sp)
                }
            } else {
                val (listLines, extraLines) = l
                @Composable fun section(title: String, rows: List<PriceLine>) {
                    if (rows.isEmpty()) return
                    Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp))
                    rows.forEach { p ->
                        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p.item, fontSize = 14.sp)
                                if (p.price != null) Text((if (p.onSale) "🏷️ " else "") + p.where + " · " + p.product, fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(p.price?.let { kr(it) } ?: "?", fontWeight = FontWeight.SemiBold,
                                color = if (p.onSale) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    val sub = rows.sumOf { it.price ?: 0.0 }
                    Text(tr("Subtotal ", "Delsum ") + kr(sub), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp), textAlign = TextAlign.End)
                }
                section(tr("From your shopping list", "Fra handlelisten din"), listLines)
                section(tr("To buy extra", "Må kjøpes i tillegg"), extraLines)
                val all = listLines + extraLines
                val total = all.sumOf { it.price ?: 0.0 }
                val unknown = all.count { it.price == null }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                val ok = budget == null || total <= budget
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Whole dinner", "Hele middagen") + if (unknown > 0) tr(" (+$unknown unknown)", " (+$unknown ukjent)") else "",
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(kr(total), fontWeight = FontWeight.Black, fontSize = 18.sp,
                        color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                }
                if (budget != null) Text(
                    if (ok) tr("✓ Under your $budget kr", "✓ Under budsjettet på $budget kr")
                    else tr("Over your $budget kr. Ask for something cheaper below.", "Over budsjettet på $budget kr. Be om noe billigere under."),
                    fontSize = 12.sp, color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                val deals = all.count { it.onSale }
                if (deals > 0) Text(tr("🏷️ $deals of the ingredients are on sale near you this week", "🏷️ $deals av ingrediensene er på tilbud nær deg denne uken"),
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                Text(tr("Whole packs, incl. what you use from your list. Deals near you, otherwise the normal store price (MENY or Oda).",
                    "Hele pakninger, inkludert det du bruker fra listen. Tilbud nær deg, ellers vanlig butikkpris (MENY eller Oda)."),
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * "Which one do you want?": a card per dish (with a photo), and for the one you pick: what you're
 * missing for it, the button to add that to your list, and (budget dinners) the price check.
 */
@Composable
private fun DishPicker(turn: Int, dishes: List<Dinner.Dish>, listNames: List<String>, answer: String) {
    val picked = ChatUi.pickedDish[turn] ?: if (dishes.size == 1) 0 else -1
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (dishes.size > 1) Text(tr("Which one do you want? Tap a dish", "Hvilken vil du lage? Trykk på en rett"),
            fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(tr("Prices for the dish you pick come from the app (this week's deals near you), not ChatGPT's guesses in the text.",
            "Prisene for retten du velger kommer fra appen (ukens tilbud nær deg), ikke ChatGPTs anslag i teksten."),
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(dishes) { j, d -> DishCard(d, selected = j == picked) { ChatUi.pickedDish[turn] = j } }
        }
        AnimatedVisibility(picked in dishes.indices, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            val d = dishes.getOrNull(picked) ?: return@AnimatedVisibility
            val fromList = Dinner.usesFromList(d, listNames)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // The app's own prices (this week's deals near you, else the normal store price) for every
                // ingredient, not ChatGPT's guesses: split into what's on your list and what to buy.
                val everything = Dinner.ingredientsForList(d, listNames)
                val onList = everything.filter { it in listNames }
                val extra = everything.filter { it !in listNames }
                if (everything.isNotEmpty()) PriceCheck(extra, onList, ChatUi.budget)
                if (d.buy.isNotEmpty()) AddMissing(d.buy, d.name)
                else Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(tr("✓ You have everything for ${d.name}", "✓ Du har alt du trenger til ${d.name}"),
                        Modifier.fillMaxWidth().padding(12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
                }
                // The recipe comes as a quick second question (the first answer is kept short so it's fast).
                val turnsNow by ChatGptBridge.turns.collectAsState()
                val status by ChatGptBridge.status.collectAsState()
                val label = Dinner.recipeLabel(d)
                val qi = turnsNow.indexOfLast { it.user && it.text == label }
                val recipe = turnsNow.getOrNull(qi + 1)?.takeIf { qi >= 0 && !it.user && (qi + 1 < turnsNow.lastIndex || status == ChatGptBridge.Status.DONE) }?.text
                if (recipe == null) {
                    FilledTonalButton(onClick = { ChatGptBridge.followUp(label, Dinner.recipeQuestion(d)) },
                        enabled = status != ChatGptBridge.Status.THINKING, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                        Text(if (qi >= 0) tr("Getting the recipe…", "Henter oppskriften …") else tr("Get the recipe for ${d.name}", "Hent oppskriften på ${d.name}"),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                MakeListButton(d, recipe?.let { Dinner.display(it) }, asked = qi >= 0)
            }
        }
    }
}

/**
 * "Lag handleliste med oppskrift": a new list with every ingredient in the recipe and the recipe itself.
 * If the recipe hasn't been fetched yet, it's fetched first and the list is made when it arrives.
 */
@Composable
private fun MakeListButton(d: Dinner.Dish, recipe: String?, asked: Boolean) {
    val made = d.name in ChatUi.madeLists
    val waiting = ChatUi.pendingList == d.name && recipe == null
    fun make(text: String) {
        val items = Dinner.ingredientsForList(d, ChatUi.items().map { it.name }, text)
        ListRepo.newListFrom(d.name, items, ChatUi.dishPics[d.name], text)
        ChatUi.madeLists += d.name
        ChatUi.pendingList = null
    }
    LaunchedEffect(recipe, ChatUi.pendingList) {
        if (recipe != null && ChatUi.pendingList == d.name && !made) make(recipe)
    }
    OutlinedButton(
        onClick = {
            if (recipe != null) make(recipe) else {
                ChatUi.pendingList = d.name
                if (!asked) ChatGptBridge.followUp(Dinner.recipeLabel(d), Dinner.recipeQuestion(d))
            }
        },
        enabled = !made && !waiting, modifier = Modifier.fillMaxWidth()
    ) {
        Icon(if (made) Icons.Filled.Check else Icons.AutoMirrored.Filled.PlaylistAdd, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
        Text(when {
            made -> tr("New list \"${d.name}\" made", "Ny liste «${d.name}» er laget")
            waiting -> tr("Getting the recipe, then making the list…", "Henter oppskriften og lager listen …")
            else -> tr("Make a shopping list + recipe for ${d.name}", "Lag handleliste med oppskrift: ${d.name}")
        }, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DishCard(d: Dinner.Dish, selected: Boolean, onClick: () -> Unit) {
    var pic by remember(d.name) { mutableStateOf<String?>(null) }
    LaunchedEffect(d.name) {
        pic = runCatching { com.handleliste.app.data.Catalog.dishPicture(d.name, d.imageQuery) }.getOrNull()
        pic?.let { ChatUi.dishPics[d.name] = it }
    }
    val border by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, label = "dish")
    val scale by animateFloatAsState(if (selected) 1.03f else 1f, label = "dishScale")
    Surface(onClick = onClick, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp,
        modifier = Modifier.width(190.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .border(if (selected) 2.5.dp else 1.dp, border, RoundedCornerShape(18.dp))) {
        Column {
            Box(Modifier.fillMaxWidth().height(110.dp).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                if (pic != null) AsyncImage(pic, d.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Text("🍽️", fontSize = 34.sp)
                if (selected) Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(26.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center) { Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp)) }
            }
            Column(Modifier.padding(10.dp)) {
                Text(d.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 19.sp)
                Text(if (d.buy.isEmpty()) tr("Nothing to buy", "Ingenting å kjøpe") else tr("Buy ${d.buy.size} more", "Kjøp ${d.buy.size} til"),
                    fontSize = 12.sp, color = if (d.buy.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Three bouncing dots in a chat bubble while ChatGPT is thinking or writing. */
@Composable
private fun Typing(writing: Boolean) {
    val t = rememberInfiniteTransition(label = "typing")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp),
            color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp
        ) {
            Row(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                repeat(3) { i ->
                    val y by t.animateFloat(
                        initialValue = 0f, targetValue = 0f,
                        animationSpec = infiniteRepeatable(keyframes {
                            durationMillis = 1200
                            0f at 0 + i * 150
                            -7f at 200 + i * 150 using FastOutSlowInEasing
                            0f at 400 + i * 150
                        }), label = "dot$i"
                    )
                    val a by t.animateFloat(0.35f, 1f, infiniteRepeatable(tween(600, delayMillis = i * 150), RepeatMode.Reverse), label = "a$i")
                    Box(Modifier.offset(y = y.dp).size(9.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = a)))
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(if (writing) tr("Writing the recipes…", "Skriver oppskriftene …") else tr("Thinking…", "Tenker …"),
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AddMissing(missing: List<String>, dish: String? = null) {
    var added by remember(missing) { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text((if (dish != null) tr("For $dish you need: ", "Til $dish mangler du: ") else tr("You'd still need: ", "Du mangler: ")) +
                missing.joinToString(", "), color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(6.dp))
            Button(onClick = { ListRepo.add(missing.joinToString(","), ChatUi.chosen().first()); added = true }, enabled = !added) {
                Icon(Icons.Filled.AddShoppingCart, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                Text(if (added) tr("Added to \"${ListRepo.lists.firstOrNull { it.id == ChatUi.chosen().first() }?.name.orEmpty()}\"", "Lagt til i «${ListRepo.lists.firstOrNull { it.id == ChatUi.chosen().first() }?.name.orEmpty()}»")
                    else tr("Add to shopping list", "Legg til i handlelisten"))
            }
        }
    }
}

@Composable
private fun Bubble(text: String, mine: Boolean) {
    // Answers use the full width (more room on a big screen); your own messages up to 85 %.
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val maxW = if (mine) maxWidth * 0.85f else maxWidth
        Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
            Surface(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = if (mine) 20.dp else 6.dp, bottomEnd = if (mine) 6.dp else 20.dp),
                color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                tonalElevation = if (mine) 0.dp else 1.dp,
                modifier = Modifier.widthIn(max = maxW)
            ) {
                if (mine) Text(text, Modifier.padding(horizontal = 16.dp, vertical = 12.dp), fontSize = 16.sp, lineHeight = 22.sp)
                else Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Pictures in ChatGPT's answer come through as ![](url) lines.
                    val img = Regex("^!\\[[^\\]]*]\\((https?://[^)]+)\\)$")
                    val blocks = mutableListOf<Pair<Boolean, String>>()
                    text.lines().forEach { line ->
                        val m = img.find(line.trim())
                        if (m != null) blocks += true to m.groupValues[1]
                        else if (blocks.lastOrNull()?.first == false) blocks[blocks.lastIndex] = false to blocks.last().second + "\n" + line
                        else blocks += false to line
                    }
                    blocks.forEach { (isImg, v) ->
                        if (isImg) AsyncImage(v, null, Modifier.fillMaxWidth().heightIn(max = 260.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                        else if (v.isNotBlank()) Text(markdown(v.trim('\n')), fontSize = 16.sp, lineHeight = 23.sp)
                    }
                }
            }
        }
    }
}

/** Just enough Markdown for ChatGPT's answers: **bold**, ### headings, bullet lines. */
internal fun markdown(s: String): AnnotatedString = buildAnnotatedString {
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
