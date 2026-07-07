package org.moashraf.sayva.clipboard

import platform.UIKit.UIPasteboard

/**
 * iOS [Clipboard] — general pasteboard via [UIPasteboard.generalPasteboard].
 *
 * The `label` parameter is ignored on iOS; the general pasteboard has no
 * caller-provided label field (system Files.app / share sheet UIs derive
 * their own preview from the string content).
 */
internal class ClipboardImpl : Clipboard {

    override fun copyText(text: String, label: String?) {
        if (text.isEmpty()) return
        UIPasteboard.generalPasteboard.string = text
    }
}

actual object ClipboardProvider {
    actual fun create(): Clipboard = ClipboardImpl()
}
