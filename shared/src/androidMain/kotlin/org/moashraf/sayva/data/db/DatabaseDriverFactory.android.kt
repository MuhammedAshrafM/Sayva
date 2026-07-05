package org.moashraf.sayva.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.moashraf.sayva.bootstrap.AndroidAppContext
import org.moashraf.sayva.db.SayvaDatabase

/**
 * Android SQLDelight driver. Uses the framework SQLite via [AndroidSqliteDriver],
 * with the Application context resolved from [AndroidAppContext].
 *
 * The database file lives in the app's private data dir at
 * `/data/data/org.moashraf.sayva/databases/sayva.db` — no external storage,
 * no WORLD_READABLE fallout.
 */
actual object DatabaseDriverFactory {
    actual fun create(): SqlDriver = AndroidSqliteDriver(
        schema = SayvaDatabase.Schema,
        context = AndroidAppContext.require(),
        name = SAYVA_DATABASE_FILENAME,
    )
}
