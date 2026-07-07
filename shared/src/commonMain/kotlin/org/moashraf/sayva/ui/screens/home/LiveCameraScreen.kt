package org.moashraf.sayva.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.moashraf.sayva.camera.CameraController
import org.moashraf.sayva.camera.CameraPreview
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.Secondary50
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.designsystem.Tertiary50
import org.moashraf.sayva.languagepack.ConfidenceBucket
import org.moashraf.sayva.languagepack.RecognitionRole
import org.moashraf.sayva.ml.Handedness
import org.moashraf.sayva.nav.Screen
import org.moashraf.sayva.nav.SayvaNavController
import org.moashraf.sayva.pipeline.Prediction
import org.moashraf.sayva.pipeline.RecognitionUiState
import org.moashraf.sayva.ui.viewmodel.LiveCameraViewModel

/**
 * Production LiveCameraScreen — the Sayva-designed hero surface for live
 * sign translation.
 *
 * ### Contract
 * This file renders. It reads [LiveCameraViewModel] state and calls
 * ViewModel actions. It does NOT touch [CameraController] beyond binding
 * the preview surface, does not import [org.moashraf.sayva.pipeline] or
 * [org.moashraf.sayva.ml] except to consume the sealed hierarchies for
 * state destructuring, and does not import [org.moashraf.sayva.clipboard]
 * or [org.moashraf.sayva.data.repository] at all. Business logic,
 * platform APIs, and persistence live behind the ViewModel.
 *
 * ### Language + model neutrality
 * Every string, color, and threshold on screen is derived from data
 * exposed by the ViewModel:
 *   * Pack chip: `viewModel.packDisplayName` — whatever the active
 *     pack's manifest declares for the current output language.
 *   * Confidence pill color: `prediction.bucket` — thresholds live in
 *     the pack manifest, never in this file.
 *   * Mode selector shows only what the active pack advertises
 *     (`viewModel.supportedRoles`).
 * Adding a new sign language means dropping a new pack under
 * `ml/packs/`; this file compiles and renders it unchanged.
 *
 * ### Developer HUD
 * Extra diagnostics (per-stage latencies, model / pack identity, sign
 * id) are gated on `viewModel.developerMode` — a Settings > Diagnostics
 * toggle. When off, the screen shows only production visuals: an FPS
 * chip, a total-processing pill, and the HAND DETECTED handedness pill
 * from the original design mock. Casual users never see the extras.
 */
@Composable
fun LiveCameraScreen(nav: SayvaNavController) {
    val viewModel: LiveCameraViewModel = koinInject()
    val camera: CameraController = koinInject()
    val state by viewModel.state.collectAsState()
    val packDisplayName by viewModel.packDisplayName.collectAsState()
    val torchOn by viewModel.torchEnabled.collectAsState()
    val isFavorited by viewModel.isFavorited.collectAsState()
    val developerMode by viewModel.developerMode.collectAsState()

    DisposableEffect(Unit) {
        viewModel.onScreenEntered(RecognitionRole.FINGERSPELLING)
        onDispose { viewModel.onScreenLeft() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                // Dark radial matte — matches the original design mock.
                // The real camera preview sits on top; the gradient shows
                // through the edges + fills the frame during permission
                // gating or a pipeline stall so we never see stark black.
                Brush.radialGradient(
                    colors = listOf(Color(0xFF3A3550), Color(0xFF1A1B25), Color(0xFF0A0B12)),
                ),
            ),
    ) {
        // Camera preview under everything else. Pinned while the OS
        // shows a permission prompt would ask CameraX to bind against
        // an unpermitted lifecycle — same guard as the dev screen.
        if (state !is RecognitionUiState.CameraPermissionRequired) {
            CameraPreview(controller = camera, modifier = Modifier.fillMaxSize())
        }

        DetectionBrackets(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 110.dp)
                .fillMaxWidth()
                .height(180.dp),
        )

        TopChrome(
            packDisplayName = packDisplayName,
            hasTorch = viewModel.hasTorch,
            torchOn = torchOn,
            onClose = { nav.back() },
            onToggleTorch = viewModel::toggleTorch,
            onSwitchLens = viewModel::switchLens,
        )

        // FPS + processing HUD chips. Present in the production design
        // (they were part of the mock) and cheap to derive from state.
        DiagnosticsChips(state = state)

        HandDetectedPill(state = state)

        if (developerMode) {
            DeveloperOverlay(
                state = state,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 148.dp, start = 14.dp, end = 14.dp),
            )
        }

        // Bottom overlay: either the permission gate (full-height card)
        // or the translation card + action rail.
        if (state is RecognitionUiState.CameraPermissionRequired) {
            CameraPermissionGate(
                onGrantAccess = { nav.navigate(Screen.Permissions) },
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            BottomStack(
                nav = nav,
                state = state,
                supportedRoles = viewModel.supportedRoles,
                isFavorited = isFavorited,
                onSpeak = { viewModel.speakCurrentLabel() },
                onCopy = viewModel::copyLabel,
                onToggleFavorite = viewModel::toggleFavorite,
                onTogglePause = viewModel::togglePause,
                onModeSelected = viewModel::setMode,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Top chrome
// ---------------------------------------------------------------------------

@Composable
private fun TopChrome(
    packDisplayName: String,
    hasTorch: Boolean,
    torchOn: Boolean,
    onClose: () -> Unit,
    onToggleTorch: () -> Unit,
    onSwitchLens: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChromeCircleButton(
            icon = "close",
            onClick = onClose,
            contentDescription = "Close live translation",
        )
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(100))
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .semantics {
                    contentDescription = "Recognition language: $packDisplayName"
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                packDisplayName,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (hasTorch) {
            ChromeCircleButton(
                icon = "bolt",
                iconColor = if (torchOn) Color(0xFFFFD700) else Color.White,
                filled = torchOn,
                onClick = onToggleTorch,
                contentDescription = if (torchOn) "Turn torch off" else "Turn torch on",
            )
            Spacer(Modifier.width(8.dp))
        }
        ChromeCircleButton(
            icon = "cameraswitch",
            onClick = onSwitchLens,
            contentDescription = "Switch camera",
        )
    }
}

@Composable
private fun ChromeCircleButton(
    icon: String,
    onClick: () -> Unit,
    iconColor: Color = Color.White,
    filled: Boolean = false,
    contentDescription: String? = null,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            .semantics {
                role = Role.Button
                if (contentDescription != null) this.contentDescription = contentDescription
            }
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        SymbolIcon(name = icon, size = 18.dp, color = iconColor, filled = filled)
    }
}

// ---------------------------------------------------------------------------
// Diagnostics chips — FPS + total-processing pill
// ---------------------------------------------------------------------------

@Composable
private fun DiagnosticsChips(state: RecognitionUiState) {
    val (fps, totalMs) = when (state) {
        is RecognitionUiState.Recognizing -> state.diagnostics.fps to
            (state.diagnostics.totalFrameNanos / 1_000_000).toInt()
        is RecognitionUiState.Paused -> state.diagnostics.fps to
            (state.diagnostics.totalFrameNanos / 1_000_000).toInt()
        else -> return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 72.dp, start = 14.dp, end = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier
                .background(Tertiary50.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                .border(1.dp, Tertiary50, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(6.dp).background(Tertiary50, CircleShape))
            Spacer(Modifier.width(5.dp))
            Text(
                "${fps.toInt()} FPS",
                style = MaterialTheme.typography.labelSmall,
                color = Tertiary50,
            )
        }
        Row(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(6.dp).background(Tertiary50, CircleShape))
            Spacer(Modifier.width(5.dp))
            Text(
                "Processing · ${totalMs}ms",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun HandDetectedPill(state: RecognitionUiState) {
    val recognizing = when (state) {
        is RecognitionUiState.Recognizing -> state.diagnostics
        is RecognitionUiState.Paused -> state.diagnostics
        else -> return
    }
    if (recognizing.handsDetected == 0) return
    val handedness = when (recognizing.primaryHandedness) {
        Handedness.Left -> "LEFT"
        Handedness.Right -> "RIGHT"
        Handedness.Unknown, null -> "HAND"
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 104.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            "HAND DETECTED · $handedness",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .background(Primary40.copy(alpha = 0.95f), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

// ---------------------------------------------------------------------------
// Developer HUD — visible only when Settings > Diagnostics > Developer mode
// ---------------------------------------------------------------------------

@Composable
private fun DeveloperOverlay(state: RecognitionUiState, modifier: Modifier) {
    val (packCode, modelId, role, arch, d, prediction) = when (state) {
        is RecognitionUiState.Recognizing -> DevSnapshot(
            state.packCode, state.modelId, state.role, state.architecture,
            state.diagnostics, state.prediction,
        )
        is RecognitionUiState.Paused -> DevSnapshot(
            state.packCode, state.modelId, state.role, state.architecture,
            state.diagnostics, state.prediction,
        )
        else -> return
    }
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            "$packCode · $modelId · $role · $arch",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f),
        )
        Text(
            "det ${d.handDetectionNanos / 1_000_000}ms · " +
                "pre ${d.preprocessingNanos / 1_000_000}ms · " +
                "inf ${d.inferenceNanos / 1_000_000}ms · " +
                "post ${d.postprocessingNanos / 1_000_000}ms · " +
                "hands ${d.handsDetected}",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f),
        )
        if (prediction != null) {
            Text(
                "sign=${prediction.sign.id} · label=${prediction.label} · " +
                    "conf=${(prediction.confidence * 100).toInt()}% (${prediction.bucket.name})",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

/** Bag-of-fields tuple for DeveloperOverlay so the destructuring above stays
 *  readable. Six-field data classes still support `component1..componentN`. */
private data class DevSnapshot(
    val packCode: String,
    val modelId: String,
    val role: String,
    val arch: String,
    val d: org.moashraf.sayva.ml.PipelineDiagnostics,
    val prediction: Prediction?,
)

// ---------------------------------------------------------------------------
// Detection brackets — decorative corner accents from the original design
// ---------------------------------------------------------------------------

@Composable
private fun DetectionBrackets(modifier: Modifier = Modifier) {
    val color = Primary40
    val thickness = 3.dp
    val cornerLen = 28.dp
    Box(modifier = modifier) {
        Box(modifier = Modifier.align(Alignment.TopStart).size(cornerLen)) {
            Box(modifier = Modifier.fillMaxWidth().height(thickness).background(color).align(Alignment.TopStart))
            Box(modifier = Modifier.fillMaxHeight().width(thickness).background(color).align(Alignment.TopStart))
        }
        Box(modifier = Modifier.align(Alignment.TopEnd).size(cornerLen)) {
            Box(modifier = Modifier.fillMaxWidth().height(thickness).background(color).align(Alignment.TopEnd))
            Box(modifier = Modifier.fillMaxHeight().width(thickness).background(color).align(Alignment.TopEnd))
        }
        Box(modifier = Modifier.align(Alignment.BottomStart).size(cornerLen)) {
            Box(modifier = Modifier.fillMaxWidth().height(thickness).background(color).align(Alignment.BottomStart))
            Box(modifier = Modifier.fillMaxHeight().width(thickness).background(color).align(Alignment.BottomStart))
        }
        Box(modifier = Modifier.align(Alignment.BottomEnd).size(cornerLen)) {
            Box(modifier = Modifier.fillMaxWidth().height(thickness).background(color).align(Alignment.BottomEnd))
            Box(modifier = Modifier.fillMaxHeight().width(thickness).background(color).align(Alignment.BottomEnd))
        }
    }
}

// ---------------------------------------------------------------------------
// Bottom stack — translation card + action rail
// ---------------------------------------------------------------------------

@Composable
private fun BottomStack(
    nav: SayvaNavController,
    state: RecognitionUiState,
    supportedRoles: List<String>,
    isFavorited: Boolean,
    onSpeak: () -> Unit,
    onCopy: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePause: () -> Unit,
    onModeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        when (state) {
            RecognitionUiState.Idle,
            RecognitionUiState.Starting,
            RecognitionUiState.CameraPermissionRequired -> {
                StatusCard("Starting camera…")
            }
            is RecognitionUiState.NoModelForMode -> {
                StatusCard(
                    "This language pack doesn't support ${prettyRoleName(state.role)} yet.",
                )
            }
            is RecognitionUiState.Error -> {
                StatusCard(
                    text = state.cause.message ?: state.cause::class.simpleName.orEmpty(),
                    isError = true,
                )
            }
            is RecognitionUiState.Recognizing -> {
                TranslationCard(
                    prediction = state.prediction,
                    isFavorited = isFavorited,
                    isPaused = false,
                    onSpeak = onSpeak,
                    onCopy = onCopy,
                    onToggleFavorite = onToggleFavorite,
                )
            }
            is RecognitionUiState.Paused -> {
                TranslationCard(
                    prediction = state.prediction,
                    isFavorited = isFavorited,
                    isPaused = true,
                    onSpeak = onSpeak,
                    onCopy = onCopy,
                    onToggleFavorite = onToggleFavorite,
                )
            }
        }

        // Mode selector — only surfaces when the active pack advertises more
        // than one role. Language-neutral: reads from viewModel.supportedRoles.
        if (state is RecognitionUiState.Recognizing && supportedRoles.size > 1) {
            Spacer(Modifier.height(10.dp))
            ModeSelector(
                supportedRoles = supportedRoles,
                activeRole = state.role,
                onSelected = onModeSelected,
                modifier = Modifier.padding(horizontal = 14.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        BottomActionRail(
            state = state,
            onConversation = { nav.navigate(Screen.Conversation) },
            onHistory = { nav.navigate(Screen.History) },
            onTogglePause = onTogglePause,
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun StatusCard(text: String, isError: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .background(Color(0xFF14152A).copy(alpha = 0.85f), RoundedCornerShape(24.dp))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            .padding(18.dp),
    ) {
        Text(
            text = text,
            color = if (isError) Color(0xFFFF7A7A) else Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun TranslationCard(
    prediction: Prediction?,
    isFavorited: Boolean,
    isPaused: Boolean,
    onSpeak: () -> Unit,
    onCopy: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .background(Color(0xFF14152A).copy(alpha = 0.85f), RoundedCornerShape(24.dp))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            .padding(18.dp),
    ) {
        if (prediction == null) {
            Text(
                if (isPaused) "Paused" else "Show a hand to the camera…",
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            return@Column
        }

        val bucketColor = when (prediction.bucket) {
            ConfidenceBucket.Show -> Tertiary50
            ConfidenceBucket.Caution -> Color(0xFFF5B84A)
            ConfidenceBucket.LowConfidence -> Color(0xFFFF7A7A)
        }
        val bucketBg = bucketColor.copy(alpha = 0.16f)
        val confidencePct = (prediction.confidence * 100).toInt()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$confidencePct% MATCH",
                    style = MaterialTheme.typography.labelSmall,
                    color = bucketColor,
                    modifier = Modifier
                        .background(bucketBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 9.dp, vertical = 3.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when (prediction.bucket) {
                        ConfidenceBucket.Show -> "Confidence"
                        ConfidenceBucket.Caution -> "Confidence · caution"
                        ConfidenceBucket.LowConfidence -> "Confidence · low"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.55f),
                )
            }
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    .semantics {
                        role = Role.Button
                        contentDescription =
                            if (isFavorited) "Remove from favorites" else "Add to favorites"
                    }
                    .clickable { onToggleFavorite() },
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(
                    name = if (isFavorited) "star" else "star_outline",
                    size = 16.dp,
                    color = if (isFavorited) Color(0xFFFFD700) else Color.White,
                    filled = isFavorited,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Confidence bar — width fills to the softmax probability. Colored
        // as a Primary→bucket gradient so bucket bleeds visually into the
        // production styling without duplicating threshold logic here.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(2.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(prediction.confidence.coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(listOf(bucketColor, Primary40)),
                        RoundedCornerShape(2.dp),
                    ),
            )
        }

        Spacer(Modifier.height(14.dp))
        Text("SIGN", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.55f))
        Spacer(Modifier.height(2.dp))
        Text(
            prediction.label,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription =
                    "Recognized ${prediction.label} at $confidencePct% confidence"
            },
        )

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(Secondary50, RoundedCornerShape(100))
                    .semantics {
                        role = Role.Button
                        contentDescription = "Speak ${prediction.label} aloud"
                    }
                    .clickable { onSpeak() }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SymbolIcon(name = "volume_up", size = 18.dp, color = Color.White, filled = true)
                Spacer(Modifier.width(6.dp))
                Text("Speak aloud", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color.White.copy(alpha = 0.12f), CircleShape)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Copy ${prediction.label}"
                    }
                    .clickable { onCopy() },
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(name = "content_copy", size = 20.dp, color = Color.White)
            }
        }
    }
}

@Composable
private fun BottomActionRail(
    state: RecognitionUiState,
    onConversation: () -> Unit,
    onHistory: () -> Unit,
    onTogglePause: () -> Unit,
) {
    val isPaused = state is RecognitionUiState.Paused
    val pauseButtonEnabled = state is RecognitionUiState.Recognizing || isPaused
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(100))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RailButton(icon = "forum", label = "Convo", onClick = onConversation)
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (pauseButtonEnabled) Color.White else Color.White.copy(alpha = 0.35f),
                    CircleShape,
                )
                .semantics {
                    role = Role.Button
                    contentDescription = if (isPaused) "Resume recognition" else "Pause recognition"
                }
                .clickable(enabled = pauseButtonEnabled) { onTogglePause() },
            contentAlignment = Alignment.Center,
        ) {
            SymbolIcon(
                name = if (isPaused) "play_arrow" else "pause",
                size = 24.dp,
                color = Color(0xFF1A1B25),
                filled = true,
            )
        }
        RailButton(icon = "history", label = "History", onClick = onHistory)
    }
}

@Composable
private fun RailButton(icon: String, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .semantics {
                role = Role.Button
                contentDescription = label
            }
            .clickable { onClick() },
    ) {
        SymbolIcon(name = icon, size = 22.dp, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

// ---------------------------------------------------------------------------
// Camera-permission gate — kept in the dark visual language of the rest of
// the screen. Routes to the standalone PermissionsScreen for the actual
// system prompt.
// ---------------------------------------------------------------------------

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
                .semantics {
                    role = Role.Button
                    contentDescription = "Grant camera access"
                }
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

// ---------------------------------------------------------------------------
// Mode selector — shows only what the active pack advertises. Kept from
// the dev screen because it's the right visual affordance and adding a new
// role adds a new pill here for free.
// ---------------------------------------------------------------------------

@Composable
private fun ModeSelector(
    supportedRoles: List<String>,
    activeRole: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(100))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for (roleId in supportedRoles) {
            val selected = roleId == activeRole
            Text(
                prettyRoleName(roleId),
                color = if (selected) Color.Black else Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .background(
                        if (selected) Color.White else Color.Transparent,
                        RoundedCornerShape(100),
                    )
                    .clickable { onSelected(roleId) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

/** Human label for a role ID. Adding a new role adds a case here. */
private fun prettyRoleName(role: String): String = when (role) {
    RecognitionRole.FINGERSPELLING -> "Fingerspelling"
    RecognitionRole.SIGN_RECOGNITION -> "Sign"
    RecognitionRole.SENTENCE_RECOGNITION -> "Sentence"
    RecognitionRole.FACIAL_EXPRESSION -> "Expression"
    else -> role
}
