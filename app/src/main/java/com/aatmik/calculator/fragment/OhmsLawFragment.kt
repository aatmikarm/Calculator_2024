package com.aatmik.calculator.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentOhmsLawBinding
import kotlin.math.pow
import kotlin.math.sqrt

class OhmsLawFragment : Fragment() {

    private lateinit var binding: FragmentOhmsLawBinding
    private var isCalculating = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentOhmsLawBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        setupTextWatchers()
    }

    private fun setupListeners() {
        binding.apply {
            // Handle back button press
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            // Calculate button
            btnCalculate.setOnClickListener {
                calculateOhmsLaw()
            }

            // Clear button
            btnClear.setOnClickListener {
                clearAllFields()
            }

            // Formula buttons for quick calculation
            btnVoltage.setOnClickListener {
                calculateSpecificValue("voltage")
            }

            btnCurrent.setOnClickListener {
                calculateSpecificValue("current")
            }

            btnResistance.setOnClickListener {
                calculateSpecificValue("resistance")
            }

            btnPower.setOnClickListener {
                calculateSpecificValue("power")
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
            etVoltage.addTextChangedListener(textWatcher)
            etCurrent.addTextChangedListener(textWatcher)
            etResistance.addTextChangedListener(textWatcher)
            etPower.addTextChangedListener(textWatcher)
        }
    }

    private fun autoCalculate() {
        binding.apply {
            val voltage = etVoltage.text.toString().toDoubleOrNull()
            val current = etCurrent.text.toString().toDoubleOrNull()
            val resistance = etResistance.text.toString().toDoubleOrNull()
            val power = etPower.text.toString().toDoubleOrNull()

            val filledFields = listOfNotNull(voltage, current, resistance, power).size

            // Auto-calculate if exactly 2 fields are filled
            if (filledFields == 2) {
                isCalculating = true
                when {
                    voltage != null && current != null -> {
                        // Calculate R and P
                        val r = voltage / current
                        val p = voltage * current
                        if (etResistance.text.isEmpty()) etResistance.setText(String.format("%.4f", r))
                        if (etPower.text.isEmpty()) etPower.setText(String.format("%.4f", p))
                    }
                    voltage != null && resistance != null -> {
                        // Calculate I and P
                        val i = voltage / resistance
                        val p = (voltage * voltage) / resistance
                        if (etCurrent.text.isEmpty()) etCurrent.setText(String.format("%.4f", i))
                        if (etPower.text.isEmpty()) etPower.setText(String.format("%.4f", p))
                    }
                    voltage != null && power != null -> {
                        // Calculate I and R
                        val i = power / voltage
                        val r = voltage / i
                        if (etCurrent.text.isEmpty()) etCurrent.setText(String.format("%.4f", i))
                        if (etResistance.text.isEmpty()) etResistance.setText(String.format("%.4f", r))
                    }
                    current != null && resistance != null -> {
                        // Calculate V and P
                        val v = current * resistance
                        val p = current * current * resistance
                        if (etVoltage.text.isEmpty()) etVoltage.setText(String.format("%.4f", v))
                        if (etPower.text.isEmpty()) etPower.setText(String.format("%.4f", p))
                    }
                    current != null && power != null -> {
                        // Calculate V and R
                        val v = power / current
                        val r = v / current
                        if (etVoltage.text.isEmpty()) etVoltage.setText(String.format("%.4f", v))
                        if (etResistance.text.isEmpty()) etResistance.setText(String.format("%.4f", r))
                    }
                    resistance != null && power != null -> {
                        // Calculate V and I
                        val v = sqrt(power * resistance)
                        val i = v / resistance
                        if (etVoltage.text.isEmpty()) etVoltage.setText(String.format("%.4f", v))
                        if (etCurrent.text.isEmpty()) etCurrent.setText(String.format("%.4f", i))
                    }
                }
                isCalculating = false
                updateResultDisplay()
            } else if (filledFields > 2) {
                // Validate consistency when more than 2 fields are filled
                validateConsistency()
            }
        }
    }

    private fun calculateSpecificValue(target: String) {
        binding.apply {
            val voltage = etVoltage.text.toString().toDoubleOrNull()
            val current = etCurrent.text.toString().toDoubleOrNull()
            val resistance = etResistance.text.toString().toDoubleOrNull()
            val power = etPower.text.toString().toDoubleOrNull()

            try {
                when (target) {
                    "voltage" -> {
                        val result = when {
                            current != null && resistance != null -> current * resistance
                            current != null && power != null -> power / current
                            resistance != null && power != null -> sqrt(power * resistance)
                            else -> {
                                showError("Need 2 other values to calculate voltage")
                                return
                            }
                        }
                        etVoltage.setText(String.format("%.4f", result))
                    }
                    "current" -> {
                        val result = when {
                            voltage != null && resistance != null -> voltage / resistance
                            voltage != null && power != null -> power / voltage
                            resistance != null && power != null -> sqrt(power / resistance)
                            else -> {
                                showError("Need 2 other values to calculate current")
                                return
                            }
                        }
                        etCurrent.setText(String.format("%.4f", result))
                    }
                    "resistance" -> {
                        val result = when {
                            voltage != null && current != null -> voltage / current
                            voltage != null && power != null -> (voltage * voltage) / power
                            current != null && power != null -> power / (current * current)
                            else -> {
                                showError("Need 2 other values to calculate resistance")
                                return
                            }
                        }
                        etResistance.setText(String.format("%.4f", result))
                    }
                    "power" -> {
                        val result = when {
                            voltage != null && current != null -> voltage * current
                            voltage != null && resistance != null -> (voltage * voltage) / resistance
                            current != null && resistance != null -> current * current * resistance
                            else -> {
                                showError("Need 2 other values to calculate power")
                                return
                            }
                        }
                        etPower.setText(String.format("%.4f", result))
                    }
                }
                updateResultDisplay()
            } catch (e: Exception) {
                showError("Calculation error: ${e.message}")
            }
        }
    }

    private fun calculateOhmsLaw() {
        binding.apply {
            val voltage = etVoltage.text.toString().toDoubleOrNull()
            val current = etCurrent.text.toString().toDoubleOrNull()
            val resistance = etResistance.text.toString().toDoubleOrNull()
            val power = etPower.text.toString().toDoubleOrNull()

            val filledFields = listOfNotNull(voltage, current, resistance, power).size

            when {
                filledFields < 2 -> {
                    showError("Please enter at least 2 values")
                }
                filledFields >= 2 -> {
                    autoCalculate()
                    Toast.makeText(requireContext(), "Calculation completed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun validateConsistency() {
        binding.apply {
            val voltage = etVoltage.text.toString().toDoubleOrNull()
            val current = etCurrent.text.toString().toDoubleOrNull()
            val resistance = etResistance.text.toString().toDoubleOrNull()
            val power = etPower.text.toString().toDoubleOrNull()

            if (voltage != null && current != null && resistance != null && power != null) {
                val calculatedV1 = current * resistance
                val calculatedV2 = sqrt(power * resistance)
                val calculatedI1 = voltage / resistance
                val calculatedI2 = power / voltage
                val calculatedR1 = voltage / current
                val calculatedR2 = (voltage * voltage) / power
                val calculatedP1 = voltage * current
                val calculatedP2 = current * current * resistance

                val tolerance = 0.01 // 1% tolerance

                val isConsistent =
                    kotlin.math.abs(voltage - calculatedV1) / voltage < tolerance &&
                            kotlin.math.abs(current - calculatedI1) / current < tolerance &&
                            kotlin.math.abs(resistance - calculatedR1) / resistance < tolerance &&
                            kotlin.math.abs(power - calculatedP1) / power < tolerance

                if (!isConsistent) {
                    tvResult.text = "⚠️ Warning: Values may not be consistent with Ohm's Law"
                } else {
                    updateResultDisplay()
                }
            }
        }
    }

    private fun updateResultDisplay() {
        binding.apply {
            val voltage = etVoltage.text.toString().toDoubleOrNull()
            val current = etCurrent.text.toString().toDoubleOrNull()
            val resistance = etResistance.text.toString().toDoubleOrNull()
            val power = etPower.text.toString().toDoubleOrNull()

            val result = StringBuilder()
            result.append("⚡ Ohm's Law Results\n\n")

            if (voltage != null) {
                result.append("🔋 Voltage: %.4f V\n".format(voltage))
            }
            if (current != null) {
                result.append("⚡ Current: %.4f A\n".format(current))
            }
            if (resistance != null) {
                result.append("🔧 Resistance: %.4f Ω\n".format(resistance))
            }
            if (power != null) {
                result.append("💡 Power: %.4f W\n".format(power))
            }

            result.append("\n📝 Formulas Used:\n")
            result.append("• V = I × R (Voltage = Current × Resistance)\n")
            result.append("• I = V ÷ R (Current = Voltage ÷ Resistance)\n")
            result.append("• R = V ÷ I (Resistance = Voltage ÷ Current)\n")
            result.append("• P = V × I (Power = Voltage × Current)\n")
            result.append("• P = I² × R (Power = Current² × Resistance)\n")
            result.append("• P = V² ÷ R (Power = Voltage² ÷ Resistance)\n")

            if (voltage != null && current != null && resistance != null && power != null) {
                result.append("\n💡 Additional Info:\n")

                // Energy calculations for different time periods
                val energyPerHour = power / 1000 // kWh
                val energyPerDay = energyPerHour * 24
                val energyPerMonth = energyPerDay * 30

                result.append("• Energy consumed per hour: %.4f kWh\n".format(energyPerHour))
                result.append("• Energy consumed per day: %.4f kWh\n".format(energyPerDay))
                result.append("• Energy consumed per month: %.4f kWh\n".format(energyPerMonth))

                // Cost estimation (assuming $0.12 per kWh)
                val costPerHour = energyPerHour * 0.12
                val costPerDay = energyPerDay * 0.12
                val costPerMonth = energyPerMonth * 0.12

                result.append("• Estimated cost per hour: $%.4f\n".format(costPerHour))
                result.append("• Estimated cost per day: $%.4f\n".format(costPerDay))
                result.append("• Estimated cost per month: $%.4f\n".format(costPerMonth))
            }

            tvResult.text = result.toString()
        }
    }

    private fun clearAllFields() {
        binding.apply {
            etVoltage.text?.clear()
            etCurrent.text?.clear()
            etResistance.text?.clear()
            etPower.text?.clear()
            tvResult.text = "Enter at least 2 values to calculate using Ohm's Law"
        }
        Toast.makeText(requireContext(), "All fields cleared", Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        binding.tvResult.text = "❌ Error: $message"
    }

    companion object {
        private const val TAG = "OhmsLawFragment"
    }
}