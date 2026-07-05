package org.moashraf.sayva.ml

import org.moashraf.sayva.languagepack.SignVocabulary
import org.moashraf.sayva.languagepack.VocabSign
import org.moashraf.sayva.ml.adapters.SingleHandKazuhitoPreprocessor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Track A smoke test — reframed for the port-and-adapter architecture.
 *
 * ## What runs here (host JVM)
 * - `ComposedSignRecognizer` argmax + input validation with the standard
 *   [ArgmaxConfidencePostprocessor] and [SingleHandKazuhitoPreprocessor]
 *   plumbed through a fake [ModelRuntime]. Pure Kotlin, no native deps.
 * - Bundled `smoke_test.tflite` sanity: file present, TFLite v3 magic bytes.
 *
 * ## What does NOT run here
 * TFLite ships `.so` binaries for Android ABIs only. On JVM host,
 * `Interpreter(...)` throws `UnsatisfiedLinkError`. Real invoke-path
 * validation happens on-device: `./gradlew :androidApp:assembleDebug`
 * proves the runtime links, and Phase 2 wire-up drives the actual native
 * call from Compose UI.
 */
class ModelRuntimeSmokeTest {

    @Test
    fun `bundled smoke_test tflite is present and looks like a TFLite FlatBuffer`() {
        val bytes = loadModelBytes()
        assertTrue(bytes.size > 1024, "Model bytes suspiciously small: ${bytes.size}")
        val id = String(bytes, offset = 4, length = 4, charset = Charsets.US_ASCII)
        assertEquals("TFL3", id, "Not a TFLite v3 model — got header '$id'")
    }

    @Test
    fun `argmax picks the highest-probability class`() {
        val runtime = FakeModelRuntime(output = floatArrayOf(0.05f, 0.90f, 0.05f))
        val recognizer = build(runtime)
        val result = recognizer.recognize(DUMMY_42_INPUT)
        assertEquals(1, result.classIndex)
        assertEquals(0.90f, result.confidence)
    }

    @Test
    fun `argmax breaks ties on the first-encountered maximum`() {
        val runtime = FakeModelRuntime(output = floatArrayOf(0.4f, 0.4f, 0.2f))
        val recognizer = build(runtime)
        val result = recognizer.recognize(DUMMY_42_INPUT)
        assertEquals(0, result.classIndex, "Tie should resolve to the earliest index")
    }

    @Test
    fun `recognizer rejects wrong-sized input via preprocessor`() {
        val runtime = FakeModelRuntime(output = floatArrayOf(1f, 0f, 0f))
        val recognizer = build(runtime)
        val ex = runCatching { recognizer.recognize(FloatArray(10)) }.exceptionOrNull()
        assertTrue(
            ex is IllegalArgumentException,
            "Expected IllegalArgumentException on wrong input size, got $ex"
        )
    }

    @Test
    fun `recognizer routes preprocessed input to the runtime`() {
        val runtime = FakeModelRuntime(output = floatArrayOf(1f, 0f, 0f))
        val recognizer = build(runtime)
        recognizer.recognize(OPEN_HAND_SAMPLE)
        assertEquals(1, runtime.calls, "Runtime should be invoked exactly once")
        // Runtime saw the preprocessor's output (still 42 floats since the sample
        // is already wrist-relative and max-abs-normalized).
        assertEquals(42, runtime.lastInput?.size)
        assertEquals(OPEN_HAND_SAMPLE[0], runtime.lastInput!![0], "Wrist coord preserved through preprocessor")
    }

    @Test
    fun `close closes the underlying runtime`() {
        val runtime = FakeModelRuntime(output = floatArrayOf(1f))
        // Use a 1-class vocab to keep the fake output consistent
        val recognizer = ComposedSignRecognizer(
            runtime = runtime,
            preprocessor = SingleHandKazuhitoPreprocessor,
            postprocessor = ArgmaxConfidencePostprocessor,
            vocabulary = SignVocabulary(1, listOf(VocabSign(0, "X", emptyList()))),
            expectedInputElements = 42,
        )
        assertEquals(0, runtime.closed)
        recognizer.close()
        assertEquals(1, runtime.closed)
    }

    @Test
    fun `Open hand sample is a well-formed preprocessed vector`() {
        assertEquals(0f, OPEN_HAND_SAMPLE[0], "Wrist x must be 0")
        assertEquals(0f, OPEN_HAND_SAMPLE[1], "Wrist y must be 0")
        val absMax = OPEN_HAND_SAMPLE.maxOf { kotlin.math.abs(it) }
        assertTrue(kotlin.math.abs(absMax - 1f) < 1e-6f)
        assertNotEquals(OPEN_HAND_SAMPLE.toList(), FloatArray(42).toList())
    }

    private fun build(runtime: FakeModelRuntime): ComposedSignRecognizer {
        val vocab = SignVocabulary(
            version = 1,
            signs = List(runtime.outputSize()) { i -> VocabSign(i, "SIGN_$i", emptyList()) },
        )
        return ComposedSignRecognizer(
            runtime = runtime,
            preprocessor = SingleHandKazuhitoPreprocessor,
            postprocessor = ArgmaxConfidencePostprocessor,
            vocabulary = vocab,
            expectedInputElements = 42,
        )
    }

    private fun loadModelBytes(): ByteArray {
        val stream = javaClass.classLoader!!.getResourceAsStream("smoke_test.tflite")
            ?: error("smoke_test.tflite missing from test classpath resources")
        return stream.use { it.readBytes() }
    }

    private class FakeModelRuntime(private val output: FloatArray) : ModelRuntime {
        var calls = 0
            private set
        var closed = 0
            private set
        var lastInput: FloatArray? = null
            private set

        fun outputSize() = output.size

        override fun invoke(input: FloatArray): FloatArray {
            calls++
            lastInput = input.copyOf()
            return output.copyOf()
        }

        override fun close() { closed++ }
    }

    private companion object {
        val DUMMY_42_INPUT = FloatArray(42) { it * 0.01f }

        val OPEN_HAND_SAMPLE = floatArrayOf(
            0.0f, 0.0f,
            0.20078740157480315f, -0.051181102362204724f,
            0.3661417322834646f, -0.18110236220472442f,
            0.484251968503937f, -0.30708661417322836f,
            0.594488188976378f, -0.38188976377952755f,
            0.2637795275590551f, -0.46062992125984253f,
            0.3425196850393701f, -0.65748031496063f,
            0.3937007874015748f, -0.7834645669291339f,
            0.42913385826771655f, -0.9015748031496063f,
            0.14960629921259844f, -0.5f,
            0.1968503937007874f, -0.7244094488188977f,
            0.23228346456692914f, -0.8740157480314961f,
            0.2559055118110236f, -1.0f,
            0.03543307086614173f, -0.4881889763779528f,
            0.031496062992125984f, -0.7047244094488189f,
            0.03543307086614173f, -0.8503937007874016f,
            0.03937007874015748f, -0.9763779527559056f,
            -0.07480314960629922f, -0.42913385826771655f,
            -0.14566929133858267f, -0.5787401574803149f,
            -0.18503937007874016f, -0.6850393700787402f,
            -0.2204724409448819f, -0.7874015748031497f,
        )
    }
}
