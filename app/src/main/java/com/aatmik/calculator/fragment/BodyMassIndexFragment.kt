package com.aatmik.calculator.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentBodyMassIndexBinding

class BodyMassIndexFragment : Fragment() {

    private lateinit var binding: FragmentBodyMassIndexBinding
    private var isMetric: Boolean = true // By default, set to Metric

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            // Handle back button press
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            // Text Watcher for height and weight fields
            etHeight.addTextChangedListener(bmiTextWatcher)
            etWeight.addTextChangedListener(bmiTextWatcher)

            // Toggle between Metric and Imperial units
            btnUnitToggle.setOnClickListener {
                toggleUnitSystem()
            }

            // Calculate BMI when the button is pressed
            btnCalculateBmi.setOnClickListener {
                calculateAndDisplayBmi()
            }
        }
    }

    // Method to toggle between Metric and Imperial units
    private fun toggleUnitSystem() {
        isMetric = !isMetric // Toggle the boolean value
        binding.apply {
            if (isMetric) {
                // Update UI for Metric
                btnUnitToggle.text = "Switch to Imperial"
                tvHeightLabel.text = "Height (cm)"
                tvWeightLabel.text = "Weight (kg)"
            } else {
                // Update UI for Imperial
                btnUnitToggle.text = "Switch to Metric"
                tvHeightLabel.text = "Height (in)"
                tvWeightLabel.text = "Weight (lb)"
            }
            // Recalculate BMI in the new unit system
            calculateAndDisplayBmi()
        }
    }

    // Method to calculate and display the BMI
    private fun calculateAndDisplayBmi() {
        binding.apply {
            val heightText = etHeight.text.toString()
            val weightText = etWeight.text.toString()

            if (heightText.isEmpty() || weightText.isEmpty()) {
                tvBmiResult.text = "Please enter valid values"
                return
            }

            try {
                var bmi: Double
                val height = heightText.toDouble()
                val weight = weightText.toDouble()

                if (isMetric) {
                    // Metric system: height in cm, weight in kg
                    val heightInMeters = height / 100
                    bmi = weight / (heightInMeters * heightInMeters)
                } else {
                    // Imperial system: height in inches, weight in pounds
                    bmi = 703 * (weight / (height * height))
                }

                // Format and display the result
                tvBmiResult.text = "Your BMI is %.2f".format(bmi)

                // Provide a description based on the BMI value
                val bmiDescription = when {
                    bmi < 18.5 -> "Underweight"
                    bmi in 18.5..24.9 -> "Normal weight"
                    bmi in 25.0..29.9 -> "Overweight"
                    else -> "Obesity"
                }
                tvBmiResult.append("\nCategory: $bmiDescription")

            } catch (e: NumberFormatException) {
                Log.e(TAG, "Invalid number format", e)
                tvBmiResult.text = "Please enter valid numerical values"
            }
        }
    }

    // TextWatcher to handle real-time input
    private val bmiTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            // Automatically calculate BMI when height and weight fields are filled
            calculateAndDisplayBmi()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentBodyMassIndexBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        private const val TAG = "BodyMassIndexFragment"
    }
}
