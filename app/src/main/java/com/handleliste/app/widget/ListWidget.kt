package com.handleliste.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.handleliste.app.MainActivity
import com.handleliste.app.data.Aisles
import com.handleliste.app.data.Item
import com.handleliste.app.data.ListRepo
import com.handleliste.app.util.Lang
import com.handleliste.app.util.tr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val bg = ColorProvider(day = Color(0xFFF4F7F2), night = Color(0xFF161D19))
private val ink = ColorProvider(day = Color(0xFF14201A), night = Color(0xFFE2E9E4))
private val soft = ColorProvider(day = Color(0xFF4F5E55), night = Color(0xFFB4C2B9))
private val green = ColorProvider(day = Color(0xFF1F7A4D), night = Color(0xFF6FD69C))
private val row = ColorProvider(day = Color.White, night = Color(0xFF222B25))
private val itemKey = ActionParameters.Key<String>("item")

/** The list on your home screen: tap an item to tick it off, tap the title to open the app. */
class ListWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        withContext(Dispatchers.Main) { Lang.init(context); ListRepo.init(context) }
        provideContent { GlanceTheme { Content() } }
    }

    @Composable
    private fun Content() {
        val list = ListRepo.current
        val open = ListRepo.items.filter { !it.checked }
        val ordered = Aisles.group(open).flatMap { it.second }
        Column(GlanceModifier.fillMaxSize().background(bg).cornerRadius(20.dp).padding(12.dp)) {
            Row(GlanceModifier.fillMaxWidth().clickable(actionStartActivity<MainActivity>()), verticalAlignment = Alignment.CenterVertically) {
                Text("🛒 " + (list?.name ?: "Handleliste"), style = TextStyle(color = ink, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.defaultWeight(), maxLines = 1)
                Text(if (open.isEmpty()) "✓" else "${open.size}", style = TextStyle(color = green, fontSize = 15.sp, fontWeight = FontWeight.Bold))
            }
            Spacer(GlanceModifier.height(8.dp))
            if (open.isEmpty()) {
                Box(GlanceModifier.fillMaxSize().clickable(actionStartActivity<MainActivity>()), contentAlignment = Alignment.Center) {
                    Text(tr("All done! Tap to add more.", "Alt er handlet! Trykk for å legge til."), style = TextStyle(color = soft, fontSize = 14.sp))
                }
            } else {
                LazyColumn {
                    items(ordered, itemId = { it.id.hashCode().toLong() }) { item -> ItemLine(item) }
                }
            }
        }
    }

    @Composable
    private fun ItemLine(item: Item) {
        Column {
            Row(GlanceModifier.fillMaxWidth().background(row).cornerRadius(12.dp).padding(horizontal = 10.dp, vertical = 8.dp)
                .clickable(actionRunCallback<ToggleItem>(actionParametersOf(itemKey to item.id))),
                verticalAlignment = Alignment.CenterVertically) {
                Box(GlanceModifier.size(18.dp).cornerRadius(9.dp).background(green)) {
                    Box(GlanceModifier.size(14.dp).cornerRadius(7.dp).background(row).padding(2.dp)) {}
                }
                Spacer(GlanceModifier.width(10.dp))
                Text(Aisles.of(item).emoji + "  " + item.name, style = TextStyle(color = ink, fontSize = 15.sp), maxLines = 1)
            }
            Spacer(GlanceModifier.height(5.dp))
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        /** Called whenever a list changes, so the widget shows the same thing as the app. */
        fun refresh(ctx: Context) { scope.launch { runCatching { ListWidget().updateAll(ctx.applicationContext) } } }
    }
}

class ToggleItem : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[itemKey] ?: return
        withContext(Dispatchers.Main) { ListRepo.init(context); ListRepo.toggle(id) }
        ListWidget().updateAll(context)
    }
}

class ListWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ListWidget()
}
