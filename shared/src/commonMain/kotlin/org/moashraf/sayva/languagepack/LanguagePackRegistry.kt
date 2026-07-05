package org.moashraf.sayva.languagepack

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Loads a pack's model bytes and manifest JSON. Bundled packs read from
 * Compose resources; downloaded packs will read from internal storage
 * (Phase 4). The Registry doesn't care which source produced them.
 */
interface PackResourceLoader {
    /** Bundled + downloaded pack codes discoverable at startup. */
    suspend fun availablePackCodes(): List<String>

    /** Read the entire `manifest.json` bytes for one pack. */
    suspend fun readManifest(packCode: String): String

    /** Read a file inside a pack (e.g. `models/fingerspelling.tflite`). */
    suspend fun readFile(packCode: String, relativePath: String): ByteArray
}

/**
 * The set of packs installed on this device (bundled + downloaded). Read-only
 * — mutations go through [LanguagePackInstaller] in Phase 4.
 */
interface LanguagePackRegistry {
    val installed: StateFlow<List<LanguagePack>>
    fun byRecognitionCode(code: String): LanguagePack?
    suspend fun refresh(): List<LanguagePack>
}

/**
 * Default implementation that walks [PackResourceLoader.availablePackCodes]
 * and parses each `manifest.json` via [PackManifestParser].
 *
 * Any pack that fails to parse is dropped with a callback to [onLoadError] —
 * we do not silently swallow errors, but one broken downloaded pack shouldn't
 * take down the entire app. Bundled packs failing to parse IS a bug (should
 * be caught at build time by `verifyPacks`).
 */
class DefaultLanguagePackRegistry(
    private val loader: PackResourceLoader,
    private val onLoadError: (packCode: String, cause: Throwable) -> Unit = { _, _ -> },
) : LanguagePackRegistry {

    private val _installed = MutableStateFlow<List<LanguagePack>>(emptyList())
    override val installed: StateFlow<List<LanguagePack>> = _installed.asStateFlow()

    override fun byRecognitionCode(code: String): LanguagePack? =
        _installed.value.firstOrNull { it.recognitionCode == code }

    override suspend fun refresh(): List<LanguagePack> {
        val codes = loader.availablePackCodes()
        val packs = mutableListOf<LanguagePack>()
        for (code in codes) {
            val pack = runCatching {
                val manifest = loader.readManifest(code)
                PackManifestParser.parse(manifest)
            }.getOrElse {
                onLoadError(code, it)
                null
            }
            if (pack != null) packs.add(pack)
        }
        _installed.value = packs
        return packs
    }
}
