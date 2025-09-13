package com.aatmik.calculator.fragment

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentLightningCalculatorBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

class LightningCalculatorFragment : Fragment() {

    private lateinit var binding: FragmentLightningCalculatorBinding
    private var isCalculating = false
    private val lightningTimes = mutableListOf<Long>()
    private val lightningIntervals = mutableListOf<Double>()

    // Pattern types for lightning prediction
    private val patternTypes = arrayOf(
        "Average Interval",
        "Trending Pattern",
        "Shortest Interval",
        "Most Common Interval"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentLightningCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()
        setupListeners()
        setupTextWatchers()
        updatePrediction()
    }

    private fun setupSpinners() {
        binding.apply {
            // Setup Pattern Type Spinner
            val patternAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                patternTypes
            )
            patternAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerPatternType.adapter = patternAdapter
        }
    }

    private fun setupListeners() {
        binding.apply {
            // Handle back button press
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            // Share button
            shareBt.setOnClickListener {
                sharePrediction()
            }

            // Add Lightning Time button
            btAddLightning.setOnClickListener {
                addLightningTime()
            }

            // Clear All button
            btClearAll.setOnClickListener {
                clearAllData()
            }

            // Manual time entry button
            btAddManualTime.setOnClickListener {
                addManualTime()
            }

            // Spinner listener
            spinnerPatternType.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    updatePrediction()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })
        }
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isCalculating) {
                    updatePrediction()
                }
            }
        }

        binding.apply {
            etCameraSettings.addTextChangedListener(textWatcher)
        }
    }

    private fun addLightningTime() {
        val currentTime = System.currentTimeMillis()
        lightningTimes.add(currentTime)

        // Calculate interval if we have previous times
        if (lightningTimes.size > 1) {
            val interval = (currentTime - lightningTimes[lightningTimes.size - 2]) / 1000.0
            lightningIntervals.add(interval)
        }

        updateLightningList()
        updatePrediction()

        Toast.makeText(requireContext(), "Lightning #${lightningTimes.size} recorded", Toast.LENGTH_SHORT).show()
    }

    private fun addManualTime() {
        val builder = AlertDialog.Builder(requireContext())
        val input = android.widget.EditText(requireContext())
        input.hint = "Enter seconds since last lightning"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        builder.setTitle("Add Manual Interval")
            .setMessage("Enter the number of seconds between lightning strikes:")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val intervalText = input.text.toString()
                val interval = intervalText.toDoubleOrNull()

                if (interval != null && interval > 0) {
                    // Add artificial time based on interval
                    val lastTime = if (lightningTimes.isEmpty()) System.currentTimeMillis() else lightningTimes.last()
                    val newTime = lastTime + (interval * 1000).toLong()

                    lightningTimes.add(newTime)
                    lightningIntervals.add(interval)

                    updateLightningList()
                    updatePrediction()

                    Toast.makeText(requireContext(), "Manual interval added: ${interval}s", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Please enter a valid positive number", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearAllData() {
        lightningTimes.clear()
        lightningIntervals.clear()
        updateLightningList()
        updatePrediction()
        Toast.makeText(requireContext(), "All data cleared", Toast.LENGTH_SHORT).show()
    }

    private fun updateLightningList() {
        binding.apply {
            val lightningData = StringBuilder()

            if (lightningTimes.isEmpty()) {
                lightningData.append("No lightning strikes recorded yet.\n\nTap 'Record Lightning' when you see a flash!")
            } else {
                lightningData.append("Recorded Lightning Strikes:\n\n")

                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

                for (i in lightningTimes.indices) {
                    val time = timeFormat.format(Date(lightningTimes[i]))
                    lightningData.append("⚡ Strike ${i + 1}: $time")

                    if (i > 0) {
                        lightningData.append(" (+${String.format("%.1f", lightningIntervals[i - 1])}s)")
                    }
                    lightningData.append("\n")
                }

                if (lightningIntervals.isNotEmpty()) {
                    lightningData.append("\nIntervals: ")
                    lightningData.append(lightningIntervals.joinToString(", ") { "${String.format("%.1f", it)}s" })
                }
            }

            tvLightningList.text = lightningData.toString()
        }
    }

    private fun updatePrediction() {
        binding.apply {
            if (lightningIntervals.isEmpty()) {
                // No data yet
                tvNextPrediction.text = "0"
                tvPredictionLabel.text = "Next Lightning In (seconds)"
                tvConfidence.text = "NO DATA"
                tvConfidence.setTextColor(android.graphics.Color.GRAY)

                updateDetailedAnalysis(PredictionResult())
                return
            }

            val patternType = spinnerPatternType.selectedItem.toString()
            val result = calculatePrediction(patternType)

            // Update main result
            tvNextPrediction.text = String.format("%.1f", result.nextInterval)
            tvPredictionLabel.text = "Next Lightning In (seconds)"

            // Update confidence with color coding
            tvConfidence.text = result.confidence
            tvConfidence.setTextColor(getConfidenceColor(result.confidence))

            // Update breakdown
            tvAverageInterval.text = String.format("%.1f s", result.averageInterval)
            tvMinInterval.text = String.format("%.1f s", result.minInterval)
            tvMaxInterval.text = String.format("%.1f s", result.maxInterval)
            tvTotalStrikes.text = lightningTimes.size.toString()

            updateDetailedAnalysis(result)
        }
    }

    private fun calculatePrediction(patternType: String): PredictionResult {
        if (lightningIntervals.isEmpty()) return PredictionResult()

        val average = lightningIntervals.average()
        val min = lightningIntervals.minOrNull() ?: 0.0
        val max = lightningIntervals.maxOrNull() ?: 0.0

        val nextInterval = when (patternType) {
            "Average Interval" -> average
            "Trending Pattern" -> calculateTrend()
            "Shortest Interval" -> min
            "Most Common Interval" -> findMostCommon()
            else -> average
        }

        val confidence = calculateConfidence(nextInterval)

        return PredictionResult(
            nextInterval = nextInterval,
            averageInterval = average,
            minInterval = min,
            maxInterval = max,
            confidence = confidence,
            patternType = patternType
        )
    }

    private fun calculateTrend(): Double {
        if (lightningIntervals.size < 3) return lightningIntervals.average()

        // Calculate trend from last 3 intervals
        val recent = lightningIntervals.takeLast(3)
        val trend = (recent.last() - recent.first()) / (recent.size - 1)

        return (recent.last() + trend).coerceAtLeast(1.0)
    }

    private fun findMostCommon(): Double {
        if (lightningIntervals.isEmpty()) return 0.0

        // Group intervals into ranges and find most common
        val ranges = lightningIntervals.groupBy { (it / 2.0).roundToInt() * 2.0 }
        val mostCommonRange = ranges.maxByOrNull { it.value.size }?.key ?: lightningIntervals.average()

        return mostCommonRange
    }

    private fun calculateConfidence(prediction: Double): String {
        if (lightningIntervals.size < 2) return "LOW"

        // Calculate standard deviation
        val average = lightningIntervals.average()
        val variance = lightningIntervals.map { (it - average) * (it - average) }.average()
        val stdDev = kotlin.math.sqrt(variance)

        // Confidence based on consistency
        val consistency = stdDev / average

        return when {
            consistency < 0.2 -> "VERY HIGH"
            consistency < 0.4 -> "HIGH"
            consistency < 0.6 -> "MEDIUM"
            consistency < 0.8 -> "LOW"
            else -> "VERY LOW"
        }
    }

    private fun getConfidenceColor(confidence: String): Int {
        return when (confidence) {
            "VERY HIGH" -> android.graphics.Color.parseColor("#00AA00")
            "HIGH" -> android.graphics.Color.parseColor("#88AA00")
            "MEDIUM" -> android.graphics.Color.parseColor("#FFAA00")
            "LOW" -> android.graphics.Color.parseColor("#FF8800")
            "VERY LOW" -> android.graphics.Color.RED
            else -> android.graphics.Color.GRAY
        }
    }

    private fun updateDetailedAnalysis(result: PredictionResult) {
        binding.apply {
            val analysis = StringBuilder()
            analysis.append("📸 Lightning Photography Timer Analysis\n\n")

            if (lightningTimes.isEmpty()) {
                analysis.append("🌩️ Getting Started:\n")
                analysis.append("1. Wait for a thunderstorm\n")
                analysis.append("2. When you see lightning, tap 'Record Lightning'\n")
                analysis.append("3. Continue recording each flash\n")
                analysis.append("4. After 3+ strikes, predictions will improve\n\n")

                analysis.append("📷 Camera Setup Tips:\n")
                analysis.append("• Use manual mode\n")
                analysis.append("• Set ISO 100-400\n")
                analysis.append("• Use wide aperture (f/8-f/11)\n")
                analysis.append("• Focus to infinity\n")
                analysis.append("• Use a sturdy tripod\n")
                analysis.append("• Enable interval timer mode\n")
            } else {
                analysis.append("📊 Pattern Analysis:\n")
                analysis.append("• Total strikes recorded: ${lightningTimes.size}\n")
                analysis.append("• Pattern type: ${result.patternType}\n")
                analysis.append("• Prediction confidence: ${result.confidence}\n")
                analysis.append("• Next predicted interval: ${String.format("%.1f", result.nextInterval)} seconds\n\n")

                analysis.append("📷 Camera Timer Setup:\n")
                analysis.append("• Set interval timer to: ${String.format("%.0f", result.nextInterval)} seconds\n")
                analysis.append("• Start timer approximately ${String.format("%.0f", result.nextInterval * 0.8)} seconds before predicted time\n")
                analysis.append("• Use burst mode if available (3-5 shots)\n")
                analysis.append("• Exposure time: 2-8 seconds\n\n")

                analysis.append("📈 Interval Statistics:\n")
                analysis.append("• Average: ${String.format("%.1f", result.averageInterval)}s\n")
                analysis.append("• Shortest: ${String.format("%.1f", result.minInterval)}s\n")
                analysis.append("• Longest: ${String.format("%.1f", result.maxInterval)}s\n")
                analysis.append("• Variation: ${String.format("%.1f", result.maxInterval - result.minInterval)}s\n\n")

                when (result.confidence) {
                    "VERY HIGH", "HIGH" -> {
                        analysis.append("✅ Pattern Reliability:\n")
                        analysis.append("• Strong consistent pattern detected\n")
                        analysis.append("• High chance of accurate prediction\n")
                        analysis.append("• Recommended to use timer setup\n")
                    }
                    "MEDIUM" -> {
                        analysis.append("⚠️ Pattern Reliability:\n")
                        analysis.append("• Moderate pattern consistency\n")
                        analysis.append("• Timer can be helpful but stay alert\n")
                        analysis.append("• Continue recording for better accuracy\n")
                    }
                    "LOW", "VERY LOW" -> {
                        analysis.append("❌ Pattern Reliability:\n")
                        analysis.append("• Irregular lightning pattern\n")
                        analysis.append("• Manual shooting may be more effective\n")
                        analysis.append("• Need more data points for accuracy\n")
                    }
                }
            }

            analysis.append("\n⚠️ Safety First: Never attempt lightning photography without proper safety precautions. Stay indoors or in a safe vehicle. Lightning is unpredictable and dangerous.")

            tvPredictionDetails.text = analysis.toString()
        }
    }

    private fun sharePrediction() {
        val options = arrayOf("Share as Text", "Share as Image")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Share Lightning Prediction")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> shareAsText()
                    1 -> captureAndShareScreenshot()
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun shareAsText() {
        val nextPrediction = binding.tvNextPrediction.text.toString()
        val confidence = binding.tvConfidence.text.toString()
        val patternType = binding.spinnerPatternType.selectedItem.toString()

        val shareText = buildString {
            append("⚡ Lightning Photography Timer\n\n")
            append("📊 Prediction Analysis:\n")
            append("Next Lightning In: ${nextPrediction} seconds\n")
            append("Pattern Type: $patternType\n")
            append("Confidence: $confidence\n\n")

            append("📷 Camera Setup:\n")
            append("• Timer Interval: ${nextPrediction} seconds\n")
            append("• Total Strikes Recorded: ${lightningTimes.size}\n")

            if (lightningIntervals.isNotEmpty()) {
                append("• Average Interval: ${binding.tvAverageInterval.text}\n")
                append("• Range: ${binding.tvMinInterval.text} - ${binding.tvMaxInterval.text}\n")
            }

            append("\nCalculated with Lightning Photography Timer\n")
            append("⚠️ Stay safe - lightning is unpredictable!")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Lightning Photography Timer Analysis")
        }

        startActivity(Intent.createChooser(intent, "Share Lightning Prediction"))
    }

    private fun captureAndShareScreenshot() {
        val screenshotView = binding.lightningResultCard
        val bitmap = Bitmap.createBitmap(
            screenshotView.width,
            screenshotView.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        screenshotView.draw(canvas)

        val file = File(requireContext().cacheDir, "lightning_timer_screenshot.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val contentUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share Lightning Timer"))
    }

    data class PredictionResult(
        val nextInterval: Double = 0.0,
        val averageInterval: Double = 0.0,
        val minInterval: Double = 0.0,
        val maxInterval: Double = 0.0,
        val confidence: String = "NO DATA",
        val patternType: String = "Average Interval"
    )

    companion object {
        private const val TAG = "LightningCalculatorFragment"
    }
}