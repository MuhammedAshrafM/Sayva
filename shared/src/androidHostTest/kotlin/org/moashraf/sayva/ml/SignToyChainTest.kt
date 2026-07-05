package org.moashraf.sayva.ml

import kotlinx.coroutines.runBlocking
import org.moashraf.sayva.languagepack.ClasspathPackResourceLoader
import org.moashraf.sayva.languagepack.DefaultLanguagePackRegistry
import org.moashraf.sayva.ml.adapters.TwoHandSequencePreprocessor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Track C end-to-end check driven by the bundled ASE pack's `temporal_v1`
 * model — now assembled through the ports & adapters instead of a
 * dedicated wrapper class.
 */
class SignToyChainTest {

    @Test
    fun `temporal model in ASE pack is 5 signs in the plan-locked order`() = runBlocking<Unit> {
        val pack = loadAsePack()
        val model = pack.modelById("temporal_v1")
        assertNotNull(model)
        val ids = model.vocabulary.signs.map { it.id }
        assertEquals(listOf("HELLO", "THANK_YOU", "PLEASE", "SORRY", "YES"), ids)
    }

    @Test
    fun `preprocessing produces T times 84 floats for a sequence`() {
        val flat = LandmarkPreprocessor.preprocessTwoHandSequence(
            frames = List(10) {
                FloatArray(42) { i -> (i + it).toFloat() * 3f } to
                    FloatArray(42) { i -> (i + it).toFloat() * 5f }
            },
            targetLength = 30,
        )
        assertEquals(30 * 84, flat.size)
    }

    @Test
    fun `ComposedSignRecognizer routes 3D sequence input through runtime and vocab`() = runBlocking<Unit> {
        val pack = loadAsePack()
        val model = pack.modelById("temporal_v1")!!

        val runtime = FakeSequenceRuntime(
            expectedElements = model.expectedInputElements,
            output = floatArrayOf(0.05f, 0.6f, 0.1f, 0.15f, 0.1f),
        )
        val recognizer = ComposedSignRecognizer(
            runtime = runtime,
            preprocessor = TwoHandSequencePreprocessor,
            postprocessor = ArgmaxConfidencePostprocessor,
            vocabulary = model.vocabulary,
            expectedInputElements = model.expectedInputElements,
        )
        val flat = FloatArray(30 * 84) { i -> i.toFloat() * 0.001f }
        val result = recognizer.recognize(flat)
        assertEquals(1, result.classIndex)
        assertEquals(0.6f, result.confidence)
        val sign = model.vocabulary.byIndex(result.classIndex)
        assertNotNull(sign)
        assertEquals("THANK_YOU", sign.id)
        assertTrue(sign.tags.contains("gratitude"))
    }

    private suspend fun loadAsePack() =
        DefaultLanguagePackRegistry(ClasspathPackResourceLoader(listOf("ase")))
            .also { it.refresh() }
            .byRecognitionCode("ase")
            ?: error("ASE pack not found on classpath")

    private class FakeSequenceRuntime(
        private val expectedElements: Int,
        private val output: FloatArray,
    ) : ModelRuntime {
        override fun invoke(input: FloatArray): FloatArray {
            require(input.size == expectedElements) {
                "FakeSequenceRuntime expected $expectedElements floats, got ${input.size}"
            }
            return output.copyOf()
        }
        override fun close() {}
    }
}
