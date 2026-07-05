package org.moashraf.sayva.ml

import kotlin.math.abs

/**
 * Hand landmark preprocessing. Ports Kazuhito's `pre_process_landmark` from
 * `hand-gesture-recognition-using-mediapipe/app.py` verbatim so on-device
 * inference sees the exact same input shape as the training set.
 *
 * The Python side lives in `ml/src/sayva_ml/preprocessing/landmark.py`. Both
 * implementations are held to the shared `landmark_parity.json` fixture — see
 * `LandmarkPreprocessorParityTest` in `androidHostTest`.
 *
 * ### Algorithm
 * 1. Translate: subtract `landmarks[0]` (the wrist) from every point.
 * 2. Flatten: `[(x0, y0), (x1, y1), ...]` → `[x0, y0, x1, y1, ...]`.
 * 3. Normalize: divide every element by the max absolute component so the
 *    result lives in [-1, 1] and is scale-invariant.
 */
object LandmarkPreprocessor {

    private const val EXPECTED_LANDMARKS = 21
    private const val OUTPUT_SIZE = 42

    /**
     * @param landmarks 21 (x, y) pairs, stored as a flat 42-float array in
     *   `[x0, y0, x1, y1, ...]` order — matches how MediaPipe's landmark
     *   results are typically flattened on the Android/iOS bridge.
     * @return 42 floats. Wrist at (0, 0); max absolute component == 1.0.
     *   A degenerate zero-length input (all landmarks at the same point)
     *   returns a zero vector rather than dividing by zero — callers should
     *   treat that as a low-confidence frame and skip it.
     */
    fun preprocess(landmarks: FloatArray): FloatArray {
        require(landmarks.size == OUTPUT_SIZE) {
            "Expected $OUTPUT_SIZE floats (21 landmarks × 2 coords), got ${landmarks.size}"
        }
        val baseX = landmarks[0]
        val baseY = landmarks[1]
        val relative = FloatArray(OUTPUT_SIZE)
        for (i in 0 until EXPECTED_LANDMARKS) {
            relative[i * 2] = landmarks[i * 2] - baseX
            relative[i * 2 + 1] = landmarks[i * 2 + 1] - baseY
        }
        var maxAbs = 0f
        for (v in relative) {
            val a = abs(v)
            if (a > maxAbs) maxAbs = a
        }
        if (maxAbs == 0f) return FloatArray(OUTPUT_SIZE)
        for (i in relative.indices) relative[i] /= maxAbs
        return relative
    }

    /**
     * Two-hand variant for temporal recognizers (Track C).
     *
     * @param left 42-float single-hand landmark array or `null` if that hand
     *   isn't in this frame. Format matches [preprocess]'s input.
     * @param right same, for the right hand.
     * @return 84 floats: `[left_42, right_42]`. Each half is normalized
     *   independently (see `pre_process_two_hand_frame` in
     *   `ml/src/sayva_ml/preprocessing/landmark.py` for design rationale).
     */
    fun preprocessTwoHandFrame(left: FloatArray?, right: FloatArray?): FloatArray {
        val out = FloatArray(TWO_HAND_FRAME_SIZE)
        val leftHalf = if (left != null) preprocess(left) else FloatArray(OUTPUT_SIZE)
        val rightHalf = if (right != null) preprocess(right) else FloatArray(OUTPUT_SIZE)
        leftHalf.copyInto(out, destinationOffset = 0)
        rightHalf.copyInto(out, destinationOffset = OUTPUT_SIZE)
        return out
    }

    /**
     * Resample and preprocess a variable-length sequence into a fixed
     * `[targetLength, 84]` shape the temporal LSTM expects.
     *
     * Short sequences: right-pad with zero frames.
     * Long sequences: uniformly sample `targetLength` frames across the clip
     *   (so a 60-frame clip becomes every-other-frame, not just the first 30).
     *
     * Returns a flat `FloatArray` of size `targetLength * 84`, ordered
     * frame-major: `[frame0_84_floats, frame1_84_floats, ...]`. Callers pass
     * this directly to `TfliteRuntime.invoke`; the Android runtime reshapes
     * to `[1, targetLength, 84]` based on the model's declared input shape.
     */
    fun preprocessTwoHandSequence(
        frames: List<Pair<FloatArray?, FloatArray?>>,
        targetLength: Int,
    ): FloatArray {
        require(targetLength > 0) { "targetLength must be positive, got $targetLength" }
        val out = FloatArray(targetLength * TWO_HAND_FRAME_SIZE)
        if (frames.isEmpty()) return out

        if (frames.size >= targetLength) {
            val step = if (targetLength > 1) (frames.size - 1).toDouble() / (targetLength - 1) else 0.0
            for (i in 0 until targetLength) {
                val idx = kotlin.math.min(
                    kotlin.math.round(i * step).toInt(),
                    frames.size - 1,
                )
                val (l, r) = frames[idx]
                val f = preprocessTwoHandFrame(l, r)
                f.copyInto(out, destinationOffset = i * TWO_HAND_FRAME_SIZE)
            }
        } else {
            for (i in frames.indices) {
                val (l, r) = frames[i]
                val f = preprocessTwoHandFrame(l, r)
                f.copyInto(out, destinationOffset = i * TWO_HAND_FRAME_SIZE)
            }
            // Remaining slots stay zero (allocated by FloatArray constructor).
        }
        return out
    }

    private const val TWO_HAND_FRAME_SIZE = 84
}
