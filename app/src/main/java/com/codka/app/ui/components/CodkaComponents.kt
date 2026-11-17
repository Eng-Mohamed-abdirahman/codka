package com.codka.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.codka.app.R
import com.codka.app.ui.theme.CodkaColors
import com.codka.app.ui.theme.CodkaShape
import com.codka.app.ui.theme.CodkaSize
import com.codka.app.ui.theme.CodkaType

/** Shared pieces of the Codka design (boards 07–12). */

enum class ButtonVariant { Primary, Secondary, Danger, Success }

/** A full-width 60 dp button with a short Somali verb (board 08). One Primary per screen. */
@Composable
fun CodkaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    @DrawableRes icon: Int? = null,
    height: Dp = CodkaSize.ButtonH
) {
    val content: @Composable () -> Unit = {
        if (icon != null) {
            Icon(painterResource(icon), null, Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = CodkaType.Button, textAlign = TextAlign.Center)
    }
    val m = modifier.fillMaxWidth().height(height)
    if (variant == ButtonVariant.Secondary) {
        OutlinedButton(
            onClick = onClick,
            modifier = m,
            shape = CodkaShape.Md,
            border = androidx.compose.foundation.BorderStroke(2.dp, CodkaColors.Indigo),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CodkaColors.Indigo)
        ) { content() }
    } else {
        val (bg, fg) =
            when (variant) {
                ButtonVariant.Danger -> CodkaColors.Alert700 to CodkaColors.White
                ButtonVariant.Success -> CodkaColors.Paid700 to CodkaColors.White
                else -> CodkaColors.Saffron to CodkaColors.Ink
            }
        Button(
            onClick = onClick,
            modifier = m,
            shape = CodkaShape.Md,
            contentPadding = PaddingValues(horizontal = 24.dp),
            colors =
            ButtonDefaults.buttonColors(
                containerColor = bg,
                contentColor = fg,
                disabledContainerColor = CodkaColors.Sand,
                disabledContentColor = CodkaColors.Muted
            )
        ) { content() }
    }
}

enum class RepeatState { Idle, Speaking, Disabled }

/**
 * "Ku celi", the heart of Codka (board 08): a 168 dp Saffron circle with a 12 dp halo.
 * Tap to replay the last payment; tap while speaking to stop.
 */
@Composable
fun RepeatButton(
    state: RepeatState,
    onClick: () -> Unit,
    label: String,
    @DrawableRes icon: Int = R.drawable.ic_speaker_line
) {
    val (bg, fg, halo) =
        when (state) {
            RepeatState.Idle -> Triple(
                CodkaColors.Saffron,
                CodkaColors.Indigo,
                CodkaColors.Saffron100
            )
            RepeatState.Speaking -> Triple(
                CodkaColors.Indigo,
                CodkaColors.Saffron,
                CodkaColors.Saffron100
            )
            RepeatState.Disabled -> Triple(CodkaColors.Sand, CodkaColors.Muted, CodkaColors.Cream)
        }
    Box(
        Modifier
            .size(CodkaSize.Repeat + CodkaSize.RepeatHalo * 2)
            .clip(CodkaShape.Pill)
            .background(halo),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onClick,
            enabled = state != RepeatState.Disabled,
            modifier = Modifier.size(CodkaSize.Repeat),
            shape = CodkaShape.Pill,
            contentPadding = PaddingValues(0.dp),
            colors =
            ButtonDefaults.buttonColors(
                containerColor = bg,
                contentColor = fg,
                disabledContainerColor = bg,
                disabledContentColor = fg
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (state == RepeatState.Speaking) {
                    Wave(CodkaColors.Saffron, listOf(16, 34, 48, 28, 40))
                } else {
                    Icon(painterResource(icon), null, Modifier.size(56.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    label,
                    style = CodkaType.Heading,
                    color = if (state == RepeatState.Speaking) CodkaColors.Cream else fg
                )
            }
        }
    }
}

/** The speaking wave: a few rounded bars (board 09). */
@Composable
fun Wave(color: Color, heights: List<Int>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEach { h ->
            Box(Modifier.width(6.dp).height(h.dp).clip(RoundedBar).background(color))
        }
    }
}

private val RoundedBar = androidx.compose.foundation.shape.RoundedCornerShape(3.dp)

/** A 44 dp provider badge with letters ("Z", "eD") or an icon (board 10). */
@Composable
fun Badge(
    color: Color,
    text: String? = null,
    @DrawableRes icon: Int? = null,
    iconColor: Color = Color.White
) {
    Box(
        Modifier.size(CodkaSize.Badge).clip(CodkaShape.Badge).background(color),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(painterResource(icon), null, Modifier.size(22.dp), tint = iconColor)
        } else if (text != null) {
            Text(
                text,
                style = CodkaType.Button.copy(fontSize = CodkaType.Caption.fontSize),
                color = Color.White
            )
        }
    }
}

/** A pill tag: provider, currency or status (board 08). */
@Composable
fun Tag(text: String, bg: Color, fg: Color, @DrawableRes icon: Int? = null, badge: String? = null) {
    Row(
        Modifier.height(36.dp).clip(CodkaShape.Pill).background(bg).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (icon != null) Icon(painterResource(icon), null, Modifier.size(18.dp), tint = fg)
        if (badge !=
            null
        ) {
            Text(
                badge,
                style = CodkaType.Button.copy(fontSize = CodkaType.Caption.fontSize),
                color = fg
            )
        }
        Text(text, style = CodkaType.Button.copy(fontSize = CodkaType.Caption.fontSize), color = fg)
    }
}

/** The 52 dp round Saffron play button of a history row. */
@Composable
fun PlayButton(onClick: () -> Unit, label: String) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(CodkaSize.Play),
        colors =
        IconButtonDefaults.iconButtonColors(
            containerColor = CodkaColors.Saffron,
            contentColor = CodkaColors.Indigo
        )
    ) { Icon(painterResource(R.drawable.ic_play), label, Modifier.size(22.dp)) }
}

/** A big icon in a filled circle (setup steps, empty states). */
@Composable
fun BigIcon(
    @DrawableRes icon: Int,
    bg: Color = CodkaColors.Saffron100,
    fg: Color = CodkaColors.Indigo,
    size: Dp = 112.dp
) {
    Box(
        Modifier.size(size).clip(CodkaShape.Pill).background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(icon), null, Modifier.size(size / 2), tint = fg)
    }
}

/** Progress dots: the current step is a wide Indigo pill (board 12). */
@Composable
fun Dots(total: Int, current: Int, modifier: Modifier = Modifier) {
    val desc = stringResource(R.string.setup_step, current + 1, total)
    Row(
        modifier.semantics { contentDescription = desc },
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(total) { i ->
            Box(
                Modifier
                    .height(10.dp)
                    .width(if (i == current) 28.dp else 10.dp)
                    .clip(CodkaShape.Pill)
                    .background(if (i == current) CodkaColors.Indigo else CodkaColors.Line)
            )
        }
    }
}

/** 56 dp round back button + title, for helper and setup screens (board 07). */
@Composable
fun SubAppBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(CodkaSize.TouchMin),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(CodkaSize.TouchMin),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = CodkaColors.Sand,
                contentColor = CodkaColors.Indigo
            )
        ) {
            Icon(
                painterResource(R.drawable.ic_back),
                stringResource(R.string.back),
                Modifier.size(26.dp)
            )
        }
        Text(title, style = CodkaType.Title, color = CodkaColors.Indigo)
    }
}

/**
 * The home app bar (board 07): logo on the left (hold it for 2 s → helper settings),
 * status pill on the right: green "Diyaar" or red "Ma shaqeynayo" (tap → helper settings).
 */
@Composable
fun HomeAppBar(ready: Boolean, onOpenHelper: () -> Unit) {
    val helperLabel = stringResource(R.string.open_helper)
    Row(
        Modifier.fillMaxWidth().height(CodkaSize.TouchMin),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            Modifier
                .clip(CodkaShape.Md)
                .combinedClickable(onClick = {
                }, onLongClick = onOpenHelper, onLongClickLabel = helperLabel)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                painterResource(R.drawable.ic_logo),
                null,
                Modifier.size(36.dp),
                tint = Color.Unspecified
            )
            Text(
                "codka",
                style = CodkaType.Display.copy(fontSize = CodkaType.Title.fontSize),
                color = CodkaColors.Indigo
            )
        }
        val (text, bg, fg, icon) =
            if (ready) {
                StatusPill(
                    stringResource(R.string.status_ready),
                    CodkaColors.Paid50,
                    CodkaColors.Paid700,
                    R.drawable.ic_check
                )
            } else {
                StatusPill(
                    stringResource(R.string.status_broken),
                    CodkaColors.Alert50,
                    CodkaColors.Alert700,
                    R.drawable.ic_warning
                )
            }
        Box(
            Modifier
                .clip(CodkaShape.Pill)
                .combinedClickable(onClick = { if (!ready) onOpenHelper() })
                .semantics { role = Role.Button }
        ) { Tag(text, bg, fg, icon) }
    }
}

private data class StatusPill(
    val text: String,
    val bg: Color,
    val fg: Color,
    @DrawableRes val icon: Int
)

enum class Tab { Home, History }

/** Two places only: Guriga and Taariikhda (board 07). 76 dp, icon in a 64 × 32 pill. */
@Composable
fun CodkaNavBar(selected: Tab, onSelect: (Tab) -> Unit) {
    // A single line on top, like the design (not a box).
    androidx.compose.material3.HorizontalDivider(color = CodkaColors.Line)
    Row(
        Modifier
            .fillMaxWidth()
            .height(CodkaSize.NavBar)
            .background(CodkaColors.White)
    ) {
        NavItem(Tab.Home, R.drawable.ic_home, stringResource(R.string.tab_home), selected, onSelect)
        NavItem(
            Tab.History,
            R.drawable.ic_history,
            stringResource(R.string.tab_history),
            selected,
            onSelect
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavItem(
    tab: Tab,
    @DrawableRes icon: Int,
    label: String,
    selected: Tab,
    onSelect: (Tab) -> Unit
) {
    val on = tab == selected
    val color = if (on) CodkaColors.Indigo else CodkaColors.Muted
    Column(
        Modifier
            .weight(1f)
            .height(CodkaSize.NavBar)
            .combinedClickable(onClick = { onSelect(tab) })
            .semantics { role = Role.Tab },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(width = 64.dp, height = 32.dp)
                .clip(CodkaShape.Pill)
                .background(if (on) CodkaColors.Saffron100 else Color.Transparent),
            contentAlignment = Alignment.Center
        ) { Icon(painterResource(icon), null, Modifier.size(26.dp), tint = color) }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = CodkaType.Button.copy(fontSize = CodkaType.Caption.fontSize),
            color = color
        )
    }
}
