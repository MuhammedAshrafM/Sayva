package org.moashraf.sayva.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.OnTertiaryContainer
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.designsystem.Tertiary50
import org.moashraf.sayva.designsystem.TertiaryContainer
import org.moashraf.sayva.nav.SayvaNavController
import org.moashraf.sayva.ui.components.AuthErrorBanner
import org.moashraf.sayva.ui.components.PrimaryButton
import org.moashraf.sayva.ui.components.TextLink
import org.moashraf.sayva.ui.viewmodel.AuthViewModel
import org.moashraf.sayva.ui.viewmodel.userMessage

private const val RESEND_COOLDOWN_SECONDS = 47

@Composable
fun ResetEmailSentScreen(nav: SayvaNavController) {
    val viewModel: AuthViewModel = koinInject()
    val state by viewModel.state.collectAsState()

    // Countdown for the "Resend in 0:XX" button. Restarts on every successful
    // resend so the user can't hammer the endpoint.
    var secondsLeft by remember { mutableStateOf(RESEND_COOLDOWN_SECONDS) }
    LaunchedEffect(state.resetEmailSent) {
        // Reset the timer whenever the VM reports a fresh send.
        if (state.resetEmailSent) secondsLeft = RESEND_COOLDOWN_SECONDS
    }
    LaunchedEffect(secondsLeft) {
        if (secondsLeft > 0) {
            delay(1000L)
            secondsLeft -= 1
        }
    }

    // If the user backs out of this screen the `resetEmailSent` flag stays
    // true, which would cause ForgotPassword to immediately re-navigate here.
    // Clear it on disposal so the flow can be re-entered cleanly.
    DisposableEffect(Unit) {
        onDispose { viewModel.clearResetSent() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(top = 48.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        Brush.linearGradient(listOf(TertiaryContainer, Tertiary50)),
                        RoundedCornerShape(36.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(name = "mark_email_read", size = 60.dp, color = Color.White, filled = true)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 0.dp)
                        .size(32.dp)
                        .border(4.dp, Color.White, CircleShape)
                        .background(Tertiary50, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    SymbolIcon(name = "check", size = 18.dp, color = Color.White, filled = true)
                }
            }
            Spacer(Modifier.padding(top = 32.dp))
            Text(
                "Check your inbox.",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 32.dp, bottom = 10.dp),
            )
            Text(
                "We sent a magic link to",
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                state.email.ifBlank { "your email" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            AuthErrorBanner(
                message = state.error?.userMessage(),
                modifier = Modifier.padding(bottom = 12.dp),
            )
            PrimaryButton(
                text = "Open email app",
                trailingIcon = "open_in_new",
                // Real email-app intent lands with a platform expect/actual;
                // for now this is a no-op so the button remains testable.
                onClick = {},
            )
            Spacer(Modifier.padding(top = 14.dp))
            val resendEnabled = secondsLeft == 0 && !state.isLoading
            TextLink(
                text = when {
                    state.isLoading -> "Resending…"
                    resendEnabled -> "Resend email"
                    else -> "Resend in 0:${secondsLeft.toString().padStart(2, '0')}"
                },
                onClick = { if (resendEnabled) viewModel.sendPasswordReset() },
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                color = if (resendEnabled) Primary40 else OnSurfaceVariant,
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .background(TertiaryContainer, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            SymbolIcon(name = "tips_and_updates", size = 22.dp, color = OnTertiaryContainer, filled = true)
            Spacer(Modifier.width(12.dp))
            Text(
                "Pro tip · Use Sayva as guest while you wait — your history will sync when you sign back in.",
                style = MaterialTheme.typography.bodySmall,
                color = OnTertiaryContainer,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.padding(bottom = 24.dp))
    }
}
