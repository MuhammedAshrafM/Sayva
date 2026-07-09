package org.moashraf.sayva.ml

import kotlin.time.TimeSource
import org.moashraf.sayva.languagepack.SignVocabulary

/**
 * A [SignRecognizer] assembled from the four ML port adapters:
 * runtime + preprocessor + postprocessor + vocabulary.
 *
 * Nothing here is framework- or model-architecture-specific — the same
 * class serves an MLP fingerspelling model, an LSTM temporal model, or
 * a Transformer sentence model. Which pieces get composed is decided at
 * factory time from the pack's manifest metadata.
 *
 * This class replaced the previous per-shape wrappers (`TfliteSignRecognizer`,
 * `TemporalSignRecognizer`) — both were doing the same argmax over a
 * runtime output, only differing in input-size validation. Both cases
 * fall out naturally here: [expectedInputElements] is provided by the
 * caller from `PackModel.input.shape`.
 */
class ComposedSignRecognizer(
    private val runtime: ModelRuntime,
    private val preprocessor: Preprocessor,
    private val postprocessor: Postprocessor,
    private val vocabulary: SignVocabulary,
    private val expectedInputElements: Int,
) : SignRecognizer {

    override fun recognize(landmarks: FloatArray): RecognitionResult {
        // Per-stage timing so `PipelineDiagnostics` can attribute latency
        // between preprocess / inference / postprocess. Uses one mark and
        // three deltas — cheaper than three separate `markNow` pairs.
        val t0 = TimeSource.Monotonic.markNow()
        val preprocessed = preprocessor.preprocess(landmarks)
        val preprocessingNanos = t0.elapsedNow().inWholeNanoseconds

        require(preprocessed.size == expectedInputElements) {
            "Preprocessed input is ${preprocessed.size} floats; model expects $expectedInputElements"
        }

        val t1 = TimeSource.Monotonic.markNow()
        val rawOutput = runtime.invoke(preprocessed)
        val inferenceNanos = t1.elapsedNow().inWholeNanoseconds

        val t2 = TimeSource.Monotonic.markNow()
        val result = postprocessor.postprocess(rawOutput, vocabulary)
        val postprocessingNanos = t2.elapsedNow().inWholeNanoseconds

        return result.copy(
            preprocessingNanos = preprocessingNanos,
            inferenceNanos = inferenceNanos,
            postprocessingNanos = postprocessingNanos,
            preprocessedInput = preprocessed,
        )
    }

    override fun close() {
        runtime.close()
    }
}
