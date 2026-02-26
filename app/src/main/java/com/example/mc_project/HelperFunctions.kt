package com.example.mc_project

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

enum class ThemeMode { SYSTEM, SENSOR }

// Used to taken time and date from the image name
fun formatTimeFromFile(path: String): String {
    return try {
        val fileName = File(path).name

        val raw = fileName.removePrefix("IMG_").substringBefore(".")

        val parser = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val date = parser.parse(raw)

        val output = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
        output.format(date!!)
    } catch (e: Exception) {
        ""
    }
}

//
data class SttUiState(
    val isListening: Boolean = false,
    val partial: String = "",
    val final: String = "",
    val error: String? = null
)


// online sources and android speech documentation was used
class SpeechToTextController(
    private val context: Context,
    private val onState: (SttUiState) -> Unit
) {
    private var recognizer: SpeechRecognizer? = null
    private var state = SttUiState()

    fun start() {
        if (state.isListening) return

        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(listener)
            }
        }

        state = state.copy(isListening = true, partial = "", final = "", error = null)
        onState(state)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
        }

        recognizer?.startListening(intent)
    }

    fun stop() {
        if (!state.isListening) return
        recognizer?.stopListening()
        state = state.copy(isListening = false)
        onState(state)
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }


    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        override fun onError(error: Int) {
            state = state.copy(isListening = false, error = "Speech error code: $error")
            onState(state)
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()

            state = state.copy(isListening = false, final = text, partial = "")
            onState(state)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()

            state = state.copy(partial = text)
            onState(state)
        }

    }
}