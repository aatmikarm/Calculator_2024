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
import com.aatmik.calculator.databinding.FragmentColumnBucklingBinding
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt

class ColumnBucklingFragment : Fragment() {

    private lateinit var binding: FragmentColumnBucklingBinding

    private val decimalFormat = DecimalFormat("#,##0.00")
    private val scientificFormat = DecimalFormat("0.###E0")

    // End condition factors (K values)
    private val endConditions = mapOf(
        "Both Ends Pinned" to 1.0,
        "Both Ends Fixed" to 0.5,
        "One Fixed, One Pinned" to 0.7,
        "One Fixed, One Free (Cantilever)" to 2.0,
        "One Fixed, One Guided" to 1.0
    )

    // Cross-section types
    private val crossSections = arrayOf(
        "Rectangular",
        "Circular Solid",
        "Circular Hollow (Pipe)",
        "I-Beam",
        "Square Hollow Section"
    )

    // Material properties (Modulus of Elasticity in GPa, Yield Strength in MPa)
    private val materials = mapOf(
        "Structural Steel (A36)" to Pair(200.0, 250.0),
        "High Strength Steel (A572)" to Pair(200.0, 345.0),
        "Stainless Steel (304)" to Pair(193.0, 215.0),
        "Aluminum 6061-T6" to Pair(69.0, 276.0),
        "Aluminum 7075-T6" to Pair(71.7, 503.0),
        "Concrete (Normal)" to Pair(30.0, 25.0),
        "Timber (Pine)" to Pair(10.0, 40.0),
        "Timber (Oak)" to Pair(12.0, 50.0),
        "Custom" to Pair(0.0, 0.0)
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentColumnBucklingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        setupSpinners()
        setupListeners()
    }

    private fun setupSpinners() {
        // End condition spinner
        val endConditionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, endConditions.keys.toList())
        endConditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEndCondition.adapter = endConditionAdapter

        // Cross-section spinner
        val crossSectionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, crossSections)
        crossSectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCrossSection.adapter = crossSectionAdapter

        // Material spinner
        val materialAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, materials.keys.toList())
        materialAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMaterial.adapter = materialAdapter

        binding.spinnerCrossSection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateDimensionInputs(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spinnerMaterial.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedMaterial = materials.keys.toList()[position]
                if (selectedMaterial == "Custom") {
                    binding.customPropertiesLayout.visibility = View.VISIBLE
                    binding.etModulusOfElasticity.text?.clear()
                    binding.etYieldStrength.text?.clear()
                } else {
                    binding.customPropertiesLayout.visibility = View.GONE
                    val (modulus, yield) = materials[selectedMaterial]!!
                    binding.etModulusOfElasticity.setText(modulus.toString())
                    binding.etYieldStrength.setText(yield.toString())
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateDimensionInputs(sectionType: Int) {
        binding.apply {
            when (sectionType) {
                0 -> { // Rectangular
                    dimension1Label.text = "Width (mm)"
                    dimension2Label.text = "Height (mm)"
                    dimension3Layout.visibility = View.GONE
                    dimension4Layout.visibility = View.GONE
                    etDimension1.hint = "e.g., 200"
                    etDimension2.hint = "e.g., 300"
                }
                1 -> { // Circular Solid
                    dimension1Label.text = "Diameter (mm)"
                    dimension2Layout.visibility = View.GONE
                    dimension3Layout.visibility = View.GONE
                    dimension4Layout.visibility = View.GONE
                    etDimension1.hint = "e.g., 200"
                }
                2 -> { // Circular Hollow
                    dimension1Label.text = "Outer Diameter (mm)"
                    dimension2Label.text = "Wall Thickness (mm)"
                    dimension2Layout.visibility = View.VISIBLE
                    dimension3Layout.visibility = View.GONE
                    dimension4Layout.visibility = View.GONE
                    etDimension1.hint = "e.g., 200"
                    etDimension2.hint = "e.g., 10"
                }
                3 -> { // I-Beam
                    dimension1Label.text = "Flange Width (mm)"
                    dimension2Label.text = "Total Height (mm)"
                    dimension3Label.text = "Flange Thickness (mm)"
                    dimension4Label.text = "Web Thickness (mm)"
                    dimension2Layout.visibility = View.VISIBLE
                    dimension3Layout.visibility = View.VISIBLE
                    dimension4Layout.visibility = View.VISIBLE
                    etDimension1.hint = "e.g., 200"
                    etDimension2.hint = "e.g., 300"
                    etDimension3.hint = "e.g., 15"
                    etDimension4.hint = "e.g., 10"
                }
                4 -> { // Square Hollow
                    dimension1Label.text = "Outer Width (mm)"
                    dimension2Label.text = "Wall Thickness (mm)"
                    dimension2Layout.visibility = View.VISIBLE
                    dimension3Layout.visibility = View.GONE
                    dimension4Layout.visibility = View.GONE
                    etDimension1.hint = "e.g., 200"
                    etDimension2.hint = "e.g., 10"
                }
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
            val length = binding.etLength.text.toString().toDoubleOrNull()
            val modulusText = binding.etModulusOfElasticity.text.toString()
            val modulus = modulusText.toDoubleOrNull()
            val yieldStrength = binding.etYieldStrength.text.toString().toDoubleOrNull()

            // Validation
            if (length == null || length <= 0) {
                Toast.makeText(context, "Please enter valid column length", Toast.LENGTH_SHORT).show()
                return
            }
            if (modulus == null || modulus <= 0) {
                Toast.makeText(context, "Please enter valid modulus of elasticity", Toast.LENGTH_SHORT).show()
                return
            }
            if (yieldStrength == null || yieldStrength <= 0) {
                Toast.makeText(context, "Please enter valid yield strength", Toast.LENGTH_SHORT).show()
                return
            }

            // Get K factor
            val endCondition = binding.spinnerEndCondition.selectedItem.toString()
            val K = endConditions[endCondition]!!

            // Calculate section properties based on cross-section type
            val sectionType = binding.spinnerCrossSection.selectedItemPosition
            val sectionProps = calculateSectionProperties(sectionType) ?: return

            // Convert modulus from GPa to MPa
            val E = modulus * 1000.0

            // Calculate critical buckling load using Euler's formula
            val effectiveLength = K * length
            val criticalLoad = (PI.pow(2) * E * sectionProps.minI) / effectiveLength.pow(2)

            // Calculate slenderness ratio
            val slendernessRatio = effectiveLength / sectionProps.minRadiusOfGyration

            // Calculate critical stress
            val criticalStress = criticalLoad / sectionProps.area

            // Determine failure mode
            val failureMode = if (criticalStress < yieldStrength) "Buckling" else "Yielding"

            // Calculate allowable load with safety factor (typically 2.0)
            val safetyFactor = 2.0
            val allowableLoad = criticalLoad / safetyFactor

            // Calculate load capacity based on failure mode
            val loadCapacity = if (failureMode == "Buckling") {
                criticalLoad
            } else {
                yieldStrength * sectionProps.area
            }

            displayResults(
                criticalLoad,
                allowableLoad,
                criticalStress,
                slendernessRatio,
                failureMode,
                sectionProps,
                effectiveLength,
                yieldStrength
            )

            binding.resultsSection.visibility = View.VISIBLE

        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateSectionProperties(sectionType: Int): SectionProperties? {
        val d1 = binding.etDimension1.text.toString().toDoubleOrNull()
        if (d1 == null || d1 <= 0) {
            Toast.makeText(context, "Please enter valid dimension", Toast.LENGTH_SHORT).show()
            return null
        }

        return when (sectionType) {
            0 -> { // Rectangular
                val d2 = binding.etDimension2.text.toString().toDoubleOrNull()
                if (d2 == null || d2 <= 0) {
                    Toast.makeText(context, "Please enter valid height", Toast.LENGTH_SHORT).show()
                    return null
                }
                val area = d1 * d2
                val Ix = (d1 * d2.pow(3)) / 12.0
                val Iy = (d2 * d1.pow(3)) / 12.0
                val minI = minOf(Ix, Iy)
                val minR = sqrt(minI / area)
                SectionProperties(area, minI, minR, Ix, Iy)
            }
            1 -> { // Circular Solid
                val area = (PI * d1.pow(2)) / 4.0
                val I = (PI * d1.pow(4)) / 64.0
                val r = d1 / 4.0
                SectionProperties(area, I, r, I, I)
            }
            2 -> { // Circular Hollow
                val t = binding.etDimension2.text.toString().toDoubleOrNull()
                if (t == null || t <= 0 || t >= d1/2) {
                    Toast.makeText(context, "Please enter valid wall thickness", Toast.LENGTH_SHORT).show()
                    return null
                }
                val di = d1 - 2 * t
                val area = (PI / 4.0) * (d1.pow(2) - di.pow(2))
                val I = (PI / 64.0) * (d1.pow(4) - di.pow(4))
                val r = sqrt(I / area)
                SectionProperties(area, I, r, I, I)
            }
            3 -> { // I-Beam
                val h = binding.etDimension2.text.toString().toDoubleOrNull()
                val tf = binding.etDimension3.text.toString().toDoubleOrNull()
                val tw = binding.etDimension4.text.toString().toDoubleOrNull()
                if (h == null || tf == null || tw == null || h <= 2*tf || d1 <= tw) {
                    Toast.makeText(context, "Please enter valid I-beam dimensions", Toast.LENGTH_SHORT).show()
                    return null
                }
                val area = 2 * d1 * tf + (h - 2*tf) * tw
                val Ix = (d1 * h.pow(3)) / 12.0 - ((d1 - tw) * (h - 2*tf).pow(3)) / 12.0
                val Iy = (2 * tf * d1.pow(3)) / 12.0 + ((h - 2*tf) * tw.pow(3)) / 12.0
                val minI = minOf(Ix, Iy)
                val minR = sqrt(minI / area)
                SectionProperties(area, minI, minR, Ix, Iy)
            }
            4 -> { // Square Hollow
                val t = binding.etDimension2.text.toString().toDoubleOrNull()
                if (t == null || t <= 0 || t >= d1/2) {
                    Toast.makeText(context, "Please enter valid wall thickness", Toast.LENGTH_SHORT).show()
                    return null
                }
                val di = d1 - 2 * t
                val area = d1.pow(2) - di.pow(2)
                val I = (d1.pow(4) - di.pow(4)) / 12.0
                val r = sqrt(I / area)
                SectionProperties(area, I, r, I, I)
            }
            else -> null
        }
    }

    private fun displayResults(
        criticalLoad: Double,
        allowableLoad: Double,
        criticalStress: Double,
        slendernessRatio: Double,
        failureMode: String,
        sectionProps: SectionProperties,
        effectiveLength: Double,
        yieldStrength: Double
    ) {
        binding.apply {
            tvCriticalLoadValue.text = "${decimalFormat.format(criticalLoad / 1000.0)} kN"
            tvAllowableLoadValue.text = "${decimalFormat.format(allowableLoad / 1000.0)} kN"
            tvCriticalStressValue.text = "${decimalFormat.format(criticalStress)} MPa"
            tvSlendernessValue.text = decimalFormat.format(slendernessRatio)
            tvFailureModeValue.text = failureMode

            tvFailureModeValue.setTextColor(
                resources.getColor(
                    if (failureMode == "Buckling") android.R.color.holo_orange_dark
                    else android.R.color.holo_red_dark,
                    null
                )
            )

            generateDetailedAnalysis(
                criticalLoad, allowableLoad, criticalStress, slendernessRatio,
                failureMode, sectionProps, effectiveLength, yieldStrength
            )
        }
    }

    private fun generateDetailedAnalysis(
        criticalLoad: Double,
        allowableLoad: Double,
        criticalStress: Double,
        slendernessRatio: Double,
        failureMode: String,
        sectionProps: SectionProperties,
        effectiveLength: Double,
        yieldStrength: Double
    ) {
        val analysis = StringBuilder()

        analysis.append("🏗️ COLUMN BUCKLING ANALYSIS\n\n")

        // Column Configuration
        analysis.append("📐 Column Configuration:\n")
        analysis.append("• Length: ${binding.etLength.text} mm\n")
        analysis.append("• End Condition: ${binding.spinnerEndCondition.selectedItem}\n")
        analysis.append("• Cross-section: ${binding.spinnerCrossSection.selectedItem}\n")
        analysis.append("• Material: ${binding.spinnerMaterial.selectedItem}\n")
        analysis.append("• Effective Length: ${decimalFormat.format(effectiveLength)} mm\n\n")

        // Section Properties
        analysis.append("📦 Section Properties:\n")
        analysis.append("• Area: ${decimalFormat.format(sectionProps.area)} mm²\n")
        analysis.append("• Min. Moment of Inertia: ${scientificFormat.format(sectionProps.minI)} mm⁴\n")
        analysis.append("• Min. Radius of Gyration: ${decimalFormat.format(sectionProps.minRadiusOfGyration)} mm\n")
        analysis.append("• Ix: ${scientificFormat.format(sectionProps.Ix)} mm⁴\n")
        analysis.append("• Iy: ${scientificFormat.format(sectionProps.Iy)} mm⁴\n\n")

        // Buckling Analysis
        analysis.append("📊 Buckling Analysis:\n")
        analysis.append("• Critical Buckling Load: ${decimalFormat.format(criticalLoad / 1000.0)} kN\n")
        analysis.append("• Critical Stress: ${decimalFormat.format(criticalStress)} MPa\n")
        analysis.append("• Yield Strength: ${decimalFormat.format(yieldStrength)} MPa\n")
        analysis.append("• Slenderness Ratio (λ): ${decimalFormat.format(slendernessRatio)}\n\n")

        // Slenderness Classification
        analysis.append("📏 Slenderness Classification:\n")
        when {
            slendernessRatio < 50 -> {
                analysis.append("• Classification: SHORT COLUMN\n")
                analysis.append("• Behavior: Primarily compression failure\n")
                analysis.append("• Note: Material strength governs\n")
            }
            slendernessRatio < 200 -> {
                analysis.append("• Classification: INTERMEDIATE COLUMN\n")
                analysis.append("• Behavior: Combined compression and buckling\n")
                analysis.append("• Note: Use empirical formulas (e.g., Rankine)\n")
            }
            else -> {
                analysis.append("• Classification: LONG/SLENDER COLUMN\n")
                analysis.append("• Behavior: Elastic buckling dominates\n")
                analysis.append("• Note: Euler formula applies\n")
            }
        }
        analysis.append("\n")

        // Failure Mode
        analysis.append("⚠️ Failure Mode:\n")
        analysis.append("• Predicted Mode: $failureMode\n")
        if (failureMode == "Buckling") {
            analysis.append("• Explanation: Critical stress < Yield strength\n")
            analysis.append("• Column will buckle before material yields\n")
        } else {
            analysis.append("• Explanation: Critical stress ≥ Yield strength\n")
            analysis.append("• Material will yield before buckling occurs\n")
        }
        analysis.append("\n")

        // Load Capacity
        analysis.append("💪 Load Capacity:\n")
        analysis.append("• Critical Load (Pcr): ${decimalFormat.format(criticalLoad / 1000.0)} kN\n")
        analysis.append("• Allowable Load (Pa): ${decimalFormat.format(allowableLoad / 1000.0)} kN\n")
        analysis.append("• Safety Factor: 2.0\n")

        val loadRatio = allowableLoad / criticalLoad
        analysis.append("• Design Efficiency: ${String.format("%.1f", loadRatio * 100)}%\n\n")

        // Design Assessment
        analysis.append("✅ Design Assessment:\n")
        if (slendernessRatio > 200) {
            analysis.append("⚠️ WARNING: Very slender column\n")
            analysis.append("• Recommendations:\n")
            analysis.append("  - Provide lateral bracing\n")
            analysis.append("  - Increase cross-section\n")
            analysis.append("  - Consider different end conditions\n")
        } else if (slendernessRatio < 50) {
            analysis.append("✅ Short column - good design\n")
            analysis.append("• Column is stocky and efficient\n")
            analysis.append("• Material strength is well utilized\n")
        } else {
            analysis.append("✅ Intermediate column\n")
            analysis.append("• Consider additional safety factors\n")
            analysis.append("• Check for combined loading effects\n")
        }
        analysis.append("\n")

        // K-Factor Information
        val K = endConditions[binding.spinnerEndCondition.selectedItem.toString()]!!
        analysis.append("🔧 Effective Length Factor (K):\n")
        analysis.append("• K-Factor: $K\n")
        analysis.append("• Effective Length: Le = K × L\n")
        analysis.append("• Le = $K × ${binding.etLength.text} = ${decimalFormat.format(effectiveLength)} mm\n\n")

        // Design Recommendations
        analysis.append("💡 Design Recommendations:\n")
        analysis.append("• Always verify local building codes\n")
        analysis.append("• Consider eccentric loading effects\n")
        analysis.append("• Check for lateral-torsional buckling\n")
        analysis.append("• Account for initial imperfections\n")
        analysis.append("• Verify connection design\n")
        analysis.append("• Consider load duration and fatigue\n")

        binding.tvDetailedAnalysis.text = analysis.toString()
    }

    private fun shareResults() {
        val options = arrayOf("Share as Text", "Share as Image")
        AlertDialog.Builder(requireContext())
            .setTitle("Share Column Analysis")
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
            append("🏗️ Column Buckling Analysis\n\n")
            append("Configuration:\n")
            append("• Length: ${binding.etLength.text} mm\n")
            append("• End Condition: ${binding.spinnerEndCondition.selectedItem}\n")
            append("• Section: ${binding.spinnerCrossSection.selectedItem}\n\n")
            append("Results:\n")
            append("• Critical Load: ${binding.tvCriticalLoadValue.text}\n")
            append("• Allowable Load: ${binding.tvAllowableLoadValue.text}\n")
            append("• Critical Stress: ${binding.tvCriticalStressValue.text}\n")
            append("• Slenderness Ratio: ${binding.tvSlendernessValue.text}\n")
            append("• Failure Mode: ${binding.tvFailureModeValue.text}\n\n")
            append("Calculated with All In One Calculator")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Column Buckling Analysis")
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
            val file = File(cachePath, "column_buckling_$timestamp.png")

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
            spinnerEndCondition.setSelection(0)
            spinnerCrossSection.setSelection(0)
            spinnerMaterial.setSelection(0)
            etLength.text?.clear()
            etDimension1.text?.clear()
            etDimension2.text?.clear()
            etDimension3.text?.clear()
            etDimension4.text?.clear()
            resultsSection.visibility = View.GONE
        }
        Toast.makeText(context, "Calculator reloaded", Toast.LENGTH_SHORT).show()
    }

    data class SectionProperties(
        val area: Double,
        val minI: Double,
        val minRadiusOfGyration: Double,
        val Ix: Double,
        val Iy: Double
    )
}