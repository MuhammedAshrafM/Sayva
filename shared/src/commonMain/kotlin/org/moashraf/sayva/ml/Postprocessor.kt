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
 * Standard MVP postprocessor: numerically-stable softmax over the raw
 * model output, then argmax. The winning class's softmax probability is
 * returned as `confidence`.
 *
 * ### Why the softmax step exists
 * Every model we ship emits raw **logits** — the trained networks end at
 * `Linear(N)` with no softmax layer (see `FingerspellingMLP.forward` +
 * the ONNX export whose output tensor is literally named `"logits"`).
 * Argmax over logits is mathematically identical to argmax over softmax
 * (softmax is monotonic), so the WINNING CLASS is correct either way.
 *
 * The confidence VALUE is what softmax fixes: raw logits are unbounded
 * reals (e.g. `12.5`, `-3.1`), so treating them as `[0, 1]` probabilities
 * makes the pack manifest's `ConfidenceThresholds` (0.60 / 0.90 buckets)
 * meaningless. Softmax here restores the documented contract: `confidence`
 * IS a probability, and the bucket cutoffs work as designed.
 *
 * ### Numerical stability
 * We subtract the max logit before exponentiation. This prevents `exp` of
 * very large positive values from overflowing to `Inf`, without changing
 * the softmax result (`exp(x-c) / Σ exp(xᵢ-c) = exp(x) / Σ exp(xᵢ)`).
 *
 * ### Ties
 * Resolve to the earliest index — matches the previous behavior so the
 * softmax-introduction is a pure confidence-semantics fix, not a
 * class-selection change.
 */
object ArgmaxConfidencePostprocessor : Postprocessor {
    const val ID: String = "argmax_confidence_v1"

    /** How many candidates to surface via [RecognitionResult.topK]. Five is
     *  the largest set the developer HUD renders; a Top-5 accuracy metric is
     *  also the standard second-best signal alongside Top-1 in vocabularies
     *  this small. */
    private const val TOP_K: Int = 5

    override fun postprocess(rawOutput: FloatArray, vocabulary: SignVocabulary): RecognitionResult {
        require(rawOutput.isNotEmpty()) { "postprocessor received empty model output" }

        // 1. Argmax + max — one pass over the input.
        var bestIndex = 0
        var maxLogit = rawOutput[0]
        for (i in 1 until rawOutput.size) {
            if (rawOutput[i] > maxLogit) {
                bestIndex = i
                maxLogit = rawOutput[i]
            }
        }

        // 2. Numerically-stable softmax denominator: sum of exp(logit - max).
        // Best class contributes exp(0) = 1 exactly; others sum to a value in
        // [n-1 close-to-uniform, ~0 spiky].
        var sumExp = 0.0
        val expShifted = DoubleArray(rawOutput.size)
        for (i in rawOutput.indices) {
            val v = kotlin.math.exp((rawOutput[i] - maxLogit).toDouble())
            expShifted[i] = v
            sumExp += v
        }

        // 3. Softmax probability of the winner. `sumExp >= 1.0` always
        // (best class contributes 1.0), so no divide-by-zero risk.
        val confidence = (1.0 / sumExp).toFloat()

        // 4. Top-K by softmax probability. Extract via a k-way partial sort:
        // cheaper than a full sort for small K (5 out of 24-100 classes) and
        // avoids allocating N floats when we only surface min(K, N).
        val k = kotlin.math.min(TOP_K, rawOutput.size)
        val topK = ArrayList<ClassProbability>(k)
        // Simple selection: repeat "find the max index not yet taken" K times.
        // For K=5 and N=24 that's 120 comparisons — negligible vs the model
        // invocation cost.
        val taken = BooleanArray(rawOutput.size)
        repeat(k) {
            var pickIdx = -1
            var pickVal = Double.NEGATIVE_INFINITY
            for (i in expShifted.indices) {
                if (!taken[i] && expShifted[i] > pickVal) {
                    pickVal = expShifted[i]
                    pickIdx = i
                }
            }
            taken[pickIdx] = true
            topK.add(ClassProbability(
                classIndex = pickIdx,
                probability = (pickVal / sumExp).toFloat(),
            ))
        }

        return RecognitionResult(
            classIndex = bestIndex,
            confidence = confidence,
            topK = topK,
        )
    }
}
