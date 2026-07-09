package org.moashraf.sayva.languagepack

/**
 * Verify that the bytes each pack ships for its model files hash to the
 * SHA-256 the pack's own manifest declared. This gives a hard signal that
 * "the app is running the model the pack meant to ship" — no stale APK
 * assets, no manifest-vs-file drift, no on-disk corruption.
 *
 * The check runs once at [LanguagePackController.bootstrap] and produces
 * a [PackIntegrityReport] the controller logs. On mismatch the controller
 * continues loading (this is diagnostic, not gating) but the log line makes
 * the divergence findable in Logcat and Crashlytics breadcrumbs.
 *
 * Language- and model-neutral: every pack that ships an `integrity` block
 * on any model participates automatically. Older packs / downloaded packs
 * without the block are reported as `Skipped` — recorded, not treated as
 * failures.
 */
object PackIntegrityVerifier {

    suspend fun verify(pack: LanguagePack, loader: PackResourceLoader): PackIntegrityReport {
        val results = pack.models.map { model ->
            val declared = model.integrity
            if (declared == null) {
                ModelIntegrityResult.Skipped(
                    modelId = model.id,
                    modelFile = model.modelFile,
                    reason = "manifest has no integrity block",
                )
            } else {
                val bytes = runCatching { loader.readFile(pack.recognitionCode, model.modelFile) }
                    .getOrElse {
                        return@map ModelIntegrityResult.ReadError(
                            modelId = model.id,
                            modelFile = model.modelFile,
                            cause = it,
                        )
                    }
                val actualSha = Sha256Hasher.hex(bytes)
                val match = actualSha.equals(declared.sha256, ignoreCase = true) &&
                    bytes.size.toLong() == declared.sizeBytes
                ModelIntegrityResult.Checked(
                    modelId = model.id,
                    modelFile = model.modelFile,
                    declaredSha256 = declared.sha256,
                    actualSha256 = actualSha,
                    declaredSizeBytes = declared.sizeBytes,
                    actualSizeBytes = bytes.size.toLong(),
                    match = match,
                )
            }
        }
        return PackIntegrityReport(
            packCode = pack.recognitionCode,
            packVersion = pack.version,
            minAppVersion = pack.minAppVersion,
            schemaVersion = pack.schemaVersion,
            results = results,
        )
    }
}

data class PackIntegrityReport(
    val packCode: String,
    val packVersion: String,
    val minAppVersion: Int,
    val schemaVersion: Int,
    val results: List<ModelIntegrityResult>,
) {
    val allMatched: Boolean get() = results.all { it is ModelIntegrityResult.Checked && it.match }
    val anyMismatched: Boolean get() = results.any { it is ModelIntegrityResult.Checked && !it.match }
    val anyErrored: Boolean get() = results.any { it is ModelIntegrityResult.ReadError }
}

sealed class ModelIntegrityResult {
    abstract val modelId: String
    abstract val modelFile: String

    data class Checked(
        override val modelId: String,
        override val modelFile: String,
        val declaredSha256: String,
        val actualSha256: String,
        val declaredSizeBytes: Long,
        val actualSizeBytes: Long,
        val match: Boolean,
    ) : ModelIntegrityResult()

    data class Skipped(
        override val modelId: String,
        override val modelFile: String,
        val reason: String,
    ) : ModelIntegrityResult()

    data class ReadError(
        override val modelId: String,
        override val modelFile: String,
        val cause: Throwable,
    ) : ModelIntegrityResult()
}
