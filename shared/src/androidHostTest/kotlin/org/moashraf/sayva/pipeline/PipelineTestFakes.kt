package org.moashraf.sayva.pipeline

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.moashraf.sayva.camera.CameraController
import org.moashraf.sayva.camera.CameraFrame
import org.moashraf.sayva.camera.CameraLens
import org.moashraf.sayva.languagepack.ConfidenceThresholds
import org.moashraf.sayva.languagepack.LabelResult
import org.moashraf.sayva.languagepack.LanguagePack
import org.moashraf.sayva.languagepack.LanguagePackRegistry
import org.moashraf.sayva.languagepack.ModelInputSpec
import org.moashraf.sayva.languagepack.ModelOutputSpec
import org.moashraf.sayva.languagepack.OutputLanguageStatus
import org.moashraf.sayva.languagepack.PackModel
import org.moashraf.sayva.languagepack.PostProcessingRules
import org.moashraf.sayva.languagepack.SignVocabulary
import org.moashraf.sayva.languagepack.SignRecognizerFactory
import org.moashraf.sayva.languagepack.TranslationRenderer
import org.moashraf.sayva.languagepack.VocabSign
import org.moashraf.sayva.ml.HandDetection
import org.moashraf.sayva.ml.HandDetector
import org.moashraf.sayva.ml.HandDetectorFactory
import org.moashraf.sayva.ml.HandLandmarks
import org.moashraf.sayva.ml.Handedness
import org.moashraf.sayva.ml.RecognitionResult
import org.moashraf.sayva.ml.SignRecognizer

/**
 * Shared test doubles for the RecognitionPipeline suite.
 *
 * These are intentionally minimal — they satisfy the contract each port
 * declares and expose observable counters so tests can assert on invocation
 * patterns (close called exactly once, camera started, etc.). They do NOT
 * change or mock the pipeline itself — the whole point of the stabilization
 * batch is to validate the current implementation as-is.
 */

// ---------------------------------------------------------------------------
// Camera
// ---------------------------------------------------------------------------

class FakeCameraController(
    /** If non-null, [start] throws this on first call. Cleared after throwing. */
    private var startThrows: Throwable? = null,
) : CameraController {

    val frameChannel = Channel<CameraFrame>(
        capacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val _frames = MutableSharedFlow<CameraFrame>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val frames: SharedFlow<CameraFrame> = _frames.asSharedFlow()

    var startCount: Int = 0
        private set
    var stopCount: Int = 0
        private set

    private var _lens: CameraLens = CameraLens.Front
    override val lens: CameraLens
        get() = _lens

    override suspend fun start() {
        startCount++
        startThrows?.let {
            startThrows = null
            throw it
        }
    }

    override suspend fun stop() {
        stopCount++
    }

    override suspend fun switchLens(lens: CameraLens) {
        _lens = lens
    }

    /** Push a synthetic frame downstream — pipeline collects and processes. */
    suspend fun emitFrame(frame: CameraFrame = syntheticFrame()) {
        _frames.emit(frame)
    }

    fun arm(next: Throwable) {
        startThrows = next
    }

    companion object {
        fun syntheticFrame(): CameraFrame = CameraFrame(
            widthPx = 640,
            heightPx = 480,
            rotationDegrees = 0,
            timestampNanos = 0L,
            platformFrame = Any(), // opaque token; PlatformFrame.close is a no-op stub in tests
        )
    }
}

// ---------------------------------------------------------------------------
// HandDetector
// ---------------------------------------------------------------------------

class FakeHandDetector(
    /** Default detection result. Tests override per-call via [nextDetection]. */
    private val defaultDetection: HandDetection = HandDetection(emptyList(), processingNanos = 100_000L),
) : HandDetector {

    private var nextDetection: HandDetection? = null
    private var nextThrow: Throwable? = null

    var detectCount: Int = 0
        private set
    var closeCount: Int = 0
        private set

    override fun detect(frame: CameraFrame): HandDetection {
        detectCount++
        nextThrow?.let {
            nextThrow = null
            throw it
        }
        return nextDetection?.also { nextDetection = null } ?: defaultDetection
    }

    override fun close() {
        closeCount++
    }

    fun armNextDetection(detection: HandDetection) {
        nextDetection = detection
    }

    fun armNextThrow(t: Throwable) {
        nextThrow = t
    }
}

class FakeHandDetectorFactory(
    private val detectorSupplier: (Int) -> FakeHandDetector = { FakeHandDetector() },
) : HandDetectorFactory {

    var createdDetectors: MutableList<FakeHandDetector> = mutableListOf()
        private set

    override fun create(maxHands: Int): HandDetector {
        val d = detectorSupplier(maxHands)
        createdDetectors.add(d)
        return d
    }
}

// ---------------------------------------------------------------------------
// SignRecognizer
// ---------------------------------------------------------------------------

class FakeSignRecognizer(
    private val result: RecognitionResult = RecognitionResult(classIndex = 0, confidence = 0.95f),
) : SignRecognizer {

    var recognizeCount: Int = 0
        private set
    var closeCount: Int = 0
        private set
    var lastInputSize: Int? = null
        private set
    private var nextThrow: Throwable? = null

    override fun recognize(landmarks: FloatArray): RecognitionResult {
        recognizeCount++
        lastInputSize = landmarks.size
        nextThrow?.let {
            nextThrow = null
            throw it
        }
        return result
    }

    override fun close() {
        closeCount++
    }

    fun armNextThrow(t: Throwable) {
        nextThrow = t
    }
}

class FakeSignRecognizerFactory(
    private val recognizerSupplier: () -> FakeSignRecognizer = { FakeSignRecognizer() },
    private var factoryThrows: Throwable? = null,
) : SignRecognizerFactory {

    var createdRecognizers: MutableList<FakeSignRecognizer> = mutableListOf()
        private set

    var createdForModelIds: MutableList<String> = mutableListOf()
        private set

    override suspend fun forModel(pack: LanguagePack, modelId: String): SignRecognizer {
        factoryThrows?.let {
            factoryThrows = null
            throw it
        }
        createdForModelIds.add(modelId)
        val r = recognizerSupplier()
        createdRecognizers.add(r)
        return r
    }

    fun armNextThrow(t: Throwable) {
        factoryThrows = t
    }
}

// ---------------------------------------------------------------------------
// TranslationRenderer
// ---------------------------------------------------------------------------

class FakeTranslationRenderer : TranslationRenderer {
    var renderCount: Int = 0
        private set

    override fun render(
        pack: LanguagePack,
        outputCode: String,
        modelId: String,
        sign: VocabSign,
    ): LabelResult {
        renderCount++
        // Return the sign id as the label — good enough for pipeline tests.
        return LabelResult(
            label = sign.id,
            effectiveOutputCode = outputCode,
            fallback = LabelResult.FallbackReason.None,
        )
    }
}

// ---------------------------------------------------------------------------
// LanguagePackRegistry — supplies a canned pack list without disk I/O.
// ---------------------------------------------------------------------------

class FakeLanguagePackRegistry(
    initial: List<LanguagePack> = emptyList(),
) : LanguagePackRegistry {
    private val _installed = MutableStateFlow(initial)
    override val installed: StateFlow<List<LanguagePack>> = _installed.asStateFlow()

    override fun byRecognitionCode(code: String): LanguagePack? =
        _installed.value.firstOrNull { it.recognitionCode == code }

    override suspend fun refresh(): List<LanguagePack> = _installed.value

    fun setInstalled(packs: List<LanguagePack>) {
        _installed.value = packs
    }
}

// ---------------------------------------------------------------------------
// Pack builders — tests compose packs matching the manifest shape without
// touching a real JSON file. Kept ultra-terse for readable test setup.
// ---------------------------------------------------------------------------

object TestPackFactory {
    const val ASE_CODE = "ase"
    const val ESL_CODE = "esl"

    fun asePack(
        includeFingerspelling: Boolean = true,
        includeTemporal: Boolean = false,
    ): LanguagePack = pack(
        recognitionCode = ASE_CODE,
        displayName = mapOf("en" to "American Sign Language", "ar" to "لغة الإشارة الأمريكية"),
        models = buildList {
            if (includeFingerspelling) add(fingerspellingModel())
            if (includeTemporal) add(temporalModel())
        },
    )

    fun eslPack(): LanguagePack = pack(
        recognitionCode = ESL_CODE,
        displayName = mapOf("en" to "Egyptian Sign Language", "ar" to "لغة الإشارة المصرية"),
        models = listOf(fingerspellingModel()),
    )

    fun pack(
        recognitionCode: String,
        displayName: Map<String, String>,
        models: List<PackModel>,
        supportedOutputs: List<String> = listOf("en", "ar"),
        defaultOutput: String = "en",
    ): LanguagePack = LanguagePack(
        schemaVersion = 1,
        recognitionCode = recognitionCode,
        displayName = displayName,
        version = "1.0.0",
        minAppVersion = 1,
        bundled = true,
        models = models,
        supportedOutputs = supportedOutputs,
        outputLanguageStatus = mapOf(
            "en" to OutputLanguageStatus.Complete,
            "ar" to OutputLanguageStatus.Stub,
        ),
        defaultOutputLanguage = defaultOutput,
        outputLabels = mapOf(
            "en" to models.associate { it.id to it.vocabulary.signs.associate { s -> s.id to s.id } },
            "ar" to models.associate { it.id to it.vocabulary.signs.associate { s -> s.id to null } },
        ),
        ttsLocaleByOutput = mapOf("en" to "en-US", "ar" to "ar-EG"),
        postProcessing = PostProcessingRules(
            spellOutBlankTimeoutMs = 700,
            sentenceAssemblyRuleset = "standard_v1",
            capitalization = "sentence_case",
        ),
    )

    fun fingerspellingModel(): PackModel = PackModel(
        id = "fingerspelling",
        role = "fingerspelling",
        architecture = "mlp",
        modelFile = "models/fingerspelling.tflite",
        runtimeType = "tflite",
        inferenceStrategy = "single_frame",
        input = ModelInputSpec(
            shape = listOf(1, 42),
            preprocessing = "single_hand_kazuhito_v1",
            maxHands = 1,
        ),
        output = ModelOutputSpec(
            shape = listOf(1, 3),
            postprocessing = "argmax_confidence_v1",
        ),
        confidenceThresholds = ConfidenceThresholds(show = 0.9f, caution = 0.6f),
        vocabulary = SignVocabulary(
            version = 1,
            signs = listOf(
                VocabSign(0, "A", emptyList()),
                VocabSign(1, "B", emptyList()),
                VocabSign(2, "C", emptyList()),
            ),
        ),
    )

    fun temporalModel(): PackModel = PackModel(
        id = "temporal_v1",
        role = "sign_recognition",
        architecture = "lstm_unrolled",
        modelFile = "models/temporal.tflite",
        runtimeType = "tflite",
        inferenceStrategy = "single_frame",
        input = ModelInputSpec(
            shape = listOf(1, 30, 84),
            preprocessing = "two_hand_sequence_v1",
            maxHands = 2,
            sequenceLength = 30,
        ),
        output = ModelOutputSpec(
            shape = listOf(1, 5),
            postprocessing = "argmax_confidence_v1",
        ),
        confidenceThresholds = ConfidenceThresholds(show = 0.8f, caution = 0.5f),
        vocabulary = SignVocabulary(
            version = 1,
            signs = listOf(
                VocabSign(0, "HELLO", emptyList()),
                VocabSign(1, "THANK_YOU", emptyList()),
                VocabSign(2, "PLEASE", emptyList()),
                VocabSign(3, "SORRY", emptyList()),
                VocabSign(4, "YES", emptyList()),
            ),
        ),
    )

    fun landmarks42(): FloatArray = FloatArray(42) { it * 0.01f }

    fun handDetection(hands: Int): HandDetection = HandDetection(
        hands = (0 until hands).map {
            HandLandmarks(
                handedness = if (it == 0) Handedness.Left else Handedness.Right,
                landmarks = landmarks42(),
            )
        },
        processingNanos = 10_000L,
    )
}
