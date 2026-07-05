package org.moashraf.sayva.ml.adapters

import org.moashraf.sayva.ml.ModelRuntime
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Android [ModelRuntime] backed by `org.tensorflow.lite.Interpreter`.
 *
 * The model is loaded from a direct [ByteBuffer] — TFLite requires this;
 * a heap-backed buffer is rejected with `IllegalArgumentException`.
 *
 * ### Rank-aware input reshaping
 * Input tensor shape is inferred from the loaded model. Supported:
 *   * `[1, N]` — MLP models like the fingerspelling classifier.
 *   * `[1, T, D]` — sequence models like the temporal LSTM.
 * Callers pass a flat [FloatArray] regardless — this class packs it into
 * the nested-array shape TFLite's Interpreter expects.
 *
 * Output tensor shape must be `[1, N]` — a single-batch classifier head.
 * Adjust when we ship a model that returns sequences or multiple heads.
 */
private class TfliteInterpreterModelRuntime(modelBytes: ByteArray) : ModelRuntime {

    private val modelBuffer: ByteBuffer = ByteBuffer
        .allocateDirect(modelBytes.size)
        .order(ByteOrder.nativeOrder())
        .apply {
            put(modelBytes)
            rewind()
        }

    private val interpreter: Interpreter = Interpreter(modelBuffer)

    private val inputShape: IntArray = interpreter.getInputTensor(0).shape().also { s ->
        require(s.isNotEmpty() && s[0] == 1) {
            "TFLite input tensor must have batch dim 1, got shape ${s.toList()}"
        }
        require(s.size in 2..3) {
            "TFLite input rank must be 2 or 3 (got ${s.size}, shape ${s.toList()})"
        }
    }

    private val expectedInputElements: Int =
        inputShape.drop(1).fold(1) { acc, d -> acc * d }

    private val outputSize: Int = run {
        val shape = interpreter.getOutputTensor(0).shape()
        require(shape.size == 2 && shape[0] == 1) {
            "TFLite output tensor must be [1, N], got ${shape.toList()}"
        }
        shape[1]
    }

    override fun invoke(input: FloatArray): FloatArray {
        require(input.size == expectedInputElements) {
            "TFLite input expected $expectedInputElements floats (shape ${inputShape.toList()}), got ${input.size}"
        }
        val packedInput: Any = when (inputShape.size) {
            2 -> arrayOf(input)
            3 -> {
                val t = inputShape[1]
                val d = inputShape[2]
                Array(1) {
                    Array(t) { frameIdx ->
                        val offset = frameIdx * d
                        FloatArray(d) { j -> input[offset + j] }
                    }
                }
            }
            else -> error("Unreachable: input rank validated at load time")
        }
        val outputBatch = Array(1) { FloatArray(outputSize) }
        interpreter.run(packedInput, outputBatch)
        return outputBatch[0]
    }

    override fun close() {
        interpreter.close()
    }
}

actual fun createTfliteModelRuntime(modelBytes: ByteArray): ModelRuntime =
    TfliteInterpreterModelRuntime(modelBytes)
