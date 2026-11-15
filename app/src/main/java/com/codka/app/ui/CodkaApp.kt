package com.codka.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.codka.app.data.CodkaDatabase
import com.codka.app.data.PaymentStore
import com.codka.app.ui.helper.HelperScreen
import com.codka.app.ui.history.HistoryScreen
import com.codka.app.ui.home.HomeScreen

private enum class Screen { HOME, HISTORY, HELPER }

/**
 * The app's three screens. Home is where the seller lives; History and the helper screen
 * are one tap away and the phone's back button returns home.
 * The data comes straight from the database, so a new payment shows up by itself.
 */
@Composable
fun CodkaApp() {
    val context = LocalContext.current
    val store = remember { PaymentStore(CodkaDatabase.get(context).payments()) }
    val last by remember { store.last() }.collectAsState(initial = null)
    val today by remember { store.today() }.collectAsState(initial = emptyList())
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }

    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

    Surface(Modifier.fillMaxSize()) {
        // Keep everything clear of the status bar and the gesture bar.
        Box(Modifier.safeDrawingPadding()) {
            when (screen) {
                Screen.HOME ->
                    HomeScreen(
                        last = last,
                        today = today,
                        onSayAgain = { Replay.say(context, it) },
                        onOpenHistory = { screen = Screen.HISTORY },
                        onOpenHelper = { screen = Screen.HELPER }
                    )
                Screen.HISTORY ->
                    HistoryScreen(
                        today = today,
                        onPlay = { Replay.say(context, it) },
                        onBack = { screen = Screen.HOME }
                    )
                Screen.HELPER -> HelperScreen(onBack = { screen = Screen.HOME })
            }
        }
    }
}
