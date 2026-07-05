package org.moashraf.sayva.languagepack

import org.moashraf.sayva.ml.ComposedSignRecognizer
import org.moashraf.sayva.ml.ModelRuntimeRegistry
import org.moashraf.sayva.ml.PostprocessorRegistry
import org.moashraf.sayva.ml.PreprocessorRegistry
import org.moashraf.sayva.ml.SignRecognizer

/**
 * Builds a [SignRecognizer] for one specific model inside a pack by
 * composing the four ML adapters the pack's manifest names:
 *   * `runtime.type` → [ModelRuntimeRegistry]
 *   * `input.preprocessing` → [PreprocessorRegistry]
 *   * `output.postprocessing` → [PostprocessorRegistry]
 *   * `inferenceStrategy` → currently only `single_frame` — future
 *     values (sliding-window, streaming) will pick different composer
 *     paths, added here without touching callers.
 *
 * Concrete implementation is in [DefaultSignRecognizerFactory] — bound
 * as a `single` in Koin. ViewModels observe [LanguagePackController.state]
 * and call the factory whenever they need a recognizer instance; they do
 * not cache across pack switches.
 */
fun interface SignRecognizerFactory {
    /**
     * Build a recognizer for the model with [modelId] inside [pack].
     * Every call constructs a fresh recognizer + underlying native
     * runtime — caller is responsible for [SignRecognizer.close].
     *
     * @throws IllegalArgumentException if the pack has no such model
     * @throws org.moashraf.sayva.ml.UnknownAdapterException if any of
     *   the manifest's adapter IDs is unknown to this app version
     */
    suspend fun forModel(pack: LanguagePack, modelId: String): SignRecognizer
}

class DefaultSignRecognizerFactory(
    private val loader: PackResourceLoader,
    private val runtimeRegistry: ModelRuntimeRegistry,
    private val preprocessorRegistry: PreprocessorRegistry,
    private val postprocessorRegistry: PostprocessorRegistry,
) : SignRecognizerFactory {

    override suspend fun forModel(pack: LanguagePack, modelId: String): SignRecognizer {
        val model = pack.modelById(modelId)
            ?: throw IllegalArgumentException(
                "Pack '${pack.recognitionCode}' has no model '$modelId'. " +
                    "Declared: ${pack.models.map { it.id }}"
            )

        // Only `single_frame` is implemented; sliding-window etc. land in
        // Phase 2 where the camera pipeline needs a stateful strategy.
        require(model.inferenceStrategy == "single_frame") {
            "Pack '${pack.recognitionCode}' model '${model.id}' declares inference " +
                "strategy '${model.inferenceStrategy}' — this app version only ships " +
                "'single_frame'. Update the app or use a different model."
        }

        val runtimeFactory = runtimeRegistry.get(model.runtimeType)
        val preprocessor = preprocessorRegistry.get(model.input.preprocessing)
        val postprocessor = postprocessorRegistry.get(model.output.postprocessing)

        val bytes = loader.readFile(pack.recognitionCode, model.modelFile)
        val runtime = runtimeFactory.create(bytes)
        return ComposedSignRecognizer(
            runtime = runtime,
            preprocessor = preprocessor,
            postprocessor = postprocessor,
            vocabulary = model.vocabulary,
            expectedInputElements = model.expectedInputElements,
        )
    }
}
