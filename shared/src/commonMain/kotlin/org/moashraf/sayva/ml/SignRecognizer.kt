package org.moashraf.sayva.ml

/**
 * Port for on-device sign recognition. Consumers depend on this interface;
 * the concrete adapter (TFLite on Android, CoreML on iOS in Track B) is
 * bound in Koin.
 *
 * Track A ships a smoke-test implementation that runs Kazuhito00's shipped
 * `keypoint_classifier.tflite` (3 classes: Open/Close/Pointer). This is
 * runtime-plumbing validation only — the classes are not part of Sayva's
 * production vocabulary and no UI displays them.
 *
 * See `NOTICES.md` for third-party model attribution.
 */
interface SignRecognizer {
    /**
     * @param landmarks Preprocessed 42-float vector: 21 hand landmarks × (x, y),
     *   translated so the wrist is at the origin and normalized so the max
     *   absolute component is 1.0. Matches Kazuhito's `pre_process_landmark`
     *   (see `ml/src/sayva_ml/preprocessing/landmark.py`).
     */
    fun recognize(landmarks: FloatArray): RecognitionResult

    /** Release native resources. Idempotent. */
    fun close()
}
