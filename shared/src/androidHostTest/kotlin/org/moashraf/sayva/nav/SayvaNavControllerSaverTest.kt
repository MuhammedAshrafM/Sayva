package org.moashraf.sayva.nav

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Regression guard for a real bug caught during on-device testing:
 *
 * Rotating the device rebuilt `MainActivity`, which discarded the
 * `remember { SayvaNavController(...) }` inside `rememberSayvaNavController`.
 * The user landed back on `Welcome` after every configuration change.
 *
 * The fix uses `rememberSaveable` with [SayvaNavControllerSaver] — this
 * test exercises the saver directly so any regression fails at CI time
 * with a specific message instead of surfacing on device.
 */
class SayvaNavControllerSaverTest {

    @Test
    fun `saver round-trips a single-screen back-stack`() {
        val nav = SayvaNavController(Screen.Welcome)
        val saved = SayvaNavControllerSaver.saveToString(nav)
        val restored = SayvaNavControllerSaver.restoreFromStrings(saved)

        assertEquals(Screen.Welcome, restored.current)
        assertTrue(!restored.canGoBack)
    }

    @Test
    fun `saver round-trips a deep back-stack`() {
        val nav = SayvaNavController(Screen.Welcome)
        nav.navigate(Screen.Home)
        nav.navigate(Screen.LiveCamera)
        nav.navigate(Screen.Settings)

        val saved = SayvaNavControllerSaver.saveToString(nav)
        val restored = SayvaNavControllerSaver.restoreFromStrings(saved)

        assertEquals(Screen.Settings, restored.current)
        assertTrue(restored.canGoBack)
        restored.back()
        assertEquals(Screen.LiveCamera, restored.current)
        restored.back()
        assertEquals(Screen.Home, restored.current)
        restored.back()
        assertEquals(Screen.Welcome, restored.current)
    }

    @Test
    fun `saver preserves parameterised screens`() {
        val nav = SayvaNavController(Screen.Home)
        nav.navigate(Screen.History)
        nav.navigate(Screen.HistoryDetail(entryId = "hx-42"))

        val saved = SayvaNavControllerSaver.saveToString(nav)
        val restored = SayvaNavControllerSaver.restoreFromStrings(saved)

        val top = restored.current
        assertIs<Screen.HistoryDetail>(top)
        assertEquals("hx-42", top.entryId)
    }

    @Test
    fun `restore of empty list falls back to Welcome`() {
        val restored = SayvaNavControllerSaver.restoreFromStrings(emptyList())
        assertEquals(Screen.Welcome, restored.current)
    }

    @Test
    fun `restore of malformed JSON falls back to Welcome`() {
        val restored = SayvaNavControllerSaver.restoreFromStrings(
            listOf("this is not valid JSON at all")
        )
        assertEquals(Screen.Welcome, restored.current)
    }

    // -----------------------------------------------------------------------
    // Compose's `Saver` interface takes `SaverScope` on the save side. We
    // don't have access to a real `SaverScope` in a plain JVM test, so we
    // invoke the save lambda via a null-scope trick: the current saver
    // implementation doesn't use `SaverScope.canBeSaved`, so passing `null`
    // works. If a future saver DOES need canBeSaved, this helper wraps the
    // failure into a clear message here.
    // -----------------------------------------------------------------------

    @Suppress("UNCHECKED_CAST")
    private fun androidx.compose.runtime.saveable.Saver<SayvaNavController, List<String>>.saveToString(
        nav: SayvaNavController,
    ): List<String> {
        val scope = FakeSaverScope
        return with(scope) { save(nav) } as List<String>
    }

    private fun androidx.compose.runtime.saveable.Saver<SayvaNavController, List<String>>.restoreFromStrings(
        value: List<String>,
    ): SayvaNavController = restore(value)
        ?: error("Saver returned null on restore — did the restore lambda's return type change?")
}

/** Minimal SaverScope for tests. Screens are always saveable — they're
 *  `@Serializable` and encode to plain strings. */
private object FakeSaverScope : androidx.compose.runtime.saveable.SaverScope {
    override fun canBeSaved(value: Any): Boolean = true
}
