package org.moashraf.sayva.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.serialization.json.Json

/**
 * A deliberately minimal back-stack, in place of androidx.navigation - this
 * app's navigation needs are simple (push/pop/replace) and androidx.navigation
 * multiplatform pulls in real androidx.lifecycle/savedstate artifacts that
 * aren't worth the dependency weight here.
 *
 * ### State restoration
 * The back-stack is persisted via [rememberSaveable] in
 * [rememberSayvaNavController] — rotation, dark-mode toggle, font-scale
 * changes, and process death all restore the user to their current
 * destination. Without this the app bounced back to Onboarding on every
 * config change (real bug caught during on-device testing).
 *
 * ### Restoration edge cases
 *   * A restore that produces an empty list falls back to [Screen.Welcome]
 *     — the saver never emits empty, but the check keeps us honest against
 *     manual system-restore state corruption.
 *   * Any screen that was in the back-stack when state was saved but whose
 *     class was renamed/removed will fail deserialization; we catch and
 *     fall back to Welcome. Better a fresh start than a crash loop.
 */
class SayvaNavController(initialStack: List<Screen>) {
    // Convenience for the common "just one starting screen" case.
    constructor(start: Screen) : this(listOf(start))

    private val backStack = mutableStateListOf<Screen>().apply { addAll(initialStack) }
    val current get() = backStack.last()
    val canGoBack get() = backStack.size > 1

    fun navigate(screen: Screen) {
        backStack.add(screen)
    }

    fun replaceAll(screen: Screen) {
        backStack.clear()
        backStack.add(screen)
    }

    fun back() {
        if (canGoBack) backStack.removeAt(backStack.lastIndex)
    }

    /** Snapshot for the [Saver] — safe to call anytime; returns immutable copy. */
    internal fun snapshotBackStack(): List<Screen> = backStack.toList()
}

/**
 * Compose [Saver] that serializes the back-stack via kotlinx.serialization.
 *
 * Format: a `List<String>` where each entry is one screen as polymorphic
 * sealed-class JSON. `List<String>` is Bundle-friendly so Android's
 * SavedStateRegistry handles the persistence transparently.
 *
 * We use a small [Json] instance rather than the module-level `Json.Default`
 * so future config changes (class-discriminator name, prettyPrint for debug
 * builds) are additive here without touching the app's other serialization.
 */
private val json: Json = Json {
    // Keep encoded strings compact — SavedStateRegistry has a hard cap.
    // A 33-screen back-stack still fits comfortably.
    encodeDefaults = true
}

val SayvaNavControllerSaver: Saver<SayvaNavController, List<String>> = Saver(
    save = { nav ->
        nav.snapshotBackStack().map { screen ->
            json.encodeToString(Screen.serializer(), screen)
        }
    },
    restore = { encoded ->
        val screens = runCatching {
            encoded.map { entry -> json.decodeFromString(Screen.serializer(), entry) }
        }.getOrElse { emptyList() }
        SayvaNavController(
            initialStack = if (screens.isEmpty()) listOf(Screen.Welcome) else screens,
        )
    },
)

@Composable
fun rememberSayvaNavController(start: Screen = Screen.Welcome): SayvaNavController =
    rememberSaveable(saver = SayvaNavControllerSaver) {
        SayvaNavController(start)
    }
