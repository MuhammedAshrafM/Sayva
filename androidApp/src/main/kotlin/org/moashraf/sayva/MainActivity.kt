package org.moashraf.sayva

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
import org.moashraf.sayva.bootstrap.AndroidActivityProvider
import org.moashraf.sayva.bootstrap.AndroidAppContext

/**
 * Single-activity host for the Compose app.
 *
 * Extends [FragmentActivity] (not just [androidx.activity.ComponentActivity]) so
 * `androidx.biometric.BiometricPrompt` can attach. FragmentActivity extends
 * ComponentActivity so all Activity-result / Compose plumbing continues to work.
 *
 * Lifecycle wiring:
 * - onCreate → bootstrap [AndroidAppContext] with the application context (long-lived)
 * - onResume / onPause → register the Activity with [AndroidActivityProvider] (weak ref)
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Bootstrap platform-specific singletons that need an Android Context BEFORE
        // Koin resolves anything that depends on them. Order matters: this must
        // precede setContent { App() } because App() installs KoinApplication.
        AndroidAppContext.init(applicationContext)

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }

    override fun onResume() {
        super.onResume()
        AndroidActivityProvider.setCurrent(this)
    }

    override fun onPause() {
        AndroidActivityProvider.clearCurrent(this)
        super.onPause()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}