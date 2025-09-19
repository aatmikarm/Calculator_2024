package com.aatmik.calculator.fragment

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aatmik.calculator.databinding.FragmentMolarityBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import startGlowAnimation
import stopGlowAnimation

class MolarityFragment : Fragment() {

    private lateinit var binding: FragmentMolarityBinding
    private var isCalculating = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMolarityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        setupSpinners()
        animateView(binding.molarityLayout)
        hideKeyboardFunctionality(view)
        setupListeners()
        setupTextWatchers()
    }

    private fun setupSpinners() {
        // Volume units spinner
        val volumeUnits = arrayOf("L", "mL", "µL", "gallon", "fl oz")
        val volumeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, volumeUnits)
        volumeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerVolumeUnit.adapter = volumeAdapter

        // Mass units spinner
        val massUnits = arrayOf("g", "mg", "µg", "kg", "oz", "lb")
        val massAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, massUnits)
        massAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMassUnit.adapter = massAdapter
    }

    private fun setupListeners() {
        binding.apply {
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            btnCalculate.setOnClickListener {
                hideKeyboard()
                calculateMolarity()
            }

            btnClear.setOnClickListener {
                clearAllFields()
            }

            // Quick calculation buttons
            btnMolarity.setOnClickListener {
                calculateSpecificValue("molarity")
            }

            btnMoles.setOnClickListener {
                calculateSpecificValue("moles")
            }

            btnVolume.setOnClickListener {
                calculateSpecificValue("volume")
            }

            btnMass.setOnClickListener {
                calculateSpecificValue("mass")
            }

            // Set up the editor action listeners
            etMass.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    etMolecularWeight.requestFocus()
                    true
                } else {
                    false
                }
            }

            etMolecularWeight.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    etVolume.requestFocus()
                    true
                } else {
                    false
                }
            }

            etVolume.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    etMolarity.requestFocus()
                    true
                } else {
                    false
                }
            }

            etMolarity.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    hideKeyboard()
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isCalculating) {
                    autoCalculate()
                }
            }
        }

        binding.apply {
            etMass.addTextChangedListener(textWatcher)
            etMolecularWeight.addTextChangedListener(textWatcher)
            etVolume.addTextChangedListener(textWatcher)
            etMolarity.addTextChangedListener(textWatcher)
        }
    }

    private fun autoCalculate() {
        binding.apply {
            val mass = etMass.text.toString().toDoubleOrNull()
            val molecularWeight = etMolecularWeight.text.toString().toDoubleOrNull()
            val volume = etVolume.text.toString().toDoubleOrNull()
            val molarity = etMolarity.text.toString().toDoubleOrNull()

            val filledFields = listOfNotNull(mass, molecularWeight, volume, molarity).size

            // Auto-calculate if we have enough information
            if (filledFields >= 3) {
                isCalculating = true
                try {
                    calculateMolarityValues()
                } catch (e: Exception) {
                    // Handle calculation errors silently for auto-calculation
                }
                isCalculating = false
                updateResultDisplay()
            }
        }
    }

    private fun calculateSpecificValue(target: String) {
        binding.apply {
            val mass = etMass.text.toString().toDoubleOrNull()
            val molecularWeight = etMolecularWeight.text.toString().toDoubleOrNull()
            val volume = etVolume.text.toString().toDoubleOrNull()
            val molarity = etMolarity.text.toString().toDoubleOrNull()

            try {
                when (target) {
                    "molarity" -> {
                        if (mass != null && molecularWeight != null && volume != null) {
                            val massInGrams = convertToGrams(mass, binding.spinnerMassUnit.selectedItem.toString())
                            val volumeInLiters = convertToLiters(volume, binding.spinnerVolumeUnit.selectedItem.toString())
                            val moles = massInGrams / molecularWeight
                            val result = moles / volumeInLiters
                            etMolarity.setText(String.format("%.6f", result))
                        } else {
                            showError("Need mass, molecular weight, and volume to calculate molarity")
                            return
                        }
                    }
                    "moles" -> {
                        if (molarity != null && volume != null) {
                            val volumeInLiters = convertToLiters(volume, binding.spinnerVolumeUnit.selectedItem.toString())
                            val result = molarity * volumeInLiters
                            // Calculate and show mass if molecular weight is available
                            if (molecularWeight != null) {
                                val massInGrams = result * molecularWeight
                                val massInSelectedUnit = convertFromGrams(massInGrams, binding.spinnerMassUnit.selectedItem.toString())
                                etMass.setText(String.format("%.6f", massInSelectedUnit))
                            }
                            Toast.makeText(requireContext(), "Moles calculated: %.6f mol".format(result), Toast.LENGTH_SHORT).show()
                        } else {
                            showError("Need molarity and volume to calculate moles")
                            return
                        }
                    }
                    "volume" -> {
                        if (mass != null && molecularWeight != null && molarity != null) {
                            val massInGrams = convertToGrams(mass, binding.spinnerMassUnit.selectedItem.toString())
                            val moles = massInGrams / molecularWeight
                            val volumeInLiters = moles / molarity
                            val volumeInSelectedUnit = convertFromLiters(volumeInLiters, binding.spinnerVolumeUnit.selectedItem.toString())
                            val result = volumeInSelectedUnit
                            etVolume.setText(String.format("%.6f", result))
                        } else {
                            showError("Need mass, molecular weight, and molarity to calculate volume")
                            return
                        }
                    }
                    "mass" -> {
                        if (molarity != null && volume != null && molecularWeight != null) {
                            val volumeInLiters = convertToLiters(volume, binding.spinnerVolumeUnit.selectedItem.toString())
                            val moles = molarity * volumeInLiters
                            val massInGrams = moles * molecularWeight
                            val massInSelectedUnit = convertFromGrams(massInGrams, binding.spinnerMassUnit.selectedItem.toString())
                            val result = massInSelectedUnit
                            etMass.setText(String.format("%.6f", result))
                        } else {
                            showError("Need molarity, volume, and molecular weight to calculate mass")
                            return
                        }
                    }
                }
                updateResultDisplay()
            } catch (e: Exception) {
                showError("Calculation error: ${e.message}")
            }
        }
    }

    private fun calculateMolarity() {
        binding.apply {
            val mass = etMass.text.toString().toDoubleOrNull()
            val molecularWeight = etMolecularWeight.text.toString().toDoubleOrNull()
            val volume = etVolume.text.toString().toDoubleOrNull()
            val molarity = etMolarity.text.toString().toDoubleOrNull()

            val filledFields = listOfNotNull(mass, molecularWeight, volume, molarity).size

            when {
                filledFields < 3 -> {
                    showError("Please enter at least 3 values")
                }
                filledFields >= 3 -> {
                    try {
                        calculateMolarityValues()
                        updateResultDisplay()
                        Toast.makeText(requireContext(), "Calculation completed", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        showError("Calculation error: ${e.message}")
                    }
                }
            }
        }
    }

    private fun calculateMolarityValues() {
        binding.apply {
            val mass = etMass.text.toString().toDoubleOrNull()
            val molecularWeight = etMolecularWeight.text.toString().toDoubleOrNull()
            val volume = etVolume.text.toString().toDoubleOrNull()
            val molarity = etMolarity.text.toString().toDoubleOrNull()

            // Convert to standard units
            val massInGrams = mass?.let { convertToGrams(it, binding.spinnerMassUnit.selectedItem.toString()) }
            val volumeInLiters = volume?.let { convertToLiters(it, binding.spinnerVolumeUnit.selectedItem.toString()) }

            // Calculate missing values
            when {
                mass != null && molecularWeight != null && volume != null && molarity == null -> {
                    val moles = massInGrams!! / molecularWeight
                    val calculatedMolarity = moles / volumeInLiters!!
                    etMolarity.setText(String.format("%.6f", calculatedMolarity))
                }
                molarity != null && volume != null && molecularWeight != null && mass == null -> {
                    val moles = molarity * volumeInLiters!!
                    val calculatedMassInGrams = moles * molecularWeight
                    val calculatedMassInSelectedUnit = convertFromGrams(calculatedMassInGrams, binding.spinnerMassUnit.selectedItem.toString())
                    etMass.setText(String.format("%.6f", calculatedMassInSelectedUnit))
                }
                molarity != null && mass != null && molecularWeight != null && volume == null -> {
                    val moles = massInGrams!! / molecularWeight
                    val calculatedVolumeInLiters = moles / molarity
                    val calculatedVolumeInSelectedUnit = convertFromLiters(calculatedVolumeInLiters, binding.spinnerVolumeUnit.selectedItem.toString())
                    etVolume.setText(String.format("%.6f", calculatedVolumeInSelectedUnit))
                }
                molarity != null && mass != null && volume != null && molecularWeight == null -> {
                    val moles = molarity * volumeInLiters!!
                    val calculatedMolecularWeight = massInGrams!! / moles
                    etMolecularWeight.setText(String.format("%.6f", calculatedMolecularWeight))
                }
            }
        }
    }

    // Unit conversion functions
    private fun convertToGrams(value: Double, unit: String): Double {
        return when (unit) {
            "g" -> value
            "mg" -> value / 1000.0
            "µg" -> value / 1000000.0
            "kg" -> value * 1000.0
            "oz" -> value * 28.3495
            "lb" -> value * 453.592
            else -> value
        }
    }

    private fun convertFromGrams(value: Double, unit: String): Double {
        return when (unit) {
            "g" -> value
            "mg" -> value * 1000.0
            "µg" -> value * 1000000.0
            "kg" -> value / 1000.0
            "oz" -> value / 28.3495
            "lb" -> value / 453.592
            else -> value
        }
    }

    private fun convertToLiters(value: Double, unit: String): Double {
        return when (unit) {
            "L" -> value
            "mL" -> value / 1000.0
            "µL" -> value / 1000000.0
            "gallon" -> value * 3.78541
            "fl oz" -> value * 0.0295735
            else -> value
        }
    }

    private fun convertFromLiters(value: Double, unit: String): Double {
        return when (unit) {
            "L" -> value
            "mL" -> value * 1000.0
            "µL" -> value * 1000000.0
            "gallon" -> value / 3.78541
            "fl oz" -> value / 0.0295735
            else -> value
        }
    }

    private fun updateResultDisplay() {
        binding.apply {
            val mass = etMass.text.toString().toDoubleOrNull()
            val molecularWeight = etMolecularWeight.text.toString().toDoubleOrNull()
            val volume = etVolume.text.toString().toDoubleOrNull()
            val molarity = etMolarity.text.toString().toDoubleOrNull()

            val result = StringBuilder()
            result.append("🧪 Molarity Calculation Results\n\n")

            // Show current values
            if (mass != null) {
                result.append("⚖️ Mass: %.6f %s\n".format(mass, binding.spinnerMassUnit.selectedItem.toString()))
            }
            if (molecularWeight != null) {
                result.append("⚗️ Molecular Weight: %.6f g/mol\n".format(molecularWeight))
            }
            if (volume != null) {
                result.append("📏 Volume: %.6f %s\n".format(volume, binding.spinnerVolumeUnit.selectedItem.toString()))
            }
            if (molarity != null) {
                result.append("🔬 Molarity: %.6f M\n".format(molarity))
            }

            // Calculate and show derived values
            if (mass != null && molecularWeight != null) {
                val massInGrams = convertToGrams(mass, binding.spinnerMassUnit.selectedItem.toString())
                val moles = massInGrams / molecularWeight
                result.append("🧮 Moles of solute: %.6f mol\n".format(moles))
            }

            result.append("\n📝 Formulas Used:\n")
            result.append("• M = n/V (Molarity = moles/Volume in L)\n")
            result.append("• n = m/MW (moles = mass/Molecular Weight)\n")
            result.append("• m = n × MW (mass = moles × Molecular Weight)\n")
            result.append("• V = n/M (Volume = moles/Molarity)\n")

            if (mass != null && molecularWeight != null && volume != null && molarity != null) {
                val massInGrams = convertToGrams(mass, binding.spinnerMassUnit.selectedItem.toString())
                val volumeInLiters = convertToLiters(volume, binding.spinnerVolumeUnit.selectedItem.toString())
                val moles = massInGrams / molecularWeight

                result.append("\n💡 Additional Information:\n")

                // Dilution calculations
                result.append("• For 1:10 dilution: %.6f M\n".format(molarity / 10))
                result.append("• For 1:100 dilution: %.6f M\n".format(molarity / 100))

                // Concentration conversions
                val ppm = (massInGrams * 1000000) / (volumeInLiters * 1000000) // assuming density = 1 g/mL
                result.append("• Concentration: %.2f ppm (assuming density = 1 g/mL)\n".format(ppm))

                // Amount of substance
                val particleCount = moles * 6.022e23
                result.append("• Number of particles: %.2e particles\n".format(particleCount))
            }

            tvResult.text = result.toString()
        }
    }

    private fun clearAllFields() {
        binding.apply {
            etMass.text?.clear()
            etMolecularWeight.text?.clear()
            etVolume.text?.clear()
            etMolarity.text?.clear()
            tvResult.text = "Enter at least 3 values to calculate molarity"
        }
        Toast.makeText(requireContext(), "All fields cleared", Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        binding.tvResult.text = "❌ Error: $message"
    }

    // Function to hide the keyboard
    private fun hideKeyboard() {
        val inputMethodManager =
            activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    private fun hideKeyboardFunctionality(view: View) {
        // Set up the touch listener for non-text box views
        view.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val imm =
                    activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                // Get the currently focused view (e.g., EditText)
                val currentFocusView = activity?.currentFocus
                if (currentFocusView != null) {
                    imm.hideSoftInputFromWindow(currentFocusView.windowToken, 0)
                    currentFocusView.clearFocus() // Clear focus to remove cursor from EditText
                }
            }
            false
        }
    }

    private fun animateView(view: View) {
        // Use coroutine to stop the animation after 3 seconds
        lifecycleScope.launch(Dispatchers.Main) {
            var glowAnimator: ObjectAnimator? = null
            // Start the glow animation and store the animator reference
            glowAnimator = view.startGlowAnimation()
            delay(3000)  // Wait for 3 seconds
            view.stopGlowAnimation(glowAnimator)  // Stop the animation after delay
        }
    }

    companion object {
        private const val TAG = "MolarityFragment"
    }
}