package org.moashraf.sayva.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState

/**
 * Accessibility modifier helpers used across every screen. Consolidated here
 * so per-screen call sites stay one line and the a11y contract is uniform.
 *
 * ### Naming
 * `a11yButtonRow(label, onClick)` — attach to a Card-shaped Row/Box that
 * acts as one big button. Merges descendants so the whole card announces as
 * a single item.
 *
 * `a11yToggleRow(label, checked, onToggle)` — attach to a Row that acts as
 * a switch (background + label + visual toggle rendered inside).
 *
 * `a11yTabRow(label, selected, onSelect)` — attach to a filter-chip Row that
 * acts as a tab between filtered views.
 *
 * `a11yLiveRegion()` — attach to dynamic status text (confidence readouts,
 * timers, streak counters) so screen readers announce updates as they happen.
 */

/**
 * A whole Row/Card that acts as one button. Screen readers announce [label] +
 * "button", and a double-tap invokes [onClick]. Descendants inside are merged
 * so decorative icons and multi-line copy don't get read individually.
 */
fun Modifier.a11yButtonRow(
    label: String,
    onClickLabel: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = this
    .clickable(enabled = enabled, role = Role.Button, onClickLabel = onClickLabel, onClick = onClick)
    .semantics(mergeDescendants = true) { contentDescription = label }

/**
 * A Row that behaves as a two-state switch. [checked] is exposed via
 * `toggleableState` so screen readers say "on" / "off" alongside the label.
 * We keep the click on the parent — pass through here rather than wire it
 * inside the visual switch widget so the whole row is a big target.
 */
fun Modifier.a11yToggleRow(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
): Modifier = this
    .clickable(role = Role.Switch, onClickLabel = if (checked) "Turn off" else "Turn on") { onToggle(!checked) }
    .semantics(mergeDescendants = true) {
        contentDescription = label
        toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
        stateDescription = if (checked) "On" else "Off"
    }

/**
 * A filter-chip Row that behaves as a tab. [isSelected] is exposed so screen
 * readers announce "selected" / "not selected" alongside the label.
 */
fun Modifier.a11yTabRow(
    label: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
): Modifier = this
    .clickable(role = Role.Tab, onClickLabel = "Show $label", onClick = onSelect)
    .semantics(mergeDescendants = true) {
        selected = isSelected
        contentDescription = label
        stateDescription = if (isSelected) "Selected" else "Not selected"
    }

/**
 * Mark a Text/Row that displays live-updating content as a polite live region —
 * screen readers speak new values without stealing focus.
 */
fun Modifier.a11yLiveRegion(): Modifier =
    this.semantics { liveRegion = LiveRegionMode.Polite }
