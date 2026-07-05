package org.moashraf.sayva.telemetry

/**
 * Provider-agnostic analytics port.
 *
 * Implementation today: `data/firebase/FirebaseAnalyticsGateway.kt`.
 * If you later leave the Google ecosystem, add `data/posthog/PostHogAnalyticsGateway.kt`
 * (or Amplitude / Plausible / etc.) using the same pattern — Supabase does not offer
 * first-party analytics.
 *
 * ### Design contract
 * - Event names and parameter keys are plain strings — no provider-specific enums.
 * - No vendor SDK types on any signature.
 * - Calls are fire-and-forget; the interface returns `Unit`, not `Result`. If analytics
 *   silently fails, the app continues. Telemetry outages must never break the product.
 *
 * ### Privacy rules (enforced by convention, not code)
 * - **Never** log recognized signs, translated text, or conversation content. That's
 *   private communication and must never leave the device (see docs/plan Security section).
 * - User IDs are stable opaque identifiers, not emails or display names.
 * - Custom user properties should not contain PII.
 */
interface AnalyticsGateway {

    /**
     * Log a discrete user action or system event.
     * @param name  snake_case, e.g. `sign_in_success`, `lesson_completed`. Keep the
     *              vocabulary small — a docs/analytics-events.md registry is worth
     *              maintaining once the app has more than 20 events.
     * @param params free-form dimensions. Values should be primitives (String, Int,
     *               Long, Double, Boolean). Nested maps are not portable across providers.
     */
    fun logEvent(name: String, params: Map<String, Any> = emptyMap())

    /**
     * Bind subsequent events to a specific user. Pass `null` on sign-out.
     * The ID here is the same [org.moashraf.sayva.auth.User.id] from the auth layer.
     */
    fun setUserId(id: String?)

    /**
     * Set a persistent user property (e.g. `plan_tier=free`, `preferred_language=asl`).
     * Passing `null` removes the property.
     */
    fun setUserProperty(key: String, value: String?)

    /**
     * Log a virtual screen view. Called from `SayvaNavController` on every navigation.
     * @param screenName snake_case screen identifier, e.g. `live_camera`, `lesson_player`.
     */
    fun logScreenView(screenName: String)
}
