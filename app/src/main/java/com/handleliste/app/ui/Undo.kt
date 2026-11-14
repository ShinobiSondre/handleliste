package com.handleliste.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.handleliste.app.util.tr
import kotlinx.coroutines.delay

/** "Fjernet melk · Angre": a short bar after deleting something, from anywhere in the app. */
object Undo {
    class Offer(val message: String, val undo: () -> Unit, val id: Long = System.nanoTime())
    var current by mutableStateOf<Offer?>(null)

    fun offer(message: String, undo: (() -> Unit)?) { if (undo != null) current = Offer(message, undo) }
}

@Composable
fun UndoBar(bottom: androidx.compose.ui.unit.Dp = 88.dp) {
    val o = Undo.current
    LaunchedEffect(o?.id) { if (o != null) { delay(4500); if (Undo.current?.id == o.id) Undo.current = null } }
    Box(Modifier.fillMaxSize().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = bottom), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(o != null, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
            var shown by androidx.compose.runtime.remember { mutableStateOf(o) }
            if (o != null) shown = o
            val s = shown ?: return@AnimatedVisibility
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.inverseSurface, shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(s.message, color = MaterialTheme.colorScheme.inverseOnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = { s.undo(); Undo.current = null }) {
                        Text(tr("Undo", "Angre"), color = MaterialTheme.colorScheme.inversePrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
