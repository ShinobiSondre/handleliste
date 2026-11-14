package com.handleliste.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.handleliste.app.data.Deals
import com.handleliste.app.data.Item
import com.handleliste.app.data.ListRepo
import com.handleliste.app.data.Parse
import com.handleliste.app.watch.StoreAlert
import com.handleliste.app.util.tr
import kotlinx.coroutines.delay

/** "Alt handlet!" with confetti, and what the deals saved you, when the last thing on a list is ticked off. */
object Celebrate {
    var shown by mutableStateOf<Double?>(null)
    var burst by mutableIntStateOf(0)

    /** About how much the ticked items saved, using this week's deals (at the store you're in, if any). */
    fun saved(items: List<Item>): Double = items.sumOf { item ->
        val offers = Deals.results.firstOrNull { it.item == item.name }?.offers.orEmpty()
        val st = StoreAlert.inStore ?: Deals.store
        val o = (if (st != null) offers.filter { Parse.sameChain(it.store, st) } else offers).minByOrNull { it.price }
        o?.prePrice?.let { (it - o.price).coerceAtLeast(0.0) } ?: 0.0
    }

    fun fire(items: List<Item>) { shown = saved(items); burst++ }
}

/** Watches the list shown and celebrates when it's all done. Drawn over the whole app. */
@Composable
fun CelebrationOverlay() {
    val its = ListRepo.items
    val open = its.count { !it.checked }
    var prev by remember { mutableIntStateOf(open) }
    LaunchedEffect(open, ListRepo.currentId) {
        if (prev > 0 && open == 0 && its.any { it.checked }) Celebrate.fire(its.filter { it.checked })
        prev = open
    }
    LaunchedEffect(Celebrate.burst) { if (Celebrate.burst > 0) { delay(3200); Celebrate.shown = null } }
    if (Celebrate.burst > 0) Confetti(Celebrate.burst, Modifier.fillMaxSize())
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(Celebrate.shown != null, enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(), exit = fadeOut()) {
            val s = Celebrate.shown ?: 0.0
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 12.dp,
                onClick = { Celebrate.shown = null }) {
                Column(Modifier.padding(horizontal = 28.dp, vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎉", fontSize = 48.sp)
                    Text(tr("All done!", "Alt handlet!"), fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Text(if (s >= 1) tr("You saved about ${kr(s)} on deals today", "Du sparte ca. ${kr(s)} på tilbud i dag")
                        else tr("Nice work. The list is empty.", "Bra jobba. Listen er tom."),
                        fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
