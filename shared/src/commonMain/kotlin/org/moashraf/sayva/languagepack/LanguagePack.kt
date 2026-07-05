package org.moashraf.sayva.languagepack

/**
 * A Language Pack — everything needed to recognize one sign language and
 * translate it to any supported spoken output.
 *
 * Every pack — ASL, EgSL, SSL, JSL, BSL, any future language — is loaded
 * from a `manifest.json` produced by `ml/scripts/generate_pack.py`. The
 * app never contains language-specific code; it discovers packs at startup
 * and treats them uniformly.
 *
 * The runtime instance is immutable. Switching packs at runtime replaces
 * the [ActiveLanguagePack]'s reference rather than mutating this object.
 */
data class LanguagePack(
    val schemaVersion: Int,
    val recognitionCode: String,
    val displayName: Map<String, String>,
    val version: String,
    val minAppVersion: Int,
    val bundled: Boolean,
    val models: List<PackModel>,
    val supportedOutputs: List<String>,
    val outputLanguageStatus: Map<String, OutputLanguageStatus>,
    val defaultOutputLanguage: String,
    val outputLabels: Map<String, Map<String, Map<String, String?>>>,
    val ttsLocaleByOutput: Map<String, String>,
    val postProcessing: PostProcessingRules,
) {
    fun modelById(id: String): PackModel? = models.firstOrNull { it.id == id }

    /**
     * Look up a model by capability role (e.g. `"fingerspelling"`,
     * `"sign_recognition"`). This is how the recognition pipeline picks a
     * model without knowing per-Pack IDs — a Pack that ships continuous-
     * sign recognition advertises it as `role: sign_recognition` regardless
     * of what the Pack decides to call its model files.
     *
     * Returns `null` if the Pack doesn't ship this capability. UI enables/
     * disables the corresponding mode based on this being non-null.
     */
    fun modelByRole(role: String): PackModel? = models.firstOrNull { it.role == role }

    /** All capability roles this Pack advertises. Used by UI mode selectors. */
    val supportedRoles: List<String> get() = models.map { it.role }

    fun displayName(outputCode: String): String =
        displayName[outputCode] ?: displayName[defaultOutputLanguage] ?: recognitionCode

    fun ttsLocale(outputCode: String): String? = ttsLocaleByOutput[outputCode]

    fun statusOf(outputCode: String): OutputLanguageStatus? =
        outputLanguageStatus[outputCode]
}

/** Translation completeness for one output language inside one pack. */
enum class OutputLanguageStatus {
    /** File exists so the schema passes; label entries are placeholders / null. */
    Stub,

    /** Some entries translated + reviewed, others still null. */
    Partial,

    /** Every vocab entry has a reviewed translation. */
    Complete;

    companion object {
        fun fromWireValue(v: String): OutputLanguageStatus = when (v.lowercase()) {
            "stub" -> Stub
            "partial" -> Partial
            "complete" -> Complete
            else -> error("Unknown outputLanguageStatus: '$v'")
        }
    }
}

/**
 * One model inside a pack. Fully declarative — no per-model code branches
 * anywhere in the app; the runtime looks up adapters by ID from the
 * respective registry.
 *
 * @param role Stable capability identifier — `fingerspelling`,
 *   `sign_recognition`, `sentence_recognition`, `facial_expression`, …
 *   The recognition pipeline picks WHICH model to run based on the user's
 *   selected mode, which maps to a role. Model IDs vary across Packs
 *   (`fingerspelling` vs `letter_recognition`), roles do not.
 * @param architecture Free-form descriptive tag (e.g. `mlp`, `lstm_unrolled`,
 *   `transformer_v1`). Purely metadata for humans + analytics; the runtime
 *   picks concrete behavior from the adapter IDs below.
 * @param runtimeType Registry key into `ModelRuntimeRegistry` — e.g.
 *   `tflite`, `coreml`, `onnx`.
 * @param inferenceStrategy Registry key describing how frames are fed to
 *   the model (`single_frame`, `sliding_window`, `streaming_stateful`).
 *   Phase 1 uses `single_frame` for every model.
 * @param confidenceThresholds Per-model UI bucketing. Different architectures
 *   have differently calibrated softmax outputs — the Pack tunes these
 *   empirically per model.
 */
data class PackModel(
    val id: String,
    val role: String,
    val architecture: String,
    val modelFile: String,
    val runtimeType: String,
    val inferenceStrategy: String,
    val input: ModelInputSpec,
    val output: ModelOutputSpec,
    val confidenceThresholds: ConfidenceThresholds,
    val vocabulary: SignVocabulary,
) {
    /** Tail-product of the input shape (drop batch dim). Handy for buffer sizing. */
    val expectedInputElements: Int
        get() = input.shape.drop(1).fold(1) { acc, d -> acc * d }
}

data class ModelInputSpec(
    val shape: List<Int>,
    /** Preprocessor adapter ID — key into `PreprocessorRegistry`. */
    val preprocessing: String,
    /**
     * Hand count the model was trained for. The pipeline configures the
     * HandDetector with this value — the caller never assumes 1 vs 2.
     */
    val maxHands: Int,
    val sequenceLength: Int? = null,
)

data class ModelOutputSpec(
    val shape: List<Int>,
    /** Postprocessor adapter ID — key into `PostprocessorRegistry`. */
    val postprocessing: String,
)

/**
 * Per-model UI bucketing thresholds. Consumed by the pipeline's confidence
 * classifier to decide which UI state to emit.
 *
 * Buckets:
 *   * `>= show` → render prediction as-is
 *   * `>= caution && < show` → render with caution styling ("try again" hint)
 *   * `< caution` → route to a low-confidence flow (suggestions, retake)
 *
 * Kept per-model because different architectures have different softmax
 * calibrations (an MLP peaks sharply; an LSTM smears). Kept per-Pack
 * because different sign vocabularies have different intrinsic difficulty.
 */
data class ConfidenceThresholds(
    val show: Float,
    val caution: Float,
) {
    init {
        require(caution in 0f..show && show in caution..1f) {
            "ConfidenceThresholds must satisfy 0 ≤ caution ≤ show ≤ 1; got show=$show, caution=$caution"
        }
    }

    fun bucketFor(confidence: Float): ConfidenceBucket = when {
        confidence >= show -> ConfidenceBucket.Show
        confidence >= caution -> ConfidenceBucket.Caution
        else -> ConfidenceBucket.LowConfidence
    }
}

enum class ConfidenceBucket { Show, Caution, LowConfidence }

data class PostProcessingRules(
    val spellOutBlankTimeoutMs: Int,
    val sentenceAssemblyRuleset: String,
    val capitalization: String,
)

/**
 * The vocabulary for one model — canonical gloss IDs in class-index order.
 * Human-readable labels come from [LanguagePack.outputLabels] via
 * [TranslationRenderer], never from here.
 */
data class SignVocabulary(
    val version: Int,
    val signs: List<VocabSign>,
) {
    val size: Int get() = signs.size

    fun byIndex(index: Int): VocabSign? = signs.getOrNull(index)

    fun byId(id: String): VocabSign? = signs.firstOrNull { it.id == id }
}

data class VocabSign(
    /** Class index — position in [SignVocabulary.signs]. */
    val index: Int,
    /** Canonical gloss (typically English uppercase, e.g. "HELLO"). */
    val id: String,
    val tags: List<String>,
)
