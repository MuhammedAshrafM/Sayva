package org.moashraf.sayva.ui.screens.critical

import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.moashraf.sayva.designsystem.OnSurface
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.Outline
import org.moashraf.sayva.designsystem.OutlineStrong
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.Primary60
import org.moashraf.sayva.designsystem.Secondary50
import org.moashraf.sayva.designsystem.SurfaceContainer
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.designsystem.Tertiary50
import org.moashraf.sayva.nav.Screen
import org.moashraf.sayva.nav.SayvaNavController
import org.moashraf.sayva.ui.components.PrimaryButton
import org.moashraf.sayva.ui.components.SecondaryButton

@Composable
fun FirstLaunchModelDownloadScreen(nav: SayvaNavController) {
    // Canned, mocked progress — no real download happens.
    val progress = remember { mutableStateOf(0.62f) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Brush.linearGradient(listOf(Primary40, Secondary50)), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    SymbolIcon(name = "sign_language", size = 22.dp, color = Color.White, filled = true)
                }
                Spacer(Modifier.width(8.dp))
                Text("Sayva", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Setting up for you",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "One-time download · then 100% offline. Future updates are tiny patches.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        // Animated download visual (static mock).
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp)
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    Brush.linearGradient(listOf(Color(0xFFE5E6FF), Color(0xFFFCFCFF))),
                    RoundedCornerShape(24.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    SymbolIcon(name = "cloud", size = 28.dp, color = Primary40, filled = true)
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    DotDecor(alpha = 1f)
                    DotDecor(alpha = 0.7f)
                    DotDecor(alpha = 0.4f)
                    DotDecor(alpha = 0.2f)
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Primary40, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    SymbolIcon(name = "smartphone", size = 28.dp, color = Color.White, filled = true)
                }
            }
        }

        // Progress card.
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp)
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(18.dp))
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("ASL model · v2026.06", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${(progress.value * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    color = Primary40,
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(8.dp).background(Outline, RoundedCornerShape(4.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.value)
                        .height(8.dp)
                        .background(Brush.horizontalGradient(listOf(Primary40, Primary60)), RoundedCornerShape(4.dp)),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "113 of 182 MB · 24 s left",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SymbolIcon(name = "wifi", size = 14.dp, color = OnSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text("Wi-Fi", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                }
            }
        }

        // Step list.
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StepRow(done = true, active = false, label = "Account synced", trailing = "1 s")
            StepRow(done = false, active = true, label = "Downloading model", trailing = "In progress")
            StepRow(done = false, active = false, icon = "verified", label = "Verifying integrity", trailing = "~3 s")
            StepRow(done = false, active = false, icon = "rocket_launch", label = "Ready to translate", trailing = null)
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
                .background(SurfaceContainer, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SymbolIcon(name = "battery_saver", size = 18.dp, color = OnSurfaceVariant, filled = true)
            Spacer(Modifier.width(10.dp))
            Text(
                "You can leave this — we'll notify you when done.",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurface,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
        ) {
            SecondaryButton(
                text = "Pause",
                onClick = { /* mock pause - no-op */ },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            PrimaryButton(
                text = "Notify when ready",
                onClick = { nav.replaceAll(Screen.Home) },
                backgroundColor = OnSurface,
                modifier = Modifier.weight(2f),
            )
        }
    }
}

@Composable
private fun DotDecor(alpha: Float) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .background(Primary40.copy(alpha = alpha), CircleShape),
    )
}

@Composable
private fun StepRow(done: Boolean, active: Boolean, icon: String = "check", label: String, trailing: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    when {
                        done -> Tertiary50
                        active -> Primary40
                        else -> OutlineStrong.copy(alpha = 0.5f)
                    },
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            when {
                done -> SymbolIcon(name = "check", size = 14.dp, color = Color.White, filled = true)
                active -> Box(modifier = Modifier.size(10.dp).background(Color.White, CircleShape))
                else -> SymbolIcon(name = icon, size = 14.dp, color = OnSurfaceVariant)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (active) Primary40 else if (done) OnSurface else OnSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        trailing?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = if (active) Primary40 else OnSurfaceVariant)
        }
    }
}
