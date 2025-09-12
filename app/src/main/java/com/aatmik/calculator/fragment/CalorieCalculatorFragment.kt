package com.aatmik.calculator.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentCalorieCalculatorBinding
import kotlin.math.roundToInt

class CalorieCalculatorFragment : Fragment() {

    private lateinit var binding: FragmentCalorieCalculatorBinding
    private var isMetric: Boolean = true // By default, set to Metric

    // Activity level multipliers for BMR
    private val activityMultipliers = mapOf(
        "Sedentary (little/no exercise)" to 1.2,
        "Lightly active (light exercise/sports 1-3 days/week)" to 1.375,
        "Moderately active (moderate exercise/sports 3-5 days/week)" to 1.55,
        "Very active (hard exercise/sports 6-7 days a week)" to 1.725,
        "Extra active (very hard exercise/sports & physical job)" to 1.9
    )

    // Goal multipliers for calorie adjustment
    private val goalMultipliers = mapOf(
        "Lose weight (0.5 kg/week)" to -0.25,
        "Lose weight (1 kg/week)" to -0.5,
        "Maintain weight" to 0.0,
        "Gain weight (0.5 kg/week)" to 0.25,
        "Gain weight (1 kg/week)" to 0.5
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentCalorieCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()
        setupListeners()
    }

    private fun setupSpinners() {
        binding.apply {
            // Setup Activity Level Spinner
            val activityAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                activityMultipliers.keys.toList()
            )
            activityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerActivityLevel.adapter = activityAdapter

            // Setup Goal Spinner
            val goalAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                goalMultipliers.keys.toList()
            )
            goalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerGoal.adapter = goalAdapter

            // Set default selection to "Maintain weight"
            spinnerGoal.setSelection(2)
        }
    }

    private fun setupListeners() {
        binding.apply {
            // Handle back button press
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            // Text Watchers for automatic calculation
            etAge.addTextChangedListener(calorieTextWatcher)
            etHeight.addTextChangedListener(calorieTextWatcher)
            etWeight.addTextChangedListener(calorieTextWatcher)

            // Toggle between Metric and Imperial units
            btnUnitToggle.setOnClickListener {
                toggleUnitSystem()
            }

            // Calculate calories when the button is pressed
            btnCalculateCalories.setOnClickListener {
                calculateAndDisplayCalories()
            }

            // Clear all fields
            btnClear.setOnClickListener {
                clearAllFields()
            }
        }
    }

    // Method to toggle between Metric and Imperial units
    private fun toggleUnitSystem() {
        isMetric = !isMetric
        binding.apply {
            if (isMetric) {
                // Update UI for Metric
                btnUnitToggle.text = "Switch to Imperial"
                tvHeightLabel.text = "Height (cm)"
                tvWeightLabel.text = "Weight (kg)"
                etHeight.hint = "Enter height in cm"
                etWeight.hint = "Enter weight in kg"
            } else {
                // Update UI for Imperial
                btnUnitToggle.text = "Switch to Metric"
                tvHeightLabel.text = "Height (inches)"
                tvWeightLabel.text = "Weight (lbs)"
                etHeight.hint = "Enter height in inches"
                etWeight.hint = "Enter weight in lbs"
            }

            // Clear previous results when switching units
            tvCalorieResult.text = ""

            // Recalculate if fields are filled
            calculateAndDisplayCalories()
        }
    }

    // Method to clear all input fields
    private fun clearAllFields() {
        binding.apply {
            etAge.text?.clear()
            etHeight.text?.clear()
            etWeight.text?.clear()
            tvCalorieResult.text = ""
            radioMale.isChecked = true // Default to male
            spinnerActivityLevel.setSelection(0) // Default to sedentary
            spinnerGoal.setSelection(2) // Default to maintain weight
        }
        Toast.makeText(requireContext(), "All fields cleared", Toast.LENGTH_SHORT).show()
    }

    // Method to calculate and display the daily calorie needs
    private fun calculateAndDisplayCalories() {
        binding.apply {
            val ageText = etAge.text.toString()
            val heightText = etHeight.text.toString()
            val weightText = etWeight.text.toString()

            // Validate inputs
            if (ageText.isEmpty() || heightText.isEmpty() || weightText.isEmpty()) {
                tvCalorieResult.text = "Please fill in all fields"
                return
            }

            try {
                val age = ageText.toInt()
                var height = heightText.toDouble()
                var weight = weightText.toDouble()
                val isMale = radioMale.isChecked

                // Input validation
                if (age < 1 || age > 120) {
                    tvCalorieResult.text = "Please enter a valid age (1-120 years)"
                    return
                }

                // Convert Imperial to Metric if needed for calculation
                if (!isMetric) {
                    height = height * 2.54 // inches to cm
                    weight = weight * 0.453592 // lbs to kg
                }

                // Additional validation for converted values
                if (height < 50 || height > 300) { // cm
                    tvCalorieResult.text = "Please enter a valid height"
                    return
                }

                if (weight < 20 || weight > 500) { // kg
                    tvCalorieResult.text = "Please enter a valid weight"
                    return
                }

                // Calculate BMR using Mifflin-St Jeor Equation
                val bmr = if (isMale) {
                    (10 * weight) + (6.25 * height) - (5 * age) + 5
                } else {
                    (10 * weight) + (6.25 * height) - (5 * age) - 161
                }

                // Get activity level multiplier
                val selectedActivity = spinnerActivityLevel.selectedItem.toString()
                val activityMultiplier = activityMultipliers[selectedActivity] ?: 1.2

                // Calculate TDEE (Total Daily Energy Expenditure)
                val tdee = bmr * activityMultiplier

                // Get goal adjustment
                val selectedGoal = spinnerGoal.selectedItem.toString()
                val goalAdjustment = goalMultipliers[selectedGoal] ?: 0.0

                // Calculate final calorie target
                val calorieTarget = tdee * (1 + goalAdjustment)

                // Display results
                displayResults(bmr.roundToInt(), tdee.roundToInt(), calorieTarget.roundToInt(), weight, height, isMale)

            } catch (e: NumberFormatException) {
                Log.e(TAG, "Invalid number format", e)
                tvCalorieResult.text = "Please enter valid numerical values"
            }
        }
    }

    private fun displayResults(bmr: Int, tdee: Int, calorieTarget: Int, weightKg: Double, heightCm: Double, isMale: Boolean) {
        binding.apply {
            val resultText = StringBuilder()

            // Main result
            resultText.append("🎯 Daily Calorie Target: $calorieTarget calories\n\n")

            // Breakdown
            resultText.append("📊 Breakdown:\n")
            resultText.append("• BMR (Base Metabolic Rate): $bmr calories\n")
            resultText.append("• TDEE (Total Daily Energy): $tdee calories\n")
            resultText.append("• Your Goal: ${binding.spinnerGoal.selectedItem}\n\n")

            // Additional info
            resultText.append("💡 Additional Info:\n")

            // Calculate BMI
            val heightInMeters = heightCm / 100
            val bmi = weightKg / (heightInMeters * heightInMeters)
            val bmiCategory = when {
                bmi < 18.5 -> "Underweight"
                bmi in 18.5..24.9 -> "Normal weight"
                bmi in 25.0..29.9 -> "Overweight"
                else -> "Obesity"
            }
            resultText.append("• BMI: %.1f ($bmiCategory)\n".format(bmi))

            // Macronutrient recommendations (basic guidelines)
            val protein = (calorieTarget * 0.20 / 4).roundToInt() // 20% of calories, 4 cal/g
            val carbs = (calorieTarget * 0.50 / 4).roundToInt() // 50% of calories, 4 cal/g
            val fats = (calorieTarget * 0.30 / 9).roundToInt() // 30% of calories, 9 cal/g

            resultText.append("• Recommended daily protein: ${protein}g\n")
            resultText.append("• Recommended daily carbs: ${carbs}g\n")
            resultText.append("• Recommended daily fats: ${fats}g\n\n")

            // Tips based on goal
            val selectedGoal = spinnerGoal.selectedItem.toString()
            when {
                selectedGoal.contains("Lose weight") -> {
                    resultText.append("💪 Weight Loss Tips:\n")
                    resultText.append("• Create a moderate calorie deficit\n")
                    resultText.append("• Focus on protein to maintain muscle\n")
                    resultText.append("• Stay hydrated and get enough sleep\n")
                    resultText.append("• Combine with regular exercise\n")
                }
                selectedGoal.contains("Gain weight") -> {
                    resultText.append("🏋️ Weight Gain Tips:\n")
                    resultText.append("• Eat in a controlled calorie surplus\n")
                    resultText.append("• Focus on nutrient-dense foods\n")
                    resultText.append("• Include strength training\n")
                    resultText.append("• Eat frequent, balanced meals\n")
                }
                else -> {
                    resultText.append("⚖️ Maintenance Tips:\n")
                    resultText.append("• Focus on balanced nutrition\n")
                    resultText.append("• Stay consistent with eating habits\n")
                    resultText.append("• Regular physical activity\n")
                    resultText.append("• Monitor weight weekly\n")
                }
            }

            tvCalorieResult.text = resultText.toString()
        }
    }

    // TextWatcher to handle real-time input
    private val calorieTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            // Only auto-calculate if all required fields are filled
            binding.apply {
                val ageText = etAge.text.toString()
                val heightText = etHeight.text.toString()
                val weightText = etWeight.text.toString()

                if (ageText.isNotEmpty() && heightText.isNotEmpty() && weightText.isNotEmpty()) {
                    calculateAndDisplayCalories()
                }
            }
        }
    }

    companion object {
        private const val TAG = "CalorieCalculatorFragment"
    }
}