package org.moashraf.sayva.ui.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.moashraf.sayva.camera.CameraController
import org.moashraf.sayva.languagepack.LanguagePackController
import org.moashraf.sayva.languagepack.RecognitionRole
import org.moashraf.sayva.languagepack.SignRecognizerFactory
import org.moashraf.sayva.languagepack.TranslationRenderer
import org.moashraf.sayva.ml.HandDetectorFactory
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

    val state: StateFlow<RecognitionUiState> = pipeline.state

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
            crashReporter.setKey("recognition_active", "true")
            crashReporter.setKey("recognition_mode", initialMode)
            analytics.logEvent(
                AnalyticsEvents.RECOGNITION_STARTED,
                mapOf(
                    AnalyticsEvents.Param.MODE to initialMode,
                    AnalyticsEvents.Param.PACK_CODE to (activePackCode() ?: "unknown"),
                ),
            )
            pipeline.start(initialMode)
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
            pipeline.stop()
            crashReporter.setKey("recognition_active", "false")
        }
    }

    private fun activePackCode(): String? =
        (packController.state.value as? LanguagePackController.State.Ready)
            ?.currentPack?.recognitionCode
}
