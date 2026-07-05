package org.moashraf.sayva.auth

import org.moashraf.sayva.buildkonfig.BuildKonfig

/**
 * Which auth backend the running app uses.
 *
 * Selected at build time via `sayva.backend=firebase` or `sayva.backend=supabase`
 * in `local.properties`. Read at startup by [AppBackend.current] via BuildKonfig.
 *
 * The DI module binds `AuthGateway` to the corresponding adapter — swapping
 * backends is a `local.properties` edit + rebuild, no code changes.
 */
enum class BackendFlavor {
    FIREBASE,
    SUPABASE,
    ;

    companion object {
        /** Case-insensitive parse; unrecognized strings default to [FIREBASE]. */
        fun parseOrDefault(raw: String): BackendFlavor =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: FIREBASE
    }
}

/** Runtime accessor for the active [BackendFlavor], sourced from BuildKonfig. */
object AppBackend {
    val current: BackendFlavor = BackendFlavor.parseOrDefault(BuildKonfig.BACKEND)
}
