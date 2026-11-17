package com.codka.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.codka.app.data.CodkaDatabase
import com.codka.app.data.PaymentStore
import com.codka.app.data.Settings
import com.codka.app.sms.BackgroundSetup
import com.codka.app.ui.components.CodkaNavBar
import com.codka.app.ui.components.Tab
import com.codka.app.ui.helper.HelperScreen
import com.codka.app.ui.helper.HelperSettingsScreen
import com.codka.app.ui.history.HistoryScreen
import com.codka.app.ui.home.HomeScreen
import com.codka.app.ui.setup.SetupScreen
import com.codka.app.ui.setup.hasSmsPermission
import com.codka.app.ui.theme.CodkaColors
import com.codka.app.voice.Announcer
import com.codka.app.voice.AnnouncerService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Screen { HOME, HISTORY, HELPER, TOOLS }

/**
 * The app's screens (design board 15, "UX flow"). Two places for the seller, Guriga and
 * Taariikhda, with tabs at the bottom. Setup opens by itself until a helper has finished it.
 * Helper settings hide behind a 2-second hold on the logo, or the red status pill.
 * Data comes straight from the database, so a new payment shows up by itself.
 */
@Composable
fun CodkaApp() {
    val context = LocalContext.current
    val db = remember { CodkaDatabase.get(context) }
    val store = remember { PaymentStore(db.payments()) }
    val settings = remember { Settings(db.settings()) }
    val scope = rememberCoroutineScope()

    // "Now" moves on every 15 s, so the home card returns to listening and "today" rolls over.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var smsOk by remember { mutableStateOf(hasSmsPermission(context)) }
    var batteryOk by remember { mutableStateOf(BackgroundSetup.isBatteryUnrestricted(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            now = System.currentTimeMillis()
        }
    }
    // Permissions can be taken away in Settings at any time: keep checking.
    LaunchedEffect(Unit) {
        while (true) {
            smsOk = hasSmsPermission(context)
            batteryOk = BackgroundSetup.isBatteryUnrestricted(context)
            delay(2_000)
        }
    }
    val day = remember(now) { PaymentStore.dayOf(now, java.util.TimeZone.getDefault()).first }
    val last by remember { store.last() }.collectAsState(initial = null)
    val today by remember(day) { store.today(now) }.collectAsState(initial = emptyList())
    val yesterday by remember(day) { store.yesterday(now) }.collectAsState(initial = emptyList())
    val recent by remember(day) { store.recent(now = now) }.collectAsState(initial = emptyList())
    val speaking by Announcer.speaking.collectAsState()
    // Null while the database is still being read: show nothing rather than flash a screen.
    val setupDone by remember { settings.watchSetupDone() }.collectAsState(initial = null)
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }

    BackHandler(enabled = screen != Screen.HOME) {
        screen = if (screen == Screen.TOOLS) Screen.HELPER else Screen.HOME
    }

    Surface(Modifier.fillMaxSize(), color = CodkaColors.Cream) {
        // Keep everything clear of the status bar and the gesture bar.
        Box(Modifier.safeDrawingPadding()) {
            when {
                setupDone == null -> Unit
                // Until a helper finishes setup, Codka opens on it.
                setupDone == false ->
                    SetupScreen(onFinished = {
                        scope.launch { settings.setSetupDone(true) }
                        screen = Screen.HOME
                    })
                screen == Screen.HELPER ->
                    HelperSettingsScreen(
                        onBack = { screen = Screen.HOME },
                        onRestartSetup = { scope.launch { settings.setSetupDone(false) } },
                        onOpenTools = { screen = Screen.TOOLS }
                    )
                screen == Screen.TOOLS -> HelperScreen(onBack = { screen = Screen.HELPER })
                else ->
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            if (screen == Screen.HISTORY) {
                                HistoryScreen(
                                    today = today,
                                    yesterday = yesterday,
                                    all = recent,
                                    now = now,
                                    onPlay = { Replay.say(context, it) }
                                )
                            } else {
                                HomeScreen(
                                    last = last,
                                    now = now,
                                    speaking = speaking,
                                    ready = smsOk && batteryOk,
                                    onRepeat = { Replay.say(context, it) },
                                    onStop = { AnnouncerService.stop() },
                                    onOpenHelper = { screen = Screen.HELPER }
                                )
                            }
                        }
                        CodkaNavBar(
                            selected = if (screen == Screen.HISTORY) Tab.History else Tab.Home,
                            onSelect = {
                                screen =
                                    if (it == Tab.History) Screen.HISTORY else Screen.HOME
                            }
                        )
                    }
            }
        }
    }
}
