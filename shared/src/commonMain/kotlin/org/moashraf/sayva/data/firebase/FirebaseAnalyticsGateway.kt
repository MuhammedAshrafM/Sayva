package org.moashraf.sayva.data.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import dev.gitlive.firebase.analytics.logEvent
import org.moashraf.sayva.telemetry.AnalyticsGateway

/**
 * Firebase Analytics implementation of [AnalyticsGateway]. Only file that imports
 * `dev.gitlive.firebase.analytics.*`.
 *
 * All methods are fire-and-forget — Firebase's Analytics SDK swallows its own errors
 * and buffers events for retry, so we don't wrap in try/catch. If analytics is
 * broken, the app keeps working (per interface contract).
 */
class FirebaseAnalyticsGateway : AnalyticsGateway {

    private val analytics = Firebase.analytics

    override fun logEvent(name: String, params: Map<String, Any>) {
        if (params.isEmpty()) {
            analytics.logEvent(name)
        } else {
            analytics.logEvent(name) {
                params.forEach { (key, value) -> param(key, value.toString()) }
            }
        }
    }

    override fun setUserId(id: String?) {
        analytics.setUserId(id)
    }

    override fun setUserProperty(key: String, value: String?) {
        analytics.setUserProperty(key, value ?: "")
    }

    override fun logScreenView(screenName: String) {
        analytics.logEvent("screen_view") {
            param("screen_name", screenName)
        }
    }
}
