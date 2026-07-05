package org.moashraf.sayva.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import org.moashraf.sayva.designsystem.SayvaShape
import org.moashraf.sayva.designsystem.SymbolIcon
import androidx.compose.ui.unit.dp

/** Small rounded label chip, e.g. category tags, status badges. */
@Composable
fun Pill(
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    icon: String? = null,
) {
    // Pills are informational — merge descendants so the icon+text read as one
    // unit ("Medical") instead of the icon getting its own announcement.
    Row(
        modifier = modifier
            .background(backgroundColor, SayvaShape.pill)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            // Icon is redundant with the label text; keep it decorative.
            SymbolIcon(name = it, size = 14.dp, color = contentColor, filled = true, contentDescription = null)
            androidx.compose.foundation.layout.Spacer(Modifier.padding(end = 4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelSmall, color = contentColor)
    }
}
