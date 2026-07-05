package org.moashraf.sayva.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import org.moashraf.sayva.designsystem.OnPrimaryContainer
import org.moashraf.sayva.designsystem.OnSecondaryContainer
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.OnTertiaryContainer
import org.moashraf.sayva.designsystem.Outline
import org.moashraf.sayva.designsystem.PrimaryContainer
import org.moashraf.sayva.designsystem.SecondaryContainer
import org.moashraf.sayva.designsystem.Surface
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.designsystem.SayvaShape
import org.moashraf.sayva.designsystem.TertiaryContainer
import org.moashraf.sayva.nav.BottomTab
import org.moashraf.sayva.nav.Screen

/** Back-arrow (+ optional title / trailing slot) row used on most non-root screens. */
@Composable
fun SayvaTopBar(
    onBack: (() -> Unit)? = null,
    title: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            // `onClickLabel` is announced by TalkBack/VoiceOver on a double-tap;
            // sighted-user text is redundant because the arrow icon is universal.
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(
                        onClickLabel = "Go back",
                        role = Role.Button,
                        onClick = onBack,
                    )
                    .semantics { contentDescription = "Back" },
                contentAlignment = Alignment.Center,
            ) {
                // Icon is decorative — the parent Box carries the semantics.
                SymbolIcon(name = "arrow_back", size = 22.dp, color = OnSurfaceVariant, contentDescription = null)
            }
        } else {
            androidx.compose.foundation.layout.Spacer(Modifier.size(40.dp))
        }
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 4.dp).weight(1f),
            )
        } else {
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
    }
}

private data class TabSpec(
    val tab: BottomTab,
    val icon: String,
    val label: String,
    val selectedBg: Color,
    val selectedFg: Color,
)

private val tabs = listOf(
    TabSpec(BottomTab.Home, "home", "Home", PrimaryContainer, OnPrimaryContainer),
    TabSpec(BottomTab.Translate, "sign_language", "Translate", SecondaryContainer, OnSecondaryContainer),
    TabSpec(BottomTab.Learn, "school", "Learn", TertiaryContainer, OnTertiaryContainer),
    TabSpec(BottomTab.You, "person", "You", PrimaryContainer, OnPrimaryContainer),
)

/** The 4-tab Home/Translate/Learn/You bottom bar shown on each tab's root screen. */
@Composable
fun SayvaBottomNav(
    current: Screen,
    onSelect: (BottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxWidth().background(Surface)) {
        androidx.compose.material3.HorizontalDivider(color = Outline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            tabs.forEach { spec ->
                val isSelected = spec.tab.screen::class == current::class
                val color = if (isSelected) spec.selectedFg else OnSurfaceVariant
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            onClickLabel = "Open ${spec.label}",
                            role = Role.Tab,
                            onClick = { onSelect(spec.tab) },
                        )
                        .semantics {
                            selected = isSelected
                            contentDescription = spec.label
                            stateDescription = if (isSelected) "Selected" else "Not selected"
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier
                            .background(if (isSelected) spec.selectedBg else Color.Transparent, SayvaShape.md)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Icon + label together — both decorative, parent carries semantics.
                        SymbolIcon(name = spec.icon, size = 22.dp, color = color, filled = isSelected, contentDescription = null)
                        Text(spec.label, style = MaterialTheme.typography.labelSmall, color = color)
                    }
                }
            }
        }
    }
}
