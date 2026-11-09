package com.handleliste.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.handleliste.app.BuildConfig
import com.handleliste.app.data.Settings
import com.handleliste.app.update.Updater
import com.handleliste.app.util.Lang
import com.handleliste.app.util.tr
import com.handleliste.app.watch.StoreAlert
import com.handleliste.app.watch.StoreWatchService
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val enableWatch = rememberEnableWatch()
    var refresh by remember { mutableIntStateOf(0) }
    val bgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refresh++
        if (!it) openAppSettings(ctx)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(tr("Settings", "Innstillinger"), style = MaterialTheme.typography.headlineMedium)

        Section(tr("Language", "Språk")) {
            val modes = listOf(Lang.Mode.AUTO to tr("Phone", "Telefon"), Lang.Mode.NO to "Norsk", Lang.Mode.EN to "English")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                modes.forEachIndexed { i, (m, label) ->
                    SegmentedButton(
                        selected = Lang.mode == m, onClick = { Lang.set(ctx, m); StoreAlert.channels(ctx) },
                        shape = SegmentedButtonDefaults.itemShape(i, modes.size)
                    ) { Text(label) }
                }
            }
        }

        Section(tr("Store reminder", "Butikkvarsel")) {
            key(refresh) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Show my list when I'm in a store", "Vis listen når jeg er i en butikk"), fontWeight = FontWeight.SemiBold)
                        Text(tr("Uses your location and free OpenStreetMap data. Nothing is sent anywhere except the map lookup.",
                            "Bruker posisjonen din og gratis kartdata fra OpenStreetMap. Ingenting sendes noe sted utenom kartoppslaget."),
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = WatchState.on, onCheckedChange = { on ->
                        if (on) enableWatch() else {
                            Settings.setWatch(ctx, false); StoreWatchService.stop(ctx); WatchState.refresh(ctx)
                        }
                    })
                }
                if (WatchState.on) {
                    Spacer(Modifier.height(8.dp))
                    if (!StoreWatchService.hasBackground(ctx) && Build.VERSION.SDK_INT >= 29) {
                        Text(tr("Tip: choose \"Allow all the time\" so the reminder keeps working after the phone restarts.",
                            "Tips: velg «Tillat hele tiden», så fortsetter varselet å virke etter at telefonen starter på nytt."),
                            fontSize = 13.sp)
                        TextButton(onClick = { bgLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) }) {
                            Text(tr("Allow all the time", "Tillat hele tiden"))
                        }
                    }
                    Text(tr("Samsung and some other phones stop apps in the background. If the reminder stops, set Handleliste's battery to \"Unrestricted\".",
                        "Samsung og noen andre telefoner stopper apper i bakgrunnen. Hvis varselet slutter å komme, sett batteriet for Handleliste til «Ubegrenset»."),
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { openBatterySettings(ctx) }) { Text(tr("Battery settings", "Batteriinnstillinger")) }
                }
                OutlinedButton(onClick = { StoreAlert.show(ctx, "KIWI (test)") }) {
                    Text(tr("Try it: pretend I'm in a store", "Prøv: lat som jeg er i en butikk"))
                }
            }
        }

        Section(tr("Deals", "Tilbud")) {
            var km by remember { mutableFloatStateOf(Settings.radiusKm(ctx).toFloat()) }
            Text(tr("Search stores within ${km.toInt()} km", "Søk i butikker innen ${km.toInt()} km"))
            Slider(value = km, onValueChange = { km = it }, valueRange = 3f..50f,
                onValueChangeFinished = { Settings.setRadiusKm(ctx, km.toInt()) })
            Spacer(Modifier.height(6.dp))
            KeyField(
                title = tr("Kassalapp key (optional, free)", "Kassalapp-nøkkel (valgfri, gratis)"),
                help = tr("Adds today's normal shelf prices per chain, so you can compare beyond flyer deals.",
                    "Legger til dagens vanlige hyllepriser per kjede, så du kan sammenligne utover tilbudsavisene."),
                initial = Settings.kassalKey(ctx), url = "https://kassal.app/api",
                onSave = { Settings.setKassalKey(ctx, it) }
            )
        }

        Section(tr("Dinner ideas", "Middagsforslag")) {
            KeyField(
                title = tr("Gemini key (free)", "Gemini-nøkkel (gratis)"),
                help = tr("From Google AI Studio. Used only to ask for recipes.", "Fra Google AI Studio. Brukes bare til å spørre om oppskrifter."),
                initial = Settings.geminiKey(ctx), url = "https://aistudio.google.com/apikey",
                onSave = { Settings.setGeminiKey(ctx, it) }
            )
        }

        Section(tr("About", "Om appen")) {
            Text(tr("Version ${BuildConfig.VERSION_NAME}", "Versjon ${BuildConfig.VERSION_NAME}"))
            Text(tr("Updates install themselves from GitHub.", "Oppdateringer installeres automatisk fra GitHub."),
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { scope.launch { Updater.check(ctx, force = true) } }) { Text(tr("Check for update", "Se etter oppdatering")) }
            Text(tr("Map data © OpenStreetMap contributors. Deals: eTilbudsavis/Tjek. Prices: Kassalapp.",
                "Kartdata © OpenStreetMap-bidragsytere. Tilbud: eTilbudsavis/Tjek. Priser: Kassalapp."),
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun KeyField(title: String, help: String, initial: String, url: String, onSave: (String) -> Unit) {
    val ctx = LocalContext.current
    var value by remember { mutableStateOf(initial) }
    var saved by remember { mutableStateOf(initial) }
    Text(title, fontWeight = FontWeight.SemiBold)
    Text(help, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(value, { value = it }, Modifier.fillMaxWidth(), singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        placeholder = { Text(tr("Paste key", "Lim inn nøkkel")) })
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { openUrl(ctx, url) }) { Text(tr("Get a key", "Hent nøkkel")) }
        Spacer(Modifier.width(4.dp))
        TextButton(onClick = { onSave(value); saved = value }, enabled = value != saved) {
            Text(if (value == saved && saved.isNotBlank()) tr("Saved ✓", "Lagret ✓") else tr("Save", "Lagre"))
        }
    }
}
