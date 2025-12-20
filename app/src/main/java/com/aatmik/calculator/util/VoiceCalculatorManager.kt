package com.aatmik.calculator.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
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

    // Operator word mappings - COMPREHENSIVE
    private val operatorWords = mapOf(
        // ADDITION
        "plus" to "+",
        "add" to "+",
        "and" to "+",
        "added to" to "+",
        "sum" to "+",
        "total" to "+",

        // SUBTRACTION
        "minus" to "-",
        "subtract" to "-",
        "less" to "-",
        "take away" to "-",
        "difference" to "-",
        "remove" to "-",

        // MULTIPLICATION - USE × (not *)
        "multiply" to "×",
        "times" to "×",
        "multiplied by" to "×",
        "into" to "×",
        "product" to "×",
        "by" to "×",
        "of" to "×",
        "multiply by" to "×",
        "cross" to "×",
        "x" to "×",
        "*" to "×",

        // DIVISION - USE ÷ (not /)
        "divide" to "÷",
        "divided by" to "÷",
        "divide by" to "÷",
        "over" to "÷",
        "slash" to "÷",
        "upon" to "÷",
        "per" to "÷",
        "/" to "÷",

        // PERCENTAGE
        "percent" to "%",
        "percentage" to "%",
        "mod" to "%",

        // DECIMAL
        "point" to ".",
        "dot" to ".",
        "decimal" to ".",

        // BRACKETS
        "open bracket" to "(",
        "close bracket" to ")",
        "open parenthesis" to "(",
        "close parenthesis" to ")",
        "bracket open" to "(",
        "bracket close" to ")",
        "left bracket" to "(",
        "right bracket" to ")",

        // EQUALS
        "equals" to "=",
        "equal" to "=",
        "is" to "=",
        "calculate" to "="
    )

    companion object {
        private const val TAG = "VoiceCalc"
        const val REQUEST_RECORD_AUDIO_PERMISSION = 200
    }

    fun startListening() {
        // **NEW: Check if voice input is enabled in Remote Config**
        if (!FirebaseConfigManager.isVoiceInputEnabled()) {
            onError("Voice input is currently disabled")
            Log.d(TAG, "Voice input blocked - disabled in Remote Config")
            return
        }

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

        Log.d(TAG, "Started listening")
        AnalyticsManager.log("voice_input_started")
    }

    fun stopListening() {
        if (!isListening) return

        isListening = false
        speechRecognizer?.stopListening()
        onStateChanged(VoiceState.PROCESSING)

        if (recognizedParts.isNotEmpty()) {
            processAndSendResult()
        }

        Log.d(TAG, "Stopped listening")
        AnalyticsManager.log("voice_input_stopped", "parts_count" to recognizedParts.size.toString())
    }

    private fun processAndSendResult() {
        val fullText = recognizedParts.joinToString(" ")
        val expression = convertToMathExpression(fullText)

        Log.d(TAG, "Raw speech: $fullText")
        Log.d(TAG, "Converted expression: $expression")

        if (expression.isNotEmpty()) {
            onResultReceived(expression, fullText)
        }

        recognizedParts.clear()
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
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

            Log.e(TAG, "Speech error: $errorMessage (code: $error)")

            onStateChanged(VoiceState.IDLE)

            if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                onError(errorMessage)
            } else if (recognizedParts.isNotEmpty()) {
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
                    Log.d(TAG, "Speech recognized: ${matches[0]}")
                }
            }

            processAndSendResult()
            isListening = false
            onStateChanged(VoiceState.IDLE)
            cleanup()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                if (matches.isNotEmpty()) {
                    val partialText = matches[0]
                    val partialExpression = convertToMathExpression(partialText)
                    Log.d(TAG, "Partial: $partialText -> $partialExpression")
                    onResultReceived(partialExpression, partialText)
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun convertToMathExpression(spokenText: String): String {
        var text = spokenText.lowercase(Locale.getDefault())
        Log.d(TAG, "Step 1 (lowercase): $text")

        // Replace operator phrases first (longer phrases before shorter ones)
        operatorWords.entries.sortedByDescending { it.key.length }.forEach { (word, symbol) ->
            if (text.contains(word)) {
                text = text.replace(word, " $symbol ")
                Log.d(TAG, "Replaced '$word' with '$symbol': $text")
            }
        }

        // Replace number words
        numberWords.forEach { (word, number) ->
            text = text.replace("\\b$word\\b".toRegex(), number)
        }
        Log.d(TAG, "Step 2 (numbers replaced): $text")

        // Handle compound numbers
        text = handleCompoundNumbers(text)
        Log.d(TAG, "Step 3 (compound numbers): $text")

        // Remove extra spaces and clean up
        text = text.replace(Regex("\\s+"), "")
            .replace("=", "")
        Log.d(TAG, "Step 4 (final): $text")

        return text
    }

    private fun handleCompoundNumbers(text: String): String {
        var result = text

        val compoundPattern = Regex("(\\d+)\\s+(\\d+)")
        while (compoundPattern.containsMatchIn(result)) {
            result = compoundPattern.replace(result) { matchResult ->
                val first = matchResult.groupValues[1].toIntOrNull() ?: 0
                val second = matchResult.groupValues[2].toIntOrNull() ?: 0

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
            Log.e(TAG, "Cleanup error: ${e.message}")
        }
        isListening = false
    }
}
//```
//
//## Key Logs Added:
//
//1. **Speech recognized**: Shows what Google heard
//2. **Raw speech**: Original spoken text
//3. **Converted expression**: Final math expression
//4. **Step-by-step conversion**: Shows each transformation
//5. **Operator replacements**: Shows when × and ÷ are inserted
//
//## How to Test:
//
//1. Say "five times three"
//2. Check Logcat for `VoiceCalc` tag
//3. You'll see:
//```
//Speech recognized: five times three
//Step 1 (lowercase): five times three
//Replaced 'times' with '×': five × three
//Step 2 (numbers replaced): 5 × 3
//Step 4 (final): 5×3
//Converted expression: 5×3