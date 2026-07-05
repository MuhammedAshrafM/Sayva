package org.moashraf.sayva.languagepack

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.moashraf.sayva.data.repository.CameraQuality
import org.moashraf.sayva.data.repository.ColorBlindMode
import org.moashraf.sayva.data.repository.DisplayMode
import org.moashraf.sayva.data.repository.SettingsRepository
import org.moashraf.sayva.data.repository.SettingsState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * State-machine tests for the top-level pack coordinator.
 *
 * Uses the bundled ASE pack loaded from the test classpath. Since the pack
 * loader is language-neutral, adding a second pack in a future test is just
 * a matter of copying files into the classpath resources — no code branch.
 */
class LanguagePackControllerTest {

    @Test
    fun `bootstrap resolves the bundled pack and default output`() = runBlocking<Unit> {
        val (controller, settings) = build()
        assertIs<LanguagePackController.State.Loading>(controller.state.value)
        controller.bootstrap()
        val ready = assertIs<LanguagePackController.State.Ready>(controller.state.value)
        assertEquals("ase", ready.currentPack.recognitionCode)
        assertEquals("en", ready.outputLanguage)
        assertTrue(ready.currentPack.supportedOutputs.containsAll(listOf("en", "ar")))
        // First-time bootstrap does not write anything back to settings unless the user changes something.
        assertNull(settings.state.value.recognitionLanguageCode)
        assertNull(settings.state.value.outputLanguageCode)
    }

    @Test
    fun `bootstrap honors persisted recognition and output codes`() = runBlocking<Unit> {
        val (controller, _) = build(
            initialSettings = SettingsState(
                recognitionLanguageCode = "ase",
                outputLanguageCode = "ar",
            ),
        )
        controller.bootstrap()
        val ready = assertIs<LanguagePackController.State.Ready>(controller.state.value)
        assertEquals("ar", ready.outputLanguage)
    }

    @Test
    fun `bootstrap falls back to pack default output when persisted output is unsupported`() = runBlocking<Unit> {
        val (controller, _) = build(
            initialSettings = SettingsState(outputLanguageCode = "fr"),
        )
        controller.bootstrap()
        val ready = assertIs<LanguagePackController.State.Ready>(controller.state.value)
        assertEquals("en", ready.outputLanguage)
    }

    @Test
    fun `switching to unknown recognition code returns false and leaves state untouched`() = runBlocking<Unit> {
        val (controller, _) = build()
        controller.bootstrap()
        val before = controller.state.value
        val result = controller.switchRecognition("xxx")
        assertFalse(result)
        assertEquals(before, controller.state.value)
    }

    @Test
    fun `set output language persists and re-emits state`() = runBlocking<Unit> {
        val (controller, settings) = build()
        controller.bootstrap()
        val ok = controller.setOutputLanguage("ar")
        assertTrue(ok)
        val ready = assertIs<LanguagePackController.State.Ready>(controller.state.value)
        assertEquals("ar", ready.outputLanguage)
        assertEquals("ar", settings.state.value.outputLanguageCode)
    }

    @Test
    fun `set output rejects codes the pack does not support`() = runBlocking<Unit> {
        val (controller, settings) = build()
        controller.bootstrap()
        val ok = controller.setOutputLanguage("fr")
        assertFalse(ok)
        // Nothing persisted, state unchanged.
        assertNull(settings.state.value.outputLanguageCode)
    }

    @Test
    fun `bootstrap on empty registry yields Error state`() = runBlocking<Unit> {
        val loader = object : PackResourceLoader {
            override suspend fun availablePackCodes(): List<String> = emptyList()
            override suspend fun readManifest(packCode: String): String = error("unreachable")
            override suspend fun readFile(packCode: String, relativePath: String): ByteArray = error("unreachable")
        }
        val registry = DefaultLanguagePackRegistry(loader)
        val settings = FakeSettingsRepository()
        val controller = LanguagePackController(registry, settings)
        controller.bootstrap()
        assertIs<LanguagePackController.State.Error>(controller.state.value)
    }

    private fun build(
        initialSettings: SettingsState = SettingsState(),
    ): Pair<LanguagePackController, FakeSettingsRepository> {
        val settings = FakeSettingsRepository(initialSettings)
        val registry = DefaultLanguagePackRegistry(
            ClasspathPackResourceLoader(listOf("ase")),
        )
        return LanguagePackController(registry, settings) to settings
    }
}

/**
 * Minimal in-memory [SettingsRepository] for tests. Only the fields the pack
 * controller touches are wired; everything else no-ops or returns defaults.
 */
private class FakeSettingsRepository(initial: SettingsState = SettingsState()) : SettingsRepository {
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
