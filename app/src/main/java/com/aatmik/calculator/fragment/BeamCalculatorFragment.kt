package com.aatmik.calculator.fragment

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentBeamCalculatorBinding
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow

class BeamCalculatorFragment : Fragment() {

    private lateinit var binding: FragmentBeamCalculatorBinding

    private val decimalFormat = DecimalFormat("#,##0.00")
    private val scientificFormat = DecimalFormat("0.###E0")

    // Beam types
    private val beamTypes = arrayOf(
        "Simply Supported - Point Load at Center",
        "Simply Supported - Uniformly Distributed Load",
        "Simply Supported - Point Load at Any Position",
        "Cantilever - Point Load at Free End",
        "Cantilever - Uniformly Distributed Load",
        "Fixed-Fixed - Point Load at Center",
        "Fixed-Fixed - Uniformly Distributed Load"
    )

    // Material properties (Modulus of Elasticity in GPa)
    private val materials = mapOf(
        "Steel" to 200.0,
        "Aluminum" to 70.0,
        "Concrete" to 30.0,
        "Wood (Pine)" to 10.0,
        "Wood (Oak)" to 12.0,
        "Brass" to 100.0,
        "Copper" to 120.0,
        "Custom" to 0.0
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentBeamCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        setupSpinners()
        setupListeners()
    }

    private fun setupSpinners() {
        // Beam type spinner
        val beamTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, beamTypes)
        beamTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerBeamType.adapter = beamTypeAdapter

        // Material spinner
        val materialAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, materials.keys.toList())
        materialAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMaterial.adapter = materialAdapter

        binding.spinnerBeamType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateInputVisibility(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spinnerMaterial.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedMaterial = materials.keys.toList()[position]
                if (selectedMaterial == "Custom") {
                    binding.customModulusLayout.visibility = View.VISIBLE
                    binding.etModulusOfElasticity.text?.clear()
                } else {
                    binding.customModulusLayout.visibility = View.GONE
                    binding.etModulusOfElasticity.setText(materials[selectedMaterial].toString())
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateInputVisibility(beamTypePosition: Int) {
        when (beamTypePosition) {
            0, 1, 5, 6 -> {
                // Simply supported or fixed-fixed beams - no load position needed
                binding.loadPositionLayout.visibility = View.GONE
            }
            2 -> {
                // Simply supported with point load at any position
                binding.loadPositionLayout.visibility = View.VISIBLE
            }
            3, 4 -> {
                // Cantilever beams - no load position needed
                binding.loadPositionLayout.visibility = View.GONE
            }
        }
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

    private fun calculate() {
        try {
            val beamType = binding.spinnerBeamType.selectedItemPosition
            val length = binding.etLength.text.toString().toDoubleOrNull()
            val load = binding.etLoad.text.toString().toDoubleOrNull()
            val width = binding.etWidth.text.toString().toDoubleOrNull()
            val height = binding.etHeight.text.toString().toDoubleOrNull()
            val modulusText = binding.etModulusOfElasticity.text.toString()
            val modulus = modulusText.toDoubleOrNull()

            // Validation
            if (length == null || length <= 0) {
                Toast.makeText(context, "Please enter valid beam length", Toast.LENGTH_SHORT).show()
                return
            }
            if (load == null || load <= 0) {
                Toast.makeText(context, "Please enter valid load", Toast.LENGTH_SHORT).show()
                return
            }
            if (width == null || width <= 0) {
                Toast.makeText(context, "Please enter valid beam width", Toast.LENGTH_SHORT).show()
                return
            }
            if (height == null || height <= 0) {
                Toast.makeText(context, "Please enter valid beam height", Toast.LENGTH_SHORT).show()
                return
            }
            if (modulus == null || modulus <= 0) {
                Toast.makeText(context, "Please enter valid modulus of elasticity", Toast.LENGTH_SHORT).show()
                return
            }

            // Get load position if needed
            var loadPosition: Double? = null
            if (binding.loadPositionLayout.visibility == View.VISIBLE) {
                loadPosition = binding.etLoadPosition.text.toString().toDoubleOrNull()
                if (loadPosition == null || loadPosition < 0 || loadPosition > length) {
                    Toast.makeText(context, "Load position must be between 0 and beam length", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            // Calculate moment of inertia (I = bh³/12 for rectangular section)
            val momentOfInertia = (width * height.pow(3)) / 12.0

            // Convert modulus from GPa to N/mm²
            val E = modulus * 1000.0

            // Calculate based on beam type
            val results = when (beamType) {
                0 -> calculateSimplySupportedPointCenter(length, load, E, momentOfInertia, width, height)
                1 -> calculateSimplySupportedUDL(length, load, E, momentOfInertia, width, height)
                2 -> calculateSimplySupportedPointAny(length, load, loadPosition!!, E, momentOfInertia, width, height)
                3 -> calculateCantileverPoint(length, load, E, momentOfInertia, width, height)
                4 -> calculateCantileverUDL(length, load, E, momentOfInertia, width, height)
                5 -> calculateFixedFixedPointCenter(length, load, E, momentOfInertia, width, height)
                6 -> calculateFixedFixedUDL(length, load, E, momentOfInertia, width, height)
                else -> BeamResults(0.0, 0.0, 0.0, 0.0, 0.0)
            }

            displayResults(results, beamType)
            binding.resultsSection.visibility = View.VISIBLE

        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateSimplySupportedPointCenter(
        L: Double, P: Double, E: Double, I: Double, b: Double, h: Double
    ): BeamResults {
        val maxDeflection = (P * L.pow(3)) / (48.0 * E * I)
        val maxMoment = (P * L) / 4.0
        val maxShear = P / 2.0
        val maxBendingStress = (maxMoment * (h / 2.0)) / I
        val reactionA = P / 2.0
        val reactionB = P / 2.0

        return BeamResults(maxDeflection, maxMoment, maxShear, maxBendingStress, reactionA, reactionB)
    }

    private fun calculateSimplySupportedUDL(
        L: Double, w: Double, E: Double, I: Double, b: Double, h: Double
    ): BeamResults {
        val totalLoad = w * L
        val maxDeflection = (5.0 * w * L.pow(4)) / (384.0 * E * I)
        val maxMoment = (w * L.pow(2)) / 8.0
        val maxShear = (w * L) / 2.0
        val maxBendingStress = (maxMoment * (h / 2.0)) / I
        val reactionA = totalLoad / 2.0
        val reactionB = totalLoad / 2.0

        return BeamResults(maxDeflection, maxMoment, maxShear, maxBendingStress, reactionA, reactionB)
    }

    private fun calculateSimplySupportedPointAny(
        L: Double, P: Double, a: Double, E: Double, I: Double, b: Double, h: Double
    ): BeamResults {
        val b_pos = L - a
        val reactionA = (P * b_pos) / L
        val reactionB = (P * a) / L
        val maxMoment = (P * a * b_pos) / L
        val maxShear = maxOf(reactionA, reactionB)

        // Deflection at load point
        val maxDeflection = (P * a.pow(2) * b_pos.pow(2)) / (3.0 * E * I * L)
        val maxBendingStress = (maxMoment * (h / 2.0)) / I

        return BeamResults(maxDeflection, maxMoment, maxShear, maxBendingStress, reactionA, reactionB)
    }

    private fun calculateCantileverPoint(
        L: Double, P: Double, E: Double, I: Double, b: Double, h: Double
    ): BeamResults {
        val maxDeflection = (P * L.pow(3)) / (3.0 * E * I)
        val maxMoment = P * L
        val maxShear = P
        val maxBendingStress = (maxMoment * (h / 2.0)) / I
        val reactionA = P

        return BeamResults(maxDeflection, maxMoment, maxShear, maxBendingStress, reactionA)
    }

    private fun calculateCantileverUDL(
        L: Double, w: Double, E: Double, I: Double, b: Double, h: Double
    ): BeamResults {
        val totalLoad = w * L
        val maxDeflection = (w * L.pow(4)) / (8.0 * E * I)
        val maxMoment = (w * L.pow(2)) / 2.0
        val maxShear = w * L
        val maxBendingStress = (maxMoment * (h / 2.0)) / I
        val reactionA = totalLoad

        return BeamResults(maxDeflection, maxMoment, maxShear, maxBendingStress, reactionA)
    }

    private fun calculateFixedFixedPointCenter(
        L: Double, P: Double, E: Double, I: Double, b: Double, h: Double
    ): BeamResults {
        val maxDeflection = (P * L.pow(3)) / (192.0 * E * I)
        val maxMoment = (P * L) / 8.0
        val maxShear = P / 2.0
        val maxBendingStress = (maxMoment * (h / 2.0)) / I
        val reactionA = P / 2.0
        val reactionB = P / 2.0

        return BeamResults(maxDeflection, maxMoment, maxShear, maxBendingStress, reactionA, reactionB)
    }

    private fun calculateFixedFixedUDL(
        L: Double, w: Double, E: Double, I: Double, b: Double, h: Double
    ): BeamResults {
        val totalLoad = w * L
        val maxDeflection = (w * L.pow(4)) / (384.0 * E * I)
        val maxMoment = (w * L.pow(2)) / 12.0
        val maxShear = (w * L) / 2.0
        val maxBendingStress = (maxMoment * (h / 2.0)) / I
        val reactionA = totalLoad / 2.0
        val reactionB = totalLoad / 2.0

        return BeamResults(maxDeflection, maxMoment, maxShear, maxBendingStress, reactionA, reactionB)
    }

    private fun displayResults(results: BeamResults, beamType: Int) {
        binding.apply {
            tvDeflectionValue.text = "${decimalFormat.format(results.maxDeflection)} mm"
            tvMomentValue.text = "${decimalFormat.format(results.maxMoment)} kN·m"
            tvShearValue.text = "${decimalFormat.format(results.maxShear)} kN"
            tvStressValue.text = "${decimalFormat.format(results.maxBendingStress)} N/mm²"

            generateDetailedAnalysis(results, beamType)
        }
    }

    private fun generateDetailedAnalysis(results: BeamResults, beamType: Int) {
        val analysis = StringBuilder()

        analysis.append("🏗️ BEAM ANALYSIS RESULTS\n\n")

        // Beam Configuration
        analysis.append("📐 Beam Configuration:\n")
        analysis.append("• Type: ${beamTypes[beamType]}\n")
        analysis.append("• Length: ${binding.etLength.text} mm\n")
        analysis.append("• Width: ${binding.etWidth.text} mm\n")
        analysis.append("• Height: ${binding.etHeight.text} mm\n")
        analysis.append("• Material: ${binding.spinnerMaterial.selectedItem}\n")
        analysis.append("• Modulus: ${binding.etModulusOfElasticity.text} GPa\n\n")

        // Loading
        analysis.append("⚖️ Loading:\n")
        val loadType = if (beamType == 1 || beamType == 4 || beamType == 6) "UDL" else "Point Load"
        analysis.append("• Type: $loadType\n")
        analysis.append("• Magnitude: ${binding.etLoad.text} kN${if (loadType == "UDL") "/m" else ""}\n")
        if (binding.loadPositionLayout.visibility == View.VISIBLE) {
            analysis.append("• Position: ${binding.etLoadPosition.text} mm from left support\n")
        }
        analysis.append("\n")

        // Reactions
        analysis.append("🔧 Support Reactions:\n")
        if (results.reactionB != null) {
            analysis.append("• Left Support (RA): ${decimalFormat.format(results.reactionA)} kN\n")
            analysis.append("• Right Support (RB): ${decimalFormat.format(results.reactionB)} kN\n")
        } else {
            analysis.append("• Fixed Support: ${decimalFormat.format(results.reactionA)} kN\n")
        }
        analysis.append("\n")

        // Critical Values
        analysis.append("📊 Critical Values:\n")
        analysis.append("• Max Deflection: ${decimalFormat.format(results.maxDeflection)} mm\n")
        analysis.append("• Max Bending Moment: ${decimalFormat.format(results.maxMoment)} kN·m\n")
        analysis.append("• Max Shear Force: ${decimalFormat.format(results.maxShear)} kN\n")
        analysis.append("• Max Bending Stress: ${decimalFormat.format(results.maxBendingStress)} N/mm²\n\n")

        // Deflection Assessment
        analysis.append("📏 Deflection Assessment:\n")
        val lengthMm = binding.etLength.text.toString().toDouble()
        val deflectionLimit = lengthMm / 360.0 // L/360 is common limit
        val deflectionRatio = lengthMm / results.maxDeflection

        analysis.append("• Deflection/Span Ratio: L/${deflectionRatio.toInt()}\n")
        analysis.append("• Typical Limit: L/360\n")
        if (results.maxDeflection <= deflectionLimit) {
            analysis.append("• Status: ✅ ACCEPTABLE (within L/360)\n")
        } else {
            analysis.append("• Status: ⚠️ EXCESSIVE (exceeds L/360)\n")
            analysis.append("• Recommendation: Increase beam depth or use stronger material\n")
        }
        analysis.append("\n")

        // Stress Assessment
        analysis.append("💪 Stress Assessment:\n")
        val materialName = binding.spinnerMaterial.selectedItem.toString()
        val allowableStress = when (materialName) {
            "Steel" -> 165.0
            "Aluminum" -> 95.0
            "Concrete" -> 10.0
            "Wood (Pine)" -> 8.0
            "Wood (Oak)" -> 10.0
            else -> 100.0
        }

        analysis.append("• Allowable Stress: ${decimalFormat.format(allowableStress)} N/mm²\n")
        analysis.append("• Actual Stress: ${decimalFormat.format(results.maxBendingStress)} N/mm²\n")
        val safetyFactor = allowableStress / results.maxBendingStress
        analysis.append("• Safety Factor: ${String.format("%.2f", safetyFactor)}\n")

        when {
            safetyFactor >= 2.0 -> {
                analysis.append("• Status: ✅ VERY SAFE (SF ≥ 2.0)\n")
            }
            safetyFactor >= 1.5 -> {
                analysis.append("• Status: ✅ SAFE (SF ≥ 1.5)\n")
            }
            safetyFactor >= 1.0 -> {
                analysis.append("• Status: ⚠️ MARGINAL (SF < 1.5)\n")
                analysis.append("• Recommendation: Consider larger section\n")
            }
            else -> {
                analysis.append("• Status: ❌ UNSAFE (SF < 1.0)\n")
                analysis.append("• Recommendation: MUST increase section size or change material\n")
            }
        }
        analysis.append("\n")

        // Section Properties
        val width = binding.etWidth.text.toString().toDouble()
        val height = binding.etHeight.text.toString().toDouble()
        val area = width * height
        val momentOfInertia = (width * height.pow(3)) / 12.0
        val sectionModulus = momentOfInertia / (height / 2.0)

        analysis.append("📦 Section Properties:\n")
        analysis.append("• Cross-sectional Area: ${decimalFormat.format(area)} mm²\n")
        analysis.append("• Moment of Inertia (I): ${scientificFormat.format(momentOfInertia)} mm⁴\n")
        analysis.append("• Section Modulus (Z): ${scientificFormat.format(sectionModulus)} mm³\n\n")

        // Design Recommendations
        analysis.append("💡 Design Notes:\n")
        analysis.append("• Always verify local building codes\n")
        analysis.append("• Consider dynamic loads and impact factors\n")
        analysis.append("• Account for self-weight of beam\n")
        analysis.append("• Check for lateral-torsional buckling\n")
        analysis.append("• Verify shear capacity at supports\n")

        binding.tvDetailedAnalysis.text = analysis.toString()
    }

    private fun shareResults() {
        val options = arrayOf("Share as Text", "Share as Image")
        AlertDialog.Builder(requireContext())
            .setTitle("Share Beam Analysis")
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
            append("🏗️ Beam Analysis Results\n\n")
            append("Configuration:\n")
            append("• Type: ${binding.spinnerBeamType.selectedItem}\n")
            append("• Length: ${binding.etLength.text} mm\n")
            append("• Section: ${binding.etWidth.text} × ${binding.etHeight.text} mm\n\n")
            append("Results:\n")
            append("• Deflection: ${binding.tvDeflectionValue.text}\n")
            append("• Moment: ${binding.tvMomentValue.text}\n")
            append("• Shear: ${binding.tvShearValue.text}\n")
            append("• Stress: ${binding.tvStressValue.text}\n\n")
            append("Calculated with All In One Calculator")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Beam Analysis")
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
            val file = File(cachePath, "beam_analysis_$timestamp.png")

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
            spinnerBeamType.setSelection(0)
            spinnerMaterial.setSelection(0)
            etLength.text?.clear()
            etLoad.text?.clear()
            etWidth.text?.clear()
            etHeight.text?.clear()
            etLoadPosition.text?.clear()
            resultsSection.visibility = View.GONE
        }
        Toast.makeText(context, "Calculator reloaded", Toast.LENGTH_SHORT).show()
    }

    data class BeamResults(
        val maxDeflection: Double,
        val maxMoment: Double,
        val maxShear: Double,
        val maxBendingStress: Double,
        val reactionA: Double,
        val reactionB: Double? = null
    )
}