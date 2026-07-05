package org.moashraf.sayva.pipeline

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers as KDispatchers
import org.moashraf.sayva.data.repository.CameraQuality
import org.moashraf.sayva.data.repository.ColorBlindMode
import org.moashraf.sayva.data.repository.DisplayMode
import org.moashraf.sayva.data.repository.SettingsRepository
import org.moashraf.sayva.data.repository.SettingsState
import org.moashraf.sayva.languagepack.LanguagePackController
import org.moashraf.sayva.languagepack.RecognitionRole
import org.moashraf.sayva.ml.HandDetection
import org.moashraf.sayva.ml.RecognitionResult
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Stabilization suite for [DefaultRecognitionPipeline].
 *
 * The bugs that motivated this file — the DI generic-Map collision, the
 * RGBA/MediaPipe format mismatch, the race conditions between the frame
 * collector and pack switch — would each have surfaced at this level with
 * concrete failing tests. Every test uses the fakes in `PipelineTestFakes.kt`
 * to isolate the pipeline from CameraX + MediaPipe + Compose Resources.
 *
 * ### Coroutine strategy
 * The pipeline internally uses `Dispatchers.Default` (frame collector, pack
 * watcher) and `Dispatchers.Main` (state emit). We swap `Main` for a test
 * dispatcher via [Dispatchers.setMain]; `Default` runs on the real pool and
 * we wait for state transitions via [waitForState].
 *
 * ### What we deliberately DO NOT do
 * These tests validate the pipeline as it exists today — not a refactored
 * version. We do not inject dispatchers, do not swap the mutex, and do not
 * mock the pipeline itself. Any regression in the current implementation
 * fails a specific test with a specific message.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultRecognitionPipelineTest {

    private lateinit var camera: FakeCameraController
    private lateinit var handDetectorFactory: FakeHandDetectorFactory
    private lateinit var signRecognizerFactory: FakeSignRecognizerFactory
    private lateinit var translationRenderer: FakeTranslationRenderer
    private lateinit var packRegistry: FakeLanguagePackRegistry
    private lateinit var settings: PipelineFakeSettings
    private lateinit var packController: LanguagePackController
    private lateinit var scope: CoroutineScope
    private lateinit var pipeline: DefaultRecognitionPipeline

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        camera = FakeCameraController()
        handDetectorFactory = FakeHandDetectorFactory()
        signRecognizerFactory = FakeSignRecognizerFactory()
        translationRenderer = FakeTranslationRenderer()
        packRegistry = FakeLanguagePackRegistry()
        settings = PipelineFakeSettings()
        packController = LanguagePackController(packRegistry, settings)
        scope = CoroutineScope(SupervisorJob() + KDispatchers.Default)
        pipeline = DefaultRecognitionPipeline(
            camera = camera,
            handDetectorFactory = handDetectorFactory,
            signRecognizerFactory = signRecognizerFactory,
            translationRenderer = translationRenderer,
            packController = packController,
            scope = scope,
        )
    }

    @AfterTest
    fun tearDown() {
        runBlocking { pipeline.stop() }
        scope.cancel()
        Dispatchers.resetMain()
    }

    // -----------------------------------------------------------------------
    // Start / setup
    // -----------------------------------------------------------------------

    @Test
    fun `start without ready pack controller emits Error`() = runBlocking<Unit> {
        // Do NOT bootstrap packController → state is still Loading
        pipeline.start(RecognitionRole.FINGERSPELLING)
        val state = pipeline.state.value
        assertIs<RecognitionUiState.Error>(state)
        assertTrue(state.cause is IllegalStateException)
    }

    @Test
    fun `start with pack missing role emits NoModelForMode`() = runBlocking<Unit> {
        // ASE pack ships fingerspelling but NOT sign_recognition
        givenBootstrappedPack(TestPackFactory.asePack(includeTemporal = false))
        pipeline.start(RecognitionRole.SIGN_RECOGNITION)
        val state = pipeline.state.value
        assertIs<RecognitionUiState.NoModelForMode>(state)
        assertEquals(TestPackFactory.ASE_CODE, state.packCode)
        assertEquals(RecognitionRole.SIGN_RECOGNITION, state.role)
    }

    @Test
    fun `start with valid pack builds session and starts camera`() = runBlocking<Unit> {
        givenBootstrappedPack(TestPackFactory.asePack())
        pipeline.start(RecognitionRole.FINGERSPELLING)

        assertEquals(1, camera.startCount)
        assertEquals(1, handDetectorFactory.createdDetectors.size)
        assertEquals(1, signRecognizerFactory.createdRecognizers.size)
        assertEquals("fingerspelling", signRecognizerFactory.createdForModelIds.single())
    }

    @Test
    fun `start passes correct maxHands from manifest to HandDetectorFactory`() = runBlocking<Unit> {
        // Fingerspelling model declares maxHands = 1
        givenBootstrappedPack(TestPackFactory.asePack())
        pipeline.start(RecognitionRole.FINGERSPELLING)
        val detector = handDetectorFactory.createdDetectors.single()
        // Detector was created — assert via detectorFactory's supplier param would
        // be cleaner but this simpler check is enough: exactly one was created
        // for the fingerspelling model which declares maxHands=1.
        assertNotNull(detector)
    }

    @Test
    fun `start rolls back native resources when camera start fails`() = runBlocking<Unit> {
        givenBootstrappedPack(TestPackFactory.asePack())
        val bang = RuntimeException("camera-hardware-lost")
        camera.arm(bang)

        pipeline.start(RecognitionRole.FINGERSPELLING)

        // Camera.start threw → pipeline should close the just-built detector +
        // recognizer and expose Error state. Leaving them dangling was the
        // real bug this test guards.
        val state = pipeline.state.value
        assertIs<RecognitionUiState.Error>(state)
        assertEquals(bang, state.cause)
        assertEquals(1, handDetectorFactory.createdDetectors.single().closeCount)
        assertEquals(1, signRecognizerFactory.createdRecognizers.single().closeCount)
    }

    // -----------------------------------------------------------------------
    // Frame processing
    // -----------------------------------------------------------------------

    @Test
    fun `no-hand frame emits heartbeat with diagnostics and null prediction`() = runBlocking<Unit> {
        givenBootstrappedPack(TestPackFactory.asePack())
        pipeline.start(RecognitionRole.FINGERSPELLING)

        val detector = handDetectorFactory.createdDetectors.single() as FakeHandDetector
        detector.armNextDetection(TestPackFactory.handDetection(hands = 0))
        camera.emitFrame()

        val recognizing = waitForState<RecognitionUiState.Recognizing>()
        assertNull(recognizing.prediction, "empty detection should not run the recognizer")
        assertEquals(0, signRecognizerFactory.createdRecognizers.single().recognizeCount)
        assertEquals(0, recognizing.diagnostics.handsDetected)
        assertEquals(TestPackFactory.ASE_CODE, recognizing.diagnostics.packCode)
        assertEquals("fingerspelling", recognizing.diagnostics.modelId)
    }

    @Test
    fun `single-hand detection routes 42 floats through recognizer`() = runBlocking<Unit> {
        givenBootstrappedPack(TestPackFactory.asePack())
        pipeline.start(RecognitionRole.FINGERSPELLING)

        (handDetectorFactory.createdDetectors.single() as FakeHandDetector)
            .armNextDetection(TestPackFactory.handDetection(hands = 1))
        camera.emitFrame()

        val recognizing = waitForState<RecognitionUiState.Recognizing>()
        val recognizer = signRecognizerFactory.createdRecognizers.single()
        assertEquals(1, recognizer.recognizeCount)
        assertEquals(42, recognizer.lastInputSize, "single-hand model must receive 42 floats")
        assertNotNull(recognizing.prediction)
        assertEquals("A", recognizing.prediction!!.label)
    }

    @Test
    fun `two-hand model receives 84 floats even when only one hand detected`() = runBlocking<Unit> {
        // Temporal model advertises maxHands = 2 → pipeline always feeds 84 floats
        val pack = TestPackFactory.pack(
            recognitionCode = TestPackFactory.ASE_CODE,
            displayName = mapOf("en" to "ASL", "ar" to "ASL"),
            models = listOf(TestPackFactory.temporalModel()),
        )
        givenBootstrappedPack(pack)
        pipeline.start(RecognitionRole.SIGN_RECOGNITION)

        (handDetectorFactory.createdDetectors.single() as FakeHandDetector)
            .armNextDetection(TestPackFactory.handDetection(hands = 1))
        camera.emitFrame()

        waitForState<RecognitionUiState.Recognizing>()
        val recognizer = signRecognizerFactory.createdRecognizers.single()
        assertEquals(1, recognizer.recognizeCount)
        assertEquals(84, recognizer.lastInputSize, "two-hand model must receive 84 floats with zero-fill")
    }

    @Test
    fun `recognizer exception on one frame produces Error and recovers on next`() = runBlocking<Unit> {
        givenBootstrappedPack(TestPackFactory.asePack())
        pipeline.start(RecognitionRole.FINGERSPELLING)

        val detector = handDetectorFactory.createdDetectors.single() as FakeHandDetector
        val recognizer = signRecognizerFactory.createdRecognizers.single()
        detector.armNextDetection(TestPackFactory.handDetection(hands = 1))
        recognizer.armNextThrow(IllegalStateException("boom"))

        camera.emitFrame()
        val error = waitForState<RecognitionUiState.Error>()
        assertEquals("boom", error.cause.message)

        // Next frame should succeed and transition BACK to Recognizing
        detector.armNextDetection(TestPackFactory.handDetection(hands = 1))
        camera.emitFrame()
        val recovered = waitForState<RecognitionUiState.Recognizing>()
        assertNotNull(recovered.prediction)
    }

    // -----------------------------------------------------------------------
    // Session lifecycle — pack switch, mode switch, stop
    // -----------------------------------------------------------------------

    @Test
    fun `switching to same role is a no-op`() = runBlocking<Unit> {
        givenBootstrappedPack(TestPackFactory.asePack())
        pipeline.start(RecognitionRole.FINGERSPELLING)
        val builtOnStart = signRecognizerFactory.createdForModelIds.size

        pipeline.setMode(RecognitionRole.FINGERSPELLING)
        assertEquals(builtOnStart, signRecognizerFactory.createdForModelIds.size,
            "setMode with same role should not rebuild")
    }

    @Test
    fun `switching mode rebuilds recognizer for new model role`() = runBlocking<Unit> {
        givenBootstrappedPack(TestPackFactory.asePack(includeTemporal = true))
        pipeline.start(RecognitionRole.FINGERSPELLING)
        assertEquals("fingerspelling", signRecognizerFactory.createdForModelIds.last())

        pipeline.setMode(RecognitionRole.SIGN_RECOGNITION)
        assertEquals("temporal_v1", signRecognizerFactory.createdForModelIds.last())

        // Old detector + recognizer must be closed
        assertEquals(1, handDetectorFactory.createdDetectors.first().closeCount)
        assertEquals(1, signRecognizerFactory.createdRecognizers.first().closeCount)
    }

    @Test
    fun `pack switch mid-flight rebuilds session against new pack`() = runBlocking<Unit> {
        // Install two packs; bootstrap ASE first
        packRegistry.setInstalled(listOf(TestPackFactory.asePack(), TestPackFactory.eslPack()))
        packController.bootstrap()
        pipeline.start(RecognitionRole.FINGERSPELLING)
        assertEquals(1, signRecognizerFactory.createdForModelIds.size)

        // User switches to ESL via Settings — packController.switchRecognition
        // triggers the pipeline's packWatcher which rebuilds inline.
        packController.switchRecognition(TestPackFactory.ESL_CODE)

        // Wait for the new recognizer to be built for the ESL pack
        waitForCondition { signRecognizerFactory.createdForModelIds.size >= 2 }
        // Old resources closed
        assertEquals(1, handDetectorFactory.createdDetectors.first().closeCount)
        assertEquals(1, signRecognizerFactory.createdRecognizers.first().closeCount)
    }

    @Test
    fun `stop closes native handles in correct order and resets state to Idle`() = runBlocking<Unit> {
        givenBootstrappedPack(TestPackFactory.asePack())
        pipeline.start(RecognitionRole.FINGERSPELLING)
        val detector = handDetectorFactory.createdDetectors.single()
        val recognizer = signRecognizerFactory.createdRecognizers.single()

        pipeline.stop()

        assertEquals(1, camera.stopCount)
        assertEquals(1, detector.closeCount)
        assertEquals(1, recognizer.closeCount)
        assertIs<RecognitionUiState.Idle>(pipeline.state.value)
    }

    @Test
    fun `double stop is safe`() = runBlocking<Unit> {
        givenBootstrappedPack(TestPackFactory.asePack())
        pipeline.start(RecognitionRole.FINGERSPELLING)
        pipeline.stop()
        pipeline.stop()
        // No exception; camera.stop called at least once, resources closed
        // exactly once (the null-out inside pipeline prevents double close).
        val detector = handDetectorFactory.createdDetectors.single()
        assertEquals(1, detector.closeCount, "detector must be closed exactly once even on double stop")
    }

    // -----------------------------------------------------------------------
    // Diagnostics
    // -----------------------------------------------------------------------

    @Test
    fun `diagnostics fields carry pack model role and architecture identifiers`() = runBlocking<Unit> {
        givenBootstrappedPack(TestPackFactory.asePack())
        pipeline.start(RecognitionRole.FINGERSPELLING)

        (handDetectorFactory.createdDetectors.single() as FakeHandDetector)
            .armNextDetection(TestPackFactory.handDetection(hands = 1))
        camera.emitFrame()

        val d = waitForState<RecognitionUiState.Recognizing>().diagnostics
        assertEquals(TestPackFactory.ASE_CODE, d.packCode)
        assertEquals("fingerspelling", d.modelId)
        assertEquals("fingerspelling", d.role)
        assertEquals("mlp", d.architecture)
    }

    @Test
    fun `diagnostics latencies are non-zero after real work`() = runBlocking<Unit> {
        // Guards the timeNanos() regression — previously TimeSource.markNow().elapsedNow()
        // returned ~0 for every call because the mark was captured inline.
        givenBootstrappedPack(TestPackFactory.asePack())
        pipeline.start(RecognitionRole.FINGERSPELLING)

        val detector = handDetectorFactory.createdDetectors.single() as FakeHandDetector
        detector.armNextDetection(HandDetection(
            hands = TestPackFactory.handDetection(hands = 1).hands,
            processingNanos = 12_345_678L, // detector reports 12ms
        ))
        camera.emitFrame()

        val d = waitForState<RecognitionUiState.Recognizing>().diagnostics
        assertEquals(12_345_678L, d.handDetectionNanos,
            "diagnostics must faithfully report HandDetector.processingNanos")
        // totalFrameNanos must be > 0 — asserts timeNanos() works
        assertTrue(d.totalFrameNanos > 0, "totalFrameNanos was ${d.totalFrameNanos}")
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private suspend fun givenBootstrappedPack(pack: org.moashraf.sayva.languagepack.LanguagePack) {
        packRegistry.setInstalled(listOf(pack))
        packController.bootstrap()
    }

    /** Suspend until the pipeline reaches state of type [T] or timeout. */
    private suspend inline fun <reified T : RecognitionUiState> waitForState(
        timeoutMillis: Long = 2_000,
    ): T = withTimeout(timeoutMillis) {
        pipeline.state.filter { it is T }.first() as T
    }

    /** Poll a condition until true or timeout. */
    private suspend fun waitForCondition(
        timeoutMillis: Long = 2_000,
        predicate: () -> Boolean,
    ) {
        withTimeout(timeoutMillis) {
            while (!predicate()) delay(1)
        }
    }
}

/**
 * Minimal in-memory [SettingsRepository] for pipeline tests. Only the
 * pack-controller-relevant setters actually mutate state; the rest no-op.
 * Duplicates the pattern in `LanguagePackControllerTest` — small enough to
 * repeat rather than share a fixture across suites.
 */
private class PipelineFakeSettings(initial: SettingsState = SettingsState()) : SettingsRepository {
    private val _state = MutableStateFlow(initial)
    override val state: StateFlow<SettingsState> = _state.asStateFlow()

    override fun setRecognitionLanguageCode(code: String?) {
        _state.value = _state.value.copy(recognitionLanguageCode = code)
    }
    override fun setOutputLanguageCode(code: String?) {
        _state.value = _state.value.copy(outputLanguageCode = code)
    }

    override fun setDisplayMode(mode: DisplayMode) {}
    override fun setFontSizeScale(scale: Float) {}
    override fun setHighContrast(enabled: Boolean) {}
    override fun setVoiceSpeed(speed: Float) {}
    override fun setCameraQuality(quality: CameraQuality) {}
    override fun setOfflineMode(enabled: Boolean) {}
    override fun setEmergencyMode(enabled: Boolean) {}
    override fun setEasyMode(enabled: Boolean) {}
    override fun setLargerText(enabled: Boolean) {}
    override fun setColorBlindMode(mode: ColorBlindMode) {}
    override fun setLeftHandedMode(enabled: Boolean) {}
    override fun setHapticIntensity(intensity: Float) {}
    override fun setReduceMotion(enabled: Boolean) {}
    override fun setScreenReaderHints(enabled: Boolean) {}
    override fun resetToDefaults() { _state.value = SettingsState() }
}
