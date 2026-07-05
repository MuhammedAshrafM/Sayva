package org.moashraf.sayva.languagepack

/**
 * Standard capability role identifiers advertised by pack models via
 * [PackModel.role]. Same string-key registry pattern used for adapters —
 * the UI mode selector maps user modes to these roles; the pipeline
 * looks up a model with [LanguagePack.modelByRole].
 *
 * Adding a new user-facing recognition mode is:
 *   1. Add a constant here
 *   2. Add the mode to the UI selector
 *   3. Any Pack that ships a model with that role starts serving the mode
 *
 * These are string values (not an enum) so future Packs can advertise
 * roles the current app version doesn't know about — the UI simply
 * ignores unknown roles and continues. That preserves forward-compat
 * when a Pack ships ahead of the app that shipped its role list.
 */
object RecognitionRole {
    /** Single-frame hand-shape classification (ASL alphabet, ArSL alphabet, …). */
    const val FINGERSPELLING: String = "fingerspelling"

    /** Temporal recognition of individual signs (Hello, Thank you, …). */
    const val SIGN_RECOGNITION: String = "sign_recognition"

    /** Continuous-signing sentence recognition. Not implemented in MVP. */
    const val SENTENCE_RECOGNITION: String = "sentence_recognition"

    /** Facial-expression recognition for grammatical role (ASL, …). Not implemented in MVP. */
    const val FACIAL_EXPRESSION: String = "facial_expression"
}
