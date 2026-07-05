package org.moashraf.sayva.ml

import org.moashraf.sayva.camera.CameraFrame

/**
 * Camera frame → hand landmarks. The port through which the recognition
 * pipeline receives detections.
 *
 * Android implements this via MediaPipe Tasks Vision (HandLandmarker);
 * iOS uses MediaPipe iOS via CocoaPods (deferred to Mac session). Both
 * expose the same [HandDetection] result shape.
 *
 * The detector is created with [maxHands] — either 1 (single-hand
 * fingerspelling) or 2 (two-hand temporal signs). Callers choose based
 * on the active pack's model requirements.
 *
 * Lifecycle: create once per active pack model (Koin scope), close when
 * switching packs or destroying the recognition screen. Model files are
 * loaded from Android assets / iOS bundle — one-time init cost.
 */
interface HandDetector {

    /**
     * Detect hands in a single frame. Runs synchronously; suitable for
     * background-dispatcher use. Returns an empty [HandDetection] if no
     * hand is detected.
     *
     * @throws IllegalStateException if called after [close].
     */
    fun detect(frame: CameraFrame): HandDetection

    /** Release native resources. Idempotent. */
    fun close()
}

/**
 * Result of one [HandDetector.detect] call — up to `maxHands` hand
 * landmark sets. Empty [hands] means "no hand detected" — the pipeline
 * treats these as low-confidence frames to skip.
 */
data class HandDetection(
    val hands: List<HandLandmarks>,
    val processingNanos: Long,
)

/**
 * One hand's landmark set — 21 landmarks per MediaPipe's HandLandmarker.
 *
 * @param handedness Left / Right — stable across frames per hand.
 * @param landmarks 21 (x, y) pairs in image-pixel coordinates, flattened
 *   as `[x0, y0, x1, y1, …]` — matches the shape [LandmarkPreprocessor]
 *   consumes.
 */
data class HandLandmarks(
    val handedness: Handedness,
    val landmarks: FloatArray,
) {
    init {
        require(landmarks.size == 42) {
            "HandLandmarks must have 21 (x, y) pairs (42 floats), got ${landmarks.size}"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HandLandmarks) return false
        return handedness == other.handedness && landmarks.contentEquals(other.landmarks)
    }

    override fun hashCode(): Int = 31 * handedness.hashCode() + landmarks.contentHashCode()
}

enum class Handedness { Left, Right, Unknown }
