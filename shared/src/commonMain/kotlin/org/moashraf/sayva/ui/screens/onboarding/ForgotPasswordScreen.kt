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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.PrimaryContainer
import org.moashraf.sayva.designsystem.SurfaceContainer
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.nav.Screen
import org.moashraf.sayva.nav.SayvaNavController
import org.moashraf.sayva.ui.components.AuthErrorBanner
import org.moashraf.sayva.ui.components.AuthTextField
import org.moashraf.sayva.ui.components.PrimaryButton
import org.moashraf.sayva.ui.components.SayvaTopBar
import org.moashraf.sayva.ui.viewmodel.AuthViewModel
import org.moashraf.sayva.ui.viewmodel.userMessage

@Composable
fun ForgotPasswordScreen(nav: SayvaNavController) {
    val viewModel: AuthViewModel = koinInject()
    val state by viewModel.state.collectAsState()

    // Auto-advance to the "check your inbox" screen once the send succeeds.
    // ResetEmailSentScreen calls `clearResetSent` on Back so this doesn't loop.
    LaunchedEffect(state.resetEmailSent) {
        if (state.resetEmailSent) nav.navigate(Screen.ResetEmailSent)
    }

    val canSubmit = state.email.isNotBlank() && !state.isLoading

    Column(modifier = Modifier.fillMaxSize()) {
        SayvaTopBar(onBack = { nav.back() })

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(top = 24.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(PrimaryContainer, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(name = "lock_reset", size = 40.dp, color = Primary40, filled = true)
            }
            Spacer(Modifier.padding(top = 20.dp))
            Text(
                "Reset password",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
            )
            Text(
                "Enter your email and we'll send a magic link.",
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            AuthTextField(
                label = "Email",
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
                enabled = !state.isLoading,
            )

            AuthErrorBanner(
                message = state.error?.userMessage(),
                modifier = Modifier.padding(top = 12.dp),
            )

            PrimaryButton(
                text = if (state.isLoading) "Sending…" else "Send reset link",
                onClick = { viewModel.sendPasswordReset() },
                enabled = canSubmit,
                modifier = Modifier.padding(top = 16.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .background(SurfaceContainer, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                SymbolIcon(name = "info", size = 18.dp, color = OnSurfaceVariant, filled = true)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Didn't get the email? Check spam, or contact support — we reply in sign language video too.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text("Remembered it? ", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
            Text(
                "Sign in",
                style = MaterialTheme.typography.labelLarge,
                color = Primary40,
                modifier = Modifier.clickable { nav.back() },
            )
        }
    }
}
