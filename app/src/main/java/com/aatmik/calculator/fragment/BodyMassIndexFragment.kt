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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            // Handle back button press
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            // Add text watchers for height and weight inputs to enable BMI calculation in real-time
            etHeight.addTextChangedListener(bmiTextWatcher)
            etWeight.addTextChangedListener(bmiTextWatcher)

            // Calculate BMI when the button is pressed
            btnCalculateBmi.setOnClickListener {
                calculateAndDisplayBmi()
            }
        }
    }

    // TextWatcher to listen for changes in height and weight fields
    private val bmiTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            // Automatically calculate BMI when the height and weight fields are filled
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
                val heightInCm = heightText.toDouble()
                val weightInKg = weightText.toDouble()

                // Convert height to meters and calculate BMI
                val heightInMeters = heightInCm / 100
                val bmi = weightInKg / (heightInMeters * heightInMeters)

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentBodyMassIndexBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        private const val TAG = "BodyMassIndexFragment"
    }
}
