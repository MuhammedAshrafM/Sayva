package org.moashraf.sayva.ml

/**
 * A single-input, single-output ML runtime — the framework-agnostic port
 * every Language Pack uses for on-device inference.
 *
 * Adapters implementing this port (TFLite, CoreML, ONNX Runtime, PyTorch
 * Mobile, …) are registered by string ID under [ModelRuntimeRegistry].
 * Each pack's manifest declares which runtime ID it needs; the app looks
 * up the matching adapter or fails to load with a "requires app v N.M+"
 * message.
 *
 * Not thread-safe. One instance per active recognizer; callers serialize
 * access or hold a runtime per background dispatcher.
 */
interface ModelRuntime {
    /** Feed one input vector, return raw model output. */
    fun invoke(input: FloatArray): FloatArray

    /** Release native resources. Idempotent. */
    fun close()
}

/**
 * Builds a [ModelRuntime] from raw model bytes for one specific framework.
 * Adapters register one factory per framework — e.g. `TfliteRuntimeFactory`
 * for `"tflite"`, `CoreMlRuntimeFactory` for `"coreml"`.
 *
 * The factory itself is stateless and reusable across models. Each `create`
 * call produces a fresh [ModelRuntime] bound to one set of weights.
 */
fun interface ModelRuntimeFactory {
    /** Load the given bytes into a runtime. Caller closes when done. */
    fun create(modelBytes: ByteArray): ModelRuntime
}

/**
 * Maps runtime IDs → factories. Populated once at DI init with every
 * adapter this app version ships. A pack referring to an unknown ID
 * fails at recognizer construction with a clear error naming the ID.
 */
class ModelRuntimeRegistry(
    private val factories: Map<String, ModelRuntimeFactory>,
) {
    fun get(runtimeId: String): ModelRuntimeFactory =
        factories[runtimeId] ?: throw UnknownAdapterException(
            dimension = "runtime",
            requestedId = runtimeId,
            availableIds = factories.keys.sorted(),
        )

    /** Whether this app version can service `runtimeId`. Used by manifest validation. */
    fun supports(runtimeId: String): Boolean = runtimeId in factories
}

/** Thrown when a manifest names an adapter this app version doesn't ship. */
class UnknownAdapterException(
    val dimension: String,
    val requestedId: String,
    val availableIds: List<String>,
) : IllegalArgumentException(
    "Pack requires $dimension adapter '$requestedId' which this app version doesn't ship. " +
        "Available: $availableIds. Update the app to a version that lists this adapter.",
)
