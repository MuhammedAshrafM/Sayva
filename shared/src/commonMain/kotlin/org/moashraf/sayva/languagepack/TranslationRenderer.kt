package org.moashraf.sayva.languagepack

/**
 * Turns a recognizer result — a `(modelId, sign)` pair from the active pack —
 * into a user-facing label in the active output language.
 *
 * Fallback semantics:
 *   * If the pack has no labels for the requested output language:
 *     use [LanguagePack.defaultOutputLanguage] and flag with
 *     [LabelResult.fallback] = `MissingOutputLanguage`.
 *   * If the pack has a labels file for that language but this specific
 *     sign is `null` (missing translation): return the gloss ID and flag
 *     with `MissingSign`.
 *   * Fully resolved translations return `fallback = None`.
 *
 * ViewModels render the label directly; the `fallback` state is what the UI
 * uses to surface a small annotation ("not translated" chip, "showing en
 * because ar not available", etc.).
 */
fun interface TranslationRenderer {
    fun render(pack: LanguagePack, outputCode: String, modelId: String, sign: VocabSign): LabelResult
}

data class LabelResult(
    val label: String,
    val effectiveOutputCode: String,
    val fallback: FallbackReason,
) {
    enum class FallbackReason {
        None,
        MissingSign,               // sign entry is null in a supported output
        MissingOutputLanguage,     // pack has no labels file for this output code
    }
}

class DefaultTranslationRenderer : TranslationRenderer {
    override fun render(
        pack: LanguagePack,
        outputCode: String,
        modelId: String,
        sign: VocabSign,
    ): LabelResult {
        val requestedLabels = pack.outputLabels[outputCode]
        if (requestedLabels == null || modelId !in requestedLabels) {
            val fallbackCode = pack.defaultOutputLanguage
            val fallbackLabel = pack.outputLabels[fallbackCode]
                ?.get(modelId)
                ?.get(sign.id)
                ?: sign.id
            return LabelResult(
                label = fallbackLabel,
                effectiveOutputCode = fallbackCode,
                fallback = LabelResult.FallbackReason.MissingOutputLanguage,
            )
        }
        val perModel = requestedLabels[modelId] ?: emptyMap()
        val label = perModel[sign.id]
        return if (label != null) {
            LabelResult(
                label = label,
                effectiveOutputCode = outputCode,
                fallback = LabelResult.FallbackReason.None,
            )
        } else {
            LabelResult(
                label = sign.id,
                effectiveOutputCode = outputCode,
                fallback = LabelResult.FallbackReason.MissingSign,
            )
        }
    }
}
