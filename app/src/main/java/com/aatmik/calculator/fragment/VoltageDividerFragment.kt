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
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentVoltageDividerBinding
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class VoltageDividerFragment : Fragment() {

    private lateinit var binding: FragmentVoltageDividerBinding

    private val decimalFormat = DecimalFormat("#,##0.####")
    private val scientificFormat = DecimalFormat("0.###E0")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentVoltageDividerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        setupListeners()
        setupTextWatchers()
    }

    private fun setupListeners() {
        binding.apply {
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            btnCalculate.setOnClickListener {
                calculate()
            }

            btnReloadCalculator.setOnClickListener {
                reloadCalculator()
            }

            btnShare.setOnClickListener {
                if (resultsSection.visibility == View.VISIBLE) {
                    shareResults()
                } else {
                    Toast.makeText(context, "Please calculate first", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Auto-calculate when all required fields are filled
                if (areRequiredFieldsFilled()) {
                    calculate()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        binding.apply {
            etInputVoltage.addTextChangedListener(textWatcher)
            etR1.addTextChangedListener(textWatcher)
            etR2.addTextChangedListener(textWatcher)
            etLoadResistance.addTextChangedListener(textWatcher)
        }
    }

    private fun areRequiredFieldsFilled(): Boolean {
        return binding.etInputVoltage.text.isNotEmpty() &&
                binding.etR1.text.isNotEmpty() &&
                binding.etR2.text.isNotEmpty()
    }

    private fun calculate() {
        try {
            val inputVoltage = binding.etInputVoltage.text.toString().toDoubleOrNull()
            val r1 = binding.etR1.text.toString().toDoubleOrNull()
            val r2 = binding.etR2.text.toString().toDoubleOrNull()
            val loadResistance = binding.etLoadResistance.text.toString().toDoubleOrNull()

            // Validation
            if (inputVoltage == null || inputVoltage <= 0) {
                Toast.makeText(context, "Please enter valid input voltage", Toast.LENGTH_SHORT).show()
                return
            }
            if (r1 == null || r1 <= 0) {
                Toast.makeText(context, "Please enter valid R1", Toast.LENGTH_SHORT).show()
                return
            }
            if (r2 == null || r2 <= 0) {
                Toast.makeText(context, "Please enter valid R2", Toast.LENGTH_SHORT).show()
                return
            }

            // Calculate without load (ideal voltage divider)
            val outputVoltageNoLoad = (r2 / (r1 + r2)) * inputVoltage

            // Calculate with load if provided
            val outputVoltageWithLoad: Double?
            val effectiveR2: Double?
            val loadCurrent: Double?
            val voltageDropDueToLoad: Double?
            val loadingError: Double?

            if (loadResistance != null && loadResistance > 0) {
                // Parallel combination of R2 and load resistance
                effectiveR2 = (r2 * loadResistance) / (r2 + loadResistance)
                outputVoltageWithLoad = (effectiveR2 / (r1 + effectiveR2)) * inputVoltage
                loadCurrent = outputVoltageWithLoad / loadResistance
                voltageDropDueToLoad = outputVoltageNoLoad - outputVoltageWithLoad
                loadingError = (voltageDropDueToLoad / outputVoltageNoLoad) * 100.0
            } else {
                outputVoltageWithLoad = null
                effectiveR2 = null
                loadCurrent = null
                voltageDropDueToLoad = null
                loadingError = null
            }

            // Calculate currents
            val totalResistance = if (effectiveR2 != null) r1 + effectiveR2 else r1 + r2
            val dividerCurrent = inputVoltage / totalResistance
            val r1Current = dividerCurrent
            val r2Current = if (outputVoltageWithLoad != null) outputVoltageWithLoad / r2 else dividerCurrent

            // Calculate power dissipation
            val powerR1 = r1Current * r1Current * r1
            val powerR2 = r2Current * r2Current * r2
            val totalPower = powerR1 + powerR2
            val powerLoad = if (loadCurrent != null && outputVoltageWithLoad != null) {
                loadCurrent * outputVoltageWithLoad
            } else null

            displayResults(
                outputVoltageNoLoad,
                outputVoltageWithLoad,
                r1Current,
                r2Current,
                loadCurrent,
                powerR1,
                powerR2,
                powerLoad,
                totalPower,
                effectiveR2,
                voltageDropDueToLoad,
                loadingError,
                inputVoltage,
                r1,
                r2,
                loadResistance
            )

            binding.resultsSection.visibility = View.VISIBLE

        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayResults(
        vOutNoLoad: Double,
        vOutWithLoad: Double?,
        i1: Double,
        i2: Double,
        iLoad: Double?,
        p1: Double,
        p2: Double,
        pLoad: Double?,
        pTotal: Double,
        r2Effective: Double?,
        vDrop: Double?,
        loadingError: Double?,
        vIn: Double,
        r1: Double,
        r2: Double,
        rLoad: Double?
    ) {
        binding.apply {
            // Output voltage
            if (vOutWithLoad != null) {
                tvOutputVoltageValue.text = "${decimalFormat.format(vOutWithLoad)} V (loaded)"
                tvIdealVoltageValue.text = "${decimalFormat.format(vOutNoLoad)} V (no load)"
                tvIdealVoltageLayout.visibility = View.VISIBLE
            } else {
                tvOutputVoltageValue.text = "${decimalFormat.format(vOutNoLoad)} V"
                tvIdealVoltageLayout.visibility = View.GONE
            }

            // Current
            tvCurrentValue.text = "${decimalFormat.format(i1 * 1000)} mA"

            // Power
            tvPowerValue.text = "${decimalFormat.format(pTotal * 1000)} mW"

            // Voltage ratio
            val ratio = (vOutNoLoad / vIn) * 100
            tvVoltageRatioValue.text = "${decimalFormat.format(ratio)}%"

            generateDetailedAnalysis(
                vOutNoLoad, vOutWithLoad, i1, i2, iLoad, p1, p2, pLoad, pTotal,
                r2Effective, vDrop, loadingError, vIn, r1, r2, rLoad
            )
        }
    }

    private fun generateDetailedAnalysis(
        vOutNoLoad: Double,
        vOutWithLoad: Double?,
        i1: Double,
        i2: Double,
        iLoad: Double?,
        p1: Double,
        p2: Double,
        pLoad: Double?,
        pTotal: Double,
        r2Effective: Double?,
        vDrop: Double?,
        loadingError: Double?,
        vIn: Double,
        r1: Double,
        r2: Double,
        rLoad: Double?
    ) {
        val analysis = StringBuilder()

        analysis.append("⚡ VOLTAGE DIVIDER ANALYSIS\n\n")

        // Configuration
        analysis.append("🔧 Circuit Configuration:\n")
        analysis.append("• Input Voltage (Vin): ${decimalFormat.format(vIn)} V\n")
        analysis.append("• Resistor R1: ${formatResistance(r1)}\n")
        analysis.append("• Resistor R2: ${formatResistance(r2)}\n")
        if (rLoad != null) {
            analysis.append("• Load Resistance: ${formatResistance(rLoad)}\n")
        }
        analysis.append("\n")

        // Voltage Analysis
        analysis.append("📊 Voltage Analysis:\n")
        analysis.append("• Output Voltage (no load): ${decimalFormat.format(vOutNoLoad)} V\n")
        if (vOutWithLoad != null) {
            analysis.append("• Output Voltage (with load): ${decimalFormat.format(vOutWithLoad)} V\n")
            if (vDrop != null) {
                analysis.append("• Voltage Drop: ${decimalFormat.format(vDrop)} V\n")
            }
            if (loadingError != null) {
                analysis.append("• Loading Error: ${decimalFormat.format(loadingError)}%\n")
            }
        }
        val v1 = vIn - vOutNoLoad
        analysis.append("• Voltage across R1: ${decimalFormat.format(v1)} V\n")
        analysis.append("• Voltage across R2: ${decimalFormat.format(vOutNoLoad)} V\n")
        analysis.append("• Voltage Ratio: ${decimalFormat.format((vOutNoLoad/vIn)*100)}%\n")
        analysis.append("\n")

        // Current Analysis
        analysis.append("⚡ Current Analysis:\n")
        analysis.append("• Divider Current: ${decimalFormat.format(i1 * 1000)} mA\n")
        analysis.append("• Current through R1: ${decimalFormat.format(i1 * 1000)} mA\n")
        analysis.append("• Current through R2: ${decimalFormat.format(i2 * 1000)} mA\n")
        if (iLoad != null) {
            analysis.append("• Load Current: ${decimalFormat.format(iLoad * 1000)} mA\n")
        }
        analysis.append("\n")

        // Power Analysis
        analysis.append("🔥 Power Dissipation:\n")
        analysis.append("• Power in R1: ${decimalFormat.format(p1 * 1000)} mW\n")
        analysis.append("• Power in R2: ${decimalFormat.format(p2 * 1000)} mW\n")
        if (pLoad != null) {
            analysis.append("• Power in Load: ${decimalFormat.format(pLoad * 1000)} mW\n")
        }
        analysis.append("• Total Power: ${decimalFormat.format(pTotal * 1000)} mW\n")

        // Resistor ratings recommendation
        val r1Rating = getRecommendedRating(p1)
        val r2Rating = getRecommendedRating(p2)
        analysis.append("• Recommended R1 Rating: $r1Rating\n")
        analysis.append("• Recommended R2 Rating: $r2Rating\n")
        analysis.append("\n")

        // Efficiency
        if (pLoad != null) {
            val efficiency = (pLoad / (pTotal + pLoad)) * 100
            analysis.append("📈 Efficiency:\n")
            analysis.append("• Power Efficiency: ${decimalFormat.format(efficiency)}%\n")
            analysis.append("• Power Loss: ${decimalFormat.format(pTotal * 1000)} mW\n")
            analysis.append("• Useful Power: ${decimalFormat.format(pLoad * 1000)} mW\n")
            analysis.append("\n")
        }

        // Loading Effect
        if (rLoad != null && loadingError != null) {
            analysis.append("⚠️ Loading Effect:\n")
            if (r2Effective != null) {
                analysis.append("• Effective R2 (R2 || RL): ${formatResistance(r2Effective)}\n")
            }
            analysis.append("• Loading Error: ${decimalFormat.format(loadingError)}%\n")

            when {
                loadingError < 1 -> {
                    analysis.append("• Assessment: ✅ NEGLIGIBLE loading effect\n")
                    analysis.append("• Load resistance is adequately high\n")
                }
                loadingError < 5 -> {
                    analysis.append("• Assessment: ⚠️ ACCEPTABLE loading effect\n")
                    analysis.append("• Consider higher load resistance for precision\n")
                }
                loadingError < 10 -> {
                    analysis.append("• Assessment: ⚠️ MODERATE loading effect\n")
                    analysis.append("• Increase load resistance or reduce R1/R2\n")
                }
                else -> {
                    analysis.append("• Assessment: ❌ SIGNIFICANT loading effect\n")
                    analysis.append("• Circuit needs redesign:\n")
                    analysis.append("  - Use buffer amplifier\n")
                    analysis.append("  - Reduce R1 and R2 values\n")
                    analysis.append("  - Increase load resistance\n")
                }
            }
            analysis.append("\n")
        }

        // Design Rules
        analysis.append("📐 Design Guidelines:\n")
        val ratio = r2 / r1
        analysis.append("• R2/R1 Ratio: ${decimalFormat.format(ratio)}\n")

        if (rLoad != null) {
            val r2ToLoadRatio = r2 / rLoad
            analysis.append("• R2/RL Ratio: ${decimalFormat.format(r2ToLoadRatio)}\n")

            if (r2ToLoadRatio > 0.1) {
                analysis.append("• Rule of Thumb: For <1% error, R2 << RL (R2 ≤ RL/10)\n")
                analysis.append("• Current R2 is ${decimalFormat.format(r2ToLoadRatio * 100)}% of RL\n")
                if (r2ToLoadRatio > 0.1) {
                    analysis.append("• ⚠️ R2 is too high relative to load\n")
                } else {
                    analysis.append("• ✅ Good ratio for minimal loading\n")
                }
            }
        }
        analysis.append("\n")

        // Standard Resistor Values
        analysis.append("🔩 Standard Resistor Values:\n")
        val nearestR1 = findNearestStandardValue(r1)
        val nearestR2 = findNearestStandardValue(r2)
        analysis.append("• Nearest E12 for R1: ${formatResistance(nearestR1)}\n")
        analysis.append("• Nearest E12 for R2: ${formatResistance(nearestR2)}\n")

        // Calculate actual output with standard values
        val vOutStandard = (nearestR2 / (nearestR1 + nearestR2)) * vIn
        val percentError = ((vOutStandard - vOutNoLoad) / vOutNoLoad) * 100
        analysis.append("• Output with standard values: ${decimalFormat.format(vOutStandard)} V\n")
        analysis.append("• Error: ${decimalFormat.format(percentError)}%\n")
        analysis.append("\n")

        // Applications
        analysis.append("💡 Applications:\n")
        when {
            ratio < 0.1 -> analysis.append("• Low voltage step-down\n• Sensor biasing\n")
            ratio < 1.0 -> analysis.append("• General voltage scaling\n• ADC biasing\n")
            ratio < 10.0 -> analysis.append("• Large voltage reduction\n• Level shifting\n")
            else -> analysis.append("• Minimal voltage drop\n• High impedance sensing\n")
        }

        analysis.append("\n⚠️ Important Notes:\n")
        analysis.append("• This is a resistive divider only\n")
        analysis.append("• Not suitable for high-power applications\n")
        analysis.append("• Consider tolerance in resistor values\n")
        analysis.append("• Temperature effects not included\n")

        binding.tvDetailedAnalysis.text = analysis.toString()
    }

    private fun formatResistance(r: Double): String {
        return when {
            r >= 1_000_000 -> "${decimalFormat.format(r / 1_000_000)} MΩ"
            r >= 1_000 -> "${decimalFormat.format(r / 1_000)} kΩ"
            else -> "${decimalFormat.format(r)} Ω"
        }
    }

    private fun getRecommendedRating(power: Double): String {
        val powerW = power
        return when {
            powerW <= 0.0625 -> "1/16 W"
            powerW <= 0.125 -> "1/8 W"
            powerW <= 0.25 -> "1/4 W"
            powerW <= 0.5 -> "1/2 W"
            powerW <= 1.0 -> "1 W"
            powerW <= 2.0 -> "2 W"
            powerW <= 5.0 -> "5 W"
            else -> "${Math.ceil(powerW).toInt()} W"
        }
    }

    private fun findNearestStandardValue(value: Double): Double {
        val e12Values = doubleArrayOf(1.0, 1.2, 1.5, 1.8, 2.2, 2.7, 3.3, 3.9, 4.7, 5.6, 6.8, 8.2)

        // Determine the order of magnitude
        var magnitude = 1.0
        var normalizedValue = value

        while (normalizedValue >= 10.0) {
            normalizedValue /= 10.0
            magnitude *= 10.0
        }
        while (normalizedValue < 1.0) {
            normalizedValue *= 10.0
            magnitude /= 10.0
        }

        // Find nearest E12 value
        var nearest = e12Values[0]
        var minDiff = Math.abs(normalizedValue - e12Values[0])

        for (stdValue in e12Values) {
            val diff = Math.abs(normalizedValue - stdValue)
            if (diff < minDiff) {
                minDiff = diff
                nearest = stdValue
            }
        }

        return nearest * magnitude
    }

    private fun shareResults() {
        val options = arrayOf("Share as Text", "Share as Image")
        AlertDialog.Builder(requireContext())
            .setTitle("Share Voltage Divider")
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
        val shareText = buildString {
            append("⚡ Voltage Divider Results\n\n")
            append("Input:\n")
            append("• Input Voltage: ${binding.etInputVoltage.text} V\n")
            append("• R1: ${binding.etR1.text} Ω\n")
            append("• R2: ${binding.etR2.text} Ω\n")
            val load = binding.etLoadResistance.text.toString()
            if (load.isNotEmpty()) {
                append("• Load: $load Ω\n")
            }
            append("\nOutput:\n")
            append("• Output Voltage: ${binding.tvOutputVoltageValue.text}\n")
            append("• Current: ${binding.tvCurrentValue.text}\n")
            append("• Power: ${binding.tvPowerValue.text}\n")
            append("• Ratio: ${binding.tvVoltageRatioValue.text}\n\n")
            append("Calculated with All In One Calculator")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Voltage Divider Analysis")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    private fun captureAndShareScreenshot() {
        try {
            val scrollView = binding.scrollView
            val bitmap = Bitmap.createBitmap(scrollView.width, scrollView.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            scrollView.draw(canvas)

            val cachePath = File(requireContext().cacheDir, "images")
            cachePath.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(cachePath, "voltage_divider_$timestamp.png")

            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

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

            startActivity(Intent.createChooser(shareIntent, "Share via"))

        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun reloadCalculator() {
        binding.apply {
            etInputVoltage.text?.clear()
            etR1.text?.clear()
            etR2.text?.clear()
            etLoadResistance.text?.clear()
            resultsSection.visibility = View.GONE
        }
        Toast.makeText(context, "Calculator reloaded", Toast.LENGTH_SHORT).show()
    }
}