package com.handleliste.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.handleliste.app.data.Offer
import com.handleliste.app.data.PriceHistory
import com.handleliste.app.util.tr

/** "🔥 Laveste på 8 uker" / "Har vært 39 kr" under a deal, once the app has seen a few weeks of prices. */
@Composable
fun VerdictTag(o: Offer, modifier: Modifier = Modifier) {
    val v = PriceHistory.verdict(o) ?: return
    Text(tr(v.en, v.no), modifier.padding(top = 2.dp).clip(RoundedCornerShape(6.dp))
        .background(if (v.good) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
        .padding(horizontal = 6.dp, vertical = 1.dp),
        fontSize = 11.sp, fontWeight = FontWeight.Bold,
        color = if (v.good) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
}
