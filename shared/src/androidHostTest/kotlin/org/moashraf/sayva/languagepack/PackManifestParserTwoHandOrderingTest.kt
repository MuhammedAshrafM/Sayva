package org.moashraf.sayva.languagepack

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Contract tests for the `twoHandOrdering` manifest field.
 *
 * Every two-hand model MUST declare its ordering — the pipeline needs the
 * pack's contract to route MediaPipe detections into the model's input
 * slots. Single-hand models MUST NOT declare it; a stray value there
 * signals a copy-paste error worth surfacing early.
 */
class PackManifestParserTwoHandOrderingTest {

    @Test
    fun `single-hand model without ordering parses with null ordering`() {
        val pack = PackManifestParser.parse(baseManifest(inputBlock = SINGLE_HAND_INPUT))
        assertNull(pack.models.first().input.twoHandOrdering)
    }

    @Test
    fun `two-hand model with left_right ordering parses to LeftRight enum`() {
        val pack = PackManifestParser.parse(
            baseManifest(inputBlock = twoHandInput("left_right")),
        )
        assertEquals(TwoHandOrdering.LeftRight, pack.models.first().input.twoHandOrdering)
    }

    @Test
    fun `two-hand model with right_left ordering parses to RightLeft enum`() {
        val pack = PackManifestParser.parse(
            baseManifest(inputBlock = twoHandInput("right_left")),
        )
        assertEquals(TwoHandOrdering.RightLeft, pack.models.first().input.twoHandOrdering)
    }

    @Test
    fun `two-hand model without ordering is rejected with actionable message`() {
        val err = runCatching {
            PackManifestParser.parse(
                baseManifest(inputBlock = twoHandInput(orderingLine = null)),
            )
        }.exceptionOrNull() ?: fail("parser should have rejected the manifest")
        val msg = requireNotNull(err.message)
        assertTrue("must declare 'twoHandOrdering'" in msg, "unexpected message: $msg")
    }

    @Test
    fun `single-hand model with ordering set is rejected`() {
        val err = runCatching {
            PackManifestParser.parse(
                baseManifest(inputBlock = SINGLE_HAND_INPUT_WITH_ORDERING),
            )
        }.exceptionOrNull() ?: fail("parser should have rejected the manifest")
        val msg = requireNotNull(err.message)
        assertTrue(
            "only valid for two-hand models" in msg,
            "unexpected message: $msg",
        )
    }

    @Test
    fun `unknown ordering value is rejected with the list of allowed values`() {
        val err = runCatching {
            PackManifestParser.parse(
                baseManifest(inputBlock = twoHandInput("random_string")),
            )
        }.exceptionOrNull() ?: fail("parser should have rejected the manifest")
        val msg = requireNotNull(err.message)
        assertTrue("left_right" in msg, "unexpected message: $msg")
        assertTrue("first_seen" in msg, "unexpected message: $msg")
    }

    private companion object {
        val SINGLE_HAND_INPUT = """
            "input": {
              "shape": [1, 42],
              "preprocessing": "single_hand_kazuhito_v1",
              "maxHands": 1
            }
        """.trimIndent()

        val SINGLE_HAND_INPUT_WITH_ORDERING = """
            "input": {
              "shape": [1, 42],
              "preprocessing": "single_hand_kazuhito_v1",
              "maxHands": 1,
              "twoHandOrdering": "left_right"
            }
        """.trimIndent()

        fun twoHandInput(orderingLine: String?): String {
            val orderingField = if (orderingLine == null) "" else
                ",\n              \"twoHandOrdering\": \"$orderingLine\""
            return """
                "input": {
                  "shape": [1, 84],
                  "preprocessing": "two_hand_v1",
                  "maxHands": 2$orderingField
                }
            """.trimIndent()
        }
    }

    /**
     * Minimal-but-valid manifest JSON with a single model whose `input` block
     * is substituted in. Every other required field is stubbed with a value
     * the parser accepts — we're testing input-block validation only.
     */
    private fun baseManifest(inputBlock: String): String = """
        {
          "schemaVersion": 1,
          "recognitionCode": "test",
          "displayName": { "en": "Test" },
          "version": "0.1.0",
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
              $inputBlock,
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
