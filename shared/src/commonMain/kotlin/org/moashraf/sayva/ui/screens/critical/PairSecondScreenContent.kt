package org.moashraf.sayva.ui.screens.critical

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.moashraf.sayva.designsystem.OnSurface
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.OnTertiaryContainer
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.Secondary50
import org.moashraf.sayva.designsystem.SurfaceContainer
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.designsystem.Tertiary50
import org.moashraf.sayva.designsystem.TertiaryContainer
import org.moashraf.sayva.nav.SayvaNavController
import org.moashraf.sayva.ui.components.SayvaTopBar

@Composable
fun PairSecondScreenContent(nav: SayvaNavController) {
    Column(modifier = Modifier.fillMaxSize()) {
        SayvaTopBar(
            onBack = { nav.back() },
            title = "Partner display",
            trailing = {
                Box(
                    modifier = Modifier.size(38.dp).background(SurfaceContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    SymbolIcon(name = "help", size = 18.dp, color = OnSurface)
                }
            },
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Show captions on another screen",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Scan with the second phone or tablet — perfect for restaurants, reception desks, or the other side of a counter.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(18.dp))

        // Mock QR-code placeholder — a simple grid pattern, not a real QR code.
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(200.dp)
                .background(Color.White, RoundedCornerShape(20.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            MockQrGrid(modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Brush.linearGradient(listOf(Primary40, Secondary50)), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    SymbolIcon(name = "sign_language", size = 18.dp, color = Color.White, filled = true)
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "OR ENTER CODE ON OTHER DEVICE",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("7", "4", "9", "2").forEach { digit ->
                    Box(
                        modifier = Modifier
                            .width(38.dp)
                            .height(48.dp)
                            .background(Color.Transparent, RoundedCornerShape(10.dp))
                            .border(2.dp, Primary40, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(digit, style = MaterialTheme.typography.headlineSmall, color = OnSurface)
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(
                "CONNECTED",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TertiaryContainer, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(36.dp).background(Tertiary50, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    SymbolIcon(name = "tablet", size = 18.dp, color = Color.White, filled = true)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Reception iPad", style = MaterialTheme.typography.titleSmall, color = OnTertiaryContainer)
                    Text(
                        "Connected · 2 m ago",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnTertiaryContainer.copy(alpha = 0.75f),
                    )
                }
                Box(modifier = Modifier.size(8.dp).background(Tertiary50, CircleShape))
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .background(OnSurface, RoundedCornerShape(100))
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SymbolIcon(name = "cast", size = 18.dp, color = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Cast to TV", style = MaterialTheme.typography.titleSmall, color = Color.White)
        }
    }
}

/** Static decorative grid that visually stands in for a QR code — not a real scannable code. */
@Composable
private fun MockQrGrid(modifier: Modifier = Modifier) {
    val pattern = remember {
        listOf(
            "1111101011",
            "1000101001",
            "1011101111",
            "1010001000",
            "1111111101",
            "0001000111",
            "1101011001",
            "1001110101",
            "1110001011",
            "1011111010",
        )
    }
    Column(modifier = modifier) {
        pattern.forEach { rowPattern ->
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                rowPattern.forEach { ch ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(if (ch == '1') OnSurface else Color.White),
                    )
                }
            }
        }
    }
}
