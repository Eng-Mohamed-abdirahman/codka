package com.codka.app.ui.setup

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.codka.app.R
import com.codka.app.parser.Currency
import com.codka.app.sms.BackgroundSetup
import com.codka.app.ui.setup.SetupFlow.Step
import com.codka.app.ui.theme.CodkaColors
import com.codka.app.voice.AnnounceRequest
import com.codka.app.voice.AnnouncerService
import kotlinx.coroutines.delay

/**
 * Task 23: the one-time setup, for the helper who prepares the seller's phone. One step per
 * screen: a big icon, a short sentence, one button that opens the right Android screen,
 * and a green tick once it's done.
 *
 * [onExit] is null on the very first run (setup has to be finished before Codka can work).
 */
@Composable
fun SetupScreen(onFinished: () -> Unit, onExit: (() -> Unit)?, onOpenTools: () -> Unit) {
    val context = LocalContext.current
    val steps = remember { SetupFlow.steps(BackgroundSetup.isTranssion()) }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var autoStartConfirmed by rememberSaveable { mutableStateOf(false) }
    var voiceHeard by rememberSaveable { mutableStateOf(false) }
    var smsAllowed by remember { mutableStateOf(hasSmsPermission(context)) }
    var batteryOk by remember { mutableStateOf(BackgroundSetup.isBatteryUnrestricted(context)) }
    var smsAsked by rememberSaveable { mutableStateOf(false) }

    // Android's own screens don't report back, so check again every second
    // (also picks up the change when the helper comes back from Settings).
    LaunchedEffect(Unit) {
        while (true) {
            smsAllowed = hasSmsPermission(context)
            batteryOk = BackgroundSetup.isBatteryUnrestricted(context)
            delay(1_000)
        }
    }
    val askPermissions =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            smsAsked = true
            smsAllowed = hasSmsPermission(context)
        }

    val state = SetupFlow.State(smsAllowed, batteryOk, autoStartConfirmed, voiceHeard)
    val step = steps[index]
    val done = SetupFlow.isDone(step, state)
    val isLast = index == steps.lastIndex

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val back: (() -> Unit)? = if (index > 0) ({ index -= 1 }) else onExit
            if (back != null) {
                IconButton(onClick = back) {
                    Icon(painterResource(R.drawable.ic_back), stringResource(R.string.back))
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
            Text(
                stringResource(R.string.setup_step, index + 1, steps.size),
                Modifier.weight(1f),
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.size(48.dp))
        }
        ProgressDots(steps.size, index)

        Column(
            Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            StepIcon(iconOf(step), done)
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(titleOf(step)),
                fontSize = 26.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            when (step) {
                Step.SMS ->
                    if (!done) {
                        BigButton(R.string.setup_sms_button) {
                            askPermissions.launch(smsPermissions())
                        }
                        // Android stops showing the question after "don't allow" twice.
                        if (smsAsked) {
                            TextButton(onClick = { openAppSettings(context) }) {
                                Text(stringResource(R.string.setup_open_settings), fontSize = 18.sp)
                            }
                        }
                    }
                Step.BATTERY ->
                    if (!done) {
                        BigButton(R.string.setup_battery_button) {
                            BackgroundSetup.openBatterySetting(context)
                        }
                    }
                Step.AUTO_START -> {
                    OutlinedButton(
                        onClick = { BackgroundSetup.openAutoStartSetting(context) },
                        modifier = Modifier.fillMaxWidth().height(64.dp)
                    ) { Text(stringResource(R.string.setup_autostart_button), fontSize = 20.sp) }
                    Spacer(Modifier.height(12.dp))
                    if (!done) {
                        BigButton(R.string.setup_autostart_confirm) { autoStartConfirmed = true }
                    }
                }
                Step.VOICE -> {
                    OutlinedButton(
                        onClick = {
                            AnnouncerService.announce(
                                context,
                                AnnounceRequest.Payment(Currency.USD, "5")
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(72.dp)
                    ) {
                        Icon(painterResource(R.drawable.ic_speaker), null, Modifier.size(32.dp))
                        Spacer(Modifier.size(12.dp))
                        Text(stringResource(R.string.setup_voice_button), fontSize = 20.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    if (!done) BigButton(R.string.setup_voice_heard) { voiceHeard = true }
                }
            }
        }

        // Next: a big green button once the step is done; a small "skip" for the steps
        // Android can't always report; nothing until SMS / voice are done.
        when {
            done ->
                Button(
                    onClick = { if (isLast) onFinished() else index++ },
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CodkaColors.Zaad)
                ) {
                    Text(
                        stringResource(if (isLast) R.string.setup_finish else R.string.setup_next),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            SetupFlow.canContinue(step, state) ->
                TextButton(onClick = {
                    index++
                }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.setup_skip), fontSize = 18.sp)
                }
            else -> Spacer(Modifier.height(72.dp))
        }
        TextButton(onClick = onOpenTools, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(R.string.setup_tools), fontSize = 14.sp, color = Color(0xFF777777))
        }
    }
}

@Composable
private fun ProgressDots(count: Int, current: Int) {
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(count) { i ->
            Box(
                Modifier
                    .padding(horizontal = 6.dp)
                    .size(if (i == current) 14.dp else 10.dp)
                    .clip(CircleShape)
                    .background(if (i <= current) CodkaColors.Ink else Color(0xFFCCCCCC))
            )
        }
    }
}

/** The step's picture in a circle; the circle turns green with a tick when it's done. */
@Composable
private fun StepIcon(@DrawableRes icon: Int, done: Boolean) {
    Box(
        Modifier
            .size(160.dp)
            .clip(CircleShape)
            .background(if (done) CodkaColors.Zaad else Color(0xFFE6E6E1)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(if (done) R.drawable.ic_check else icon),
            null,
            Modifier.size(88.dp),
            tint = if (done) Color.White else CodkaColors.Ink
        )
    }
}

@Composable
private fun BigButton(@StringRes text: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(72.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CodkaColors.Ink)
    ) { Text(stringResource(text), fontSize = 22.sp, fontWeight = FontWeight.Bold) }
}

@DrawableRes
private fun iconOf(step: Step) = when (step) {
    Step.SMS -> R.drawable.ic_sms
    Step.BATTERY -> R.drawable.ic_battery
    Step.AUTO_START -> R.drawable.ic_power
    Step.VOICE -> R.drawable.ic_speaker
}

@StringRes
private fun titleOf(step: Step) = when (step) {
    Step.SMS -> R.string.setup_sms_title
    Step.BATTERY -> R.string.setup_battery_title
    Step.AUTO_START -> R.string.setup_autostart_title
    Step.VOICE -> R.string.setup_voice_title
}

fun hasSmsPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) ==
        PackageManager.PERMISSION_GRANTED

/** SMS, plus notifications on Android 13+ so the "Codka is speaking" notice can show. */
private fun smsPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        arrayOf(Manifest.permission.RECEIVE_SMS)
    }

private fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
    )
}
