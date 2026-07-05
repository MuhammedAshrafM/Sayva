package org.moashraf.sayva.pipeline

import org.moashraf.sayva.languagepack.ConfidenceBucket
import org.moashraf.sayva.languagepack.VocabSign
import org.moashraf.sayva.ml.PipelineDiagnostics

/**
 * Everything a LiveCamera screen needs, in one sealed hierarchy.
 *
 * The screen `collectAsState()`s this and renders one branch per subtype.
 * All state includes a `packCode` + `modelId` when available so any debug
 * overlay or analytics tag can reference them without extra plumbing.
 */
sealed class RecognitionUiState {

    /** Before any camera / model is started. */
    data object Idle : RecognitionUiState()

    /** Between "start pressed" and "first frame ready" — usually 100–500 ms. */
    data object Starting : RecognitionUiState()

    /**
     * Active pack doesn't advertise the requested capability. UI should
     * disable the mode selector for this role and show a hint like
     * "This language pack doesn't include sign recognition yet."
     */
    data class NoModelForMode(
        val packCode: String,
        val role: String,
    ) : RecognitionUiState()

    /**
     * Camera + model are up; each frame updates [prediction] +
     * [diagnostics]. `prediction = null` means "no hand detected in
     * this frame" — the pipeline emitted a heartbeat with diagnostics
     * so the UI can still show live latency numbers.
     */
    data class Recognizing(
        val packCode: String,
        val modelId: String,
        val role: String,
        val architecture: String,
        val prediction: Prediction?,
        val diagnostics: PipelineDiagnostics,
    ) : RecognitionUiState()

    /**
     * A frame- or lifecycle-level failure. The pipeline enters this state
     * and stops emitting until [RecognitionPipeline.start] is called again.
     * `packCode` / `modelId` may be null if the failure happened during
     * setup before those were resolved.
     */
    data class Error(
        val cause: Throwable,
        val packCode: String? = null,
        val modelId: String? = null,
    ) : RecognitionUiState()
}

/**
 * One resolved recognition result. Renderable-directly by the UI.
 *
 * @param sign winning vocabulary entry
 * @param confidence softmax probability
 * @param bucket UI treatment per pack thresholds (Show / Caution / LowConfidence)
 * @param label user-facing string via [org.moashraf.sayva.languagepack.TranslationRenderer]
 * @param effectiveOutputCode language code the label is actually in — may
 *   differ from the requested output when we fell back (see
 *   [org.moashraf.sayva.languagepack.LabelResult.fallback])
 */
data class Prediction(
    val sign: VocabSign,
    val confidence: Float,
    val bucket: ConfidenceBucket,
    val label: String,
    val effectiveOutputCode: String,
)
