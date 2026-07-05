package org.moashraf.sayva.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import org.moashraf.sayva.db.SayvaDatabase

/**
 * iOS SQLDelight driver via SQLiter's [NativeSqliteDriver]. The database file lives
 * under the app sandbox's Documents dir, matching typical iOS app-data expectations
 * (iCloud backup opt-in works, external processes cannot access it).
 */
actual object DatabaseDriverFactory {
    actual fun create(): SqlDriver = NativeSqliteDriver(
        schema = SayvaDatabase.Schema,
        name = SAYVA_DATABASE_FILENAME,
    )
}
