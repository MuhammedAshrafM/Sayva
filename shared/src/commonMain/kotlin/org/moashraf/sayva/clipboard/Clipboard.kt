package org.moashraf.sayva.clipboard

/**
 * Platform-agnostic clipboard port. UI never imports Android or iOS
 * clipboard APIs directly — this keeps commonMain compilable and every
 * caller testable through a fake implementation.
 *
 * The clipboard is intentionally write-only from the app's perspective;
 * we don't offer a `read()` because no current use case wants to
 * consume clipboard contents, and doing so would trigger platform
 * clipboard-access prompts on iOS 14+.
 */
interface Clipboard {

    /**
     * Copy [text] to the system clipboard. Any prior clipboard content is
     * overwritten. `null` or an empty string is a no-op — callers shouldn't
     * need to guard.
     */
    fun copyText(text: String, label: String? = null)
}

/**
 * Factory for the platform's [Clipboard]. The Koin module binds this
 * as a `single` and calls `create()` at app startup.
 */
expect object ClipboardProvider {
    fun create(): Clipboard
}
