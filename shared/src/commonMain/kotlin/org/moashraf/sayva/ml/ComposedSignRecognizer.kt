package org.moashraf.sayva.ml

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
        val preprocessed = preprocessor.preprocess(landmarks)
        require(preprocessed.size == expectedInputElements) {
            "Preprocessed input is ${preprocessed.size} floats; model expects $expectedInputElements"
        }
        val rawOutput = runtime.invoke(preprocessed)
        return postprocessor.postprocess(rawOutput, vocabulary)
    }

    override fun close() {
        runtime.close()
    }
}
