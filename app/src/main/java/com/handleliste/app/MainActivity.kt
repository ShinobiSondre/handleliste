package com.handleliste.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.handleliste.app.data.ListRepo
import com.handleliste.app.data.Settings
import com.handleliste.app.ui.ChatUi
import com.handleliste.app.ui.DealsScreen
import com.handleliste.app.ui.PickerUi
import com.handleliste.app.ui.ProductPicker
import com.handleliste.app.ui.DinnerChat
import com.handleliste.app.ui.DinnerScreen
import com.handleliste.app.ui.HandlelisteTheme
import com.handleliste.app.ui.InStoreSplash
import com.handleliste.app.ui.ListScreen
import com.handleliste.app.ui.SettingsScreen
import com.handleliste.app.ui.WatchState
import com.handleliste.app.ui.runDealsCheck
import com.handleliste.app.update.UpdateOverlay
import com.handleliste.app.update.Updater
import com.handleliste.app.util.Lang
import com.handleliste.app.util.tr
import com.handleliste.app.watch.StoreAlert
import com.handleliste.app.watch.StoreWatchService
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val openTab = androidx.compose.runtime.mutableIntStateOf(-1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Lang.init(this)
        // Pictures load with the app's name as User-Agent: Wikimedia refuses anonymous image downloads.
        coil.Coil.setImageLoader(coil.ImageLoader.Builder(this)
            .okHttpClient {
                okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
                    chain.proceed(chain.request().newBuilder()
                        .header("User-Agent", "Handleliste/1.0 (Android; github.com/ShinobiSondre/handleliste)").build())
                }.build()
            }
            .crossfade(true)
            .build())
        ListRepo.init(this)
        com.handleliste.app.data.DinnerPrefs.init(this)
        StoreAlert.channels(this)
        enableEdgeToEdge()
        handle(intent)
        Updater.start(this)
        com.handleliste.app.data.Alerts.schedule(this)
        setContent {
            HandlelisteTheme {
                var tab by rememberSaveable { mutableIntStateOf(0) }
                LaunchedEffect(openTab.intValue) { if (openTab.intValue >= 0) { tab = openTab.intValue; openTab.intValue = -1 } }
                // Deals are checked by themselves: when the app opens and a moment after the lists change.
                val openNames = ListRepo.open.map { it.name + "/" + it.pin?.name.orEmpty() }.sorted().joinToString("|")
                LaunchedEffect(openNames) {
                    if (openNames.isNotEmpty()) { delay(1200); runDealsCheck(this@MainActivity) }
                }
                BackHandler(enabled = tab != 0 || StoreAlert.splash != null || ChatUi.open || PickerUi.itemId != null) {
                    when {
                        PickerUi.itemId != null -> PickerUi.itemId = null
                        ChatUi.open -> ChatUi.open = false
                        StoreAlert.splash != null -> StoreAlert.splash = null
                        else -> tab = 0
                    }
                }
                Box(Modifier.fillMaxSize()) {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        bottomBar = {
                            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                                val tabs = listOf(
                                    Triple(Icons.AutoMirrored.Filled.List, tr("List", "Liste"), 0),
                                    Triple(Icons.Filled.LocalOffer, tr("Deals", "Tilbud"), 1),
                                    Triple(Icons.Filled.Restaurant, tr("Dinner", "Middag"), 2),
                                    Triple(Icons.Filled.Settings, tr("Settings", "Innstillinger"), 3),
                                )
                                tabs.forEach { (icon, label, i) ->
                                    NavigationBarItem(selected = tab == i, onClick = { tab = i },
                                        icon = { Icon(icon, null) }, label = { Text(label, maxLines = 1) })
                                }
                            }
                        }
                    ) { pad ->
                        Box(Modifier.padding(pad)) {
                          // Tabs slide in from the side they're on.
                          AnimatedContent(tab, transitionSpec = {
                              val dir = if (targetState > initialState) 1 else -1
                              (slideInHorizontally(tween(260)) { it / 6 * dir } + fadeIn(tween(260))) togetherWith
                                  (slideOutHorizontally(tween(200)) { -it / 6 * dir } + fadeOut(tween(160)))
                          }, label = "tabs") { shownTab ->
                            when (shownTab) {
                                0 -> ListScreen(
                                    onOpenDeals = { tab = 1 },
                                    onDinner = { tab = 2 },
                                    onSettings = { tab = 3 },
                                )
                                1 -> DealsScreen()
                                2 -> DinnerScreen()
                                else -> SettingsScreen()
                            }
                          }
                        }
                    }
                    if (ChatUi.open) DinnerChat(onClose = { ChatUi.open = false })
                    InStoreSplash()
                    ProductPicker()
                    UpdateOverlay()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Updater.onResume(this)
        WatchState.refresh(this)
        // Keep the store watcher running while it's switched on (Android may have stopped it).
        if (Settings.watchOn(this) && StoreWatchService.hasLocation(this)) StoreWatchService.start(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        intent?.getStringExtra(StoreAlert.EXTRA_STORE)?.let { StoreAlert.splash = it }
        // A deal notification opens the Deals tab.
        intent?.getIntExtra(com.handleliste.app.data.Alerts.EXTRA_TAB, -1)?.takeIf { it >= 0 }?.let { openTab.intValue = it }
    }
}
