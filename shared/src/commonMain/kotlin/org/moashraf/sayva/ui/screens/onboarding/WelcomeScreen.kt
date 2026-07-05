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
import androidx.compose.ui.unit.dp
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.OutlineStrong
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.Secondary50
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.nav.Screen
import org.moashraf.sayva.nav.SayvaNavController
import org.moashraf.sayva.ui.components.PrimaryButton

@Composable
fun WelcomeScreen(nav: SayvaNavController) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                "Skip",
                color = OnSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clickable(role = Role.Button, onClickLabel = "Skip onboarding") { nav.replaceAll(Screen.Login) }
                    .padding(8.dp),
            )
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 32.dp)
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.linearGradient(listOf(Primary40, Color(0xFF8A8EF5), Secondary50)),
                    RoundedCornerShape(32.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            SymbolIcon(name = "sign_language", size = 140.dp, color = Color.White, filled = true)
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)) {
            Text(
                "Talk with your hands.",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                "Sayva listens through your camera and speaks for you — translating sign language to text and voice in real time.",
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceVariant,
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            DotIndicator(active = true)
            Spacer(Modifier.width(6.dp))
            DotIndicator(active = false)
            Spacer(Modifier.width(6.dp))
            DotIndicator(active = false)
        }

        PrimaryButton(
            text = "Get started",
            trailingIcon = "arrow_forward",
            onClick = { nav.navigate(Screen.HowAiWorks) },
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
        )
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
