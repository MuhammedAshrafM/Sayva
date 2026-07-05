package org.moashraf.sayva.designsystem

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sayva.shared.generated.designsystem.materialSymbolsFilledFamily
import sayva.shared.generated.designsystem.materialSymbolsOutlineFamily

/**
 * Renders a Material Symbols Rounded icon as a styled glyph.
 *
 * Accessibility: pass a non-null [contentDescription] when the icon conveys
 * meaning to the user (e.g. a button's action). Pass `null` when the icon is
 * purely decorative — a redundant accent next to labelled text — so screen
 * readers skip it entirely.
 *
 * The `null` default here is temporary during the Phase 1.5 migration
 * (see docs/phase-1-tickets.md P1-06). Remove the default once all call
 * sites are explicit — see the cleanup step after P1-36.
 */
@Composable
fun SymbolIcon(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = Color.Unspecified,
    filled: Boolean = false,
    contentDescription: String? = null,
) {
    val fontFamily = if (filled) materialSymbolsFilledFamily() else materialSymbolsOutlineFamily()
    val a11yModifier = if (contentDescription == null) {
        Modifier.clearAndSetSemantics { }
    } else {
        Modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        }
    }
    BasicText(
        text = MaterialSymbol.glyph(name),
        modifier = modifier.size(size).then(a11yModifier),
        style = TextStyle(
            fontFamily = fontFamily,
            fontSize = size.value.sp,
            color = color,
            textAlign = TextAlign.Center,
        ),
    )
}
