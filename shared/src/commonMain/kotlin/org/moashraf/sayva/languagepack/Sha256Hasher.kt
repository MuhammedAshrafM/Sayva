package org.moashraf.sayva.languagepack

/**
 * Cross-platform SHA-256 for pack integrity verification. Kept as a
 * dedicated port so the runtime can hash small model files without
 * pulling in a JVM crypto shim on iOS.
 *
 * Returned digests are lowercase hex, 64 characters. Callers compare
 * case-insensitively against the manifest's declared value.
 */
expect object Sha256Hasher {
    fun hex(bytes: ByteArray): String
}
