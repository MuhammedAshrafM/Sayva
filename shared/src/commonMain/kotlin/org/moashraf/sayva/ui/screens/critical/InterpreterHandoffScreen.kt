package org.moashraf.sayva.ui.screens.critical

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.moashraf.sayva.data.InterpreterOption
import org.moashraf.sayva.data.MockSayvaData
import org.moashraf.sayva.designsystem.ErrorColor
import org.moashraf.sayva.designsystem.OnSurface
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.Outline
import org.moashraf.sayva.designsystem.OutlineStrong
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.Primary60
import org.moashraf.sayva.designsystem.Secondary50
import org.moashraf.sayva.designsystem.SecondaryContainer
import org.moashraf.sayva.designsystem.SuccessColor
import org.moashraf.sayva.designsystem.Surface
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.designsystem.TertiaryContainer
import org.moashraf.sayva.nav.SayvaNavController

@Composable
fun InterpreterHandoffScreen(nav: SayvaNavController) {
    val options = MockSayvaData.interpreterOptions

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(listOf(Color(0xFF3A3550), Color(0xFF1A1B25), Color(0xFF0A0B12))),
            ),
    ) {
        // Top warning banner.
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 14.dp)
                .padding(top = 16.dp)
                .fillMaxWidth()
                .background(ErrorColor.copy(alpha = 0.95f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SymbolIcon(name = "warning", size = 20.dp, color = Color.White, filled = true)
            Spacer(Modifier.width(10.dp))
            Text(
                "Medical/legal context detected. A certified human interpreter is recommended.",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
        }

        // Bottom sheet.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Surface, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 28.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(OutlineStrong, RoundedCornerShape(2.dp)),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Brush.linearGradient(listOf(Primary40, Secondary50)), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    SymbolIcon(name = "support_agent", size = 22.dp, color = Color.White, filled = true)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Connect to a human", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Certified video relay services · 24/7",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Sayva translates well — but for high-stakes moments (legal, medical, financial) a certified human interpreter is the gold standard.",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    InterpreterCard(option = option, onClick = { nav.back() })
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TertiaryContainer, RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF6BCFAB), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                SymbolIcon(name = "verified", size = 18.dp, color = Color(0xFF005544), filled = true)
                Spacer(Modifier.width(8.dp))
                Text(
                    "VRS calls are free for deaf users in the US (FCC) & UK (NHS). No insurance needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF005544),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFF4F4F8), RoundedCornerShape(100))
                        .clickable { nav.back() }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text("Keep using Sayva", style = MaterialTheme.typography.labelLarge, color = OnSurface)
                }
                Spacer(Modifier.width(8.dp))
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { /* mock - no additional options to show */ }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text("More options", style = MaterialTheme.typography.labelLarge, color = OnSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun InterpreterCard(option: InterpreterOption, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(if (option.featured) 2.dp else 1.dp, if (option.featured) Primary40 else Outline, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (option.featured) Brush.linearGradient(listOf(Primary40, Primary60)) else Brush.linearGradient(listOf(SecondaryContainer, SecondaryContainer)),
                        RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(
                    name = option.icon,
                    size = 18.dp,
                    color = if (option.featured) Color.White else Secondary50,
                    filled = true,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(option.name, style = MaterialTheme.typography.titleSmall)
                Text(option.detail, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
            }
            if (option.featured) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(option.waitTime, style = MaterialTheme.typography.labelLarge, color = SuccessColor)
                    Text(option.priceLabel, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                }
            } else {
                SymbolIcon(name = "chevron_right", size = 20.dp, color = OnSurfaceVariant)
            }
        }
        if (option.featured) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Primary40, RoundedCornerShape(100))
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SymbolIcon(name = "phone_in_talk", size = 16.dp, color = Color.White, filled = true)
                Spacer(Modifier.width(6.dp))
                Text("Call now", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
        }
    }
}
