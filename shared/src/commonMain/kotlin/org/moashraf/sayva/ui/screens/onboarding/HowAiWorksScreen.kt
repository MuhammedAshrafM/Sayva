package org.moashraf.sayva.ui.screens.onboarding

import androidx.compose.foundation.background
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
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.ui.components.a11yButtonRow
import org.moashraf.sayva.designsystem.OutlineStrong
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.Secondary50
import org.moashraf.sayva.designsystem.SurfaceContainer
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.designsystem.Tertiary50
import org.moashraf.sayva.nav.Screen
import org.moashraf.sayva.nav.SayvaNavController

@Composable
fun HowAiWorksScreen(nav: SayvaNavController) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(role = Role.Button, onClickLabel = "Go back") { nav.back() }
                    .semantics { contentDescription = "Back" },
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(name = "arrow_back", size = 20.dp, color = OnSurfaceVariant, contentDescription = null)
            }
            Text(
                "Skip",
                color = OnSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clickable(role = Role.Button, onClickLabel = "Skip onboarding") { nav.replaceAll(Screen.Login) }
                    .padding(8.dp),
            )
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(top = 8.dp, bottom = 24.dp)) {
            Text(
                "Three steps. Instant understanding.",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                "Everything runs on your device. Nothing leaves your phone.",
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceVariant,
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            StepCard(
                gradientStart = Color(0xFFE5E6FF),
                borderColor = Color(0xFFBCBEFF),
                iconBg = Primary40,
                icon = "videocam",
                stepLabel = "STEP 1",
                stepColor = Primary40,
                title = "Your camera sees",
                description = "Hands, face, & pose tracked at 60 fps.",
            )
            StepCard(
                gradientStart = Color(0xFFFFE2DE),
                borderColor = Color(0xFFFF9A8F),
                iconBg = Secondary50,
                icon = "auto_awesome",
                stepLabel = "STEP 2",
                stepColor = Color(0xFF8C2F25),
                title = "On-device AI understands",
                description = "Transformer model · 24 ms latency.",
            )
            StepCard(
                gradientStart = Color(0xFFD4E7DE),
                borderColor = Color(0xFF6BCFAB),
                iconBg = Tertiary50,
                icon = "record_voice_over",
                stepLabel = "STEP 3",
                stepColor = Color(0xFF005544),
                title = "It speaks for you",
                description = "Natural voice in your language.",
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .background(SurfaceContainer, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SymbolIcon(name = "lock", size = 18.dp, color = Tertiary50, filled = true)
            Spacer(Modifier.width(10.dp))
            Text(
                "On-device · video never leaves your phone.",
                style = MaterialTheme.typography.labelMedium,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            DotIndicator(active = true)
            Spacer(Modifier.width(6.dp))
            DotIndicator(active = false)
            Spacer(Modifier.width(6.dp))
            DotIndicator(active = false)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Row(
                modifier = Modifier
                    .background(Color(0xFF1A1B25), RoundedCornerShape(100))
                    .a11yButtonRow(label = "Next") { nav.navigate(Screen.TwoWayIntro) }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Next", style = MaterialTheme.typography.titleSmall, color = Color.White)
                Spacer(Modifier.width(8.dp))
                SymbolIcon(name = "arrow_forward", size = 18.dp, color = Color.White, contentDescription = null)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun StepCard(
    gradientStart: Color,
    borderColor: Color,
    iconBg: Color,
    icon: String,
    stepLabel: String,
    stepColor: Color,
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(listOf(gradientStart, SurfaceContainer)),
                RoundedCornerShape(20.dp),
            )
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(iconBg, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            SymbolIcon(name = icon, size = 26.dp, color = Color.White, filled = true)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                stepLabel,
                style = MaterialTheme.typography.labelSmall,
                color = stepColor,
            )
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 2.dp),
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DotIndicator(active: Boolean) {
    Box(
        modifier = Modifier
            .height(6.dp)
            .width(if (active) 24.dp else 6.dp)
            .background(if (active) Primary40 else OutlineStrong, CircleShape),
    )
}
