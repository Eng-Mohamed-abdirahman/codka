package com.codka.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.codka.app.ui.helper.HelperScreen
import com.codka.app.ui.history.HistoryScreen
import com.codka.app.ui.home.HomeScreen
import com.codka.app.ui.setup.SetupScreen
import com.codka.app.ui.setup.hasSmsPermission
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Screen { HOME, HISTORY, SETUP, TOOLS }

/**
 * The app's screens. Home is where the seller lives; History is one tap away. The setup
 * (task 23) opens by itself until a helper has finished it, then only from the small menu.
 * The phone's back button returns home. Data comes straight from the database, so a new
 * payment shows up by itself.
 */
@Composable
fun CodkaApp() {
    val context = LocalContext.current
    val store = remember { PaymentStore(CodkaDatabase.get(context).payments()) }
    val last by remember { store.last() }.collectAsState(initial = null)
    val today by remember { store.today() }.collectAsState(initial = emptyList())
    val settings = remember { Settings(CodkaDatabase.get(context).settings()) }
    // Null while the database is still being read: show nothing rather than flash a screen.
    val setupDone by remember { settings.watchSetupDone() }.collectAsState(initial = null)
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var smsAllowed by remember { mutableStateOf(hasSmsPermission(context)) }
    val scope = rememberCoroutineScope()

    // The permission can be taken away in Settings at any time: keep checking.
    LaunchedEffect(Unit) {
        while (true) {
            smsAllowed = hasSmsPermission(context)
            delay(2_000)
        }
    }
    BackHandler(enabled = screen != Screen.HOME) {
        screen = if (screen == Screen.TOOLS) Screen.SETUP else Screen.HOME
    }

    Surface(Modifier.fillMaxSize()) {
        // Keep everything clear of the status bar and the gesture bar.
        Box(Modifier.safeDrawingPadding()) {
            if (setupDone == null) return@Box
            // Until a helper finishes setup, Codka opens on it.
            val current = if (setupDone == false && screen != Screen.TOOLS) Screen.SETUP else screen
            when (current) {
                Screen.HOME ->
                    HomeScreen(
                        last = last,
                        today = today,
                        onSayAgain = { Replay.say(context, it) },
                        onOpenHistory = { screen = Screen.HISTORY },
                        onOpenSetup = { screen = Screen.SETUP },
                        smsAllowed = smsAllowed
                    )
                Screen.HISTORY ->
                    HistoryScreen(
                        today = today,
                        onPlay = { Replay.say(context, it) },
                        onBack = { screen = Screen.HOME }
                    )
                Screen.SETUP ->
                    SetupScreen(
                        onFinished = {
                            scope.launch { settings.setSetupDone(true) }
                            screen = Screen.HOME
                        },
                        // The first time, setup must be finished before Codka can work.
                        onExit = if (setupDone == true) ({ screen = Screen.HOME }) else null,
                        onOpenTools = { screen = Screen.TOOLS }
                    )
                Screen.TOOLS -> HelperScreen(onBack = { screen = Screen.SETUP })
            }
        }
    }
}
