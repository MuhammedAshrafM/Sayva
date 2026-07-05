package org.moashraf.sayva.bootstrap

import android.content.Context

/**
 * Single Android [Context] holder consumed by every platform-specific factory that
 * needs one (secure storage, settings, SQLDelight driver, permission controller, etc.).
 *
 * `MainActivity.onCreate` calls [init] once with the application context BEFORE
 * `setContent { App() }` runs — that ordering is critical because Koin resolves
 * platform bindings during composition, and any missing context surfaces here as
 * a fail-fast [IllegalStateException] rather than as a confusing NPE further down.
 *
 * Using `applicationContext` (not `this` / the Activity) is deliberate — long-lived
 * services must not leak an Activity reference. Everything downstream that only
 * needs a Context should be fine; anything needing the Activity itself (biometric
 * prompt, permission launcher) needs a different mechanism.
 */
object AndroidAppContext {
    private var applicationContext: Context? = null

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    fun require(): Context = checkNotNull(applicationContext) {
        "AndroidAppContext.init(applicationContext) must be called from " +
            "MainActivity.onCreate before setContent { App() }. Without it, any Koin " +
            "binding that resolves a platform-specific factory (SecureStorage, " +
            "SettingsStorage, DatabaseDriverFactory, etc.) will fail here."
    }
}
