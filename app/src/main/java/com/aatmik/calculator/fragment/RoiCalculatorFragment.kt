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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aatmik.calculator.R
import com.aatmik.calculator.databinding.FragmentRoiCalculatorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import startGlowAnimation
import stopGlowAnimation
import kotlin.math.abs
import kotlin.math.pow

class RoiCalculatorFragment : Fragment() {

    private lateinit var binding: FragmentRoiCalculatorBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentRoiCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        animateView(binding.roiLayout)
        hideKeyboardFunctionality(view)
        setupSpinners()

        binding.apply {
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            // Set a click listener on the Calculate button
            btnCalculate.setOnClickListener {
                hideKeyboard()
                calculateROI()
            }

            // Clear button functionality
            btnClear.setOnClickListener {
                clearAllFields()
            }

            // Text watchers for real-time calculation
            etInitialInvestment.addTextChangedListener(createTextWatcher())
            etFinalValue.addTextChangedListener(createTextWatcher())
            etTimePeriod.addTextChangedListener(createTextWatcher())
            etAdditionalInvestment.addTextChangedListener(createTextWatcher())

            // Set up editor action listeners
            etInitialInvestment.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    etFinalValue.requestFocus()
                    true
                } else {
                    false
                }
            }

            etFinalValue.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    etTimePeriod.requestFocus()
                    true
                } else {
                    false
                }
            }

            etTimePeriod.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    if (cardAdditionalInvestment.visibility == View.VISIBLE) {
                        etAdditionalInvestment.requestFocus()
                    } else {
                        hideKeyboard()
                    }
                    true
                } else {
                    false
                }
            }

            etAdditionalInvestment.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    hideKeyboard()
                    true
                } else {
                    hideKeyboard()
                    false
                }
            }
        }
    }

    private fun setupSpinners() {
        // Setup calculation type spinner
        val calculationTypes = arrayOf(
            "Simple ROI",
            "Annualized ROI",
            "ROI with Additional Investment"
        )

        val calculationAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, calculationTypes)
        calculationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerCalculationType.adapter = calculationAdapter

        binding.spinnerCalculationType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateUIForCalculationType(position)
                updateInstructions(position)
                calculateROI()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Setup time unit spinner
        val timeUnits = arrayOf("Days", "Months", "Years")

        val timeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, timeUnits)
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerTimeUnit.adapter = timeAdapter

        binding.spinnerTimeUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                calculateROI()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateUIForCalculationType(calculationType: Int) {
        binding.apply {
            when (calculationType) {
                0 -> { // Simple ROI
                    layoutTimePeriod.visibility = View.GONE
                    cardAdditionalInvestment.visibility = View.GONE
                    layoutAnnualizedROI.visibility = View.GONE
                }
                1 -> { // Annualized ROI
                    layoutTimePeriod.visibility = View.VISIBLE
                    cardAdditionalInvestment.visibility = View.GONE
                    layoutAnnualizedROI.visibility = View.VISIBLE
                }
                2 -> { // ROI with Additional Investment
                    layoutTimePeriod.visibility = View.VISIBLE
                    cardAdditionalInvestment.visibility = View.VISIBLE
                    layoutAnnualizedROI.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun updateInstructions(calculationType: Int) {
        val instructions = when (calculationType) {
            0 -> "Enter initial and final values for simple ROI calculation"
            1 -> "Enter values and time period for annualized ROI calculation"
            2 -> "Enter all values including additional investment over time"
            else -> "Enter investment details to calculate ROI"
        }
        binding.tvInstructions.text = instructions
    }

    private fun createTextWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                calculateROI()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
    }

    private fun calculateROI() {
        binding.apply {
            val initialStr = etInitialInvestment.text.toString()
            val finalStr = etFinalValue.text.toString()
            val timePeriodStr = etTimePeriod.text.toString()
            val additionalStr = etAdditionalInvestment.text.toString()

            if (initialStr.isEmpty() || finalStr.isEmpty()) {
                clearResults()
                return
            }

            try {
                val initialInvestment = initialStr.toDouble()
                val finalValue = finalStr.toDouble()
                val timePeriod = if (timePeriodStr.isNotEmpty()) timePeriodStr.toDouble() else 1.0
                val additionalInvestment = if (additionalStr.isNotEmpty()) additionalStr.toDouble() else 0.0

                if (initialInvestment <= 0) {
                    showError("Initial investment must be greater than 0")
                    return
                }

                val calculationType = spinnerCalculationType.selectedItemPosition
                val result = calculateROIResult(initialInvestment, finalValue, timePeriod, additionalInvestment, calculationType)

                displayResults(result, initialInvestment, finalValue, timePeriod, additionalInvestment, calculationType)

            } catch (e: NumberFormatException) {
                showError("Please enter valid numbers")
            }
        }
    }

    private fun calculateROIResult(
        initialInvestment: Double,
        finalValue: Double,
        timePeriod: Double,
        additionalInvestment: Double,
        calculationType: Int
    ): ROIResult {

        val totalInvestment = initialInvestment + additionalInvestment
        val totalGainLoss = finalValue - totalInvestment

        return when (calculationType) {
            0 -> { // Simple ROI
                val simpleROI = ((finalValue - initialInvestment) / initialInvestment) * 100
                ROIResult(simpleROI, 0.0, totalGainLoss, initialInvestment)
            }
            1 -> { // Annualized ROI
                val timeInYears = convertToYears(timePeriod, binding.spinnerTimeUnit.selectedItemPosition)
                val annualizedROI = if (timeInYears > 0) {
                    ((finalValue / initialInvestment).pow(1.0 / timeInYears) - 1) * 100
                } else 0.0

                val simpleROI = ((finalValue - initialInvestment) / initialInvestment) * 100
                ROIResult(simpleROI, annualizedROI, totalGainLoss, initialInvestment)
            }
            2 -> { // ROI with Additional Investment
                val timeInYears = convertToYears(timePeriod, binding.spinnerTimeUnit.selectedItemPosition)
                val annualizedROI = if (timeInYears > 0 && totalInvestment > 0) {
                    ((finalValue / totalInvestment).pow(1.0 / timeInYears) - 1) * 100
                } else 0.0

                val simpleROI = if (totalInvestment > 0) ((finalValue - totalInvestment) / totalInvestment) * 100 else 0.0
                ROIResult(simpleROI, annualizedROI, totalGainLoss, totalInvestment)
            }
            else -> ROIResult(0.0, 0.0, 0.0, initialInvestment)
        }
    }

    private fun convertToYears(timePeriod: Double, timeUnit: Int): Double {
        return when (timeUnit) {
            0 -> timePeriod / 365.0 // Days to years
            1 -> timePeriod / 12.0  // Months to years
            2 -> timePeriod         // Years
            else -> timePeriod
        }
    }

    private fun displayResults(
        result: ROIResult,
        initialInvestment: Double,
        finalValue: Double,
        timePeriod: Double,
        additionalInvestment: Double,
        calculationType: Int
    ) {
        binding.apply {
            // Display ROI percentage with color coding
            tvRoiPercentage.text = String.format("%.2f%%", result.simpleROI)

            // Color code based on performance
            val color = when {
                result.simpleROI > 0 -> ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
                result.simpleROI < 0 -> ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
                else -> ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
            }
            tvRoiPercentage.setTextColor(color)

            // Display total gain/loss
            val gainLossText = if (result.totalGainLoss >= 0) {
                String.format("+%.2f", result.totalGainLoss)
            } else {
                String.format("-%.2f", abs(result.totalGainLoss))
            }
            tvTotalGainLoss.text = gainLossText
            tvTotalGainLoss.setTextColor(color)

            // Display annualized ROI if applicable
            if (layoutAnnualizedROI.visibility == View.VISIBLE) {
                tvAnnualizedRoi.text = String.format("%.2f%%", result.annualizedROI)
            }

            // Display total investment
            tvTotalInvestment.text = String.format("%.2f", result.totalInvestment)

            // Display performance assessment
            val performance = when {
                result.simpleROI > 15 -> "Excellent"
                result.simpleROI > 8 -> "Good"
                result.simpleROI > 0 -> "Positive"
                result.simpleROI == 0.0 -> "Neutral"
                result.simpleROI > -10 -> "Poor"
                else -> "Very Poor"
            }
            tvPerformance.text = performance
            tvPerformance.setTextColor(color)

            // Display calculation details
            displayCalculationDetails(initialInvestment, finalValue, timePeriod, additionalInvestment, calculationType, result)
        }
    }

    private fun displayCalculationDetails(
        initialInvestment: Double,
        finalValue: Double,
        timePeriod: Double,
        additionalInvestment: Double,
        calculationType: Int,
        result: ROIResult
    ) {
        val details = StringBuilder()

        details.append("Investment Summary:\n")
        details.append("Initial Investment: ${String.format("%.2f", initialInvestment)}\n")
        details.append("Final Value: ${String.format("%.2f", finalValue)}\n")

        if (additionalInvestment > 0) {
            details.append("Additional Investment: ${String.format("%.2f", additionalInvestment)}\n")
            details.append("Total Investment: ${String.format("%.2f", result.totalInvestment)}\n")
        }

        details.append("\n")

        when (calculationType) {
            0 -> { // Simple ROI
                details.append("Simple ROI Calculation:\n")
                details.append("ROI = ((Final Value - Initial Investment) / Initial Investment) × 100\n")
                details.append("ROI = ((${String.format("%.2f", finalValue)} - ${String.format("%.2f", initialInvestment)}) / ${String.format("%.2f", initialInvestment)}) × 100\n")
                details.append("ROI = (${String.format("%.2f", finalValue - initialInvestment)} / ${String.format("%.2f", initialInvestment)}) × 100\n")
                details.append("ROI = ${String.format("%.2f", result.simpleROI)}%\n")
            }

            1 -> { // Annualized ROI
                val timeUnit = binding.spinnerTimeUnit.selectedItem.toString().lowercase()
                val timeInYears = convertToYears(timePeriod, binding.spinnerTimeUnit.selectedItemPosition)

                details.append("Time Period: ${String.format("%.1f", timePeriod)} $timeUnit (${String.format("%.2f", timeInYears)} years)\n\n")

                details.append("Simple ROI:\n")
                details.append("${String.format("%.2f", result.simpleROI)}%\n\n")

                details.append("Annualized ROI Calculation:\n")
                details.append("Annualized ROI = ((Final Value / Initial Investment)^(1/Years)) - 1) × 100\n")
                details.append("Annualized ROI = ((${String.format("%.2f", finalValue)} / ${String.format("%.2f", initialInvestment)})^(1/${String.format("%.2f", timeInYears)}) - 1) × 100\n")
                details.append("Annualized ROI = ${String.format("%.2f", result.annualizedROI)}%\n")
            }

            2 -> { // ROI with Additional Investment
                val timeUnit = binding.spinnerTimeUnit.selectedItem.toString().lowercase()
                val timeInYears = convertToYears(timePeriod, binding.spinnerTimeUnit.selectedItemPosition)

                details.append("Time Period: ${String.format("%.1f", timePeriod)} $timeUnit (${String.format("%.2f", timeInYears)} years)\n\n")

                details.append("Total ROI Calculation:\n")
                details.append("Total ROI = ((Final Value - Total Investment) / Total Investment) × 100\n")
                details.append("Total ROI = ((${String.format("%.2f", finalValue)} - ${String.format("%.2f", result.totalInvestment)}) / ${String.format("%.2f", result.totalInvestment)}) × 100\n")
                details.append("Total ROI = ${String.format("%.2f", result.simpleROI)}%\n\n")

                details.append("Annualized ROI = ${String.format("%.2f", result.annualizedROI)}%\n")
            }
        }

        binding.tvCalculationDetails.text = details.toString()
    }

    private fun clearAllFields() {
        binding.apply {
            etInitialInvestment.text?.clear()
            etFinalValue.text?.clear()
            etTimePeriod.text?.clear()
            etAdditionalInvestment.text?.clear()
            spinnerCalculationType.setSelection(0)
            spinnerTimeUnit.setSelection(2) // Default to years
            clearResults()
        }
        Toast.makeText(requireContext(), "All fields cleared", Toast.LENGTH_SHORT).show()
    }

    private fun clearResults() {
        binding.apply {
            tvRoiPercentage.text = "0%"
            tvTotalGainLoss.text = "0"
            tvAnnualizedRoi.text = "0%"
            tvTotalInvestment.text = "0"
            tvPerformance.text = "Neutral"
            tvCalculationDetails.text = "Enter investment details to see calculation breakdown"

            // Reset colors
            val defaultColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
            tvRoiPercentage.setTextColor(defaultColor)
            tvTotalGainLoss.setTextColor(defaultColor)
            tvPerformance.setTextColor(defaultColor)
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        clearResults()
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

    // Data class to hold ROI calculation results
    data class ROIResult(
        val simpleROI: Double,
        val annualizedROI: Double,
        val totalGainLoss: Double,
        val totalInvestment: Double
    )

    companion object {
        private const val TAG = "RoiCalculatorFragment"
    }
}