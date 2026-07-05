package org.moashraf.sayva.ml.adapters

import org.moashraf.sayva.ml.LandmarkPreprocessor
import org.moashraf.sayva.ml.Preprocessor

/**
 * MVP preprocessor adapters. Each is a thin wrapper over the pure math in
 * [LandmarkPreprocessor], exposed under a stable string ID so that pack
 * manifests can name them.
 *
 * Adding a new preprocessing pipeline is one file: implement [Preprocessor],
 * register the ID in the Koin module, ship the app version, and any pack
 * that names the ID starts working.
 */

/**
 * Kazuhito's single-hand normalization — 42-float landmark array in,
 * 42-float wrist-relative + max-abs-normalized array out. Passthrough
 * on already-preprocessed input from the training pipeline.
 *
 * Registered as `single_hand_kazuhito_v1`.
 */
object SingleHandKazuhitoPreprocessor : Preprocessor {
    const val ID: String = "single_hand_kazuhito_v1"

    override fun preprocess(input: FloatArray): FloatArray =
        LandmarkPreprocessor.preprocess(input)
}

/**
 * Two-hand sequence preprocessor — expects an already-flat `T * 84`
 * FloatArray (the caller has assembled a fixed-length window of
 * `preprocessTwoHandFrame` outputs). Identity passthrough since the
 * per-frame normalization has already happened.
 *
 * When the camera pipeline lands in Phase 2, this adapter will grow to
 * do the accumulation itself so callers just push individual frames.
 *
 * Registered as `two_hand_sequence_v1`.
 */
object TwoHandSequencePreprocessor : Preprocessor {
    const val ID: String = "two_hand_sequence_v1"

    override fun preprocess(input: FloatArray): FloatArray {
        require(input.size % 84 == 0) {
            "two_hand_sequence_v1 input must be a multiple of 84 (2 hands × 21 landmarks × 2 coords), got ${input.size}"
        }
        return input
    }
}
