package org.moashraf.sayva.ml

import kotlinx.coroutines.runBlocking
import org.moashraf.sayva.languagepack.ClasspathPackResourceLoader
import org.moashraf.sayva.languagepack.DefaultLanguagePackRegistry
import org.moashraf.sayva.languagepack.OutputLanguageStatus
import org.moashraf.sayva.languagepack.SignVocabulary
import org.moashraf.sayva.ml.adapters.SingleHandKazuhitoPreprocessor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Track A/B end-to-end validation for a **pack-driven** recognizer.
 *
 * Walks the whole chain against the bundled ASE (American Sign Language)
 * pack from the pack registry — no compile-time vocabulary constants,
 * no per-shape recognizer classes.
 *
 * The Kotlin TFLite native lib isn't available on host JVM tests, so the
 * actual inference step here is exercised through a `FakeModelRuntime`.
 * Real invoke-path validation happens at APK-assemble time (JNI links)
 * and on-device.
 */
class FullChainTest {

    @Test
    fun `bundled ASE pack loads with fingerspelling + temporal models`() = runBlocking<Unit> {
        val pack = loadAsePack()

        assertEquals("ase", pack.recognitionCode)
        assertEquals(2, pack.models.size)

        val fs = pack.modelById("fingerspelling")
        assertNotNull(fs, "ASE pack must expose a fingerspelling model")
        assertEquals(24, fs.vocabulary.size, "fingerspelling vocab must be 24 letters (skip J, Z)")
        assertTrue("A" in fs.vocabulary.signs.map { it.id })
        assertTrue("Y" in fs.vocabulary.signs.map { it.id })
        assertTrue("J" !in fs.vocabulary.signs.map { it.id })
    }

    @Test
    fun `ASE pack ships both canonical output labels (en complete, ar stub)`() = runBlocking<Unit> {
        val pack = loadAsePack()
        assertTrue("en" in pack.supportedOutputs)
        assertTrue("ar" in pack.supportedOutputs)
        assertEquals(OutputLanguageStatus.Complete, pack.outputLanguageStatus["en"])
        assertEquals(OutputLanguageStatus.Stub, pack.outputLanguageStatus["ar"])
    }

    @Test
    fun `ASE fingerspelling model declares the expected adapter IDs`() = runBlocking<Unit> {
        val pack = loadAsePack()
        val fs = pack.modelById("fingerspelling")!!
        assertEquals("mlp", fs.architecture)
        assertEquals("tflite", fs.runtimeType)
        assertEquals("single_frame", fs.inferenceStrategy)
        assertEquals("single_hand_kazuhito_v1", fs.input.preprocessing)
        assertEquals("argmax_confidence_v1", fs.output.postprocessing)
    }

    @Test
    fun `ASE temporal model declares different adapters — proves manifest drives shape`() = runBlocking<Unit> {
        val pack = loadAsePack()
        val tp = pack.modelById("temporal_v1")!!
        assertEquals("lstm_unrolled", tp.architecture)
        assertEquals("two_hand_sequence_v1", tp.input.preprocessing)
        // Postprocessor is the same across both — that's fine; adapter IDs
        // are independent axes and a pack can reuse a shared postprocessor.
        assertEquals("argmax_confidence_v1", tp.output.postprocessing)
    }

    @Test
    fun `ComposedSignRecognizer routes preprocess then runtime then postprocess`() = runBlocking<Unit> {
        val pack = loadAsePack()
        val fs = pack.modelById("fingerspelling")!!

        // 24-class output pretending "B" is the top class.
        val fakeRuntime = FakeModelRuntime(
            outputSize = fs.vocabulary.size,
            dominantIndex = 1,
            dominantProb = 0.87f,
        )
        val recognizer = ComposedSignRecognizer(
            runtime = fakeRuntime,
            preprocessor = SingleHandKazuhitoPreprocessor,
            postprocessor = ArgmaxConfidencePostprocessor,
            vocabulary = fs.vocabulary,
            expectedInputElements = fs.expectedInputElements,
        )

        val raw = FloatArray(42) { i -> if (i % 2 == 0) 300f + i else 200f + i }
        val result = recognizer.recognize(raw)

        // Recognizer saw preprocessed input, not raw.
        assertEquals(42, fakeRuntime.lastInput?.size)
        assertEquals(1, fakeRuntime.calls)
        // Preprocessed input's wrist (index 0/1) is (0, 0).
        assertEquals(0f, fakeRuntime.lastInput!![0])
        assertEquals(0f, fakeRuntime.lastInput!![1])

        val sign = fs.vocabulary.byIndex(result.classIndex)
        assertNotNull(sign)
        assertEquals("B", sign.id)
        assertEquals(0.87f, result.confidence)
    }

    private suspend fun loadAsePack() =
        DefaultLanguagePackRegistry(ClasspathPackResourceLoader(listOf("ase")))
            .also { it.refresh() }
            .byRecognitionCode("ase")
            ?: error("ASE pack not found on classpath")

    private class FakeModelRuntime(
        private val outputSize: Int,
        private val dominantIndex: Int,
        private val dominantProb: Float,
    ) : ModelRuntime {
        var calls = 0
            private set
        var lastInput: FloatArray? = null
            private set

        override fun invoke(input: FloatArray): FloatArray {
            calls++
            lastInput = input.copyOf()
            val other = (1f - dominantProb) / (outputSize - 1)
            return FloatArray(outputSize) { if (it == dominantIndex) dominantProb else other }
        }
        override fun close() {}
    }

    @Suppress("unused") private val _svUsed: SignVocabulary? = null
}
