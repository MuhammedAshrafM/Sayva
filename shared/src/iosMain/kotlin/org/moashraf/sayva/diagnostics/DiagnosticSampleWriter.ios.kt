@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.moashraf.sayva.diagnostics

import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.writeToURL

actual object DiagnosticSampleWriter {
    actual fun write(fileName: String, json: String): String {
        val fm = NSFileManager.defaultManager
        val docs = fm.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).firstOrNull() as? NSURL
            ?: error("iOS document directory unavailable")
        val samples = docs.URLByAppendingPathComponent("diagnostic_samples", true)
            ?: error("Failed to derive samples directory")
        fm.createDirectoryAtURL(samples, true, null, null)
        val outUrl = samples.URLByAppendingPathComponent(fileName)
            ?: error("Failed to derive sample file path")
        (json as NSString).writeToURL(outUrl, true, NSUTF8StringEncoding, null)
        return outUrl.absoluteString ?: fileName
    }
}
