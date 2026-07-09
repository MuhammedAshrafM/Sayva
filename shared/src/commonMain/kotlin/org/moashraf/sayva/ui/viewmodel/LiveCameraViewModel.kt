package org.moashraf.sayva.ui.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.moashraf.sayva.camera.CameraController
import org.moashraf.sayva.camera.CameraLens
import org.moashraf.sayva.clipboard.Clipboard
import org.moashraf.sayva.data.repository.FavoritesRepository
import org.moashraf.sayva.data.repository.SettingsRepository
import org.moashraf.sayva.diagnostics.DiagnosticSampleWriter
import org.moashraf.sayva.languagepack.LanguagePackController
import org.moashraf.sayva.languagepack.RecognitionRole
import org.moashraf.sayva.languagepack.SignRecognizerFactory
import org.moashraf.sayva.languagepack.TranslationRenderer
import org.moashraf.sayva.ml.HandDetectorFactory
import org.moashraf.sayva.permission.PermissionController
import org.moashraf.sayva.permission.SayvaPermission
import org.moashraf.sayva.pipeline.DefaultRecognitionPipeline
import org.moashraf.sayva.pipeline.RecognitionPipeline
import org.moashraf.sayva.pipeline.RecognitionUiState
import org.moashraf.sayva.speech.speakText
import org.moashraf.sayva.telemetry.AnalyticsEvents
import org.moashraf.sayva.telemetry.AnalyticsGateway
import org.moashraf.sayva.telemetry.CrashReporter

/**
 * ViewModel for `LiveCameraScreen`. Owns the [RecognitionPipeline]'s
 * coroutine scope and exposes [RecognitionUiState] to Compose.
 *
 * Bound as a Koin `single` — one instance app-wide because there's one
 * LiveCameraScreen. [onScreenLeft] stops the pipeline; [onScreenEntered]
 * restarts it. This lets the user tap into other screens and come back
 * without re-instantiating.
 */
class LiveCameraViewModel(
    private val camera: CameraController,
    handDetectorFactory: HandDetectorFactory,
    signRecognizerFactory: SignRecognizerFactory,
    translationRenderer: TranslationRenderer,
    private val packController: LanguagePackController,
    private val permissionController: PermissionController,
    private val analytics: AnalyticsGateway,
    private val crashReporter: CrashReporter,
    private val favorites: FavoritesRepository,
    private val settings: SettingsRepository,
    private val clipboard: Clipboard,
) {

    private val scope: CoroutineScope = MainScope()

    private val pipeline: RecognitionPipeline = DefaultRecognitionPipeline(
        camera = camera,
        handDetectorFactory = handDetectorFactory,
        signRecognizerFactory = signRecognizerFactory,
        translationRenderer = translationRenderer,
        packController = packController,
        scope = scope,
    )

    /**
     * Combined state: emits [RecognitionUiState.CameraPermissionRequired]
     * when the pre-start permission check fails; otherwise mirrors the
     * pipeline's state 1:1 by initializing to the pipeline's state on
     * every check pass.
     *
     * The permission-required state is a ViewModel concern, not a pipeline
     * concern — the pipeline stays permission-agnostic (never asked, never
     * checks). That keeps the pipeline's contract tight and lets the
     * ViewModel own the "who gates the start button" decision.
     */
    private val _viewState = MutableStateFlow<RecognitionUiState>(RecognitionUiState.Idle)
    val state: StateFlow<RecognitionUiState> = _viewState.asStateFlow()

    private var mirrorJob: kotlinx.coroutines.Job? = null

    /** Coroutine that suspends waiting for pack Ready and then starts the
     *  pipeline. Held here so [onScreenLeft] can cancel a pending start — a
     *  user leaving the screen during the ~200ms bootstrap window must not
     *  wake the camera after the fact. */
    private var pendingStartJob: kotlinx.coroutines.Job? = null

    /**
     * All roles supported by the active pack. UI mode selector shows only
     * these; picking one calls [setMode].
     */
    val supportedRoles: List<String>
        get() {
            val ready = packController.state.value
                as? LanguagePackController.State.Ready
                ?: return emptyList()
            return ready.currentPack.supportedRoles
        }

    fun onScreenEntered(initialMode: String = RecognitionRole.FINGERSPELLING) {
        pendingStartJob?.cancel()
        pendingStartJob = scope.launch {
            // Gate on camera permission BEFORE touching the pipeline. If
            // denied, we surface CameraPermissionRequired and let the screen
            // route to PermissionsScreen. The pipeline never sees a failed
            // camera.start() from a missing permission — cleaner separation
            // and the user gets a proper affordance instead of an error card.
            if (!permissionController.isGranted(SayvaPermission.Camera)) {
                _viewState.value = RecognitionUiState.CameraPermissionRequired
                return@launch
            }

            // Wait for the pack subsystem to be Ready before logging or
            // starting the pipeline. Otherwise a user who reaches LiveCamera
            // in the ~200ms window between app launch and pack bootstrap
            // completing would (a) emit a `recognition_started` event tagged
            // `pack_code=unknown` — corrupting analytics — and (b) see the
            // pipeline immediately fault into Error via its own not-ready
            // guard. Suspending here surfaces neither: the screen shows the
            // pipeline's `Idle` state (the initial view state) until Ready.
            val ready = packController.state
                .filterIsInstance<LanguagePackController.State.Ready>()
                .first()

            crashReporter.setKey("recognition_active", "true")
            crashReporter.setKey("recognition_mode", initialMode)
            analytics.logEvent(
                AnalyticsEvents.RECOGNITION_STARTED,
                mapOf(
                    AnalyticsEvents.Param.MODE to initialMode,
                    AnalyticsEvents.Param.PACK_CODE to ready.currentPack.recognitionCode,
                ),
            )
            startMirroringPipeline()
            pipeline.start(initialMode)
        }
    }

    /**
     * Called by the screen after the user returns from PermissionsScreen —
     * re-checks permission and starts recognition if the grant went through.
     * If still denied we stay in [RecognitionUiState.CameraPermissionRequired].
     */
    fun onCameraPermissionMaybeChanged(initialMode: String = RecognitionRole.FINGERSPELLING) {
        onScreenEntered(initialMode)
    }

    /**
     * Wire pipeline.state → _viewState. Cancels any previous mirror so
     * `onScreenEntered` can be called safely after a permission-denied cycle.
     */
    private fun startMirroringPipeline() {
        mirrorJob?.cancel()
        mirrorJob = scope.launch {
            pipeline.state.collect { s -> _viewState.value = s }
        }
    }

    fun setMode(role: String) {
        scope.launch {
            // Same rationale as `onScreenEntered`: wait for a Ready pack so
            // the mode-change event carries a real pack code, and the
            // pipeline's setMode call resolves against a valid session.
            val ready = packController.state
                .filterIsInstance<LanguagePackController.State.Ready>()
                .first()

            crashReporter.setKey("recognition_mode", role)
            analytics.logEvent(
                AnalyticsEvents.RECOGNITION_MODE_CHANGED,
                mapOf(
                    AnalyticsEvents.Param.MODE to role,
                    AnalyticsEvents.Param.PACK_CODE to ready.currentPack.recognitionCode,
                ),
            )
            pipeline.setMode(role)
        }
    }

    fun onScreenLeft() {
        // Cancel a pending start first so a Ready arriving mid-teardown
        // doesn't restart the pipeline after we've asked it to stop.
        pendingStartJob?.cancel()
        pendingStartJob = null
        scope.launch {
            mirrorJob?.cancel()
            mirrorJob = null
            pipeline.stop()
            _viewState.value = RecognitionUiState.Idle
            crashReporter.setKey("recognition_active", "false")
        }
    }

    // -----------------------------------------------------------------------
    // Derived state — exposes CameraController / SettingsRepository /
    // FavoritesRepository views the LiveCameraScreen needs to render. The UI
    // is language- and model-agnostic because everything below reads from
    // pack/state, never from a hardcoded language name or model id.
    // -----------------------------------------------------------------------

    /** True while the currently bound camera has a torch (flash) unit. */
    val hasTorch: Boolean get() = camera.hasTorch

    /** Observable torch state — reflects platform overrides too. */
    val torchEnabled: StateFlow<Boolean> get() = camera.torchEnabled

    /** Which lens is currently active. Read on demand by the switch button. */
    val currentLens: CameraLens get() = camera.lens

    /** Developer HUD gate — off by default; toggle lives in Settings > Diagnostics. */
    val developerMode: StateFlow<Boolean> = settings.state
        .map { it.developerMode }
        .stateIn(scope, SharingStarted.Eagerly, initialValue = settings.state.value.developerMode)

    /**
     * Pack chip label — the currently active recognition pack's display name
     * in the currently active output language. Falls back to the pack's own
     * default output when the pack subsystem is still loading. Language-
     * neutral: whatever the pack's manifest advertises shows here.
     */
    val packDisplayName: StateFlow<String> = packController.state
        .map { s ->
            when (s) {
                is LanguagePackController.State.Ready ->
                    s.currentPack.displayName(s.outputLanguage)
                LanguagePackController.State.Loading -> "…"
                is LanguagePackController.State.Error -> "—"
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, initialValue = "…")

    /**
     * True when the pipeline's current prediction (Recognizing OR Paused
     * with a last-known snapshot) is already a saved favorite. Derived
     * from the live favorites list so external mutations (delete from the
     * Favorites screen, etc.) reflect back here without polling.
     */
    val isFavorited: StateFlow<Boolean> = combine(
        _viewState,
        favorites.observeAll(),
    ) { state, favs ->
        val (packCode, signId) = state.currentSignIdentity() ?: return@combine false
        val expected = favorites.favoriteIdForSign(packCode, signId)
        favs.any { it.id == expected }
    }.stateIn(scope, SharingStarted.Eagerly, initialValue = false)

    /**
     * Pull the `(packCode, signId)` pair off any state that carries a
     * prediction. Returns `null` for states where the concept doesn't apply
     * (Idle, Starting, permission-required, NoModelForMode, Error, or a
     * Recognizing/Paused whose prediction is null because no hand was in
     * frame).
     */
    private fun RecognitionUiState.currentSignIdentity(): Pair<String, String>? = when (this) {
        is RecognitionUiState.Recognizing -> prediction?.sign?.id?.let { packCode to it }
        is RecognitionUiState.Paused -> prediction?.sign?.id?.let { packCode to it }
        else -> null
    }

    // -----------------------------------------------------------------------
    // Actions — every button on LiveCameraScreen routes through here so the
    // Compose layer stays free of platform + persistence knowledge.
    // -----------------------------------------------------------------------

    /**
     * Pause the pipeline if recognizing, resume it if paused. Any other
     * state (Idle, Starting, Error, NoModelForMode, PermissionRequired) is
     * a no-op — the button is disabled in those states in the UI.
     */
    fun togglePause() {
        scope.launch {
            when (state.value) {
                is RecognitionUiState.Recognizing -> {
                    pipeline.pause()
                    analytics.logEvent(AnalyticsEvents.RECOGNITION_PAUSED)
                    crashReporter.setKey("recognition_paused", "true")
                }
                is RecognitionUiState.Paused -> {
                    pipeline.resume()
                    analytics.logEvent(AnalyticsEvents.RECOGNITION_RESUMED)
                    crashReporter.setKey("recognition_paused", "false")
                }
                else -> Unit
            }
        }
    }

    /** Flip the torch on/off. No-op when the current camera has no flash unit. */
    fun toggleTorch() {
        if (!camera.hasTorch) return
        val next = !camera.torchEnabled.value
        scope.launch {
            camera.setTorchEnabled(next)
            analytics.logEvent(
                AnalyticsEvents.CAMERA_TORCH_TOGGLED,
                mapOf(AnalyticsEvents.Param.ENABLED to next),
            )
        }
    }

    /** Swap between the front and back cameras. Preserves the current session. */
    fun switchLens() {
        scope.launch {
            val next = if (camera.lens == CameraLens.Front) CameraLens.Back else CameraLens.Front
            camera.switchLens(next)
            analytics.logEvent(
                AnalyticsEvents.CAMERA_LENS_SWITCHED,
                mapOf(AnalyticsEvents.Param.LENS to next.name.lowercase()),
            )
        }
    }

    /**
     * Toggle the "favorite" state for the sign currently on the translation
     * card. No-op when there's no prediction. Consumes only ViewModel-side
     * data — the FavoritesRepository owns persistence.
     */
    fun toggleFavorite() {
        val current = state.value
        val (packCode, signId) = current.currentSignIdentity() ?: return
        val label = when (current) {
            is RecognitionUiState.Recognizing -> current.prediction?.label
            is RecognitionUiState.Paused -> current.prediction?.label
            else -> null
        } ?: return
        scope.launch {
            val nowFavorited = favorites.toggleFavoriteFromSign(packCode, signId, label)
            analytics.logEvent(
                AnalyticsEvents.RECOGNITION_FAVORITE_TOGGLED,
                mapOf(
                    AnalyticsEvents.Param.PACK_CODE to packCode,
                    AnalyticsEvents.Param.SIGN_ID to signId,
                    AnalyticsEvents.Param.ENABLED to nowFavorited,
                ),
            )
        }
    }

    /**
     * Speak the current prediction's label via the platform TTS gateway.
     * No-op when there's no prediction — the UI dims the Speak button in
     * those states, but the ViewModel guards anyway.
     */
    fun speakCurrentLabel() {
        val label = state.value.currentLabel() ?: return
        speakText(label)
    }

    /**
     * Copy the current prediction's label to the platform clipboard. No-op
     * when there's no prediction to copy (state is not Recognizing/Paused
     * or the last prediction was null).
     */
    fun copyLabel() {
        val label = state.value.currentLabel() ?: return
        clipboard.copyText(label, label = "Sayva")
        analytics.logEvent(AnalyticsEvents.RECOGNITION_LABEL_COPIED)
    }

    /** Pull the current label off any state that carries a prediction. Kept
     *  as an extension so `speakCurrentLabel` and `copyLabel` read state
     *  identically — no drift between the two audio/clipboard paths. */
    private fun RecognitionUiState.currentLabel(): String? = when (this) {
        is RecognitionUiState.Recognizing -> prediction?.label
        is RecognitionUiState.Paused -> prediction?.label
        else -> null
    }

    // -----------------------------------------------------------------------
    // Developer-mode diagnostics — save a per-frame sample for Python-side
    // comparison via `test_golden_inference.py`.
    // -----------------------------------------------------------------------

    private val _lastSavedSamplePath = MutableStateFlow<String?>(null)
    /** Absolute path of the last diagnostic sample the user saved, or `null`
     *  before the first save. Screen surfaces this as a toast-style caption. */
    val lastSavedSamplePath: StateFlow<String?> = _lastSavedSamplePath.asStateFlow()

    /**
     * Dump the current frame's diagnostics — raw landmarks, preprocessed
     * features, top-5 candidates, pack + model identity — to a JSON file in
     * the app's external files dir. Format matches `golden_inference.json`
     * one-case-per-file so it drops straight into Python's regression
     * pipeline. No-op when there's no live prediction.
     */
    fun saveDiagnosticSample() {
        val recognizing = state.value as? RecognitionUiState.Recognizing
            ?: state.value as? RecognitionUiState.Paused
            ?: return
        val (packCode, modelId, prediction, diagnostics) = when (recognizing) {
            is RecognitionUiState.Recognizing -> Sample(
                recognizing.packCode, recognizing.modelId,
                recognizing.prediction, recognizing.diagnostics,
            )
            is RecognitionUiState.Paused -> Sample(
                recognizing.packCode, recognizing.modelId,
                recognizing.prediction, recognizing.diagnostics,
            )
            else -> return
        }
        if (prediction == null || diagnostics.rawLandmarks == null ||
            diagnostics.preprocessedFeatures == null
        ) return

        // Look up the pack so we can include its version + model version in
        // the file — this is exactly what Python's golden fixture pins.
        val pack = (packController.state.value as? LanguagePackController.State.Ready)
            ?.availablePacks?.firstOrNull { it.recognitionCode == packCode }
        val model = pack?.modelById(modelId)
        val json = buildSampleJson(
            packCode = packCode,
            packVersion = pack?.version ?: "unknown",
            modelId = modelId,
            modelFile = model?.modelFile ?: "unknown",
            modelIntegritySha = model?.integrity?.sha256,
            prediction = prediction,
            diagnostics = diagnostics,
            vocabOrder = model?.vocabulary?.signs?.map { it.id } ?: emptyList(),
        )
        val fileName = "sample_${packCode}_${modelId}_${prediction.sign.id}_" +
            "${diagnostics.hashCode().toUInt().toString(16)}.json"
        val path = runCatching { DiagnosticSampleWriter.write(fileName, json) }
            .onFailure { crashReporter.log("saveDiagnosticSample failed: ${it.message}") }
            .getOrNull() ?: return
        _lastSavedSamplePath.value = path
        crashReporter.log("Diagnostic sample saved: $path")
    }

    private data class Sample(
        val packCode: String,
        val modelId: String,
        val prediction: org.moashraf.sayva.pipeline.Prediction?,
        val diagnostics: org.moashraf.sayva.ml.PipelineDiagnostics,
    )

    /** Hand-rolled JSON builder. kotlinx.serialization here would pull the
     *  Prediction/Diagnostics types into the serialization module in a way
     *  that changes their contract for a diagnostic-only dump — cheaper to
     *  emit exactly the shape golden_inference.json uses. */
    private fun buildSampleJson(
        packCode: String,
        packVersion: String,
        modelId: String,
        modelFile: String,
        modelIntegritySha: String?,
        prediction: org.moashraf.sayva.pipeline.Prediction,
        diagnostics: org.moashraf.sayva.ml.PipelineDiagnostics,
        vocabOrder: List<String>,
    ): String {
        val raw = diagnostics.rawLandmarks!!
        val features = diagnostics.preprocessedFeatures!!
        val topK = prediction.topK
        return buildString {
            append('{')
            append("\"captured_at_epoch_ms\":").append(currentEpochMs()).append(',')
            append("\"pack_code\":\"").append(packCode).append("\",")
            append("\"pack_version\":\"").append(packVersion).append("\",")
            append("\"model_id\":\"").append(modelId).append("\",")
            append("\"model_file\":\"").append(modelFile).append("\",")
            if (modelIntegritySha != null) {
                append("\"model_integrity_sha256\":\"").append(modelIntegritySha).append("\",")
            }
            append("\"source_frame_width_px\":").append(diagnostics.sourceFrameWidthPx).append(',')
            append("\"source_frame_height_px\":").append(diagnostics.sourceFrameHeightPx).append(',')
            append("\"vocab_order\":[")
            vocabOrder.forEachIndexed { i, id ->
                if (i > 0) append(',')
                append('"').append(id).append('"')
            }
            append("],")
            // Raw landmarks as pairs matching golden_inference.json's shape.
            append("\"raw_landmarks_21\":[")
            for (i in 0 until 21) {
                if (i > 0) append(',')
                append('[').append(raw[i * 2].toInt()).append(',')
                    .append(raw[i * 2 + 1].toInt()).append(']')
            }
            append("],")
            append("\"features_42\":[")
            for (i in features.indices) {
                if (i > 0) append(',')
                append(features[i])
            }
            append("],")
            append("\"predicted_class_index\":").append(prediction.sign.index).append(',')
            append("\"predicted_sign_id\":\"").append(prediction.sign.id).append("\",")
            append("\"confidence\":").append(prediction.confidence).append(',')
            append("\"confidence_bucket\":\"").append(prediction.bucket.name).append("\",")
            append("\"topK\":[")
            topK.forEachIndexed { i, cp ->
                if (i > 0) append(',')
                append("{\"class_index\":").append(cp.classIndex)
                append(",\"sign_id\":\"").append(vocabOrder.getOrElse(cp.classIndex) { "?" }).append("\"")
                append(",\"probability\":").append(cp.probability).append('}')
            }
            append("]}")
        }
    }
}

/** Local epoch-millis fetch. Kept out of the class body so it can be
 *  swapped for a fake in a test without exposing a constructor param. */
private fun currentEpochMs(): Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
