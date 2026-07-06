package org.moashraf.sayva.ui.screens.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.moashraf.sayva.camera.CameraController
import org.moashraf.sayva.camera.CameraPreview
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.designsystem.Tertiary50
import org.moashraf.sayva.languagepack.ConfidenceBucket
import org.moashraf.sayva.languagepack.RecognitionRole
import org.moashraf.sayva.nav.Screen
import org.moashraf.sayva.nav.SayvaNavController
import org.moashraf.sayva.pipeline.Prediction
import org.moashraf.sayva.pipeline.RecognitionUiState
import org.moashraf.sayva.speech.speakText
import org.moashraf.sayva.ui.viewmodel.LiveCameraViewModel

/**
 * ⚠️  TEMPORARY DEVELOPER / DEBUG SCREEN — not the production design.
 *
 * The Sayva production UI for live camera translation uses the designed
 * layout: dark gradient viewport, corner detection brackets, hero-sized
 * recognized-sign label, confidence dot in Tertiary50, `SymbolIcon` back
 * button — all in the design system's typography + color tokens. This
 * file replaced that mock during Phase 2 spikes so we could validate the
 * recognition pipeline end-to-end with visible diagnostics (FPS, per-stage
 * latency, hand count, confidence bucket).
 *
 * Track P2-S7 covers rebuilding the production UI around the same
 * [LiveCameraViewModel] — the ViewModel + pipeline are the stable API,
 * only this presentation layer swaps. Everything the production UI needs
 * is already exposed on `RecognitionUiState` (label, confidence, bucket,
 * pack code, supported modes). Diagnostics move to a hidden long-press
 * debug overlay or a Settings > Diagnostics screen once we integrate.
 *
 * Do NOT extend this screen with production-quality visuals — improvements
 * to the look-and-feel should land on the integration ticket instead.
 *
 * ### Language + model neutrality (retained during integration)
 * Everything the UI renders is derived from [RecognitionUiState]. The pack
 * badge, the mode selector, the confidence bucket, and the label all come
 * from what the active pack manifest declares — this file contains no
 * hardcoded language name, model ID, or sign vocabulary. The production
 * UI must preserve this contract.
 */
@Composable
fun LiveCameraScreen(nav: SayvaNavController) {
    val viewModel: LiveCameraViewModel = koinInject()
    val camera: CameraController = koinInject()
    val state by viewModel.state.collectAsState()

    DisposableEffect(Unit) {
        viewModel.onScreenEntered(RecognitionRole.FINGERSPELLING)
        onDispose { viewModel.onScreenLeft() }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Only draw the preview when we're past the permission gate — pinning
        // the AndroidView while the OS is showing a permission dialog can
        // cause CameraX to bind against a not-yet-permitted lifecycle.
        if (state !is RecognitionUiState.CameraPermissionRequired) {
            CameraPreview(controller = camera, modifier = Modifier.fillMaxSize())
        }

        // Top bar — pack chip + close button.
        TopBar(nav = nav, state = state, modifier = Modifier.align(Alignment.TopCenter))

        if (state is RecognitionUiState.CameraPermissionRequired) {
            CameraPermissionGate(
                onGrantAccess = { nav.navigate(Screen.Permissions) },
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            // Bottom — recognition result + confidence + mode selector.
            BottomOverlay(
                state = state,
                supportedRoles = viewModel.supportedRoles,
                onModeSelected = { viewModel.setMode(it) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/**
 * Full-screen affordance when camera permission hasn't been granted. Kept
 * simple in the dev UI; the production redesign (P2-S7) rehydrates this in
 * the Sayva design language.
 */
@Composable
private fun CameraPermissionGate(
    onGrantAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SymbolIcon(name = "photo_camera", size = 48.dp, color = Color.White)
        Spacer(Modifier.height(16.dp))
        Text(
            "Camera access needed",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Sayva needs your camera to recognize signs. Nothing leaves your device.",
            color = Color.White.copy(alpha = 0.75f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(100))
                .clickable { onGrantAccess() }
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text(
                "Grant access",
                color = Color.Black,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun TopBar(
    nav: SayvaNavController,
    state: RecognitionUiState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                .clickable { nav.back() },
            contentAlignment = Alignment.Center,
        ) {
            SymbolIcon(name = "close", size = 20.dp, color = Color.White)
        }

        Spacer(Modifier.padding(6.dp))

        Text(
            text = packChipText(state),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(100))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )

        Spacer(Modifier.padding(6.dp))

        // Diagnostics — always visible during Phase 2. Gate on debug builds later.
        DiagnosticsChip(state = state)
    }
}

@Composable
private fun BottomOverlay(
    state: RecognitionUiState,
    supportedRoles: List<String>,
    onModeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.60f))
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (state) {
            RecognitionUiState.Idle,
            RecognitionUiState.Starting,
            RecognitionUiState.CameraPermissionRequired -> {
                // CameraPermissionRequired is handled by the full-screen
                // affordance in `LiveCameraScreen`; we render a neutral
                // placeholder here so the bottom overlay branch stays valid.
                Text("Starting camera…", color = Color.White.copy(alpha = 0.75f))
            }
            is RecognitionUiState.NoModelForMode -> {
                Text(
                    "This language pack doesn't support ${prettyRoleName(state.role)}.",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            is RecognitionUiState.Error -> {
                Text(
                    "Recognition error: ${state.cause.message ?: state.cause::class.simpleName}",
                    color = Color(0xFFFF7A7A),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            is RecognitionUiState.Recognizing -> {
                RecognitionCard(prediction = state.prediction)
                if (supportedRoles.size > 1) {
                    Spacer(Modifier.height(14.dp))
                    ModeSelector(
                        supportedRoles = supportedRoles,
                        activeRole = state.role,
                        onSelected = onModeSelected,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecognitionCard(prediction: Prediction?) {
    if (prediction == null) {
        Text(
            "Show a hand to the camera…",
            color = Color.White.copy(alpha = 0.75f),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        return
    }

    val (bucketColor, bucketLabel) = when (prediction.bucket) {
        ConfidenceBucket.Show -> Tertiary50 to "confident"
        ConfidenceBucket.Caution -> Color(0xFFF5B84A) to "caution"
        ConfidenceBucket.LowConfidence -> Color(0xFFFF7A7A) to "low"
    }

    Text(
        prediction.label,
        color = Color.White,
        style = MaterialTheme.typography.displaySmall,
        modifier = Modifier
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription =
                    "Recognized ${prediction.label} at ${(prediction.confidence * 100).toInt()}% confidence"
            }
            .clickable { speakText(prediction.label) },
    )
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(bucketColor, CircleShape))
        Spacer(Modifier.padding(4.dp))
        Text(
            "${(prediction.confidence * 100).toInt()}% · $bucketLabel",
            color = Color.White.copy(alpha = 0.80f),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ModeSelector(
    supportedRoles: List<String>,
    activeRole: String,
    onSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(100))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for (role in supportedRoles) {
            val selected = role == activeRole
            Text(
                prettyRoleName(role),
                color = if (selected) Color.Black else Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .background(
                        if (selected) Color.White else Color.Transparent,
                        RoundedCornerShape(100),
                    )
                    .clickable { onSelected(role) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun DiagnosticsChip(state: RecognitionUiState) {
    val d = (state as? RecognitionUiState.Recognizing)?.diagnostics ?: return
    // Latency buckets are now attributed per-stage — `ComposedSignRecognizer`
    // populates the split. `pre` covers landmarks → model input, `inf` is
    // ModelRuntime.invoke, `post` is postprocess. `det` remains MediaPipe.
    Text(
        "${d.fps.toInt()} fps · det ${(d.handDetectionNanos / 1_000_000)} ms · " +
            "pre ${(d.preprocessingNanos / 1_000_000)} ms · " +
            "inf ${(d.inferenceNanos / 1_000_000)} ms · " +
            "post ${(d.postprocessingNanos / 1_000_000)} ms · " +
            "hands ${d.handsDetected}",
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.7f),
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/**
 * Pack chip text — reads the active pack + role from the recognition state.
 * No hardcoded language name; whatever the pack's manifest advertises for
 * the current output language shows here.
 */
private fun packChipText(state: RecognitionUiState): String = when (state) {
    RecognitionUiState.Idle, RecognitionUiState.Starting -> "· starting"
    RecognitionUiState.CameraPermissionRequired -> "· permission needed"
    is RecognitionUiState.NoModelForMode -> "${state.packCode.uppercase()} · no model"
    is RecognitionUiState.Recognizing -> "${state.packCode.uppercase()} · ${prettyRoleName(state.role)}"
    is RecognitionUiState.Error -> "error"
}

/** Human label for a role ID. Adding a new role adds a case here. */
private fun prettyRoleName(role: String): String = when (role) {
    RecognitionRole.FINGERSPELLING -> "Fingerspelling"
    RecognitionRole.SIGN_RECOGNITION -> "Sign"
    RecognitionRole.SENTENCE_RECOGNITION -> "Sentence"
    RecognitionRole.FACIAL_EXPRESSION -> "Expression"
    else -> role
}
