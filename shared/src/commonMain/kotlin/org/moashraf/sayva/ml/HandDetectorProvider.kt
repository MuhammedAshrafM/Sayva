package org.moashraf.sayva.ml

/**
 * Platform factory for [HandDetector]. Bound via Koin as a `single`
 * factory that produces one detector per active pack model.
 *
 * Android: MediaPipe Tasks Vision, loading the `hand_landmarker.task` asset.
 * iOS: MediaPipe iOS binary (deferred to Mac session).
 */
expect object HandDetectorProvider {
    /**
     * @param maxHands 1 for single-hand fingerspelling recognition, 2 for
     *   two-hand temporal signs. Determined by the active pack's model.
     */
    fun create(maxHands: Int): HandDetector
}
