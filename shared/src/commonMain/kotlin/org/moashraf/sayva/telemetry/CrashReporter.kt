package org.moashraf.sayva.telemetry

/**
 * Provider-agnostic crash reporting port.
 *
 * Implementation today: `data/firebase/FirebaseCrashReporter.kt`.
 * If you later leave the Google ecosystem, add `data/sentry/SentryCrashReporter.kt`
 * — Supabase does not offer first-party crash reporting.
 *
 * ### Design contract
 * - No `throw`s — calls are fire-and-forget. Reporting failure must never cascade.
 * - `Throwable` is a Kotlin stdlib type, portable across vendors.
 * - User-facing "did you crash?" UX lives in `CrashReportScreen`, which uses this
 *   interface to submit the actual report.
 */
interface CrashReporter {

    /**
     * Report a caught exception as a non-fatal crash.
     * Use when catching-and-recovering from an unexpected error — the app doesn't
     * die, but you want visibility into how often the recovery path fires.
     */
    fun recordException(throwable: Throwable)

    /**
     * Attach a breadcrumb message to the next crash report.
     * Providers typically buffer the last N of these (Crashlytics: 64 KB;
     * Sentry: 100 breadcrumbs). Use for narrative context: `"user tapped translate"`.
     */
    fun log(message: String)

    /**
     * Attach a persistent key/value to all subsequent reports.
     * Example: `setKey("current_screen", "live_camera")`.
     */
    fun setKey(key: String, value: String)

    /**
     * Bind subsequent reports to a specific user. Pass `null` on sign-out.
     * Same ID as [AnalyticsGateway.setUserId] and [org.moashraf.sayva.auth.User.id].
     */
    fun setUserId(id: String?)
}
