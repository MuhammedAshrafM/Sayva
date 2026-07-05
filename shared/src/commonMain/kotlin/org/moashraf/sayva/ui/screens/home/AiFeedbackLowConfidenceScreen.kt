package org.moashraf.sayva.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.moashraf.sayva.data.MockSayvaData
import org.moashraf.sayva.ui.components.a11yButtonRow
import org.moashraf.sayva.data.RecognitionSuggestion
import org.moashraf.sayva.designsystem.ErrorColor
import org.moashraf.sayva.designsystem.OnSurface
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.Outline
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.PrimaryContainer
import org.moashraf.sayva.designsystem.Secondary50
import org.moashraf.sayva.designsystem.SecondaryContainer
import org.moashraf.sayva.designsystem.SurfaceContainer
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.designsystem.WarningColor
import org.moashraf.sayva.designsystem.WarningContainer
import org.moashraf.sayva.nav.SayvaNavController

@Composable
fun AiFeedbackLowConfidenceScreen(nav: SayvaNavController) {
    val suggestions = MockSayvaData.lowConfidenceSuggestions

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(colors = listOf(Color(0xFF3A3550), Color(0xFF1A1B25), Color(0xFF0A0B12))),
            ),
    ) {
        // Top chrome: close, help.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    .clickable(role = Role.Button, onClickLabel = "Close") { nav.back() }
                    .semantics { contentDescription = "Close" },
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(name = "close", size = 18.dp, color = Color.White, contentDescription = null)
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    .clickable(role = Role.Button, onClickLabel = "Help") { }
                    .semantics { contentDescription = "Help" },
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(name = "help", size = 18.dp, color = Color.White, contentDescription = null)
            }
        }

        // Warning chip.
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 88.dp)
                .background(ErrorColor.copy(alpha = 0.95f), RoundedCornerShape(100))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SymbolIcon(name = "warning_amber", size = 16.dp, color = Color.White, filled = true)
            Spacer(Modifier.width(6.dp))
            Text("LOW CONFIDENCE · 42%", style = MaterialTheme.typography.labelMedium, color = Color.White)
        }

        // Bottom sheet.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xFFFCFCFF), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 32.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(Color(0xFFC7C8D8), RoundedCornerShape(2.dp)),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(32.dp).background(SecondaryContainer, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    SymbolIcon(name = "auto_fix_high", size = 18.dp, color = Secondary50, filled = true)
                }
                Spacer(Modifier.width(10.dp))
                Text("Hmm — did you mean?", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "I'm not 100% sure. Pick the closest match or try again.",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))

            suggestions.forEachIndexed { index, suggestion ->
                SuggestionRow(
                    suggestion = suggestion,
                    highlighted = index == 0,
                    onClick = { nav.back() },
                )
                if (index != suggestions.lastIndex) Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(14.dp))

            // Tip card.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WarningContainer, RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFFBE5B8), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                SymbolIcon(name = "lightbulb", size = 18.dp, color = WarningColor, filled = true)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Try better lighting on your hands & hold the sign for 1 full second.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurface,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(14.dp))

            // Actions.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(SurfaceContainer, RoundedCornerShape(100))
                        .a11yButtonRow(label = "Type instead") { nav.back() }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SymbolIcon(name = "keyboard", size = 16.dp, color = OnSurface, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Type instead", style = MaterialTheme.typography.labelLarge, color = OnSurface)
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(Primary40, RoundedCornerShape(100))
                        .a11yButtonRow(label = "Try again") { nav.back() }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SymbolIcon(name = "replay", size = 16.dp, color = Color.White, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Try again", style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    suggestion: RecognitionSuggestion,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(if (highlighted) 2.dp else 1.dp, if (highlighted) Primary40 else Outline, RoundedCornerShape(14.dp))
            .a11yButtonRow(label = "Pick ${suggestion.sign}, ${suggestion.confidence} percent") { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(if (highlighted) PrimaryContainer else SurfaceContainer, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            SymbolIcon(
                name = suggestion.icon,
                size = 22.dp,
                color = if (highlighted) Primary40 else OnSurfaceVariant,
                filled = highlighted,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(suggestion.sign, style = MaterialTheme.typography.titleSmall)
            Text(suggestion.description, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        }
        Text(
            "${suggestion.confidence}%",
            style = MaterialTheme.typography.labelLarge,
            color = if (highlighted) Color(0xFF1B1E7A) else OnSurfaceVariant,
            modifier = Modifier
                .background(if (highlighted) PrimaryContainer else SurfaceContainer, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
