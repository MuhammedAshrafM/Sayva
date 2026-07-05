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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.OutlineStrong
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.Secondary50
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.nav.Screen
import org.moashraf.sayva.nav.SayvaNavController
import org.moashraf.sayva.ui.components.AuthErrorBanner
import org.moashraf.sayva.ui.components.AuthTextField
import org.moashraf.sayva.ui.components.PrimaryButton
import org.moashraf.sayva.ui.components.SayvaTopBar
import org.moashraf.sayva.ui.components.SecondaryButton
import org.moashraf.sayva.ui.viewmodel.AuthViewModel
import org.moashraf.sayva.ui.viewmodel.userMessage

@Composable
fun LoginScreen(nav: SayvaNavController) {
    val viewModel: AuthViewModel = koinInject()
    val state by viewModel.state.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }

    // Once auth succeeds (sign-in / guest), currentUser flips non-null and we
    // exit the auth flow. `replaceAll` clears the back stack so Back from Home
    // doesn't return here.
    LaunchedEffect(currentUser) {
        if (currentUser != null) nav.replaceAll(Screen.Home)
    }

    val canSubmit = state.email.isNotBlank() && state.password.isNotEmpty() && !state.isLoading

    Column(modifier = Modifier.fillMaxSize()) {
        SayvaTopBar(onBack = { nav.back() })

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 24.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Brush.linearGradient(listOf(Primary40, Secondary50)), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    SymbolIcon(name = "sign_language", size = 22.dp, color = Color.White, filled = true)
                }
                Spacer(Modifier.width(8.dp))
                Text("Sayva", style = MaterialTheme.typography.headlineSmall)
            }
            Text(
                "Welcome back.",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            Text(
                "Sign in to sync your history & streak.",
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceVariant,
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AuthTextField(
                label = "Email",
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                enabled = !state.isLoading,
            )
            AuthTextField(
                label = "Password",
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                isPassword = !passwordVisible,
                imeAction = ImeAction.Done,
                enabled = !state.isLoading,
                trailingIcon = if (passwordVisible) "visibility" else "visibility_off",
                onTrailingIconClick = { passwordVisible = !passwordVisible },
            )

            Text(
                "Forgot password?",
                style = MaterialTheme.typography.labelLarge,
                color = Primary40,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { nav.navigate(Screen.ForgotPassword) },
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )

            AuthErrorBanner(message = state.error?.userMessage())

            PrimaryButton(
                text = if (state.isLoading) "Signing in…" else "Sign in",
                onClick = { viewModel.signIn() },
                enabled = canSubmit,
                modifier = Modifier.padding(top = 8.dp),
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Box(Modifier.weight(1f).height(1.dp).background(OutlineStrong))
                Text("  OR  ", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                Box(Modifier.weight(1f).height(1.dp).background(OutlineStrong))
            }

            // Biometric wiring lands with P1-14 activation — the BiometricPrompt
            // gateway is bound but requires cached credentials to unlock. For
            // now the button is a visual placeholder.
            SecondaryButton(text = "Biometric sign-in", leadingIcon = "fingerprint", onClick = {})

            Text(
                "Continue as guest",
                style = MaterialTheme.typography.labelLarge,
                color = OnSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !state.isLoading) { viewModel.signInAnonymously() }
                    .padding(16.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text("New here? ", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
            Text(
                "Create account",
                style = MaterialTheme.typography.labelLarge,
                color = Primary40,
                modifier = Modifier.clickable { nav.navigate(Screen.Register) },
            )
        }
    }
}
