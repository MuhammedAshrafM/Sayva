package org.moashraf.sayva.languagepack

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests the fallback semantics of [DefaultTranslationRenderer] against the
 * bundled ASE pack — which ships English at `complete` and Arabic at `stub`
 * (all null entries). Same code path handles a future partial-status pack.
 */
class TranslationRendererTest {

    private val renderer = DefaultTranslationRenderer()

    @Test
    fun `complete label renders the exact string with no fallback flag`() = runBlocking<Unit> {
        val pack = loadAsePack()
        val sign = pack.modelById("fingerspelling")!!.vocabulary.byId("A")!!
        val result = renderer.render(pack, "en", "fingerspelling", sign)
        assertEquals("A", result.label)
        assertEquals("en", result.effectiveOutputCode)
        assertEquals(LabelResult.FallbackReason.None, result.fallback)
    }

    @Test
    fun `null Arabic label renders gloss with MissingSign fallback`() = runBlocking<Unit> {
        val pack = loadAsePack()
        val sign = pack.modelById("temporal_v1")!!.vocabulary.byId("HELLO")!!
        val result = renderer.render(pack, "ar", "temporal_v1", sign)
        assertEquals("HELLO", result.label)
        assertEquals("ar", result.effectiveOutputCode)
        assertEquals(LabelResult.FallbackReason.MissingSign, result.fallback)
    }

    @Test
    fun `unsupported output code falls back to pack default with reason`() = runBlocking<Unit> {
        val pack = loadAsePack()
        val sign = pack.modelById("temporal_v1")!!.vocabulary.byId("HELLO")!!
        val result = renderer.render(pack, "fr", "temporal_v1", sign)
        assertEquals("Hello", result.label, "Should fall back to English label")
        assertEquals("en", result.effectiveOutputCode)
        assertEquals(LabelResult.FallbackReason.MissingOutputLanguage, result.fallback)
    }

    @Test
    fun `unknown model id in supported output falls back to gloss`() = runBlocking<Unit> {
        val pack = loadAsePack()
        val sign = pack.modelById("fingerspelling")!!.vocabulary.byId("A")!!
        val result = renderer.render(pack, "en", "does_not_exist", sign)
        assertEquals("A", result.label)
    }

    private suspend fun loadAsePack(): LanguagePack {
        val registry = DefaultLanguagePackRegistry(ClasspathPackResourceLoader(listOf("ase")))
        registry.refresh()
        return registry.byRecognitionCode("ase")!!
    }
}
