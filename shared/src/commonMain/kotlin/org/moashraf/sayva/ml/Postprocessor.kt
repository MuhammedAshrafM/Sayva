package org.moashraf.sayva.ml

import org.moashraf.sayva.languagepack.SignVocabulary

/**
 * Transforms raw model output into a [RecognitionResult] the app can
 * present to the user.
 *
 * The MVP adapter is [ArgmaxConfidencePostprocessor] (`argmax_confidence_v1`)
 * — argmax over softmax probabilities. Future adapters:
 *   * `ctc_greedy_v1` — CTC decoding for continuous fingerspelling
 *   * `beam_search_v1` — n-best decoding with language-model rescoring
 *   * `threshold_gated_v1` — reject predictions below a per-vocab-entry
 *     confidence threshold
 *
 * Stateless — one instance per adapter, shared across recognizers.
 */
fun interface Postprocessor {
    fun postprocess(rawOutput: FloatArray, vocabulary: SignVocabulary): RecognitionResult
}

class PostprocessorRegistry(
    private val adapters: Map<String, Postprocessor>,
) {
    fun get(postprocessingId: String): Postprocessor =
        adapters[postprocessingId] ?: throw UnknownAdapterException(
            dimension = "postprocessing",
            requestedId = postprocessingId,
            availableIds = adapters.keys.sorted(),
        )

    fun supports(postprocessingId: String): Boolean = postprocessingId in adapters
}

/**
 * Standard MVP postprocessor: argmax over the raw output, treating the
 * corresponding element as a confidence score. If the model doesn't emit
 * a softmax head, callers should chain a softmax preprocessor into the
 * runtime — but every model we've shipped so far ends with a softmax
 * layer, so raw output IS a probability distribution.
 *
 * Ties resolve to the earliest index — matches the [TfliteSignRecognizer]
 * behavior the previous batch shipped, so no user-visible change from the
 * refactor.
 */
object ArgmaxConfidencePostprocessor : Postprocessor {
    const val ID: String = "argmax_confidence_v1"

    override fun postprocess(rawOutput: FloatArray, vocabulary: SignVocabulary): RecognitionResult {
        require(rawOutput.isNotEmpty()) { "postprocessor received empty model output" }
        var bestIndex = 0
        var bestProb = rawOutput[0]
        for (i in 1 until rawOutput.size) {
            if (rawOutput[i] > bestProb) {
                bestIndex = i
                bestProb = rawOutput[i]
            }
        }
        return RecognitionResult(classIndex = bestIndex, confidence = bestProb)
    }
}
