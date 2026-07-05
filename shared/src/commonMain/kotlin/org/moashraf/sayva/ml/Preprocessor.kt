package org.moashraf.sayva.ml

/**
 * Transforms raw signer input (camera frame landmarks, video sequences, etc.)
 * into the flat model-input vector a specific model was trained on.
 *
 * Adapters implementing this port register by ID in [PreprocessorRegistry].
 * Each pack model manifests the preprocessing ID it needs — e.g.
 * `single_hand_kazuhito_v1` for Kazuhito's 21-landmark normalization,
 * `two_hand_sequence_v1` for the 30-frame two-hand LSTM path. A pack could
 * ship a `face_landmark_v1` model tomorrow with zero app changes required.
 *
 * Preprocessors are stateless — a single instance per adapter is bound in
 * DI and shared by every recognizer that needs it.
 */
fun interface Preprocessor {
    /**
     * @param input Raw input the caller has prepared — shape depends on the
     *   pipeline. For single-hand recognizers this is a 42-float landmark
     *   array; for two-hand sequences it's a variable-length sequence flat
     *   to `T * 84`.
     * @return Model input as a flat FloatArray. Length must match
     *   `PackModel.input.shape`'s tail product.
     */
    fun preprocess(input: FloatArray): FloatArray
}

/**
 * Maps preprocessing IDs → adapter instances. Same pattern as
 * [ModelRuntimeRegistry] — one lookup, one clear error on miss.
 */
class PreprocessorRegistry(
    private val adapters: Map<String, Preprocessor>,
) {
    fun get(preprocessingId: String): Preprocessor =
        adapters[preprocessingId] ?: throw UnknownAdapterException(
            dimension = "preprocessing",
            requestedId = preprocessingId,
            availableIds = adapters.keys.sorted(),
        )

    fun supports(preprocessingId: String): Boolean = preprocessingId in adapters
}
