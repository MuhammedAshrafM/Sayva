package org.moashraf.sayva.ml


/**
 * Per-frame diagnostics emitted alongside each recognition result. The
 * recognition pipeline builds one of these on every processed frame and
 * embeds it in the UI state so:
 *   * A debug overlay can render live latencies for tuning
 *   * Analytics events can tag frame-level performance
 *   * Crashlytics keys carry the active pack + model + recent latency
 *
 * Language- and model-agnostic: every field is derived from work that
 * happens regardless of which Pack is active. `packCode` + `modelId` are
 * carried through so an aggregate view can slice by (pack, model).
 *
 * All timings are in nanoseconds — sub-microsecond precision. Frontends
 * that render values divide by 1_000_000 for display.
 */
data class PipelineDiagnostics(
    /** Active pack — e.g. `"ase"`. */
    val packCode: String,
    /** Active model ID within the pack — e.g. `"fingerspelling"`. */
    val modelId: String,
    /** Model role — e.g. `"fingerspelling"`. */
    val role: String,
    /** Model architecture tag from the manifest — e.g. `"mlp"`. */
    val architecture: String,

    /** Total wall clock from frame received → recognition emitted. */
    val totalFrameNanos: Long,
    /** MediaPipe HandLandmarker latency. */
    val handDetectionNanos: Long,
    /** Landmark → model-input transformation latency. */
    val preprocessingNanos: Long,
    /** ModelRuntime.invoke() latency. */
    val inferenceNanos: Long,
    /** Postprocessor + vocabulary lookup latency. */
    val postprocessingNanos: Long,

    /** Number of hands MediaPipe found in this frame. */
    val handsDetected: Int,
    /**
     * Handedness of the first (highest-confidence) detected hand, when any.
     * The production LiveCameraScreen renders a "HAND DETECTED · <side>"
     * pill from this — the field is language-neutral because handedness
     * doesn't depend on the active pack.
     */
    val primaryHandedness: Handedness?,
    /**
     * Raw 21 (x, y) landmarks for the primary detected hand, flattened as
     * `[x0, y0, x1, y1, …, x20, y20]` in the pixel-scale coordinate frame
     * MediaPipe reports (0 ≤ x < frame.widthPx, 0 ≤ y < frame.heightPx).
     * Null when no hand was detected. Populated for developer-mode
     * diagnostics — the skeleton overlay draws directly from this.
     */
    val rawLandmarks: FloatArray? = null,
    /**
     * The exact 42 floats fed into the model runtime, AFTER the pack's
     * preprocessor ran. For a single-hand pack this is the wrist-relative
     * max-abs-normalized vector; for a two-hand pack it's `[left_42, right_42]`.
     * Null when no hand was detected. Populated for developer-mode diagnostics
     * so the on-device sample-export matches golden_inference.json exactly.
     */
    val preprocessedFeatures: FloatArray? = null,
    /** Width of the frame MediaPipe processed, in pixels. Needed to render
     *  the developer skeleton overlay against the preview canvas at the
     *  right scale. */
    val sourceFrameWidthPx: Int = 0,
    /** Height of the frame MediaPipe processed. See [sourceFrameWidthPx]. */
    val sourceFrameHeightPx: Int = 0,
    /** Softmax confidence of the winning class, when a prediction happened. */
    val confidence: Float?,
    /**
     * Frames-per-second running average maintained by the pipeline. Rolling
     * window of the last N frames — smoothed so it doesn't jitter on the UI.
     */
    val fps: Float,
)
