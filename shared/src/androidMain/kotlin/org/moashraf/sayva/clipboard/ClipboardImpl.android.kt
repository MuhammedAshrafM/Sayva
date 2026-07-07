package org.moashraf.sayva.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import org.moashraf.sayva.bootstrap.AndroidAppContext

/**
 * Android [Clipboard] — thin wrapper around [ClipboardManager].
 *
 * ### Empty payloads
 * Silently skips empty strings; the platform's clipboard would happily
 * store an empty ClipData but that's usually a UI bug (user tapped Copy
 * with no recognition in progress). Guarding once here means every
 * ViewModel doesn't have to.
 */
internal class ClipboardImpl : Clipboard {

    override fun copyText(text: String, label: String?) {
        if (text.isEmpty()) return
        val context = AndroidAppContext.require()
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText(label ?: "Sayva", text))
    }
}

actual object ClipboardProvider {
    actual fun create(): Clipboard = ClipboardImpl()
}
