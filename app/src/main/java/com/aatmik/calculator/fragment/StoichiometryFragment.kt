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
import com.aatmik.calculator.databinding.FragmentStoichiometryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import startGlowAnimation
import stopGlowAnimation

class StoichiometryFragment : Fragment() {

    private lateinit var binding: FragmentStoichiometryBinding
    private var isCalculating = false

    // Common molecular weights (g/mol)
    private val commonMolecularWeights = mapOf(
        "H2O" to 18.015,
        "CO2" to 44.01,
        "O2" to 32.00,
        "H2" to 2.016,
        "N2" to 28.014,
        "CH4" to 16.04,
        "NH3" to 17.03,
        "HCl" to 36.46,
        "NaCl" to 58.44,
        "CaCO3" to 100.09,
        "C6H12O6" to 180.16,
        "C2H6O" to 46.07,
        "H2SO4" to 98.08,
        "NaOH" to 40.00,
        "C" to 12.01,
        "Fe" to 55.85,
        "Al" to 26.98,
        "Cu" to 63.55,
        "Zn" to 65.38
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentStoichiometryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        setupSpinners()
        animateView(binding.stoichiometryLayout)
        hideKeyboardFunctionality(view)
        setupListeners()
        setupTextWatchers()
    }

    private fun setupSpinners() {
        // Unit spinners
        val massUnits = arrayOf("g", "mg", "kg", "oz", "lb")
        val volumeUnits = arrayOf("L", "mL", "m³", "cm³")
        val calculationTypes = arrayOf("Mass to Mass", "Moles to Moles", "Mass to Moles", "Moles to Mass", "Volume to Mass", "Limiting Reagent")

        val massAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, massUnits)
        massAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerGivenUnit.adapter = massAdapter
        binding.spinnerWantedUnit.adapter = massAdapter

        val calculationAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, calculationTypes)
        calculationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCalculationType.adapter = calculationAdapter
    }

    private fun setupListeners() {
        binding.apply {
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            btnCalculate.setOnClickListener {
                hideKeyboard()
                calculateStoichiometry()
            }

            btnClear.setOnClickListener {
                clearAllFields()
            }

            btnBalance.setOnClickListener {
                balanceEquation()
            }

            btnLookupMW.setOnClickListener {
                lookupMolecularWeight()
            }

            // Set up the editor action listeners
            etChemicalEquation.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    etGivenCompound.requestFocus()
                    true
                } else {
                    false
                }
            }

            etGivenCompound.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    etGivenQuantity.requestFocus()
                    true
                } else {
                    false
                }
            }

            etGivenQuantity.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    etGivenMW.requestFocus()
                    true
                } else {
                    false
                }
            }

            etGivenMW.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    etWantedCompound.requestFocus()
                    true
                } else {
                    false
                }
            }

            etWantedCompound.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    etWantedMW.requestFocus()
                    true
                } else {
                    false
                }
            }

            etWantedMW.setOnEditorActionListener { _, actionId, event ->
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
                    autoLookupMW()
                }
            }
        }

        binding.apply {
            etGivenCompound.addTextChangedListener(textWatcher)
            etWantedCompound.addTextChangedListener(textWatcher)
        }
    }

    private fun autoLookupMW() {
        binding.apply {
            val givenCompound = etGivenCompound.text.toString().trim()
            val wantedCompound = etWantedCompound.text.toString().trim()

            // Auto-fill molecular weights for common compounds
            if (givenCompound in commonMolecularWeights && etGivenMW.text.isEmpty()) {
                etGivenMW.setText(commonMolecularWeights[givenCompound].toString())
            }

            if (wantedCompound in commonMolecularWeights && etWantedMW.text.isEmpty()) {
                etWantedMW.setText(commonMolecularWeights[wantedCompound].toString())
            }
        }
    }

    private fun lookupMolecularWeight() {
        binding.apply {
            val givenCompound = etGivenCompound.text.toString().trim()
            val wantedCompound = etWantedCompound.text.toString().trim()

            var found = false

            if (givenCompound in commonMolecularWeights) {
                etGivenMW.setText(commonMolecularWeights[givenCompound].toString())
                found = true
            }

            if (wantedCompound in commonMolecularWeights) {
                etWantedMW.setText(commonMolecularWeights[wantedCompound].toString())
                found = true
            }

            if (found) {
                Toast.makeText(requireContext(), "Molecular weights found and filled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Compounds not found in database. Enter manually.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun balanceEquation() {
        binding.apply {
            val equation = etChemicalEquation.text.toString().trim()

            if (equation.isEmpty()) {
                showError("Enter a chemical equation first")
                return
            }

            // Simple equation balancing for common reactions (this is a simplified implementation)
            val balancedEquation = when {
                equation.contains("H2") && equation.contains("O2") && equation.contains("H2O") ->
                    "2H2 + O2 → 2H2O"
                equation.contains("CH4") && equation.contains("O2") && equation.contains("CO2") ->
                    "CH4 + 2O2 → CO2 + 2H2O"
                equation.contains("C2H6") && equation.contains("O2") && equation.contains("CO2") ->
                    "2C2H6 + 7O2 → 4CO2 + 6H2O"
                equation.contains("NH3") && equation.contains("O2") && equation.contains("NO") ->
                    "4NH3 + 5O2 → 4NO + 6H2O"
                equation.contains("CaCO3") && equation.contains("HCl") ->
                    "CaCO3 + 2HCl → CaCl2 + CO2 + H2O"
                equation.contains("NaOH") && equation.contains("HCl") ->
                    "NaOH + HCl → NaCl + H2O"
                else -> equation // Return original if not in our simple database
            }

            if (balancedEquation != equation) {
                etChemicalEquation.setText(balancedEquation)
                Toast.makeText(requireContext(), "Equation balanced!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Could not auto-balance. Please balance manually.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun calculateStoichiometry() {
        binding.apply {
            val equation = etChemicalEquation.text.toString().trim()
            val givenCompound = etGivenCompound.text.toString().trim()
            val givenQuantity = etGivenQuantity.text.toString().toDoubleOrNull()
            val givenMW = etGivenMW.text.toString().toDoubleOrNull()
            val wantedCompound = etWantedCompound.text.toString().trim()
            val wantedMW = etWantedMW.text.toString().toDoubleOrNull()
            val givenCoeff = etGivenCoefficient.text.toString().toDoubleOrNull() ?: 1.0
            val wantedCoeff = etWantedCoefficient.text.toString().toDoubleOrNull() ?: 1.0

            when {
                equation.isEmpty() -> {
                    showError("Please enter a chemical equation")
                }
                givenCompound.isEmpty() || wantedCompound.isEmpty() -> {
                    showError("Please enter both given and wanted compounds")
                }
                givenQuantity == null -> {
                    showError("Please enter the given quantity")
                }
                givenMW == null || wantedMW == null -> {
                    showError("Please enter molecular weights for both compounds")
                }
                givenCoeff <= 0 || wantedCoeff <= 0 -> {
                    showError("Coefficients must be positive numbers")
                }
                else -> {
                    try {
                        performStoichiometryCalculation(
                            givenQuantity, givenMW, wantedMW, givenCoeff, wantedCoeff,
                            givenCompound, wantedCompound, equation
                        )
                        updateResultDisplay()
                        Toast.makeText(requireContext(), "Stoichiometry calculation completed", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        showError("Calculation error: ${e.message}")
                    }
                }
            }
        }
    }

    private fun performStoichiometryCalculation(
        givenQuantity: Double, givenMW: Double, wantedMW: Double,
        givenCoeff: Double, wantedCoeff: Double,
        givenCompound: String, wantedCompound: String, equation: String
    ) {
        binding.apply {
            val calculationType = spinnerCalculationType.selectedItem.toString()
            val givenUnit = spinnerGivenUnit.selectedItem.toString()
            val wantedUnit = spinnerWantedUnit.selectedItem.toString()

            // Convert given quantity to grams if necessary
            val givenQuantityInGrams = convertToGrams(givenQuantity, givenUnit)

            // Basic stoichiometry: given mass → moles → mole ratio → wanted moles → wanted mass
            val givenMoles = givenQuantityInGrams / givenMW
            val moleRatio = wantedCoeff / givenCoeff
            val wantedMoles = givenMoles * moleRatio
            val wantedMassInGrams = wantedMoles * wantedMW

            // Convert to desired unit
            val wantedQuantity = convertFromGrams(wantedMassInGrams, wantedUnit)

            // Store results for display
            val result = StringBuilder()
            result.append("Stoichiometry Calculation Results\n\n")
            result.append("Equation: $equation\n\n")
            result.append("Given: $givenQuantity $givenUnit of $givenCompound\n")
            result.append("Molecular Weight of $givenCompound: $givenMW g/mol\n")
            result.append("Coefficient of $givenCompound: $givenCoeff\n\n")

            result.append("Step-by-Step Calculation:\n")
            result.append("1. Convert to moles:\n")
            result.append("   $givenQuantityInGrams g ÷ $givenMW g/mol = %.6f mol\n\n".format(givenMoles))

            result.append("2. Apply mole ratio:\n")
            result.append("   Ratio = $wantedCoeff/$givenCoeff = %.4f\n".format(moleRatio))
            result.append("   %.6f mol × %.4f = %.6f mol of $wantedCompound\n\n".format(givenMoles, moleRatio, wantedMoles))

            result.append("3. Convert to mass:\n")
            result.append("   %.6f mol × $wantedMW g/mol = %.6f g\n\n".format(wantedMoles, wantedMassInGrams))

            result.append("Final Answer:\n")
            result.append("%.6f $wantedUnit of $wantedCompound\n\n".format(wantedQuantity))

            // Additional calculations
            result.append("Additional Information:\n")
            result.append("• Moles of $givenCompound used: %.6f mol\n".format(givenMoles))
            result.append("• Moles of $wantedCompound produced: %.6f mol\n".format(wantedMoles))
            result.append("• Molar ratio ($wantedCompound:$givenCompound): %.4f:1\n".format(moleRatio))

            if (wantedQuantity > 1000 && wantedUnit == "g") {
                result.append("• Result in kg: %.6f kg\n".format(wantedQuantity / 1000))
            }

            tvResult.text = result.toString()
        }
    }

    // Unit conversion functions
    private fun convertToGrams(value: Double, unit: String): Double {
        return when (unit) {
            "g" -> value
            "mg" -> value / 1000.0
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
            "kg" -> value / 1000.0
            "oz" -> value / 28.3495
            "lb" -> value / 453.592
            else -> value
        }
    }

    private fun updateResultDisplay() {
        // Result is already updated in performStoichiometryCalculation
        // This method can be used for additional UI updates if needed
    }

    private fun clearAllFields() {
        binding.apply {
            etChemicalEquation.text?.clear()
            etGivenCompound.text?.clear()
            etGivenQuantity.text?.clear()
            etGivenMW.text?.clear()
            etGivenCoefficient.setText("1")
            etWantedCompound.text?.clear()
            etWantedMW.text?.clear()
            etWantedCoefficient.setText("1")
            tvResult.text = "Enter chemical equation and known quantities to perform stoichiometry calculations"
        }
        Toast.makeText(requireContext(), "All fields cleared", Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        binding.tvResult.text = "Error: $message"
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
        private const val TAG = "StoichiometryFragment"
    }
}