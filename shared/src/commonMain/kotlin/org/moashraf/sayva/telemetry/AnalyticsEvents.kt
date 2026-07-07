package org.moashraf.sayva.telemetry

/**
 * Central catalog of every analytics event name and parameter key the app emits.
 *
 * ### Why constants instead of ad-hoc strings
 * - Typos become compile errors, not silent metric loss.
 * - The full event vocabulary is discoverable in one place — no `grep` archaeology.
 * - Renaming an event is a safe refactor.
 * - Docs can be attached to each name explaining when it fires.
 *
 * ### Naming rules
 * - snake_case, verb-past-tense: `sign_in_succeeded`, not `signInSuccess`.
 * - No PII in names or values — see [AnalyticsGateway] privacy rules.
 * - Group related events by a shared prefix (`auth_`, `learn_`, `conversation_`).
 */
object AnalyticsEvents {

    // ---- Auth ---------------------------------------------------------------

    const val AUTH_SIGN_IN_ATTEMPTED = "auth_sign_in_attempted"
    const val AUTH_SIGN_IN_SUCCEEDED = "auth_sign_in_succeeded"
    const val AUTH_SIGN_IN_FAILED = "auth_sign_in_failed"
    const val AUTH_REGISTER_ATTEMPTED = "auth_register_attempted"
    const val AUTH_REGISTER_SUCCEEDED = "auth_register_succeeded"
    const val AUTH_REGISTER_FAILED = "auth_register_failed"
    const val AUTH_GUEST_STARTED = "auth_guest_started"
    const val AUTH_PASSWORD_RESET_REQUESTED = "auth_password_reset_requested"
    const val AUTH_SIGN_OUT = "auth_sign_out"

    // ---- Learn --------------------------------------------------------------

    const val LEARN_PRACTICE_SESSION_COMPLETED = "learn_practice_session_completed"

    // ---- Memory / History / Favorites --------------------------------------

    const val HISTORY_FILTER_CHANGED = "history_filter_changed"
    const val FAVORITE_FILTER_CHANGED = "favorite_filter_changed"

    // ---- Conversation ------------------------------------------------------

    const val CONVERSATION_SAVED = "conversation_saved"
    const val CONVERSATION_FAVORITE_TOGGLED = "conversation_favorite_toggled"

    // ---- Settings ----------------------------------------------------------

    const val SETTING_CHANGED = "setting_changed"

    // ---- Permissions -------------------------------------------------------

    const val PERMISSION_RESULT = "permission_result"

    // ---- Navigation --------------------------------------------------------

    const val SCREEN_VIEWED = "screen_viewed"

    // ---- Recognition -------------------------------------------------------

    /** User opened the LiveCamera and the pipeline started. */
    const val RECOGNITION_STARTED = "recognition_started"

    /** User switched between recognition modes (fingerspelling / sign_recognition / …). */
    const val RECOGNITION_MODE_CHANGED = "recognition_mode_changed"

    /** User paused live recognition — camera stays bound; frame processing suspended. */
    const val RECOGNITION_PAUSED = "recognition_paused"

    /** User resumed from a paused live recognition session. */
    const val RECOGNITION_RESUMED = "recognition_resumed"

    /** User tapped the torch button. `enabled` boolean records the resulting state. */
    const val CAMERA_TORCH_TOGGLED = "camera_torch_toggled"

    /** User swapped between front and back cameras. */
    const val CAMERA_LENS_SWITCHED = "camera_lens_switched"

    /** User tapped Copy on the translation card. */
    const val RECOGNITION_LABEL_COPIED = "recognition_label_copied"

    /** User tapped the star to add / remove a live-recognition favorite. */
    const val RECOGNITION_FAVORITE_TOGGLED = "recognition_favorite_toggled"

    // ---- Crash reporting ---------------------------------------------------

    /** User tapped "Send report" on the CrashReportScreen. */
    const val CRASH_REPORT_SUBMITTED = "crash_report_submitted"

    /** User tapped "Not now" — dismissed the crash prompt without reporting. */
    const val CRASH_REPORT_DISMISSED = "crash_report_dismissed"

    // ---- Parameter keys ----------------------------------------------------

    object Param {
        /** Auth backend picked at build time. Values: `firebase`, `supabase`. */
        const val BACKEND = "backend"

        /** Typed error variant name for `auth_*_failed`. */
        const val ERROR = "error"

        /** Duration of the operation in milliseconds. Long. */
        const val DURATION_MS = "duration_ms"

        /** For learn events. */
        const val XP_EARNED = "xp_earned"
        const val SIGNS_LEARNED = "signs_learned"
        const val LESSON_ID = "lesson_id"

        /** For history / favorites filter events. Snake-case filter name. */
        const val FILTER = "filter"

        /** For conversation events. */
        const val MESSAGE_COUNT = "message_count"
        const val CONVERSATION_ID = "conversation_id"
        const val CATEGORY = "category"

        /** For settings events. */
        const val KEY = "key"
        const val VALUE = "value"

        /** For permission events. Values: camera / microphone / notifications. */
        const val PERMISSION = "permission"
        const val GRANTED = "granted"

        /** For screen_viewed. */
        const val SCREEN_NAME = "screen_name"

        /** For recognition events. RecognitionRole ID. */
        const val MODE = "mode"

        /** Language pack recognition code, e.g. `"ase"`. */
        const val PACK_CODE = "pack_code"

        /** For torch / favorite / lens events — the resulting boolean state. */
        const val ENABLED = "enabled"

        /** For lens_switched — "front" or "back". */
        const val LENS = "lens"

        /** For favorite toggle — sign id within the active pack, e.g. "A" or "hello". */
        const val SIGN_ID = "sign_id"

        /** For crash_report_submitted. Booleans mirror the on-screen toggles. */
        const val INCLUDES_LOGS = "includes_logs"
        const val INCLUDES_DEVICE_INFO = "includes_device_info"
        const val INCLUDES_SCREENSHOT = "includes_screenshot"
        const val HAS_USER_DESCRIPTION = "has_user_description"
    }
}
