package com.handleliste.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.handleliste.app.data.Deals
import com.handleliste.app.data.Dinner
import com.handleliste.app.data.ListRepo
import com.handleliste.app.data.Recipe
import com.handleliste.app.data.Settings
import com.handleliste.app.util.tr
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DinnerScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var extra by rememberSaveable { mutableStateOf("") }
    var useDeals by rememberSaveable { mutableStateOf(true) }
    var hasKey by remember { mutableStateOf(Settings.geminiKey(ctx).isNotBlank()) }
    var keyInput by remember { mutableStateOf("") }
    val wishes = remember { mutableStateListOf<String>() }
    val wishOptions = listOf(
        tr("Quick (under 30 min)", "Rask (under 30 min)"), tr("Vegetarian", "Vegetar"), tr("Kid-friendly", "Barnevennlig"),
        tr("Cheap", "Billig"), tr("Healthy", "Sunn"), tr("Cosy / comfort food", "Kos / hverdagsfavoritt"),
    )

    fun onSale(): List<String> =
        if (useDeals) Deals.results.filter { it.offers.isNotEmpty() }.map { it.item } else emptyList()
    fun listNames() = ListRepo.items.map { it.name }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(tr("What's for dinner?", "Hva blir det til middag?"), style = MaterialTheme.typography.headlineMedium)
            Text(tr("Ideas made from your shopping list by AI", "Forslag laget av AI ut fra handlelisten din"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
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
            Spacer(Modifier.height(8.dp))
            if (hasKey) {
                Button(
                    onClick = { scope.launch { Dinner.suggest(ctx, listNames(), extra, onSale(), wishes.joinToString(", ")) } },
                    enabled = !Dinner.loading, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text(if (Dinner.recipes.isEmpty()) tr("Suggest dinner", "Foreslå middag") else tr("New ideas", "Nye forslag"))
                }
                if (Dinner.loading) {
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)))
                }
            } else {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Key, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp))
                            Text(tr("One-time setup: a free AI key", "Engangsoppsett: en gratis AI-nøkkel"), style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(tr("Dinner ideas use Google's Gemini, which is free with your own key. Sign in with Google, tap \"Create API key\", copy it and paste it here.",
                            "Middagsforslagene bruker Googles Gemini, som er gratis med din egen nøkkel. Logg inn med Google, trykk «Create API key», kopier den og lim den inn her."),
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { openUrl(ctx, "https://aistudio.google.com/apikey") }) {
                            Text(tr("Get a free key", "Hent gratis nøkkel")); Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(16.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(keyInput, { keyInput = it }, Modifier.fillMaxWidth(), singleLine = true,
                            label = { Text(tr("Paste key", "Lim inn nøkkel")) }, visualTransformation = PasswordVisualTransformation())
                        Spacer(Modifier.height(8.dp))
                        Row {
                            Button(onClick = { Settings.setGeminiKey(ctx, keyInput); hasKey = keyInput.isNotBlank() }, enabled = keyInput.length > 20) {
                                Text(tr("Save", "Lagre"))
                            }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { Dinner.openInChatGpt(ctx, Dinner.prompt(listNames(), extra, onSale(), wishes.joinToString(", "))) }) {
                                Text(tr("Ask ChatGPT instead", "Spør ChatGPT i stedet"))
                            }
                        }
                    }
                }
            }
            Dinner.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
        items(Dinner.recipes, key = { it.title }) { RecipeCard(it) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeCard(r: Recipe) {
    var open by remember { mutableStateOf(false) }
    var added by remember { mutableStateOf(false) }
    Card(Modifier.animateContentSize(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.clickable { open = !open }.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Timer, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text(tr("${r.minutes} min · ${r.servings} servings", "${r.minutes} min · ${r.servings} porsjoner"),
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (r.why.isNotBlank()) { Spacer(Modifier.height(6.dp)); Text(r.why) }
            if (r.uses.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(tr("From your list: ", "Fra listen: ") + r.uses.joinToString(", "), fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
            if (r.missing.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(tr("You'd also need: ", "Du trenger også: ") + r.missing.joinToString(", "), fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = { ListRepo.add(r.missing.joinToString(",")); added = true }, enabled = !added) {
                    Icon(Icons.Filled.AddShoppingCart, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                    Text(if (added) tr("Added to the list", "Lagt til i listen") else tr("Add to shopping list", "Legg til i handlelisten"))
                }
            }
            if (open) {
                Spacer(Modifier.height(12.dp))
                Text(tr("Ingredients", "Ingredienser"), fontWeight = FontWeight.Bold)
                r.ingredients.forEach { Text("• $it", Modifier.padding(start = 4.dp, top = 2.dp)) }
                Spacer(Modifier.height(10.dp))
                Text(tr("How to make it", "Slik gjør du"), fontWeight = FontWeight.Bold)
                r.steps.forEachIndexed { i, s -> Text("${i + 1}. $s", Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp)) }
            } else {
                Text(tr("Tap for the recipe", "Trykk for oppskriften"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
