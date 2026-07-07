package org.moashraf.sayva.camera

import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.moashraf.sayva.bootstrap.AndroidActivityProvider
import org.moashraf.sayva.bootstrap.AndroidAppContext
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * CameraX-backed [CameraController] for Android.
 *
 * Provides two use-cases bound to the current lifecycle owner:
 *   * [Preview] — surface displayed by [CameraPreview] (`androidMain` composable
 *     via `AndroidView` wrapping a `PreviewView`).
 *   * [ImageAnalysis] — YUV_420_888 frames delivered to a background
 *     executor; each becomes a [CameraFrame] emitted on [frames].
 *
 * ### Backpressure
 * The [ImageAnalysis] use-case is set to `STRATEGY_KEEP_ONLY_LATEST` — old
 * frames get dropped if analysis is slow. The [MutableSharedFlow] uses
 * `extraBufferCapacity=1` + `DROP_OLDEST` for the same reason on the flow
 * side. Together this means when downstream (MediaPipe + recognizer) is
 * slower than the camera's target frame rate, we skip frames instead of
 * queuing them — recognition on a stale frame is worse than recognition on
 * every third fresh one.
 */
internal class CameraControllerImpl : CameraController {

    private val analysisExecutor = Executors.newSingleThreadExecutor()

    /**
     * Backpressure channel — capacity 1, `DROP_OLDEST` semantics, and
     * `onUndeliveredElement` closes the dropped [ImageProxy] so CameraX
     * gets its frame buffer back.
     *
     * ### Why not a SharedFlow
     * `MutableSharedFlow<T>` with `DROP_OLDEST` silently drops the oldest
     * value with no callback — the dropped `ImageProxy` leaks and CameraX
     * stalls after a handful of them. `Channel(1, DROP_OLDEST, onUndeliveredElement)`
     * gives us the same effective backpressure with correct cleanup.
     */
    private val frameChannel = Channel<CameraFrame>(
        capacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        onUndeliveredElement = { droppedFrame ->
            (droppedFrame.platformFrame as? ImageProxy)?.close()
        },
    )
    private val flowScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    override val frames: SharedFlow<CameraFrame> = frameChannel
        .receiveAsFlow()
        .shareIn(
            scope = flowScope,
            started = SharingStarted.WhileSubscribed(replayExpirationMillis = 0),
            replay = 0,
        )

    private val _lens = MutableStateFlow(CameraLens.Front)
    override val lens: CameraLens get() = _lens.value

    // Torch state — updated from CameraX's LiveData whenever the platform
    // reports a change (user tap, battery override, or successful setTorch
    // call). Stays `false` when no camera is bound OR the current camera
    // has no flash unit; readers never need to distinguish the two.
    private val _torchEnabled = MutableStateFlow(false)
    override val torchEnabled: StateFlow<Boolean> = _torchEnabled.asStateFlow()

    // Cached from the bound Camera's cameraInfo. Front cameras generally
    // return false; back cameras generally true. We rebind on switchLens
    // so this stays current.
    @Volatile private var _hasTorch: Boolean = false
    override val hasTorch: Boolean get() = _hasTorch

    // Handle to the current CameraX Camera — needed for cameraControl
    // (torch, zoom, focus). Cleared in stop() so we don't leak a
    // reference across a permission or lens rebind cycle.
    private var boundCamera: Camera? = null
    private var torchStateObserver: Observer<Int>? = null

    // PreviewView is cached so start() can pull the same instance across
    // suspensions. Created lazily by `bindPreviewView()` from the Compose side.
    internal val previewView: PreviewView by lazy {
        PreviewView(AndroidAppContext.require()).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    private var previewUseCase: Preview? = null
    private var analysisUseCase: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null

    override suspend fun start() {
        val context = AndroidAppContext.require()
        val activity = AndroidActivityProvider.require()

        val provider = getProviderSuspending(context)
        cameraProvider = provider

        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            // Ask CameraX for RGBA_8888 frames directly. Two reasons:
            //   1. MediaPipe Tasks Vision (0.10.14+) internally coerces the
            //      input to a Bitmap for the HandLandmarker task and asserts
            //      `Bitmap.Config.ARGB_8888`. Feeding it YUV via
            //      `MediaImageBuilder(image)` crashes with
            //      "android media image must use RGBA_8888 config".
            //   2. `ImageProxy.toBitmap()` is a fast copy when the source is
            //      RGBA_8888; on YUV it goes through JPEG encode/decode which
            //      costs ~15ms per frame. We do enough MediaPipe work per
            //      frame already.
            // CameraX's internal conversion is hardware-accelerated on most
            // devices and adds ~1-2ms per frame.
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also { ia ->
                ia.setAnalyzer(analysisExecutor) { proxy ->
                    val frame = CameraFrame(
                        widthPx = proxy.width,
                        heightPx = proxy.height,
                        rotationDegrees = proxy.imageInfo.rotationDegrees,
                        timestampNanos = proxy.imageInfo.timestamp,
                        platformFrame = proxy,
                    )
                    // Every code path that fails to hand the frame to the
                    // pipeline closes the proxy here — `Channel.trySend`
                    // returns a failure result when the channel is closed
                    // or full without buffering (DROP_OLDEST does buffer +
                    // triggers onUndeliveredElement for the evicted one, so
                    // this path only fires when the channel is closed).
                    val result = frameChannel.trySend(frame)
                    if (!result.isSuccess) proxy.close()
                }
            }

        val selector = when (_lens.value) {
            CameraLens.Front -> CameraSelector.DEFAULT_FRONT_CAMERA
            CameraLens.Back -> CameraSelector.DEFAULT_BACK_CAMERA
        }

        val camera: Camera = withContext(Dispatchers.Main) {
            provider.unbindAll()
            detachTorchObserver()
            provider.bindToLifecycle(activity, selector, preview, analysis)
        }

        previewUseCase = preview
        analysisUseCase = analysis
        boundCamera = camera
        _hasTorch = camera.cameraInfo.hasFlashUnit()
        // Reflect the platform's real torch state — includes automatic
        // off when a lens without a flash gets bound. Observing avoids
        // us guessing after a rebind.
        withContext(Dispatchers.Main) {
            val observer = Observer<Int> { state ->
                _torchEnabled.value = state == TorchState.ON
            }
            camera.cameraInfo.torchState.observeForever(observer)
            torchStateObserver = observer
        }
    }

    override suspend fun stop() {
        withContext(Dispatchers.Main) {
            detachTorchObserver()
            cameraProvider?.unbindAll()
        }
        previewUseCase = null
        analysisUseCase = null
        boundCamera = null
        _hasTorch = false
        _torchEnabled.value = false
        // Note: analysisExecutor + flowScope live for the CameraController's
        // lifetime (Koin single). We do NOT shut down here so a subsequent
        // start() can rebind cleanly. When Koin one day supports scoped
        // singletons for this VM, tear them down in a proper close() hook.
    }

    private fun detachTorchObserver() {
        val observer = torchStateObserver ?: return
        boundCamera?.cameraInfo?.torchState?.removeObserver(observer)
        torchStateObserver = null
    }

    override suspend fun setTorchEnabled(enabled: Boolean) {
        val camera = boundCamera ?: return
        if (!camera.cameraInfo.hasFlashUnit()) return
        withContext(Dispatchers.Main) {
            // enableTorch returns a ListenableFuture — awaiting it here
            // lets the caller know when the platform has applied it.
            // We don't propagate the future's value because torchState
            // LiveData is the source of truth for readers.
            awaitFuture(camera.cameraControl.enableTorch(enabled))
        }
    }

    private suspend fun <T> awaitFuture(
        future: com.google.common.util.concurrent.ListenableFuture<T>,
    ): T = suspendCancellableCoroutine { cont ->
        future.addListener(
            {
                try {
                    cont.resume(future.get())
                } catch (t: Throwable) {
                    cont.resumeWith(Result.failure(t))
                }
            },
            ContextCompat.getMainExecutor(AndroidAppContext.require()),
        )
    }

    override suspend fun switchLens(lens: CameraLens) {
        if (_lens.value == lens) return
        _lens.value = lens
        // Rebind to apply the new selector.
        if (cameraProvider != null) start()
    }

    private suspend fun getProviderSuspending(
        context: android.content.Context,
    ): ProcessCameraProvider = suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            {
                try {
                    cont.resume(future.get())
                } catch (t: Throwable) {
                    Log.e("Camera", "ProcessCameraProvider init failed", t)
                    cont.resumeWith(Result.failure(t))
                }
            },
            ContextCompat.getMainExecutor(context),
        )
    }
}

actual object CameraControllerProvider {
    actual fun create(): CameraController = CameraControllerImpl()
}
