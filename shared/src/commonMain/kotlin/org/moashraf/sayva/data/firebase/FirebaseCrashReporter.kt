package org.moashraf.sayva.data.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.crashlytics.crashlytics
import org.moashraf.sayva.telemetry.CrashReporter

/**
 * Crashlytics implementation of [CrashReporter]. Only file that imports
 * `dev.gitlive.firebase.crashlytics.*`.
 *
 * Like [FirebaseAnalyticsGateway], calls are fire-and-forget. Crashlytics buffers
 * reports locally when offline and uploads them on next launch — we don't need
 * to handle transport failures at this layer.
 */
class FirebaseCrashReporter : CrashReporter {

    private val crashlytics = Firebase.crashlytics

    override fun recordException(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }

    override fun log(message: String) {
        crashlytics.log(message)
    }

    override fun setKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }

    override fun setUserId(id: String?) {
        crashlytics.setUserId(id ?: "")
    }
}
