package com.codka.app.ui.helper

import android.content.Context
import android.content.pm.ApplicationInfo
import android.media.AudioManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codka.app.R
import com.codka.app.parser.Currency
import com.codka.app.sms.BackgroundSetup
import com.codka.app.ui.components.Badge
import com.codka.app.ui.components.ButtonVariant
import com.codka.app.ui.components.CodkaButton
import com.codka.app.ui.components.SubAppBar
import com.codka.app.ui.setup.hasSmsPermission
import com.codka.app.ui.setup.openAppSettings
import com.codka.app.ui.theme.CodkaColors
import com.codka.app.ui.theme.CodkaShape
import com.codka.app.ui.theme.CodkaType
import com.codka.app.voice.AnnounceRequest
import com.codka.app.voice.AnnouncerService
import kotlinx.coroutines.delay

/** Below this media volume the seller may not hear Codka at a busy stall. */
private const val LOW_VOLUME_PERCENT = 50

/**
 * Codka's "admin", for the helper (design boards 14 and 18): reached by holding the logo for
 * 2 seconds, or from the red status pill. The health check comes first, each problem with a
 * "Hagaaji" that opens the exact Android setting.
 */
@Composable
fun HelperSettingsScreen(onBack: () -> Unit, onRestartSetup: () -> Unit, onOpenTools: () -> Unit) {
    val context = LocalContext.current
    var smsOk by remember { mutableStateOf(hasSmsPermission(context)) }
    var batteryOk by remember { mutableStateOf(BackgroundSetup.isBatteryUnrestricted(context)) }
    var volume by remember { mutableIntStateOf(mediaVolumePercent(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            smsOk = hasSmsPermission(context)
            batteryOk = BackgroundSetup.isBatteryUnrestricted(context)
            volume = mediaVolumePercent(context)
            delay(1_000)
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(
            rememberScrollState()
        ).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SubAppBar(stringResource(R.string.helper_title), onBack)

        val smsDetail = if (smsOk) R.string.health_sms_on else R.string.health_sms_off
        val batteryDetail =
            if (batteryOk) R.string.health_battery_ok else R.string.health_battery_limited
        SectionLabel(stringResource(R.string.health_label))
        Group {
            HealthRow(
                ok = smsOk,
                title = stringResource(R.string.health_sms),
                detail = stringResource(smsDetail),
                onFix = { openAppSettings(context) }
            )
            Divider()
            HealthRow(
                ok = batteryOk,
                title = stringResource(R.string.health_battery),
                detail = stringResource(batteryDetail),
                onFix = { BackgroundSetup.openBatterySetting(context) }
            )
            Divider()
            HealthRow(
                ok = volume >= LOW_VOLUME_PERCENT,
                title = stringResource(R.string.health_volume),
                detail = stringResource(R.string.health_volume_detail, volume),
                onFix = { raiseMediaVolume(context) }
            )
            if (BackgroundSetup.isTranssion()) {
                Divider()
                // Android can't tell whether auto-start is on, so this row only offers the screen.
                HealthRow(
                    ok = null,
                    title = stringResource(R.string.health_autostart),
                    detail = stringResource(R.string.health_autostart_detail),
                    onFix = { BackgroundSetup.openAutoStartSetting(context) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        CodkaButton(
            stringResource(R.string.setup_voice_title),
            { AnnouncerService.announce(context, AnnounceRequest.Payment(Currency.USD, "10")) },
            icon = R.drawable.ic_speaker_line
        )
        CodkaButton(
            stringResource(R.string.restart_setup),
            onRestartSetup,
            variant = ButtonVariant.Secondary
        )
        Text(
            stringResource(R.string.about_line, versionName(context)),
            Modifier.align(Alignment.CenterHorizontally),
            style = CodkaType.Caption,
            color = CodkaColors.Muted
        )
        // The SMS log and test buttons are for development only: never in a release build.
        if (isDebugBuild(context)) {
            TextButton(
                onClick = onOpenTools,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    stringResource(R.string.setup_tools),
                    style = CodkaType.Caption,
                    color = CodkaColors.Muted
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = CodkaType.Caption,
        fontWeight = FontWeight.Bold,
        color = CodkaColors.Muted
    )
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(CodkaShape.Md)
            .background(CodkaColors.White)
            .border(1.dp, CodkaColors.Line, CodkaShape.Md)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) { content() }
}

@Composable
private fun Divider() = HorizontalDivider(color = CodkaColors.Line)

/** One item of the health check: green tick, red warning, or (null) a neutral question. */
@Composable
private fun HealthRow(ok: Boolean?, title: String, detail: String, onFix: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when (ok) {
            true -> Badge(
                CodkaColors.Paid50,
                icon = R.drawable.ic_check,
                iconColor = CodkaColors.Paid700
            )
            false -> Badge(
                CodkaColors.Alert50,
                icon = R.drawable.ic_warning,
                iconColor = CodkaColors.Alert700
            )
            null -> Badge(
                CodkaColors.Saffron50,
                icon = R.drawable.ic_question,
                iconColor = CodkaColors.Saffron800
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = CodkaType.Body,
                fontWeight = FontWeight.Bold,
                color = CodkaColors.Ink
            )
            Text(detail, style = CodkaType.Caption, color = CodkaColors.Muted)
        }
        if (ok != true) {
            FixButton(onFix)
        }
    }
}

private fun mediaVolumePercent(context: Context): Int {
    val audio = context.getSystemService(AudioManager::class.java) ?: return 0
    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    return audio.getStreamVolume(AudioManager.STREAM_MUSIC) * 100 / max
}

/** Raises the media volume a step and shows Android's volume slider so the helper can adjust it. */
private fun raiseMediaVolume(context: Context) {
    val audio = context.getSystemService(AudioManager::class.java) ?: return
    audio.adjustStreamVolume(
        AudioManager.STREAM_MUSIC,
        AudioManager.ADJUST_RAISE,
        AudioManager.FLAG_SHOW_UI
    )
}

private fun versionName(context: Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull()
    ?: ""

private fun isDebugBuild(context: Context) =
    (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

/** The small "Hagaaji" button of a health-check row (board 14): 44 dp, 16 sp, Indigo outline. */
@Composable
private fun FixButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(44.dp),
        shape = CodkaShape.Md,
        border = BorderStroke(2.dp, CodkaColors.Indigo),
        contentPadding = PaddingValues(horizontal = 14.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = CodkaColors.Indigo)
    ) {
        Text(
            stringResource(R.string.fix),
            style = CodkaType.Button.copy(fontSize = CodkaType.Body.fontSize),
            maxLines = 1,
            softWrap = false
        )
    }
}
