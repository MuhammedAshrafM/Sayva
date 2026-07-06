package org.moashraf.sayva.pipeline

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.TimeSource
import org.moashraf.sayva.camera.CameraController
import org.moashraf.sayva.camera.CameraFrame
import org.moashraf.sayva.languagepack.LanguagePack
import org.moashraf.sayva.languagepack.LanguagePackController
import org.moashraf.sayva.languagepack.PackModel
import org.moashraf.sayva.languagepack.SignRecognizerFactory
import org.moashraf.sayva.languagepack.TranslationRenderer
import org.moashraf.sayva.ml.HandDetector
import org.moashraf.sayva.ml.HandDetectorFactory
import org.moashraf.sayva.ml.PipelineDiagnostics
import org.moashraf.sayva.ml.SignRecognizer

/**
 * Orchestrates: camera frames → hand detection → recognizer → UI state.
 *
 * The pipeline is fully driven by the active [LanguagePack]:
 *   * `pack.modelByRole(mode)` picks which model to run
 *   * `model.input.maxHands` configures the [HandDetector]
 *   * `model.input.preprocessing` + `model.output.postprocessing` are wired
 *     inside [SignRecognizerFactory] via the standard adapter registries
 *   * `model.confidenceThresholds.bucketFor(...)` decides UI bucketing
 *
 * No language-specific or model-shape-specific logic anywhere here. A future
 * Pack that ships a Transformer-based sentence recognizer with a different
 * runtime works with zero changes to this class — just needs adapter IDs
 * registered in Koin and a manifest with a matching role.
 *
 * Lifecycle:
 *   1. UI screen enters → `start(mode)` — sets state to Recognizing
 *   2. Camera emits frames → each is detected + recognized → [state] updates
 *   3. User switches mode → `setMode(newMode)` — rebuilds recognizer inline
 *   4. Pack switch (from Settings) — pipeline observes [LanguagePackController]
 *      and rebuilds the recognizer on the fly
 *   5. UI screen leaves → `stop()` — pipeline releases native resources
 */
interface RecognitionPipeline {
    val state: StateFlow<RecognitionUiState>

    /**
     * Begin recognition in the given [role] — one of the constants from
     * [org.moashraf.sayva.languagepack.RecognitionRole]. If the active
     * pack doesn't declare a model for this role, [state] becomes
     * [RecognitionUiState.NoModelForMode].
     */
    suspend fun start(role: String)

    /** Change modes without restarting the camera. */
    suspend fun setMode(role: String)

    /** Release camera + native resources. Safe to call repeatedly. */
    suspend fun stop()
}

class DefaultRecognitionPipeline(
    private val camera: CameraController,
    private val handDetectorFactory: HandDetectorFactory,
    private val signRecognizerFactory: SignRecognizerFactory,
    private val translationRenderer: TranslationRenderer,
    private val packController: LanguagePackController,
    private val scope: CoroutineScope,
    /**
     * After this many CONSECUTIVE frame-processing errors, the pipeline
     * cancels its collector and latches at [RecognitionUiState.Error] until
     * [start] is invoked again. Guards against a corrupt model or a wedged
     * native handle burning 30 FPS worth of CPU + Crashlytics events until
     * the user quits.
     *
     * Any single successful frame resets the counter — a transient recognizer
     * hiccup still recovers naturally on the very next frame.
     */
    private val frameErrorBackoffThreshold: Int = 5,
) : RecognitionPipeline {

    private val _state = MutableStateFlow<RecognitionUiState>(RecognitionUiState.Idle)
    override val state: StateFlow<RecognitionUiState> = _state.asStateFlow()

    /**
     * Guards both the setup fields (`currentRole` / `currentPack` / `currentModel`)
     * and the native handles (`handDetector` / `recognizer`). Every mutator AND
     * every reader takes this lock — frame processing sees a consistent snapshot
     * even when a pack switch is racing with the collector.
     */
    private val mutex = Mutex()

    /** Immutable snapshot of the setup for one recognizer session. Swapped
     *  atomically inside [buildForActivePack]; the frame collector reads a
     *  local copy so mid-frame swaps don't tear. */
    private data class Session(
        val role: String,
        val pack: LanguagePack,
        val model: PackModel,
        val handDetector: HandDetector,
        val recognizer: SignRecognizer,
    )

    private var currentRole: String? = null
    private var currentSession: Session? = null

    private var frameCollector: Job? = null
    private var packWatcher: Job? = null

    /** Consecutive frame-processing errors since the last successful frame or
     *  the last [start]. Mutated only inside [processFrame], which the frame
     *  collector invokes sequentially — no cross-coroutine access. */
    private var consecutiveFrameErrors: Int = 0

    // Rolling FPS window — smoothed so the debug overlay doesn't jitter.
    private val recentFrameNanos = ArrayDeque<Long>()

    /** Fixed reference mark; every `timeNanos()` call returns elapsed since here.
     *  `markNow().elapsedNow()` back-to-back returns ~0 — that's the bug this
     *  replaces. */
    private val timeOrigin = TimeSource.Monotonic.markNow()

    private fun timeNanos(): Long = timeOrigin.elapsedNow().inWholeNanoseconds

    override suspend fun start(role: String) = mutex.withLock {
        _state.value = RecognitionUiState.Starting
        currentRole = role
        consecutiveFrameErrors = 0

        val ready = packController.state.value as? LanguagePackController.State.Ready
            ?: run {
                _state.value = RecognitionUiState.Error(
                    IllegalStateException("Language pack subsystem not ready yet"),
                )
                return@withLock
            }

        if (!buildForActivePack(role, ready.currentPack)) return@withLock

        // Camera start can throw (permission, hardware) — if it fails, roll back
        // the resources we just allocated so we don't leak them.
        try {
            camera.start()
        } catch (t: Throwable) {
            currentSession?.close()
            currentSession = null
            _state.value = RecognitionUiState.Error(
                cause = t,
                packCode = ready.currentPack.recognitionCode,
                modelId = currentSession?.model?.id,
            )
            return@withLock
        }

        launchFrameCollector()
        launchPackWatcher()
    }

    override suspend fun setMode(role: String) = mutex.withLock {
        if (role == currentRole) return@withLock
        currentRole = role
        val ready = packController.state.value as? LanguagePackController.State.Ready
        if (ready != null) buildForActivePack(role, ready.currentPack)
    }

    override suspend fun stop(): Unit = mutex.withLock {
        // Stop collectors first, then wait for them to drain, then close native
        // handles. This order matters: closing before joining could race
        // `detector.detect(...)` calls in flight.
        frameCollector?.cancelAndJoin()
        packWatcher?.cancelAndJoin()
        frameCollector = null
        packWatcher = null
        camera.stop()
        currentSession?.close()
        currentSession = null
        currentRole = null
        consecutiveFrameErrors = 0
        recentFrameNanos.clear()
        _state.value = RecognitionUiState.Idle
    }

    /**
     * Rebuild the detector + recognizer for [pack] serving [role]. Assumes
     * mutex is held. Model file I/O + native init happen on Dispatchers.IO
     * so we never block the Main thread even when called from a UI-triggered
     * suspend.
     */
    private suspend fun buildForActivePack(role: String, pack: LanguagePack): Boolean {
        val model = pack.modelByRole(role)
        if (model == null) {
            currentSession?.close()
            currentSession = null
            _state.value = RecognitionUiState.NoModelForMode(
                packCode = pack.recognitionCode,
                role = role,
            )
            return false
        }
        return try {
            // Load model bytes + init native handles OFF the Main thread. Every
            // caller into buildForActivePack goes through the same withContext,
            // so Main is never blocked even during first-launch resolution.
            val nextSession = withContext(Dispatchers.Default) {
                val detector = handDetectorFactory.create(maxHands = model.input.maxHands)
                val rec = signRecognizerFactory.forModel(pack, model.id)
                Session(role, pack, model, detector, rec)
            }
            // Only close the OLD session AFTER the new one is fully built —
            // partial-failure rollback: if construction throws, current stays intact.
            currentSession?.close()
            currentSession = nextSession
            true
        } catch (t: Throwable) {
            _state.value = RecognitionUiState.Error(
                cause = t,
                packCode = pack.recognitionCode,
                modelId = model.id,
            )
            false
        }
    }

    private fun Session.close() {
        runCatching { recognizer.close() }
        runCatching { handDetector.close() }
    }

    private fun launchFrameCollector() {
        frameCollector = scope.launch(Dispatchers.Default) {
            camera.frames.collect { frame ->
                processFrame(frame)
            }
        }
    }

    /**
     * Watches [LanguagePackController.state]. When the active pack changes
     * (user switched via Settings), we rebuild the detector + recognizer on
     * the fly so recognition keeps flowing under the new pack — no restart.
     */
    private fun launchPackWatcher() {
        packWatcher = scope.launch(Dispatchers.Default) {
            packController.state.collect { s ->
                val ready = s as? LanguagePackController.State.Ready ?: return@collect
                mutex.withLock {
                    val role = currentRole ?: return@withLock
                    val existing = currentSession
                    if (existing != null && existing.pack.recognitionCode == ready.currentPack.recognitionCode) {
                        return@withLock
                    }
                    buildForActivePack(role, ready.currentPack)
                }
            }
        }
    }

    private suspend fun processFrame(frame: CameraFrame) {
        // Snapshot the session under the mutex so pack/mode switches happening
        // during this frame's processing don't tear our references. If no
        // session is active (Idle, Error, NoModelForMode), close the frame
        // and drop — no diagnostics update needed.
        val session = mutex.withLock { currentSession } ?: return closeFrame(frame)

        val model = session.model
        val pack = session.pack
        val detector = session.handDetector
        val rec = session.recognizer

        val totalStart = timeNanos()
        try {
            val detection = detector.detect(frame)
            val handDetectionNanos = detection.processingNanos

            val prediction: Prediction?
            val preprocessingNanos: Long
            val inferenceNanos: Long
            val postprocessingNanos: Long

            if (detection.hands.isEmpty()) {
                // No hand this frame — still emit a heartbeat so diagnostics update.
                prediction = null
                preprocessingNanos = 0L
                inferenceNanos = 0L
                postprocessingNanos = 0L
            } else {
                // Assemble model input. Single-hand: use the first detected hand's
                // 42 floats. Two-hand: concatenate `[left, right]` with zero-fill
                // when a hand is missing.
                val landmarks: FloatArray = when (model.input.maxHands) {
                    1 -> detection.hands.first().landmarks
                    2 -> assembleTwoHand(detection)
                    else -> error("Unsupported maxHands ${model.input.maxHands}")
                }
                val result = rec.recognize(landmarks)
                preprocessingNanos = result.preprocessingNanos
                inferenceNanos = result.inferenceNanos
                postprocessingNanos = result.postprocessingNanos
                prediction = buildPrediction(pack, model, result)
            }

            val totalEnd = timeNanos()
            val diagnostics = PipelineDiagnostics(
                packCode = pack.recognitionCode,
                modelId = model.id,
                role = model.role,
                architecture = model.architecture,
                totalFrameNanos = totalEnd - totalStart,
                handDetectionNanos = handDetectionNanos,
                preprocessingNanos = preprocessingNanos,
                inferenceNanos = inferenceNanos,
                postprocessingNanos = postprocessingNanos,
                handsDetected = detection.hands.size,
                confidence = prediction?.confidence,
                fps = updateFps(totalEnd),
            )

            consecutiveFrameErrors = 0
            withContext(Dispatchers.Main) {
                _state.value = RecognitionUiState.Recognizing(
                    packCode = pack.recognitionCode,
                    modelId = model.id,
                    role = model.role,
                    architecture = model.architecture,
                    prediction = prediction,
                    diagnostics = diagnostics,
                )
            }
        } catch (t: Throwable) {
            consecutiveFrameErrors += 1
            withContext(Dispatchers.Main) {
                _state.value = RecognitionUiState.Error(
                    cause = t,
                    packCode = pack.recognitionCode,
                    modelId = model.id,
                )
            }
            if (consecutiveFrameErrors >= frameErrorBackoffThreshold) {
                // Persistent failure — the model, native detector, or the
                // frame path itself is wedged. Stop consuming frames so we
                // don't burn 30 FPS worth of CPU + Crashlytics reports until
                // the user quits. Requires an explicit `start()` to resume.
                triggerErrorBackoff()
            }
        } finally {
            closeFrame(frame)
        }
    }

    /**
     * Tear down the frame collector + pack watcher after persistent frame
     * errors. Runs on the collector coroutine itself; we launch the cleanup
     * on [scope] so we don't try to `cancelAndJoin` our own job. State is
     * already at [RecognitionUiState.Error] — we deliberately do NOT reset
     * it to Idle so the UI still surfaces the failure to the user.
     */
    private fun triggerErrorBackoff() {
        scope.launch {
            mutex.withLock {
                frameCollector?.cancel()
                packWatcher?.cancel()
                frameCollector = null
                packWatcher = null
                camera.stop()
                currentSession?.close()
                currentSession = null
            }
        }
    }

    private fun assembleTwoHand(detection: org.moashraf.sayva.ml.HandDetection): FloatArray {
        // For MVP: put first hand in slot 0, second in slot 1. Handedness-aware
        // routing (Left → slot 0, Right → slot 1) is a Phase 2 follow-up —
        // needs the preprocessor to advertise its ordering convention.
        val out = FloatArray(84)
        detection.hands.getOrNull(0)?.landmarks?.copyInto(out, 0)
        detection.hands.getOrNull(1)?.landmarks?.copyInto(out, 42)
        return out
    }

    private fun buildPrediction(
        pack: LanguagePack,
        model: PackModel,
        result: org.moashraf.sayva.ml.RecognitionResult,
    ): Prediction? {
        val sign = model.vocabulary.byIndex(result.classIndex) ?: return null
        val output = packController.state.value.let { s ->
            (s as? LanguagePackController.State.Ready)?.outputLanguage
                ?: pack.defaultOutputLanguage
        }
        val labelResult = translationRenderer.render(
            pack = pack, outputCode = output, modelId = model.id, sign = sign,
        )
        return Prediction(
            sign = sign,
            confidence = result.confidence,
            bucket = model.confidenceThresholds.bucketFor(result.confidence),
            label = labelResult.label,
            effectiveOutputCode = labelResult.effectiveOutputCode,
        )
    }

    /** Rolling FPS across the last ~30 frames. */
    private fun updateFps(nowNanos: Long): Float {
        recentFrameNanos.addLast(nowNanos)
        while (recentFrameNanos.size > 30) recentFrameNanos.removeFirst()
        if (recentFrameNanos.size < 2) return 0f
        val span = (recentFrameNanos.last() - recentFrameNanos.first()).coerceAtLeast(1)
        val frames = recentFrameNanos.size - 1
        return frames * 1_000_000_000f / span
    }

    /** The camera controller may hand out a per-platform frame handle
     *  that needs releasing (Android ImageProxy). Only that concern is
     *  platform-specific; we call an `expect fun` so commonMain stays clean. */
    private fun closeFrame(frame: CameraFrame) = closePlatformFrame(frame)
}

/**
 * Release native resources associated with [frame]. Android holds a CameraX
 * `ImageProxy` that must be closed after processing; iOS holds a
 * `CVPixelBuffer` that releases via ARC and is a no-op here.
 */
internal expect fun closePlatformFrame(frame: CameraFrame)
