package org.moashraf.sayva.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.moashraf.sayva.designsystem.OutlineStrong
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.SayvaShape
import org.moashraf.sayva.designsystem.SymbolIcon

/**
 * Solid pill CTA — the app's primary action button everywhere.
 *
 * ### Accessibility
 * The whole Row carries `Role.Button` with a merged content description equal
 * to [text], so TalkBack/VoiceOver announces one item ("Sign in, button")
 * rather than reading the leading/trailing icons separately. When [enabled] is
 * false we set the `disabled` semantic — assistive tech announces the button
 * as unavailable and skips it in swipe navigation on some readers.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = Primary40,
    contentColor: Color = Color.White,
    leadingIcon: String? = null,
    trailingIcon: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(SayvaShape.pill)
            .background(if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.4f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = text
                if (!enabled) disabled()
            }
            .padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icons purely decorative — the button semantic on the Row carries the label.
        leadingIcon?.let {
            SymbolIcon(name = it, size = 18.dp, color = contentColor, contentDescription = null)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleSmall, color = contentColor)
        trailingIcon?.let {
            Spacer(Modifier.width(8.dp))
            SymbolIcon(name = it, size = 18.dp, color = contentColor, contentDescription = null)
        }
    }
}

/** Outlined pill — secondary action. Same a11y treatment as [PrimaryButton]. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: String? = null,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(SayvaShape.pill)
            .background(Color.White)
            .border(1.dp, OutlineStrong, SayvaShape.pill)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = text }
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingIcon?.let {
            SymbolIcon(name = it, size = 20.dp, color = Primary40, contentDescription = null)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = contentColor)
    }
}

/** Plain text link, e.g. "Forgot password?". Announced as a button, not plain text. */
@Composable
fun TextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Primary40,
) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
    )
}
