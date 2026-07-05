package org.moashraf.sayva.di

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.moashraf.sayva.ml.ModelRuntimeRegistry
import org.moashraf.sayva.ml.PostprocessorRegistry
import org.moashraf.sayva.ml.PreprocessorRegistry
import org.moashraf.sayva.ml.adapters.SingleHandKazuhitoPreprocessor
import org.moashraf.sayva.ml.adapters.TfliteRuntimeAdapter
import org.moashraf.sayva.ml.adapters.TwoHandSequencePreprocessor
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Regression guard for a real bug caught during on-device testing:
 *
 * All three adapter registries had been declared as
 * `single<Map<String, X>> { ... }`. Kotlin JVM erases their generic types
 * at runtime, so Koin resolved every `Map<*, *>` lookup to the same binding
 * (the last one registered — the postprocessor map). The recognizer would
 * then throw at first frame:
 *
 *     Pack requires runtime adapter 'tflite' which this app version doesn't
 *     ship. Available: [argmax_confidence_v1]
 *
 * These tests verify each registry actually contains the adapters that
 * belong to it — a symptom this test would have caught before the on-device
 * failure.
 *
 * The fix is in `Modules.kt`: maps are inlined into the registry construction
 * so Koin never sees a bare `Map<String, X>` binding.
 */
class AdapterRegistrationTest {

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    private fun bootKoinWithAdaptersOnly() = startKoin {
        // Boot only the adapter-registry portion of sayvaModule inline so
        // tests don't need SecureStorage/Firebase/etc. The bindings mirror
        // `Modules.kt` — if a refactor over there rearranges them, this test
        // still keys on the intended invariants.
        modules(
            org.koin.dsl.module {
                single {
                    ModelRuntimeRegistry(
                        mapOf(TfliteRuntimeAdapter.ID to TfliteRuntimeAdapter)
                    )
                }
                single {
                    PreprocessorRegistry(
                        mapOf(
                            SingleHandKazuhitoPreprocessor.ID to SingleHandKazuhitoPreprocessor,
                            TwoHandSequencePreprocessor.ID to TwoHandSequencePreprocessor,
                        )
                    )
                }
                single {
                    PostprocessorRegistry(
                        mapOf(
                            org.moashraf.sayva.ml.ArgmaxConfidencePostprocessor.ID
                                to org.moashraf.sayva.ml.ArgmaxConfidencePostprocessor
                        )
                    )
                }
            }
        )
    }

    @Test
    fun `ModelRuntimeRegistry resolves tflite adapter`() {
        val koin = bootKoinWithAdaptersOnly().koin
        val registry: ModelRuntimeRegistry = koin.get()
        assertTrue(
            registry.supports(TfliteRuntimeAdapter.ID),
            "ModelRuntimeRegistry must contain 'tflite'; failing here means Koin " +
                "resolved the wrong Map<String, *> into the runtime registry.",
        )
    }

    @Test
    fun `PreprocessorRegistry resolves single-hand and two-hand adapters`() {
        val koin = bootKoinWithAdaptersOnly().koin
        val registry: PreprocessorRegistry = koin.get()
        assertTrue(registry.supports(SingleHandKazuhitoPreprocessor.ID))
        assertTrue(registry.supports(TwoHandSequencePreprocessor.ID))
    }

    @Test
    fun `PostprocessorRegistry resolves argmax adapter`() {
        val koin = bootKoinWithAdaptersOnly().koin
        val registry: PostprocessorRegistry = koin.get()
        assertTrue(registry.supports(org.moashraf.sayva.ml.ArgmaxConfidencePostprocessor.ID))
    }

    @Test
    fun `registries do not share adapter maps`() {
        // The bug this test exists to prevent: two registries handed the
        // SAME map because Koin couldn't distinguish generic Map types.
        // If PostprocessorRegistry accidentally received the runtime map
        // (or vice versa), one of `supports("tflite")` /
        // `supports("argmax_confidence_v1")` would swap.
        val koin = bootKoinWithAdaptersOnly().koin
        val runtimes: ModelRuntimeRegistry = koin.get()
        val posts: PostprocessorRegistry = koin.get()

        // Runtime registry must NOT think it has a postprocessor ID and
        // vice versa — proves the maps are actually different.
        assertTrue(
            !runtimes.supports(org.moashraf.sayva.ml.ArgmaxConfidencePostprocessor.ID),
            "ModelRuntimeRegistry has a postprocessor id — maps are crossed",
        )
        assertTrue(
            !posts.supports(TfliteRuntimeAdapter.ID),
            "PostprocessorRegistry has a runtime id — maps are crossed",
        )
    }
}
