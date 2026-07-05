package org.moashraf.sayva.speech

import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance

private val synthesizer = AVSpeechSynthesizer()

actual fun speakText(text: String) {
    synthesizer.speakUtterance(AVSpeechUtterance(string = text))
}
