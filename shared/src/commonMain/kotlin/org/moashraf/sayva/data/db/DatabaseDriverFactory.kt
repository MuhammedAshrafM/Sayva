package org.moashraf.sayva.data.db

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform factory for the [SqlDriver] backing [SayvaDatabase][org.moashraf.sayva.db.SayvaDatabase].
 *
 * Implementations:
 * - **Android:** `AndroidSqliteDriver` wrapping the framework SQLite via
 *   `AndroidSqliteDriver(schema, context, "sayva.db")`.
 * - **iOS:** `NativeSqliteDriver` from `co.touchlab:sqliter-driver`, opening the DB
 *   at `~/Documents/sayva.db` inside the app's sandbox.
 *
 * The Koin binding in `di/Modules.kt` calls [create] once at first resolve and
 * stores the driver as a singleton — SQLite handles are cheap but not
 * thread-hostile when reused.
 */
expect object DatabaseDriverFactory {
    fun create(): SqlDriver
}

/** Filename used across platforms so migrations and debugging tools have one name to remember. */
const val SAYVA_DATABASE_FILENAME = "sayva.db"
