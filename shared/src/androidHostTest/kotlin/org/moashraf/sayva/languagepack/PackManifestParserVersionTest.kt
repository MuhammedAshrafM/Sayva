package org.moashraf.sayva.languagepack

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Contract tests for the `version` field's SemVer constraint.
 *
 * The Python-side generator enforces this at build time (see
 * `ml/src/sayva_ml/packs/manifest.py`). This suite provides the same guard
 * on the runtime parser — defense in depth for a distributed manifest that
 * a downloaded pack installer might otherwise ship with a broken value.
 */
class PackManifestParserVersionTest {

    @Test
    fun `valid SemVer parses`() {
        val pack = PackManifestParser.parse(manifest("1.2.3"))
        assertEquals("1.2.3", pack.version)
    }

    @Test
    fun `prerelease and build metadata parse`() {
        assertEquals("0.2.3-beta.1", PackManifestParser.parse(manifest("0.2.3-beta.1")).version)
        assertEquals("1.0.0+build.7", PackManifestParser.parse(manifest("1.0.0+build.7")).version)
    }

    @Test
    fun `empty version rejected with actionable message`() {
        val err = runCatching { PackManifestParser.parse(manifest("")) }
            .exceptionOrNull() ?: fail("empty version should reject")
        assertTrue("non-empty" in (err.message ?: ""), "unexpected: ${err.message}")
    }

    @Test
    fun `whitespace-only version rejected as empty`() {
        val err = runCatching { PackManifestParser.parse(manifest("   ")) }
            .exceptionOrNull() ?: fail("whitespace version should reject")
        assertTrue("non-empty" in (err.message ?: ""), "unexpected: ${err.message}")
    }

    @Test
    fun `MAJOR-only version rejected`() {
        val err = runCatching { PackManifestParser.parse(manifest("1")) }
            .exceptionOrNull() ?: fail("MAJOR-only should reject")
        assertTrue("not valid SemVer" in (err.message ?: ""), "unexpected: ${err.message}")
    }

    @Test
    fun `MAJOR-MINOR-only version rejected`() {
        val err = runCatching { PackManifestParser.parse(manifest("1.0")) }
            .exceptionOrNull() ?: fail("MAJOR.MINOR-only should reject")
        assertTrue("not valid SemVer" in (err.message ?: ""), "unexpected: ${err.message}")
    }

    @Test
    fun `leading v version rejected`() {
        val err = runCatching { PackManifestParser.parse(manifest("v1.0.0")) }
            .exceptionOrNull() ?: fail("'v1.0.0' should reject")
        assertTrue("not valid SemVer" in (err.message ?: ""), "unexpected: ${err.message}")
    }

    private fun manifest(version: String): String = """
        {
          "schemaVersion": 1,
          "recognitionCode": "test",
          "displayName": { "en": "Test" },
          "version": "$version",
          "minAppVersion": 1,
          "bundled": true,
          "models": [
            {
              "id": "m1",
              "role": "fingerspelling",
              "architecture": "mlp",
              "modelFile": "models/m1.tflite",
              "runtime": { "type": "tflite" },
              "inferenceStrategy": "single_frame",
              "input": {
                "shape": [1, 42],
                "preprocessing": "single_hand_kazuhito_v1",
                "maxHands": 1
              },
              "output": { "shape": [1, 1], "postprocessing": "argmax_confidence_v1" },
              "confidenceThresholds": { "show": 0.9, "caution": 0.6 },
              "vocabulary": { "version": 1, "signs": [{ "id": "A", "tags": [] }] }
            }
          ],
          "supportedOutputs": ["en"],
          "outputLanguageStatus": { "en": "complete" },
          "defaultOutputLanguage": "en",
          "outputLabels": { "en": { "m1": { "A": "A" } } },
          "ttsLocaleByOutput": { "en": "en-US" },
          "postProcessing": {
            "spellOutBlankTimeoutMs": 700,
            "sentenceAssemblyRuleset": "standard_v1",
            "capitalization": "sentence_case"
          }
        }
    """.trimIndent()
}
