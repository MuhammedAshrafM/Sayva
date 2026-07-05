package org.moashraf.sayva.ml

/**
 * Fun-interface factory for [HandDetector] instances — bound in Koin as
 * a `single`. Wraps the platform-specific [HandDetectorProvider] so the
 * pipeline can constructor-inject a factory and swap it in tests without
 * knowing about `expect object`.
 *
 * A pack model's `input.maxHands` drives the argument — the pipeline
 * reads that from the manifest and never picks a value itself.
 */
fun interface HandDetectorFactory {
    fun create(maxHands: Int): HandDetector
}

/** Default factory that delegates to the platform [HandDetectorProvider]. */
class DefaultHandDetectorFactory : HandDetectorFactory {
    override fun create(maxHands: Int): HandDetector =
        HandDetectorProvider.create(maxHands)
}
