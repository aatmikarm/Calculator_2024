package com.aatmik.calculator.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.core.app.ActivityCompat
import java.util.Locale

class VoiceCalculatorManager(
    private val context: Context,
    private val onStateChanged: (VoiceState) -> Unit,
    private val onResultReceived: (expression: String, spokenText: String) -> Unit,
    private val onError: (errorMessage: String) -> Unit
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val recognizedParts = mutableListOf<String>()

    enum class VoiceState {
        IDLE,
        LISTENING,
        PROCESSING
    }

    // Number word mappings
    private val numberWords = mapOf(
        "zero" to "0", "one" to "1", "two" to "2", "three" to "3", "four" to "4",
        "five" to "5", "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9",
        "ten" to "10", "eleven" to "11", "twelve" to "12", "thirteen" to "13",
        "fourteen" to "14", "fifteen" to "15", "sixteen" to "16", "seventeen" to "17",
        "eighteen" to "18", "nineteen" to "19", "twenty" to "20", "thirty" to "30",
        "forty" to "40", "fifty" to "50", "sixty" to "60", "seventy" to "70",
        "eighty" to "80", "ninety" to "90", "hundred" to "100", "thousand" to "1000"
    )

    // Operator word mappings
    private val operatorWords = mapOf(
        "plus" to "+", "add" to "+", "and" to "+",
        "minus" to "-", "subtract" to "-", "less" to "-",
        "multiply" to "*", "times" to "*", "multiplied by" to "*",
        "divide" to "/", "divided by" to "/",
        "percent" to "%", "percentage" to "%",
        "point" to ".", "dot" to ".",
        "open bracket" to "(", "close bracket" to ")",
        "open parenthesis" to "(", "close parenthesis" to ")",
        "bracket open" to "(", "bracket close" to ")",
        "equals" to "=", "equal" to "=", "calculate" to "="
    )

    fun startListening() {
        if (!checkPermission()) {
            onError("Microphone permission required")
            return
        }

        if (isListening) {
            stopListening()
            return
        }

        recognizedParts.clear()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createRecognitionListener())
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
        }

        isListening = true
        onStateChanged(VoiceState.LISTENING)
        speechRecognizer?.startListening(intent)

        AnalyticsManager.log("voice_input_started")
    }

    fun stopListening() {
        if (!isListening) return

        isListening = false
        speechRecognizer?.stopListening()
        onStateChanged(VoiceState.PROCESSING)

        // Process the final result
        if (recognizedParts.isNotEmpty()) {
            processAndSendResult()
        }

        AnalyticsManager.log("voice_input_stopped", "parts_count" to recognizedParts.size.toString())
    }

    private fun processAndSendResult() {
        val fullText = recognizedParts.joinToString(" ")
        val expression = convertToMathExpression(fullText)

        if (expression.isNotEmpty()) {
            onResultReceived(expression, fullText)
        } else {
            //onError("Could not understand the calculation")
        }

        recognizedParts.clear()
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            // Speech recognizer is ready
        }

        override fun onBeginningOfSpeech() {
            // User started speaking
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Sound level changed - can be used for visual feedback
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // Audio buffer received
        }

        override fun onEndOfSpeech() {
            // User stopped speaking
            onStateChanged(VoiceState.PROCESSING)
        }

        override fun onError(error: Int) {
            isListening = false

            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                else -> "Recognition error"
            }

            onStateChanged(VoiceState.IDLE)

            // Only show error if we got an actual error, not just silence
            if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                onError(errorMessage)
            } else if (recognizedParts.isNotEmpty()) {
                // If we have partial results, process them
                processAndSendResult()
            } else {
                onError("No speech detected. Please try again.")
            }

            cleanup()

            AnalyticsManager.log("voice_input_error", "error_code" to error.toString())
        }

        override fun onResults(results: Bundle?) {
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                if (matches.isNotEmpty()) {
                    recognizedParts.add(matches[0])
                }
            }

            // Process the result when speech ends
            processAndSendResult()
            isListening = false
            onStateChanged(VoiceState.IDLE)
            cleanup()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                if (matches.isNotEmpty()) {
                    // Update UI with partial result
                    val partialText = matches[0]
                    val partialExpression = convertToMathExpression(partialText)

                    // Send partial update (you can handle this differently if needed)
                    onResultReceived(partialExpression, partialText)
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            // Future events
        }
    }

    private fun convertToMathExpression(spokenText: String): String {
        var text = spokenText.lowercase(Locale.getDefault())

        // Replace operator phrases first (longer phrases before shorter ones)
        operatorWords.entries.sortedByDescending { it.key.length }.forEach { (word, symbol) ->
            text = text.replace(word, " $symbol ")
        }

        // Replace number words
        numberWords.forEach { (word, number) ->
            text = text.replace("\\b$word\\b".toRegex(), number)
        }

        // Handle compound numbers (e.g., "twenty five" -> "25")
        text = handleCompoundNumbers(text)

        // Remove extra spaces and clean up
        text = text.replace(Regex("\\s+"), "")
            .replace("=", "") // Remove equals if present

        return text
    }

    private fun handleCompoundNumbers(text: String): String {
        var result = text

        // Handle patterns like "twenty five" -> "25"
        val compoundPattern = Regex("(\\d+)\\s+(\\d+)")
        while (compoundPattern.containsMatchIn(result)) {
            result = compoundPattern.replace(result) { matchResult ->
                val first = matchResult.groupValues[1].toIntOrNull() ?: 0
                val second = matchResult.groupValues[2].toIntOrNull() ?: 0

                // If first number is a multiple of 10 (20, 30, etc.) and second is less than 10
                if (first % 10 == 0 && second < 10) {
                    (first + second).toString()
                } else {
                    "${matchResult.groupValues[1]}${matchResult.groupValues[2]}"
                }
            }
        }

        return result
    }

    private fun checkPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun cleanup() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
        isListening = false
    }

    companion object {
        const val REQUEST_RECORD_AUDIO_PERMISSION = 200
    }
}