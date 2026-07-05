package org.moashraf.sayva.speech

import android.content.Context
import android.speech.tts.TextToSpeech

/**
 * Holds the platform TTS engine. [init] must be called once with an app
 * context before [speakText] does anything - the app's entry point Activity
 * does this on startup.
 */
object AndroidSpeech {
    var engine: TextToSpeech? = null
        private set

    fun init(context: Context) {
        if (engine != null) return
        engine = TextToSpeech(context.applicationContext) {}
    }
}

actual fun speakText(text: String) {
    AndroidSpeech.engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sayva-utterance")
}
