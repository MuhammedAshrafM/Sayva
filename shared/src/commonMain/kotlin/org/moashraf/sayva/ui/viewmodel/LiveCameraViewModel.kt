package org.moashraf.sayva.ui.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.moashraf.sayva.camera.CameraController
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
    camera: CameraController,
    handDetectorFactory: HandDetectorFactory,
    signRecognizerFactory: SignRecognizerFactory,
    translationRenderer: TranslationRenderer,
    private val packController: LanguagePackController,
    private val permissionController: PermissionController,
    private val analytics: AnalyticsGateway,
    private val crashReporter: CrashReporter,
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
        scope.launch {
            // Gate on camera permission BEFORE touching the pipeline. If
            // denied, we surface CameraPermissionRequired and let the screen
            // route to PermissionsScreen. The pipeline never sees a failed
            // camera.start() from a missing permission — cleaner separation
            // and the user gets a proper affordance instead of an error card.
            if (!permissionController.isGranted(SayvaPermission.Camera)) {
                _viewState.value = RecognitionUiState.CameraPermissionRequired
                return@launch
            }

            crashReporter.setKey("recognition_active", "true")
            crashReporter.setKey("recognition_mode", initialMode)
            analytics.logEvent(
                AnalyticsEvents.RECOGNITION_STARTED,
                mapOf(
                    AnalyticsEvents.Param.MODE to initialMode,
                    AnalyticsEvents.Param.PACK_CODE to (activePackCode() ?: "unknown"),
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
            crashReporter.setKey("recognition_mode", role)
            analytics.logEvent(
                AnalyticsEvents.RECOGNITION_MODE_CHANGED,
                mapOf(
                    AnalyticsEvents.Param.MODE to role,
                    AnalyticsEvents.Param.PACK_CODE to (activePackCode() ?: "unknown"),
                ),
            )
            pipeline.setMode(role)
        }
    }

    fun onScreenLeft() {
        scope.launch {
            mirrorJob?.cancel()
            mirrorJob = null
            pipeline.stop()
            _viewState.value = RecognitionUiState.Idle
            crashReporter.setKey("recognition_active", "false")
        }
    }

    private fun activePackCode(): String? =
        (packController.state.value as? LanguagePackController.State.Ready)
            ?.currentPack?.recognitionCode
}
