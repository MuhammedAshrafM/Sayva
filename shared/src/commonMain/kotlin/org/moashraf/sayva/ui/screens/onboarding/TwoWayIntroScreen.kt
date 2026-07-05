package org.moashraf.sayva.ui.screens.onboarding

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
import org.moashraf.sayva.designsystem.ErrorColor
import org.moashraf.sayva.ui.components.a11yButtonRow
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.OutlineStrong
import org.moashraf.sayva.designsystem.Outline
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.Secondary50
import org.moashraf.sayva.designsystem.SurfaceContainer
import org.moashraf.sayva.designsystem.SurfaceContainerHigh
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.nav.Screen
import org.moashraf.sayva.nav.SayvaNavController

@Composable
fun TwoWayIntroScreen(nav: SayvaNavController) {
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
                "Conversation, not commands.",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                buildString {
                    append("Sayva works both ways — sign-to-voice for them, voice-to-text for you.")
                },
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceVariant,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(330.dp)
                .background(
                    Brush.verticalGradient(listOf(SurfaceContainer, SurfaceContainerHigh)),
                    RoundedCornerShape(24.dp),
                )
                .padding(20.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("LIVE", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(6.dp).background(ErrorColor, CircleShape),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("REC", style = MaterialTheme.typography.labelSmall, color = ErrorColor)
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ChatBubble(
                    text = "Hi! How are you?",
                    fromSign = true,
                    align = Alignment.Start,
                )
                ChatBubble(
                    text = "I'm great! Coffee?",
                    fromSign = false,
                    align = Alignment.End,
                )
                ChatBubble(
                    text = "Yes — extra milk.",
                    fromSign = true,
                    align = Alignment.Start,
                )
                ListeningBubble()
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            DotIndicator(active = false)
            Spacer(Modifier.width(6.dp))
            DotIndicator(active = false)
            Spacer(Modifier.width(6.dp))
            DotIndicator(active = true)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Row(
                modifier = Modifier
                    .background(Color(0xFF1A1B25), RoundedCornerShape(100))
                    .a11yButtonRow(label = "Almost done, continue") { nav.navigate(Screen.Permissions) }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Almost done", style = MaterialTheme.typography.titleSmall, color = Color.White)
                Spacer(Modifier.width(8.dp))
                SymbolIcon(name = "arrow_forward", size = 18.dp, color = Color.White, contentDescription = null)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ChatBubble(text: String, fromSign: Boolean, align: Alignment.Horizontal) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromSign) Arrangement.Start else Arrangement.End,
    ) {
        if (fromSign) {
            AvatarIcon(icon = "sign_language", bg = Primary40)
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(Primary40, RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(text, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            }
        } else {
            Box(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                    .border(1.dp, Outline, RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.width(8.dp))
            AvatarIcon(icon = "mic", bg = Secondary50)
        }
    }
}

@Composable
private fun ListeningBubble() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box(
            modifier = Modifier
                .background(Secondary50.copy(alpha = 0.15f), RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                .border(1.dp, Color(0xFFFF9A8F), RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SymbolIcon(name = "graphic_eq", size = 14.dp, color = OnSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Text("Listening…", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
            }
        }
        Spacer(Modifier.width(8.dp))
        AvatarIcon(icon = "mic", bg = Secondary50)
    }
}

@Composable
private fun AvatarIcon(icon: String, bg: Color) {
    Box(
        modifier = Modifier.size(28.dp).background(bg, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        SymbolIcon(name = icon, size = 14.dp, color = Color.White, filled = true)
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
