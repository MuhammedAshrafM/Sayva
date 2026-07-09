package org.moashraf.sayva.diagnostics

/**
 * Write a diagnostic sample JSON to app-external storage so it can be
 * pulled with `adb pull` (Android) or via Files.app (iOS) for
 * comparison with Python's golden inference tooling.
 *
 * The JSON shape mirrors `ml/tests/fixtures/golden_inference.json` — an
 * on-device sample dropped here can be piped into
 * `test_golden_inference.py` with minimal massaging.
 */
expect object DiagnosticSampleWriter {
    /**
     * Write [json] to a file named [fileName] under the app's shareable
     * sample directory. Returns the absolute path so callers can surface
     * it to the user ("Saved to /sdcard/…").
     */
    fun write(fileName: String, json: String): String
}
