package org.moashraf.sayva.languagepack

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.moashraf.sayva.data.repository.SettingsRepository

/**
 * Top-level state manager for language-pack selection.
 *
 * Responsibilities:
 *   1. On [bootstrap], refresh the [LanguagePackRegistry] and pick the
 *      active pack from the persisted recognition code (or a sensible
 *      default for a fresh install).
 *   2. Expose a [state] StateFlow that ViewModels observe. During startup
 *      the state is [State.Loading]; on success it becomes [State.Ready];
 *      on any failure — a bundled pack that fails to parse, corrupt
 *      resources — it becomes [State.Error] with a diagnosable cause.
 *   3. Provide suspend methods for switching recognition / output
 *      languages. Every switch persists via [SettingsRepository] and
 *      re-emits [state].
 *
 * The controller does NOT own a [org.moashraf.sayva.ml.SignRecognizer].
 * The Koin-bound [SignRecognizerFactory] takes a pack + model ID and
 * builds one on demand — callers read [state] to know which pack to feed
 * the factory.
 *
 * ### Default output language resolution
 * On first run (no persisted output code), we prefer the pack's
 * [LanguagePack.defaultOutputLanguage] rather than trying to infer from
 * system locale. Locale-based fallback is an extra rule that pushes
 * complexity into the domain; Phase 2 will revisit if we get feedback
 * that Arabic-locale users don't want English by default even on the
 * ASL pack. Keeping the choice in-pack keeps the contract uniform.
 */
class LanguagePackController(
    private val registry: LanguagePackRegistry,
    private val settings: SettingsRepository,
    private val mvpDefaultRecognitionCode: String = MVP_DEFAULT_RECOGNITION_CODE,
) {

    sealed class State {
        /** Cold start — bundled packs haven't loaded yet. UI should show a splash / disable pack-dependent controls. */
        data object Loading : State()

        /** Registry loaded, active pack + output resolved. */
        data class Ready(
            val currentPack: LanguagePack,
            val outputLanguage: String,
            val availablePacks: List<LanguagePack>,
        ) : State() {
            val supportedOutputs: List<String> get() = currentPack.supportedOutputs

            /** Convenience for the Settings UI — one entry per installed pack. */
            data class PackSummary(
                val recognitionCode: String,
                val displayNameByOutput: Map<String, String>,
            )

            val packSummaries: List<PackSummary> = availablePacks.map { p ->
                PackSummary(
                    recognitionCode = p.recognitionCode,
                    displayNameByOutput = p.displayName,
                )
            }
        }

        /** Registry failed to produce a usable pack. Bundled-pack errors here are a build-time bug — file it. */
        data class Error(val cause: Throwable) : State()
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    /**
     * Populate the registry and resolve the persisted (or default) pack.
     * Call once at app start from a `LaunchedEffect(Unit)`. Idempotent —
     * calling again after [State.Ready] refreshes the pack list without
     * changing the current selection.
     */
    suspend fun bootstrap() {
        try {
            val packs = registry.refresh()
            if (packs.isEmpty()) {
                _state.value = State.Error(
                    IllegalStateException("No language packs installed. Bundled ASL pack should always be present.")
                )
                return
            }

            val storedCode = settings.state.value.recognitionLanguageCode
            val activePack = pickInitialPack(packs, storedCode)
            val activeOutput = pickInitialOutput(activePack, settings.state.value.outputLanguageCode)

            _state.value = State.Ready(
                currentPack = activePack,
                outputLanguage = activeOutput,
                availablePacks = packs,
            )
        } catch (e: Throwable) {
            _state.value = State.Error(e)
        }
    }

    /**
     * Switch to another installed pack. Persists the choice. If the current
     * output language is unsupported by the new pack, falls back to that
     * pack's [LanguagePack.defaultOutputLanguage].
     * @return `true` on success. `false` when the controller isn't Ready
     *   or [code] is unknown.
     */
    suspend fun switchRecognition(code: String): Boolean {
        val ready = _state.value as? State.Ready ?: return false
        val next = registry.byRecognitionCode(code) ?: return false
        if (next.recognitionCode == ready.currentPack.recognitionCode) return true

        val nextOutput = if (ready.outputLanguage in next.supportedOutputs) ready.outputLanguage
        else next.defaultOutputLanguage

        settings.setRecognitionLanguageCode(code)
        if (nextOutput != ready.outputLanguage) settings.setOutputLanguageCode(nextOutput)

        _state.value = ready.copy(currentPack = next, outputLanguage = nextOutput)
        return true
    }

    /**
     * Set the output language for the current pack. Must be one of the
     * pack's [LanguagePack.supportedOutputs].
     */
    suspend fun setOutputLanguage(code: String): Boolean {
        val ready = _state.value as? State.Ready ?: return false
        if (code !in ready.currentPack.supportedOutputs) return false
        if (code == ready.outputLanguage) return true

        settings.setOutputLanguageCode(code)
        _state.value = ready.copy(outputLanguage = code)
        return true
    }

    private fun pickInitialPack(
        packs: List<LanguagePack>,
        storedCode: String?,
    ): LanguagePack {
        if (storedCode != null) {
            val stored = packs.firstOrNull { it.recognitionCode == storedCode }
            if (stored != null) return stored
        }
        val default = packs.firstOrNull { it.recognitionCode == mvpDefaultRecognitionCode }
        return default ?: packs.first()
    }

    private fun pickInitialOutput(pack: LanguagePack, storedOutput: String?): String {
        if (storedOutput != null && storedOutput in pack.supportedOutputs) return storedOutput
        return pack.defaultOutputLanguage
    }

    companion object {
        /**
         * MVP recognition code — American Sign Language. Chosen because
         * public datasets (WLASL, MS-ASL, Kaggle ASL Alphabet) make it the
         * fastest first Pack to validate the pipeline. Once a user installs
         * additional Packs, their selection wins.
         *
         * This constant lives here — not scattered — so a future rename
         * (or a different "seed" pack chosen for a specific market) is a
         * one-line change with a single grep hit.
         */
        const val MVP_DEFAULT_RECOGNITION_CODE = "ase"
    }
}
