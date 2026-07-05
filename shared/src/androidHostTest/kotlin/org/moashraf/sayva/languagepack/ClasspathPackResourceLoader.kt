package org.moashraf.sayva.languagepack

/**
 * A [PackResourceLoader] backed by the JVM test classpath. Used by the
 * androidHostTest suite; production code uses a Compose-Resources-backed
 * loader (added in the same batch as the Koin wiring).
 *
 * Pack layout expected on the classpath:
 *   language_packs/{code}/manifest.json
 *   language_packs/{code}/models/[modelId].tflite
 */
class ClasspathPackResourceLoader(
    private val bundledCodes: List<String>,
) : PackResourceLoader {

    override suspend fun availablePackCodes(): List<String> = bundledCodes

    override suspend fun readManifest(packCode: String): String {
        val path = "language_packs/$packCode/manifest.json"
        val stream = javaClass.classLoader?.getResourceAsStream(path)
            ?: error("Pack manifest not on classpath: $path")
        return stream.use { it.readBytes().decodeToString() }
    }

    override suspend fun readFile(packCode: String, relativePath: String): ByteArray {
        val path = "language_packs/$packCode/$relativePath"
        val stream = javaClass.classLoader?.getResourceAsStream(path)
            ?: error("Pack file not on classpath: $path")
        return stream.use { it.readBytes() }
    }
}
