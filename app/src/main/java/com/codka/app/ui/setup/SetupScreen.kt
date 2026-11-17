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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.codka.app.R
import com.codka.app.parser.Currency
import com.codka.app.sms.BackgroundSetup
import com.codka.app.ui.components.BigIcon
import com.codka.app.ui.components.ButtonVariant
import com.codka.app.ui.components.CodkaButton
import com.codka.app.ui.components.Dots
import com.codka.app.ui.components.RepeatButton
import com.codka.app.ui.components.RepeatState
import com.codka.app.ui.setup.SetupFlow.Step
import com.codka.app.ui.theme.CodkaColors
import com.codka.app.ui.theme.CodkaShape
import com.codka.app.ui.theme.CodkaType
import com.codka.app.voice.AnnounceRequest
import com.codka.app.voice.AnnouncerService
import kotlinx.coroutines.delay

/**
 * Task 23, in the Codka design (boards 12, 16, 17): the helper's first three minutes.
 * One decision per screen, one Saffron button at the bottom, progress dots at the top.
 */
@Composable
fun SetupScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val steps = SetupFlow.steps
    var index by rememberSaveable { mutableIntStateOf(0) }
    var voiceHeard by rememberSaveable { mutableStateOf(false) }
    var smsAsked by rememberSaveable { mutableStateOf(false) }
    var smsAllowed by remember { mutableStateOf(hasSmsPermission(context)) }
    var batteryOk by remember { mutableStateOf(BackgroundSetup.isBatteryUnrestricted(context)) }

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

    val state = SetupFlow.State(smsAllowed, batteryOk, voiceHeard)
    val next: () -> Unit = { if (index == steps.lastIndex) onFinished() else index += 1 }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 20.dp)) {
        Dots(steps.size, index, Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp))
        when (steps[index]) {
            Step.WELCOME -> Welcome(next)
            Step.SMS ->
                SmsStep(
                    allowed = state.smsAllowed,
                    refused = smsAsked && !state.smsAllowed,
                    onAllow = { askPermissions.launch(smsPermissions()) },
                    onOpenSettings = { openAppSettings(context) },
                    onNext = next
                )
            Step.BATTERY ->
                BatteryStep(
                    done = state.batteryUnrestricted,
                    isTranssion = remember { BackgroundSetup.isTranssion() },
                    onOpen = { BackgroundSetup.openBatterySetting(context) },
                    onOpenAutoStart = { BackgroundSetup.openAutoStartSetting(context) },
                    onNext = next
                )
            Step.VOICE ->
                VoiceStep(
                    onPlay = {
                        AnnouncerService.announce(
                            context,
                            AnnounceRequest.Payment(Currency.USD, "10")
                        )
                    },
                    onHeard = {
                        voiceHeard = true
                        next()
                    }
                )
        }
    }
}

/** S1: logo, what Codka does, "your messages stay on this phone", Bilow. */
@Composable
private fun ColumnScope.Welcome(onStart: () -> Unit) {
    Column(
        Modifier.weight(1f).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically)
    ) {
        Icon(
            painterResource(R.drawable.ic_logo),
            null,
            Modifier.size(144.dp),
            tint = Color.Unspecified
        )
        Text(
            "codka",
            style = CodkaType.Display.copy(fontSize = 56.sp, lineHeight = 52.sp),
            color = CodkaColors.Indigo
        )
        Text(
            stringResource(R.string.welcome_title),
            style = CodkaType.Title,
            color = CodkaColors.Indigo
        )
        Text(
            stringResource(R.string.welcome_body),
            Modifier.widthIn(max = 290.dp),
            style = CodkaType.BodyL,
            color = CodkaColors.Muted,
            textAlign = TextAlign.Center
        )
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(CodkaShape.Lg)
            .background(CodkaColors.Indigo50)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painterResource(R.drawable.ic_shield),
            null,
            Modifier.size(24.dp),
            tint = CodkaColors.Indigo
        )
        Spacer(Modifier.size(14.dp))
        Text(stringResource(R.string.private_note), style = CodkaType.Body, color = CodkaColors.Ink)
    }
    Spacer(Modifier.height(20.dp))
    CodkaButton(stringResource(R.string.setup_start), onStart)
}

/** S2: one permission, with a plain reason. Three looks: to do, done, refused (board 12). */
@Composable
private fun ColumnScope.SmsStep(
    allowed: Boolean,
    refused: Boolean,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    onNext: () -> Unit
) {
    StepBody(
        icon =
        when {
            allowed -> StepIcon(R.drawable.ic_check, CodkaColors.Paid50, CodkaColors.Paid700)
            refused -> StepIcon(
                R.drawable.ic_question,
                CodkaColors.Saffron50,
                CodkaColors.Saffron800
            )
            else -> StepIcon(R.drawable.ic_message)
        },
        title =
        stringResource(
            when {
                allowed -> R.string.setup_sms_done
                refused -> R.string.setup_sms_refused
                else -> R.string.setup_sms_title
            }
        ),
        titleColor = if (allowed) CodkaColors.Paid700 else CodkaColors.Indigo,
        body =
        stringResource(
            when {
                allowed -> R.string.setup_sms_done_body
                refused -> R.string.setup_sms_refused_body
                else -> R.string.setup_sms_body
            }
        )
    ) {
        if (!allowed && !refused) CheckLine(stringResource(R.string.setup_no_internet))
    }
    when {
        allowed -> CodkaButton(stringResource(R.string.setup_next), onNext)
        refused -> CodkaButton(stringResource(R.string.setup_open_settings), onOpenSettings)
        else -> CodkaButton(stringResource(R.string.setup_sms_button), onAllow)
    }
}

/** S3: keep Codka running. Tecno / Infinix / itel also need auto-start. */
@Composable
private fun ColumnScope.BatteryStep(
    done: Boolean,
    isTranssion: Boolean,
    onOpen: () -> Unit,
    onOpenAutoStart: () -> Unit,
    onNext: () -> Unit
) {
    StepBody(
        icon =
        if (done) {
            StepIcon(R.drawable.ic_check, CodkaColors.Paid50, CodkaColors.Paid700)
        } else {
            StepIcon(R.drawable.ic_battery)
        },
        title = stringResource(
            if (done) R.string.setup_battery_done else R.string.setup_battery_title
        ),
        titleColor = if (done) CodkaColors.Paid700 else CodkaColors.Indigo,
        body = stringResource(R.string.setup_battery_body)
    ) {
        if (isTranssion) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(CodkaShape.Md)
                    .background(CodkaColors.White)
                    .border(1.dp, CodkaColors.Line, CodkaShape.Md)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("TECNO · INFINIX · ITEL", style = CodkaType.Caption, color = CodkaColors.Muted)
                Text(
                    stringResource(R.string.setup_autostart_tip),
                    style = CodkaType.Body,
                    color = CodkaColors.Ink
                )
                TextButton(onClick = onOpenAutoStart) {
                    Text(
                        stringResource(R.string.setup_autostart_button),
                        style = CodkaType.Button,
                        color = CodkaColors.Indigo
                    )
                }
            }
        }
    }
    if (done) {
        CodkaButton(stringResource(R.string.setup_next), onNext)
    } else {
        CodkaButton(stringResource(R.string.setup_open_settings), onOpen)
        Spacer(Modifier.height(12.dp))
        CodkaButton(
            stringResource(R.string.setup_did_it),
            onNext,
            variant = ButtonVariant.Secondary,
            height = 52.dp
        )
    }
}

/** S4: hear the test, then "Haa, waan maqlay" finishes setup; "Maya" shows what to check. */
@Composable
private fun ColumnScope.VoiceStep(onPlay: () -> Unit, onHeard: () -> Unit) {
    var notHeard by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.weight(1f).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
    ) {
        Text(
            stringResource(R.string.setup_voice_title),
            style = CodkaType.Display.copy(fontSize = 30.sp),
            color = CodkaColors.Indigo
        )
        RepeatButton(
            state = RepeatState.Idle,
            onClick = onPlay,
            label = stringResource(R.string.play),
            icon = R.drawable.ic_play
        )
        Text(
            stringResource(R.string.setup_voice_body),
            Modifier.widthIn(max = 300.dp),
            style = CodkaType.BodyL,
            color = CodkaColors.Muted,
            textAlign = TextAlign.Center
        )
        Text(
            stringResource(R.string.setup_voice_question),
            style = CodkaType.Title.copy(fontSize = 24.sp),
            color = CodkaColors.Indigo
        )
        if (notHeard) {
            Text(
                stringResource(R.string.setup_voice_not_heard),
                Modifier
                    .fillMaxWidth()
                    .clip(CodkaShape.Lg)
                    .background(CodkaColors.Saffron50)
                    .padding(16.dp),
                style = CodkaType.Body,
                color = CodkaColors.Saffron800
            )
        }
    }
    CodkaButton(
        stringResource(R.string.setup_voice_heard),
        onHeard,
        variant = ButtonVariant.Success
    )
    Spacer(Modifier.height(12.dp))
    CodkaButton(
        stringResource(R.string.setup_voice_no),
        { notHeard = true },
        variant = ButtonVariant.Secondary,
        height = 52.dp
    )
}

private data class StepIcon(
    val icon: Int,
    val bg: Color = CodkaColors.Saffron100,
    val fg: Color = CodkaColors.Indigo
)

/** The middle of a step: big icon in a circle, title, a plain reason, then extras. */
@Composable
private fun ColumnScope.StepBody(
    icon: StepIcon,
    title: String,
    titleColor: Color,
    body: String,
    extra: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier.weight(1f).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
    ) {
        BigIcon(icon.icon, icon.bg, icon.fg)
        Text(
            title,
            style = CodkaType.Display.copy(fontSize = 30.sp, lineHeight = 35.sp),
            color = titleColor
        )
        Text(body, style = CodkaType.BodyL, color = CodkaColors.Muted)
        extra()
    }
}

@Composable
private fun CheckLine(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(CodkaShape.Md)
            .background(CodkaColors.White)
            .border(1.dp, CodkaColors.Line, CodkaShape.Md)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        com.codka.app.ui.components.Badge(
            CodkaColors.Paid50,
            icon = R.drawable.ic_check,
            iconColor = CodkaColors.Paid700
        )
        Text(text, style = CodkaType.Body, color = CodkaColors.Ink)
    }
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

fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
    )
}
