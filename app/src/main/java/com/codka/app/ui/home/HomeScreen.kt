package com.codka.app.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.codka.app.R
import com.codka.app.parser.Currency
import com.codka.app.sms.BackgroundSetup
import com.codka.app.sms.SpikeLog
import com.codka.app.voice.AnnounceRequest
import com.codka.app.voice.AnnouncerService
import kotlinx.coroutines.delay

// Task 07 spike screen: ask for SMS permission and show the spike log.
// The real home screen (big amount + repeat button) is task 20.
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var log by remember { mutableStateOf(SpikeLog.read(context)) }
    val requestPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasPermission = granted
        }

    var batteryOk by remember { mutableStateOf(BackgroundSetup.isBatteryUnrestricted(context)) }
    val isTranssion = remember { BackgroundSetup.isTranssion() }

    // Refresh every 2 seconds while the screen is open (also picks up the battery
    // setting when the user comes back from Settings).
    LaunchedEffect(Unit) {
        while (true) {
            log = SpikeLog.read(context)
            batteryOk = BackgroundSetup.isBatteryUnrestricted(context)
            delay(2_000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.spike_title), style = MaterialTheme.typography.headlineMedium)

        if (hasPermission) {
            Text(stringResource(R.string.spike_permission_ok))
        } else {
            Text(stringResource(R.string.spike_permission_needed))
            Button(onClick = { requestPermission.launch(Manifest.permission.RECEIVE_SMS) }) {
                Text(stringResource(R.string.spike_allow_sms))
            }
        }

        // Without these, phones like Infinix freeze Codka and drop the payment SMS.
        Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.titleMedium)
        if (batteryOk) {
            Text(stringResource(R.string.setup_battery_ok))
        } else {
            Text(stringResource(R.string.setup_battery_needed))
            Button(onClick = { BackgroundSetup.openBatterySetting(context) }) {
                Text(stringResource(R.string.setup_battery_button))
            }
        }
        // Android can't tell us whether App Launch is on, so the button always shows.
        if (isTranssion) {
            Text(stringResource(R.string.setup_autostart_needed))
            OutlinedButton(onClick = { BackgroundSetup.openAutoStartSetting(context) }) {
                Text(stringResource(R.string.setup_autostart_button))
            }
        }

        // Task 18: hear the announcer without waiting for a real payment.
        Text(
            stringResource(R.string.voice_test_title),
            style = MaterialTheme.typography.titleMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                AnnouncerService.announce(
                    context,
                    AnnounceRequest.Payment(Currency.USD, "45.25", "633000001")
                )
            }) { Text(stringResource(R.string.voice_test_payment)) }
            OutlinedButton(onClick = {
                AnnouncerService.announce(
                    context,
                    AnnounceRequest.Payment(Currency.SLSH, "1250000", "+252 654 000 004")
                )
            }) { Text(stringResource(R.string.voice_test_number)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { AnnouncerService.announce(context, AnnounceRequest.Fake) }) {
                Text(stringResource(R.string.voice_test_fake))
            }
            OutlinedButton(onClick = {
                AnnouncerService.announce(context, AnnounceRequest.Unknown)
            }) {
                Text(stringResource(R.string.voice_test_unknown))
            }
        }

        OutlinedButton(onClick = {
            SpikeLog.clear(context)
            log = emptyList()
        }) {
            Text(stringResource(R.string.spike_clear_log))
        }

        if (log.isEmpty()) {
            Text(stringResource(R.string.spike_log_empty))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(log.reversed()) { line ->
                    Text(
                        line,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
