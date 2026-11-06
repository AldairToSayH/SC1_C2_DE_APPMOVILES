package com.example.examenunidad3.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class AndroidVoiceSpeaker(context: Context) : TextToSpeech.OnInitListener {
    private val textToSpeech = TextToSpeech(context.applicationContext, this)
    private var ready = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            val peruSpanish = Locale.forLanguageTag("es-PE")
            val result = textToSpeech.setLanguage(peruSpanish)
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                textToSpeech.language = Locale.forLanguageTag("es")
            }
        }
    }

    fun speak(message: String) {
        if (ready && message.isNotBlank()) {
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, "voice-command")
        }
    }

    fun shutdown() {
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
}
