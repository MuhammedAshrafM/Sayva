@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.moashraf.sayva.languagepack

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.posix.uint8_tVar

actual object Sha256Hasher {
    actual fun hex(bytes: ByteArray): String {
        if (bytes.isEmpty()) {
            // CC_SHA256 accepts null when len=0, but that's a UB path via
            // Kotlin/Native; short-circuit to the known-empty digest.
            return "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        }
        val digest = ByteArray(CC_SHA256_DIGEST_LENGTH)
        bytes.usePinned { inPin ->
            digest.usePinned { outPin ->
                CC_SHA256(
                    inPin.addressOf(0),
                    bytes.size.toUInt(),
                    outPin.addressOf(0).reinterpret(),
                )
            }
        }
        val chars = CharArray(digest.size * 2)
        for (i in digest.indices) {
            val v = digest[i].toInt() and 0xFF
            chars[i * 2] = HEX_DIGITS[v ushr 4]
            chars[i * 2 + 1] = HEX_DIGITS[v and 0x0F]
        }
        return String(chars)
    }

    private val HEX_DIGITS = "0123456789abcdef".toCharArray()

    // Needed because `usePinned.addressOf(0).reinterpret()` needs a target;
    // CC_SHA256 signs its output as `uint8_t*`. The extension lives here to
    // keep the Kotlin/Native cinterop noise out of the main function.
    private inline fun <reified T : Any> kotlinx.cinterop.CPointer<*>.reinterpret(): kotlinx.cinterop.CPointer<uint8_tVar> =
        @Suppress("UNCHECKED_CAST")
        this as kotlinx.cinterop.CPointer<uint8_tVar>
}
