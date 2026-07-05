package org.moashraf.sayva.ml.adapters

import org.moashraf.sayva.ml.ModelRuntime

/**
 * iOS TFLite [ModelRuntime] stub.
 *
 * TFLite's Swift/Obj-C runtime works on iOS but requires either CocoaPods
 * or a hand-managed cinterop def with the `TensorFlowLiteC.framework`
 * binary. That plumbing is deferred — the first iOS Pack that needs TFLite
 * will either add the cinterop wrapper OR the Pack manifest declares a
 * `coreml` runtime for the iOS variant of its model and the recognizer
 * factory picks whichever is supported per platform.
 *
 * A Pack that declares `runtime.type: tflite` on iOS today fails to load
 * with a clear "requires runtime 'tflite' — not supported on this platform"
 * message. That's the correct behavior — no silent degradation.
 */
actual fun createTfliteModelRuntime(modelBytes: ByteArray): ModelRuntime {
    throw NotImplementedError(
        "Runtime adapter 'tflite' is not implemented on iOS. Pack manifests targeting " +
            "iOS should declare a CoreML runtime variant of their model."
    )
}
