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
import com.aatmik.calculator.databinding.FragmentIdealGasLawBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import startGlowAnimation
import stopGlowAnimation
import kotlin.math.pow

class IdealGasLawFragment : Fragment() {

    private lateinit var binding: FragmentIdealGasLawBinding
    private var isCalculating = false

    // Gas constant R in different units
    private val gasConstants = mapOf(
        "atm·L/(mol·K)" to 0.08206,
        "kPa·L/(mol·K)" to 8.314,
        "mmHg·L/(mol·K)" to 62.36,
        "bar·L/(mol·K)" to 0.08314,
        "J/(mol·K)" to 8.314
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentIdealGasLawBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        setupSpinners()
        animateView(binding.gasLawLayout)
        hideKeyboardFunctionality(view)
        setupListeners()
        setupTextWatchers()
        updateGasConstant()
    }

    private fun setupSpinners() {
        // Pressure units
        val pressureUnits = arrayOf("atm", "kPa", "mmHg", "bar", "Pa")
        val pressureAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, pressureUnits)
        pressureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPressureUnit.adapter = pressureAdapter

        // Volume units
        val volumeUnits = arrayOf("L", "mL", "m³", "cm³")
        val volumeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, volumeUnits)
        volumeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerVolumeUnit.adapter = volumeAdapter

        // Amount units
        val amountUnits = arrayOf("mol", "mmol", "μmol", "kmol")
        val amountAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, amountUnits)
        amountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAmountUnit.adapter = amountAdapter

        // Temperature units
        val temperatureUnits = arrayOf("K", "°C", "°F")
        val temperatureAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, temperatureUnits)
        temperatureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTemperatureUnit.adapter = temperatureAdapter

        // Calculation type
        val calculationTypes = arrayOf("Find Pressure (P)", "Find Volume (V)", "Find Amount (n)", "Find Temperature (T)", "Combined Gas Law")
        val calculationAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, calculationTypes)
        calculationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCalculationType.adapter = calculationAdapter

        // Set listeners for unit changes
        binding.spinnerPressureUnit.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateGasConstant()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
    }

    private fun setupListeners() {
        binding.apply {
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            btnCalculate.setOnClickListener {
                hideKeyboard()
                calculateIdealGas()
            }

            btnClear.setOnClickListener {
                clearAllFields()
            }

            btnSTP.setOnClickListener {
                setStandardConditions(true) // STP
            }

            btnSATP.setOnClickListener {
                setStandardConditions(false) // SATP
            }

            // Quick calculation buttons
            btnSolveP.setOnClickListener {
                binding.spinnerCalculationType.setSelection(0)
                calculateIdealGas()
            }

            btnSolveV.setOnClickListener {
                binding.spinnerCalculationType.setSelection(1)
                calculateIdealGas()
            }

            btnSolveN.setOnClickListener {
                binding.spinnerCalculationType.setSelection(2)
                calculateIdealGas()
            }

            btnSolveT.setOnClickListener {
                binding.spinnerCalculationType.setSelection(3)
                calculateIdealGas()
            }

            // Set up the editor action listeners
            etPressure.setOnEditorActionListener { _, actionId, event ->
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
                    etAmount.requestFocus()
                    true
                } else {
                    false
                }
            }

            etAmount.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    etTemperature.requestFocus()
                    true
                } else {
                    false
                }
            }

            etTemperature.setOnEditorActionListener { _, actionId, event ->
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
            etPressure.addTextChangedListener(textWatcher)
            etVolume.addTextChangedListener(textWatcher)
            etAmount.addTextChangedListener(textWatcher)
            etTemperature.addTextChangedListener(textWatcher)
        }
    }

    private fun autoCalculate() {
        binding.apply {
            val pressure = etPressure.text.toString().toDoubleOrNull()
            val volume = etVolume.text.toString().toDoubleOrNull()
            val amount = etAmount.text.toString().toDoubleOrNull()
            val temperature = etTemperature.text.toString().toDoubleOrNull()

            val filledFields = listOfNotNull(pressure, volume, amount, temperature).size

            // Auto-calculate if exactly 3 fields are filled
            if (filledFields == 3) {
                isCalculating = true
                try {
                    calculateIdealGasValues()
                } catch (e: Exception) {
                    // Handle calculation errors silently for auto-calculation
                }
                isCalculating = false
                updateResultDisplay()
            }
        }
    }

    private fun setStandardConditions(isSTP: Boolean) {
        binding.apply {
            if (isSTP) {
                // STP: 0°C (273.15 K), 1 atm
                etTemperature.setText("273.15")
                spinnerTemperatureUnit.setSelection(0) // K
                etPressure.setText("1")
                spinnerPressureUnit.setSelection(0) // atm
                Toast.makeText(requireContext(), "STP conditions set (0°C, 1 atm)", Toast.LENGTH_SHORT).show()
            } else {
                // SATP: 25°C (298.15 K), 1 bar
                etTemperature.setText("298.15")
                spinnerTemperatureUnit.setSelection(0) // K
                etPressure.setText("1")
                spinnerPressureUnit.setSelection(3) // bar
                Toast.makeText(requireContext(), "SATP conditions set (25°C, 1 bar)", Toast.LENGTH_SHORT).show()
            }
            updateGasConstant()
        }
    }

    private fun updateGasConstant() {
        val pressureUnit = binding.spinnerPressureUnit.selectedItem.toString()
        val rValue = when (pressureUnit) {
            "atm" -> 0.08206
            "kPa" -> 8.314
            "mmHg" -> 62.36
            "bar" -> 0.08314
            "Pa" -> 8314.0
            else -> 0.08206
        }

        binding.tvGasConstant.text = "R = $rValue ${getGasConstantUnit()}"
    }

    private fun getGasConstantUnit(): String {
        val pressureUnit = binding.spinnerPressureUnit.selectedItem.toString()
        return when (pressureUnit) {
            "atm" -> "atm·L/(mol·K)"
            "kPa" -> "kPa·L/(mol·K)"
            "mmHg" -> "mmHg·L/(mol·K)"
            "bar" -> "bar·L/(mol·K)"
            "Pa" -> "Pa·L/(mol·K)"
            else -> "atm·L/(mol·K)"
        }
    }

    private fun getGasConstantValue(): Double {
        val pressureUnit = binding.spinnerPressureUnit.selectedItem.toString()
        return when (pressureUnit) {
            "atm" -> 0.08206
            "kPa" -> 8.314
            "mmHg" -> 62.36
            "bar" -> 0.08314
            "Pa" -> 8314.0
            else -> 0.08206
        }
    }

    private fun calculateIdealGas() {
        binding.apply {
            val pressure = etPressure.text.toString().toDoubleOrNull()
            val volume = etVolume.text.toString().toDoubleOrNull()
            val amount = etAmount.text.toString().toDoubleOrNull()
            val temperature = etTemperature.text.toString().toDoubleOrNull()
            val calculationType = spinnerCalculationType.selectedItem.toString()

            val filledFields = listOfNotNull(pressure, volume, amount, temperature).size

            when {
                filledFields < 3 -> {
                    showError("Please enter at least 3 values to solve for the 4th")
                }
                filledFields == 4 -> {
                    // Verify if values are consistent with ideal gas law
                    validateIdealGasLaw()
                }
                else -> {
                    try {
                        calculateIdealGasValues()
                        updateResultDisplay()
                        Toast.makeText(requireContext(), "Calculation completed", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        showError("Calculation error: ${e.message}")
                    }
                }
            }
        }
    }

    private fun calculateIdealGasValues() {
        binding.apply {
            var pressure = etPressure.text.toString().toDoubleOrNull()
            var volume = etVolume.text.toString().toDoubleOrNull()
            var amount = etAmount.text.toString().toDoubleOrNull()
            var temperature = etTemperature.text.toString().toDoubleOrNull()

            // Convert all values to standard units for calculation
            pressure = pressure?.let { convertPressure(it, spinnerPressureUnit.selectedItem.toString(), "atm") }
            volume = volume?.let { convertVolume(it, spinnerVolumeUnit.selectedItem.toString(), "L") }
            amount = amount?.let { convertAmount(it, spinnerAmountUnit.selectedItem.toString(), "mol") }
            temperature = temperature?.let { convertTemperature(it, spinnerTemperatureUnit.selectedItem.toString(), "K") }

            val R = 0.08206 // atm·L/(mol·K)

            // Calculate missing value using PV = nRT
            when {
                pressure == null && volume != null && amount != null && temperature != null -> {
                    val calculatedP = (amount * R * temperature) / volume
                    val convertedP = convertPressure(calculatedP, "atm", spinnerPressureUnit.selectedItem.toString())
                    etPressure.setText(String.format("%.6f", convertedP))
                }
                volume == null && pressure != null && amount != null && temperature != null -> {
                    val calculatedV = (amount * R * temperature) / pressure
                    val convertedV = convertVolume(calculatedV, "L", spinnerVolumeUnit.selectedItem.toString())
                    etVolume.setText(String.format("%.6f", convertedV))
                }
                amount == null && pressure != null && volume != null && temperature != null -> {
                    val calculatedN = (pressure * volume) / (R * temperature)
                    val convertedN = convertAmount(calculatedN, "mol", spinnerAmountUnit.selectedItem.toString())
                    etAmount.setText(String.format("%.6f", convertedN))
                }
                temperature == null && pressure != null && volume != null && amount != null -> {
                    val calculatedT = (pressure * volume) / (amount * R)
                    val convertedT = convertTemperature(calculatedT, "K", spinnerTemperatureUnit.selectedItem.toString())
                    etTemperature.setText(String.format("%.6f", convertedT))
                }
            }
        }
    }

    private fun validateIdealGasLaw() {
        binding.apply {
            val pressure = etPressure.text.toString().toDoubleOrNull()
            val volume = etVolume.text.toString().toDoubleOrNull()
            val amount = etAmount.text.toString().toDoubleOrNull()
            val temperature = etTemperature.text.toString().toDoubleOrNull()

            if (pressure != null && volume != null && amount != null && temperature != null) {
                // Convert to standard units
                val pAtm = convertPressure(pressure, spinnerPressureUnit.selectedItem.toString(), "atm")
                val vL = convertVolume(volume, spinnerVolumeUnit.selectedItem.toString(), "L")
                val nMol = convertAmount(amount, spinnerAmountUnit.selectedItem.toString(), "mol")
                val tK = convertTemperature(temperature, spinnerTemperatureUnit.selectedItem.toString(), "K")

                val R = 0.08206
                val leftSide = pAtm * vL
                val rightSide = nMol * R * tK
                val percentError = kotlin.math.abs((leftSide - rightSide) / rightSide) * 100

                if (percentError < 1.0) {
                    tvResult.text = "✓ Values are consistent with ideal gas law (Error: ${String.format("%.2f", percentError)}%)"
                } else {
                    tvResult.text = "⚠ Values may not be consistent with ideal gas law (Error: ${String.format("%.2f", percentError)}%)"
                }
                updateResultDisplay()
            }
        }
    }

    // Unit conversion functions
    private fun convertPressure(value: Double, fromUnit: String, toUnit: String): Double {
        if (fromUnit == toUnit) return value

        // Convert to atm first
        val valueInAtm = when (fromUnit) {
            "atm" -> value
            "kPa" -> value / 101.325
            "mmHg" -> value / 760.0
            "bar" -> value / 1.01325
            "Pa" -> value / 101325.0
            else -> value
        }

        // Convert from atm to target unit
        return when (toUnit) {
            "atm" -> valueInAtm
            "kPa" -> valueInAtm * 101.325
            "mmHg" -> valueInAtm * 760.0
            "bar" -> valueInAtm * 1.01325
            "Pa" -> valueInAtm * 101325.0
            else -> valueInAtm
        }
    }

    private fun convertVolume(value: Double, fromUnit: String, toUnit: String): Double {
        if (fromUnit == toUnit) return value

        // Convert to L first
        val valueInL = when (fromUnit) {
            "L" -> value
            "mL" -> value / 1000.0
            "m³" -> value * 1000.0
            "cm³" -> value / 1000.0
            else -> value
        }

        // Convert from L to target unit
        return when (toUnit) {
            "L" -> valueInL
            "mL" -> valueInL * 1000.0
            "m³" -> valueInL / 1000.0
            "cm³" -> valueInL * 1000.0
            else -> valueInL
        }
    }

    private fun convertAmount(value: Double, fromUnit: String, toUnit: String): Double {
        if (fromUnit == toUnit) return value

        // Convert to mol first
        val valueInMol = when (fromUnit) {
            "mol" -> value
            "mmol" -> value / 1000.0
            "μmol" -> value / 1000000.0
            "kmol" -> value * 1000.0
            else -> value
        }

        // Convert from mol to target unit
        return when (toUnit) {
            "mol" -> valueInMol
            "mmol" -> valueInMol * 1000.0
            "μmol" -> valueInMol * 1000000.0
            "kmol" -> valueInMol / 1000.0
            else -> valueInMol
        }
    }

    private fun convertTemperature(value: Double, fromUnit: String, toUnit: String): Double {
        if (fromUnit == toUnit) return value

        // Convert to K first
        val valueInK = when (fromUnit) {
            "K" -> value
            "°C" -> value + 273.15
            "°F" -> (value - 32) * 5/9 + 273.15
            else -> value
        }

        // Convert from K to target unit
        return when (toUnit) {
            "K" -> valueInK
            "°C" -> valueInK - 273.15
            "°F" -> (valueInK - 273.15) * 9/5 + 32
            else -> valueInK
        }
    }

    private fun updateResultDisplay() {
        binding.apply {
            val pressure = etPressure.text.toString().toDoubleOrNull()
            val volume = etVolume.text.toString().toDoubleOrNull()
            val amount = etAmount.text.toString().toDoubleOrNull()
            val temperature = etTemperature.text.toString().toDoubleOrNull()

            if (pressure != null && volume != null && amount != null && temperature != null) {
                val result = StringBuilder()
                result.append("Ideal Gas Law Results\n\n")

                result.append("Given Values:\n")
                result.append("• Pressure: $pressure ${spinnerPressureUnit.selectedItem}\n")
                result.append("• Volume: $volume ${spinnerVolumeUnit.selectedItem}\n")
                result.append("• Amount: $amount ${spinnerAmountUnit.selectedItem}\n")
                result.append("• Temperature: $temperature ${spinnerTemperatureUnit.selectedItem}\n\n")

                // Convert to standard units for calculations
                val pAtm = convertPressure(pressure, spinnerPressureUnit.selectedItem.toString(), "atm")
                val vL = convertVolume(volume, spinnerVolumeUnit.selectedItem.toString(), "L")
                val nMol = convertAmount(amount, spinnerAmountUnit.selectedItem.toString(), "mol")
                val tK = convertTemperature(temperature, spinnerTemperatureUnit.selectedItem.toString(), "K")

                result.append("Standard Units:\n")
                result.append("• P = %.6f atm\n".format(pAtm))
                result.append("• V = %.6f L\n".format(vL))
                result.append("• n = %.6f mol\n".format(nMol))
                result.append("• T = %.6f K\n".format(tK))
                result.append("• R = 0.08206 atm·L/(mol·K)\n\n")

                result.append("Verification:\n")
                val leftSide = pAtm * vL
                val rightSide = nMol * 0.08206 * tK
                result.append("PV = %.6f atm·L\n".format(leftSide))
                result.append("nRT = %.6f atm·L\n".format(rightSide))
                val error = kotlin.math.abs((leftSide - rightSide) / rightSide) * 100
                result.append("Error: %.3f%%\n\n".format(error))

                // Additional calculations
                result.append("Additional Information:\n")

                // Molar volume at these conditions
                val molarVolume = vL / nMol
                result.append("• Molar volume: %.3f L/mol\n".format(molarVolume))

                // Density (assuming ideal gas)
                result.append("• Pressure in other units:\n")
                result.append("  - %.3f kPa\n".format(pAtm * 101.325))
                result.append("  - %.1f mmHg\n".format(pAtm * 760))
                result.append("  - %.6f bar\n".format(pAtm * 1.01325))

                result.append("• Temperature in other units:\n")
                result.append("  - %.2f °C\n".format(tK - 273.15))
                result.append("  - %.2f °F\n".format((tK - 273.15) * 9/5 + 32))

                tvResult.text = result.toString()
            }
        }
    }

    private fun clearAllFields() {
        binding.apply {
            etPressure.text?.clear()
            etVolume.text?.clear()
            etAmount.text?.clear()
            etTemperature.text?.clear()
            tvResult.text = "Enter any 3 values to calculate the 4th using the Ideal Gas Law"
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
        private const val TAG = "IdealGasLawFragment"
    }
}