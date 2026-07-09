package org.moashraf.sayva.languagepack

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Known-answer test — pins the hex format + confirms `MessageDigest.getInstance("SHA-256")`
 * produces the same digest the CLI + Python's `hashlib.sha256` do for well-known inputs.
 *
 * Guards against a byte-order or lookup-table mistake in the hex encoder in
 * [Sha256Hasher]. If this drifts, the pack integrity verifier's SHA will not
 * match what `generate_pack.py` recorded — that would surface as a spurious
 * mismatch log line at every app start.
 */
class Sha256HasherTest {

    @Test
    fun `empty input hashes to the standard SHA-256 empty digest`() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            Sha256Hasher.hex(ByteArray(0)),
        )
    }

    @Test
    fun `ascii 'abc' hashes to the NIST test vector`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            Sha256Hasher.hex("abc".encodeToByteArray()),
        )
    }

    @Test
    fun `output is always lowercase 64-char hex`() {
        val digest = Sha256Hasher.hex("sayva".encodeToByteArray())
        assertEquals(64, digest.length)
        assertEquals(digest, digest.lowercase())
        for (c in digest) {
            assertEquals(true, c in '0'..'9' || c in 'a'..'f',
                "Non-hex char '$c' in digest '$digest'")
        }
    }
}
