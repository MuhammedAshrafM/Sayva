package org.moashraf.sayva.ml.adapters

import org.moashraf.sayva.ml.ModelRuntime
import org.moashraf.sayva.ml.ModelRuntimeFactory

/**
 * TFLite [ModelRuntimeFactory]. The per-platform work — native lib linking,
 * Interpreter construction, tensor shape validation — lives in the
 * expect/actual [createTfliteModelRuntime] function. This class exists so
 * DI has a stable adapter object to register under [ID].
 *
 * Registered as `tflite`.
 *
 * ## When we add CoreML
 * Phase 2 lands `CoreMlRuntimeAdapter` sitting next to this file. Both
 * implement the same [ModelRuntimeFactory] port; DI registers them under
 * `tflite` and `coreml` respectively. On iOS, if a pack declares
 * `runtime.type: tflite` and this app doesn't ship the TFLite iOS adapter
 * (or has decided to drop it), the pack fails to load with a clear message
 * — no silent degradation.
 */
object TfliteRuntimeAdapter : ModelRuntimeFactory {
    const val ID: String = "tflite"

    override fun create(modelBytes: ByteArray): ModelRuntime =
        createTfliteModelRuntime(modelBytes)
}

/**
 * Platform-specific TFLite [ModelRuntime] constructor.
 *
 * Android: real TFLite Interpreter (`org.tensorflow:tensorflow-lite`).
 * iOS: currently a stub that throws (Track A explicitly deferred iOS
 * TFLite cinterop). Once Phase 2 lands the CoreML adapter, the iOS side
 * of packs that need TFLite either:
 *   * Gets a real cinterop wrapper against `TensorFlowLiteC.framework`, or
 *   * Refuses to load with the clear "requires runtime tflite (not available on iOS)" error.
 * That decision is per-Pack: if a Pack ships both a `.tflite` for Android
 * and a `.mlpackage` for iOS as separate model entries, the recognizer
 * factory picks the one whose runtime is supported on the current platform.
 */
expect fun createTfliteModelRuntime(modelBytes: ByteArray): ModelRuntime
