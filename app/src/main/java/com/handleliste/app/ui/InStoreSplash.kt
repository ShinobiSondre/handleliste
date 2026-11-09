package com.handleliste.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
        if (store != null && ListRepo.open.isNotEmpty() && System.currentTimeMillis() - Deals.fetchedAt > 6 * 3600_000L) {
            runCatching { runDealsCheck(ctx) }
        }
    }
    AnimatedVisibility(store != null, enter = fadeIn() + slideInVertically { it / 6 }, exit = fadeOut() + slideOutVertically { it / 6 }) {
        val name = store.orEmpty()
        val groups = ListRepo.openByList()
        val open = groups.flatMap { it.second }
        val done = ListRepo.lists.flatMap { ListRepo.itemsOf(it.id) }.filter { it.checked }
        val here = Deals.atStore(name)
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Box(
                Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xFF2E9E66), Color(0xFF145C39))))
                    .statusBarsPadding().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 20.dp)
            ) {
                IconButton(onClick = { StoreAlert.splash = null }, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(Icons.Filled.Close, tr("Close", "Lukk"), tint = Color.White)
                }
                Column(Modifier.padding(top = 12.dp, end = 40.dp)) {
                    Box(Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.ShoppingCart, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(tr("You're in the store!", "Du er i butikken!"), color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(name, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black, lineHeight = 34.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (open.isEmpty()) tr("All done. Nice!", "Alt er handlet. Bra!")
                        else tr("${open.size} left to pick up", "${open.size} igjen å plukke"),
                        color = Color(0xFFFFD54F), fontSize = 16.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp)) {
                if (here.isNotEmpty()) {
                    item {
                        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.padding(bottom = 10.dp)) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Text(tr("🏷️ On sale here", "🏷️ På tilbud her"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                here.take(6).forEach { (item, o) ->
                                    Text("$item: ${o.heading} · ${kr(o.price)}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                        }
                    }
                }
                groups.forEach { (list, its) ->
                    if (groups.size > 1) item(key = "h-" + list.id) {
                        Text(list.name, Modifier.padding(top = 10.dp, bottom = 2.dp), style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    items(its, key = { it.id }) { ItemRow(it, big = true) }
                }
                if (done.isNotEmpty()) {
                    item { Text(tr("In the cart", "I kurven"), Modifier.padding(top = 14.dp, bottom = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(done, key = { it.id }) { ItemRow(it, big = true) }
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
