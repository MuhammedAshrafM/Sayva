package org.moashraf.sayva.diagnostics

import org.moashraf.sayva.bootstrap.AndroidAppContext
import java.io.File

actual object DiagnosticSampleWriter {
    actual fun write(fileName: String, json: String): String {
        val context = AndroidAppContext.require()
        // App-external files dir — visible via `adb pull` and File apps
        // without requesting extra permissions. Cleared when the user
        // uninstalls the app.
        val root = context.getExternalFilesDir(null)
            ?: context.filesDir
        val samples = File(root, "diagnostic_samples").apply { mkdirs() }
        val outFile = File(samples, fileName)
        outFile.writeText(json)
        return outFile.absolutePath
    }
}
