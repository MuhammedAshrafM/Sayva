package org.moashraf.sayva.languagepack

import java.security.MessageDigest

actual object Sha256Hasher {
    actual fun hex(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        // Uppercase-avoiding formatter — a straight char lookup is faster than
        // repeated `%02x.format(...)` calls for small inputs like a 7 KB model.
        val chars = CharArray(digest.size * 2)
        for (i in digest.indices) {
            val v = digest[i].toInt() and 0xFF
            chars[i * 2] = HEX_DIGITS[v ushr 4]
            chars[i * 2 + 1] = HEX_DIGITS[v and 0x0F]
        }
        return String(chars)
    }

    private val HEX_DIGITS = "0123456789abcdef".toCharArray()
}
