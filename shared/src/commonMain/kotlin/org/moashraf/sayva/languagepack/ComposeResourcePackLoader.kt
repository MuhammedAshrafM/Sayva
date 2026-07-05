package org.moashraf.sayva.languagepack

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.ExperimentalResourceApi
import sayva.shared.generated.resources.Res

/**
 * [PackResourceLoader] backed by Compose Multiplatform resources — the
 * production path for **bundled** packs on both Android and iOS.
 *
 * Downloaded packs (Phase 4) get their own `FileSystemPackResourceLoader`
 * over `context.filesDir` (Android) / `NSFileManager.URLsForDirectory`
 * (iOS). Both implement the same [PackResourceLoader] interface, so
 * [DefaultLanguagePackRegistry] can consume the union without caring where
 * a pack came from.
 *
 * Bundled packs are enumerated by reading
 * `files/language_packs/index.json` — a manifest emitted by
 * `ml/scripts/generate_pack.py`. Compose Resources doesn't expose a directory
 * listing API, so the index is how we discover what shipped without
 * hardcoding pack codes in Kotlin.
 */
@OptIn(ExperimentalResourceApi::class)
class ComposeResourcePackLoader : PackResourceLoader {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun availablePackCodes(): List<String> {
        val bytes = Res.readBytes("files/language_packs/index.json")
        val root = json.parseToJsonElement(bytes.decodeToString()).jsonObject
        val bundled = root["bundled"]?.jsonArray ?: return emptyList()
        return bundled.mapNotNull { entry ->
            entry.jsonObject["recognitionCode"]?.jsonPrimitive?.content
        }
    }

    override suspend fun readManifest(packCode: String): String {
        val bytes = Res.readBytes("files/language_packs/$packCode/manifest.json")
        return bytes.decodeToString()
    }

    override suspend fun readFile(packCode: String, relativePath: String): ByteArray {
        return Res.readBytes("files/language_packs/$packCode/$relativePath")
    }
}
