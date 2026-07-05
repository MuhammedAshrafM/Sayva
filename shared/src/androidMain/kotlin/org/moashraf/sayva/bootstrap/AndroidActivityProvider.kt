package org.moashraf.sayva.bootstrap

import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference

/**
 * Holds a weak reference to the currently-resumed [FragmentActivity].
 *
 * Populated by `MainActivity` in its onResume/onPause callbacks so any consumer
 * that needs an Activity (biometric prompt, permission launcher, share sheet)
 * can retrieve it on demand without leaking references across configuration
 * changes or process death.
 *
 * ### Why weak reference
 * If we held a strong reference, killing/recreating MainActivity would leak the
 * old instance. A weak ref lets the GC clean up when the Activity is truly gone.
 *
 * ### Why FragmentActivity specifically
 * `androidx.biometric.BiometricPrompt` requires FragmentActivity (not just
 * ComponentActivity). MainActivity is upgraded to extend FragmentActivity to
 * satisfy this — it's a lightweight change; FragmentActivity extends
 * ComponentActivity so all existing behavior is preserved.
 *
 * ### Null return semantics
 * Consumers should treat `current()` returning null as "not currently in
 * foreground" — usually a signal to defer/queue the action until the Activity
 * comes back, or to abort with a UI error.
 */
object AndroidActivityProvider {
    private var activityRef: WeakReference<FragmentActivity>? = null

    fun setCurrent(activity: FragmentActivity) {
        activityRef = WeakReference(activity)
    }

    fun clearCurrent(activity: FragmentActivity) {
        // Only clear if the current ref is still this activity — avoids races
        // where onResume of a new Activity fires before onPause of the old one.
        if (activityRef?.get() === activity) {
            activityRef = null
        }
    }

    fun current(): FragmentActivity? = activityRef?.get()

    fun require(): FragmentActivity = checkNotNull(current()) {
        "No FragmentActivity is currently resumed. Ensure MainActivity registers " +
            "itself in onResume/onPause via AndroidActivityProvider, and don't call " +
            "this from background contexts."
    }
}
