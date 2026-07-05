package org.moashraf.sayva.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.moashraf.sayva.designsystem.OnSurface
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.OutlineStrong
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.SymbolIcon

/**
 * Auth-flow labeled text field — the editable counterpart of the private
 * `LabeledField` composables that used to live inside each auth screen. The
 * border color animates to Primary40 on focus and the trailing icon toggles
 * password visibility when [isPassword] is set.
 *
 * We use `BasicTextField` (not `OutlinedTextField`) because Material's outlined
 * field imposes its own label-above / notched-border look that would clash with
 * the design here — we want the label inside the border, small.
 */
@Composable
fun AuthTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    enabled: Boolean = true,
    trailingIcon: String? = null,
    onTrailingIconClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (focused) 1.5.dp else 1.dp,
                color = if (focused) Primary40 else OutlineStrong,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (focused) Primary40 else OnSurfaceVariant,
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = LocalTextStyle.current.merge(
                    MaterialTheme.typography.bodyLarge.copy(color = OnSurface),
                ),
                cursorBrush = SolidColor(Primary40),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                    imeAction = imeAction,
                ),
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                interactionSource = interactionSource,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        trailingIcon?.let { icon ->
            Spacer(Modifier.width(8.dp))
            // When the trailing icon is a password-visibility toggle it needs
            // Role.Button + a descriptive label so screen readers announce
            // "Show password, button" rather than a bare "visibility icon".
            val iconModifier = if (onTrailingIconClick != null) {
                val label = when (icon) {
                    "visibility" -> "Hide password"
                    "visibility_off" -> "Show password"
                    else -> "Field action"
                }
                Modifier
                    .clickable(role = Role.Button, onClickLabel = label, onClick = onTrailingIconClick)
                    .semantics { contentDescription = label }
            } else Modifier
            SymbolIcon(
                name = icon,
                size = 20.dp,
                color = OnSurfaceVariant,
                modifier = iconModifier,
                // Parent modifier carries the semantics when interactive;
                // when static, the icon stays decorative alongside the label.
                contentDescription = null,
            )
        }
    }
}

/**
 * Inline error banner shown above the primary CTA on auth screens. Renders
 * nothing when [message] is null so callers can wire it directly to a
 * `state.error?.userMessage()` expression.
 */
@Composable
fun AuthErrorBanner(message: String?, modifier: Modifier = Modifier) {
    if (message.isNullOrBlank()) return
    // `liveRegion = Polite` makes TalkBack/VoiceOver announce the message the
    // moment it appears without stealing focus. `error(message)` also lets
    // screen readers wrapping around the associated field surface the error
    // via the standard "there was an error" affordance.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.errorContainer,
                RoundedCornerShape(10.dp),
            )
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
                error(message)
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        SymbolIcon(
            name = "error",
            size = 18.dp,
            color = MaterialTheme.colorScheme.onErrorContainer,
            filled = true,
            contentDescription = null,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}
