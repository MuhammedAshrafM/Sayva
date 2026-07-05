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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.OutlineStrong
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.designsystem.Tertiary50
import org.moashraf.sayva.nav.Screen
import org.moashraf.sayva.nav.SayvaNavController
import org.moashraf.sayva.ui.components.AuthErrorBanner
import org.moashraf.sayva.ui.components.AuthTextField
import org.moashraf.sayva.ui.components.PrimaryButton
import org.moashraf.sayva.ui.components.SayvaTopBar
import org.moashraf.sayva.ui.viewmodel.AuthViewModel
import org.moashraf.sayva.ui.viewmodel.userMessage

@Composable
fun RegisterScreen(nav: SayvaNavController) {
    val viewModel: AuthViewModel = koinInject()
    val state by viewModel.state.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(true) }

    LaunchedEffect(currentUser) {
        if (currentUser != null) nav.replaceAll(Screen.Home)
    }

    // Password strength heuristic — real check should mirror the backend's rules
    // (Firebase requires 6+; we push for 8+ to nudge users toward stronger).
    val strengthLevel = passwordStrength(state.password)

    val canSubmit = state.email.isNotBlank() &&
        state.password.length >= 8 &&
        termsAccepted &&
        !state.isLoading

    Column(modifier = Modifier.fillMaxSize()) {
        SayvaTopBar(onBack = { nav.back() })

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp)) {
            Text(
                "Create your account.",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            Text(
                "Free forever. No ads. Ever.",
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceVariant,
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AuthTextField(
                label = "Full name",
                value = state.displayName,
                onValueChange = viewModel::onDisplayNameChange,
                imeAction = ImeAction.Next,
                enabled = !state.isLoading,
            )
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
                trailingIcon = if (passwordVisible) "visibility_off" else "visibility",
                onTrailingIconClick = { passwordVisible = !passwordVisible },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(4) { i ->
                    val filled = i < strengthLevel
                    Box(
                        Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(
                                if (filled) Tertiary50 else OutlineStrong,
                                RoundedCornerShape(2.dp),
                            ),
                    )
                }
            }
            Text(
                text = strengthLabel(strengthLevel),
                style = MaterialTheme.typography.labelSmall,
                color = if (strengthLevel >= 3) Tertiary50 else OnSurfaceVariant,
            )

            // Language picker unchanged (P1-25 will wire it to a settings write).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, OutlineStrong, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Primary sign language", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                    Text("ASL · American", style = MaterialTheme.typography.bodyLarge)
                }
                SymbolIcon(name = "expand_more", size = 20.dp, color = OnSurfaceVariant)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clickable { termsAccepted = !termsAccepted },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            if (termsAccepted) Primary40 else Color.Transparent,
                            RoundedCornerShape(4.dp),
                        )
                        .border(
                            width = if (termsAccepted) 0.dp else 1.5.dp,
                            color = OutlineStrong,
                            shape = RoundedCornerShape(4.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (termsAccepted) {
                        SymbolIcon(name = "check", size = 14.dp, color = Color.White, filled = true)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "I agree to the Terms and Privacy Policy. Sayva will never sell my data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }

            AuthErrorBanner(message = state.error?.userMessage())
        }

        Spacer(Modifier.weight(1f))

        PrimaryButton(
            text = if (state.isLoading) "Creating account…" else "Create account",
            onClick = { viewModel.register() },
            enabled = canSubmit,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text("Have an account? ", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
            Text(
                "Sign in",
                style = MaterialTheme.typography.labelLarge,
                color = Primary40,
                modifier = Modifier.clickable { nav.navigate(Screen.Login) },
            )
        }
    }
}

/** Returns 0..4 based on length + character class variety. Displayed as pill fills. */
private fun passwordStrength(password: String): Int {
    if (password.isEmpty()) return 0
    var score = 0
    if (password.length >= 8) score++
    if (password.length >= 12) score++
    if (password.any { it.isDigit() } && password.any { it.isLetter() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    return score.coerceIn(0, 4)
}

private fun strengthLabel(level: Int): String = when (level) {
    0 -> "Enter a password"
    1 -> "Too weak"
    2 -> "Okay"
    3 -> "Strong password"
    else -> "Very strong password"
}
