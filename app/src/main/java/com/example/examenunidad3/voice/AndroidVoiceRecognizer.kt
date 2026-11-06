package com.example.examenunidad3.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class AndroidVoiceRecognizer(
    context: Context,
    private val onListeningChanged: (Boolean) -> Unit,
    private val onTranscript: (String) -> Unit,
    private val onErrorMessage: (String) -> Unit
) : RecognitionListener {
    private val appContext = context.applicationContext
    private val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
        setRecognitionListener(this@AndroidVoiceRecognizer)
    }

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(appContext)

    fun start() {
        if (!isAvailable) {
            onErrorMessage("El reconocimiento de voz no está disponible en este dispositivo.")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-PE")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-PE")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Di un comando para la aplicación")
        }
        speechRecognizer.startListening(intent)
    }

    fun stop() {
        speechRecognizer.stopListening()
        onListeningChanged(false)
    }

    fun destroy() {
        speechRecognizer.destroy()
    }

    override fun onReadyForSpeech(params: Bundle?) = onListeningChanged(true)
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = onListeningChanged(false)

    override fun onError(error: Int) {
        onListeningChanged(false)
        onErrorMessage(error.toFriendlyMessage())
    }

    override fun onResults(results: Bundle?) {
        onListeningChanged(false)
        val transcript = results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
        if (transcript.isNullOrBlank()) {
            onErrorMessage("No pude reconocer el comando. Inténtalo nuevamente.")
        } else {
            onTranscript(transcript)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    private fun Int.toFriendlyMessage(): String = when (this) {
        SpeechRecognizer.ERROR_AUDIO -> "No se pudo capturar el audio."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Falta permiso para usar el micrófono."
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "No hay conexión para reconocer la voz."
        SpeechRecognizer.ERROR_NO_MATCH -> "No entendí el comando. Inténtalo nuevamente."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "El micrófono está ocupado. Espera un momento."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No escuché ningún comando."
        else -> "Ocurrió un error durante el reconocimiento de voz."
    }
}
