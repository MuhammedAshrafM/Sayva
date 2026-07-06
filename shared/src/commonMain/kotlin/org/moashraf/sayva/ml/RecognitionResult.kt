package org.moashraf.sayva.ml

/**
 * Output of a single [SignRecognizer.recognize] call.
 *
 * @param classIndex Argmax over the model's output probabilities. Index into
 *   `Vocabulary.signs` once the codegen from `ml/configs/vocabulary.yaml`
 *   lands (P1-17). For the Track A smoke test, the mapping is:
 *   `0 = Open`, `1 = Close`, `2 = Pointer` per Kazuhito's shipped labels.
 * @param confidence Softmax probability for [classIndex]. Range `[0f, 1f]`.
 *   Callers apply Phase 2's bucketing thresholds (≥0.9 show, 0.6–0.89 caution,
 *   <0.6 trigger low-confidence flow) per docs/AI_PIPELINE.md.
 */
data class RecognitionResult(
    val classIndex: Int,
    val confidence: Float,
    /** Nanoseconds spent inside the preprocessor — landmarks → model input.
     *  Zero when the recognizer doesn't instrument its stages (e.g. test fakes,
     *  legacy adapters). Populated by [ComposedSignRecognizer]. */
    val preprocessingNanos: Long = 0L,
    /** Nanoseconds spent inside the model runtime's `invoke`. */
    val inferenceNanos: Long = 0L,
    /** Nanoseconds spent inside the postprocessor — raw output → class + prob. */
    val postprocessingNanos: Long = 0L,
)
