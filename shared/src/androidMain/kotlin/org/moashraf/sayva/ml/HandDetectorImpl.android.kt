package org.moashraf.sayva.ml

import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import org.moashraf.sayva.bootstrap.AndroidAppContext
import org.moashraf.sayva.camera.CameraFrame

/**
 * MediaPipe Tasks Vision-backed [HandDetector].
 *
 * Uses `hand_landmarker.task` bundled as an app asset. The model file must
 * live at `androidMain/assets/mediapipe/hand_landmarker.task`. Ship it as a
 * one-time download (see `MediaPipeAssets.md` next to this file for the URL
 * + integrity SHA); a Gradle task can automate the fetch — for MVP we drop
 * the file in manually.
 *
 * ### Threading
 * Uses `RunningMode.IMAGE` which runs synchronously. The recognition pipeline
 * dispatches [detect] onto a background dispatcher so the main thread never
 * blocks. If per-frame latency becomes an issue, switch to `LIVE_STREAM` and
 * pipe results through a callback — but for a 21-landmark detector at 30 fps
 * on a Pixel 6+, sync IMAGE mode is comfortable.
 */
internal class HandDetectorImpl(private val maxHands: Int) : HandDetector {

    private val landmarker: HandLandmarker = run {
        val context = AndroidAppContext.require()
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("mediapipe/hand_landmarker.task")
            .build()
        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE)
            .setNumHands(maxHands)
            // Detection threshold aligned with the Python training pipeline
            // (see ml/src/sayva_ml/data/mediapipe_workers.py). A/B on the ASL
            // Alphabet showed 0.3 uniformly beats 0.5 across all 24 classes
            // with no regressions. Presence + tracking stay at 0.5 — those
            // are inference-time continuity thresholds that don't affect
            // train/serve parity for single-frame recognition.
            .setMinHandDetectionConfidence(0.3f)
            .setMinHandPresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .build()
        HandLandmarker.createFromOptions(context, options)
    }

    private var closed = false

    override fun detect(frame: CameraFrame): HandDetection {
        check(!closed) { "HandDetector.detect called after close()" }
        val proxy = frame.platformFrame as? ImageProxy
            ?: error(
                "Android HandDetectorImpl expects platformFrame to be an ImageProxy, " +
                    "got ${frame.platformFrame::class.java.simpleName}",
            )

        val started = System.nanoTime()
        // CameraX 1.3+ toBitmap() is a direct pixel copy when the ImageProxy's
        // underlying Image is RGBA_8888 (see CameraControllerImpl — we set
        // `OUTPUT_IMAGE_FORMAT_RGBA_8888` on the ImageAnalysis builder). The
        // returned Bitmap is guaranteed ARGB_8888 config, which is what
        // MediaPipe's BitmapImageBuilder requires.
        //
        // Historical bug: previously we wrapped `proxy.image` (raw YUV Image)
        // in MediaImageBuilder. MediaPipe Tasks Vision 0.10.14 internally
        // coerces to Bitmap and asserts ARGB_8888 → crash with
        // "android media image must use RGBA_8888 config".
        val bitmap = proxy.toBitmap()
        val mpImage = BitmapImageBuilder(bitmap).build()
        val options = ImageProcessingOptions.builder()
            .setRotationDegrees(frame.rotationDegrees)
            .build()
        val result: HandLandmarkerResult = landmarker.detect(mpImage, options)
        val elapsed = System.nanoTime() - started

        val hands = mutableListOf<HandLandmarks>()
        val landmarkLists = result.landmarks()
        val handednessLists = result.handedness()
        for (i in landmarkLists.indices) {
            val lm = landmarkLists[i]
            if (lm.size != 21) continue
            val flat = FloatArray(42)
            for (j in 0 until 21) {
                flat[j * 2] = lm[j].x() * frame.widthPx
                flat[j * 2 + 1] = lm[j].y() * frame.heightPx
            }
            val handedness = handednessLists.getOrNull(i)
                ?.firstOrNull()?.categoryName()?.let {
                    when (it) {
                        "Left" -> Handedness.Left
                        "Right" -> Handedness.Right
                        else -> Handedness.Unknown
                    }
                } ?: Handedness.Unknown
            hands.add(HandLandmarks(handedness = handedness, landmarks = flat))
        }
        return HandDetection(hands = hands, processingNanos = elapsed)
    }

    override fun close() {
        if (closed) return
        closed = true
        landmarker.close()
    }
}

actual object HandDetectorProvider {
    actual fun create(maxHands: Int): HandDetector = HandDetectorImpl(maxHands)
}
