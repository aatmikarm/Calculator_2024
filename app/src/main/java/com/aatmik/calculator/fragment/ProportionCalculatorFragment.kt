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
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aatmik.calculator.databinding.FragmentProportionCalculatorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import startGlowAnimation
import stopGlowAnimation
import kotlin.math.abs
import kotlin.math.round

class ProportionCalculatorFragment : Fragment() {

    private lateinit var binding: FragmentProportionCalculatorBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentProportionCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        animateView(binding.proportionLayout)
        hideKeyboardFunctionality(view)

        binding.apply {
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            // Set a click listener on the Calculate button
            btnCalculate.setOnClickListener {
                hideKeyboard()
                solveProportion()
            }

            // Clear button functionality
            btnClear.setOnClickListener {
                clearAllFields()
            }

            // Text watchers for real-time calculation
            etValue1.addTextChangedListener(createTextWatcher())
            etValue2.addTextChangedListener(createTextWatcher())
            etValue3.addTextChangedListener(createTextWatcher())
            etValue4.addTextChangedListener(createTextWatcher())

            // Set up editor action listeners for better navigation
            etValue1.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    etValue2.requestFocus()
                    true
                } else {
                    false
                }
            }

            etValue2.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    etValue3.requestFocus()
                    true
                } else {
                    false
                }
            }

            etValue3.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    etValue4.requestFocus()
                    true
                } else {
                    false
                }
            }

            etValue4.setOnEditorActionListener { _, actionId, event ->
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

    private fun createTextWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                solveProportion()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
    }

    private fun solveProportion() {
        binding.apply {
            val value1Str = etValue1.text.toString()
            val value2Str = etValue2.text.toString()
            val value3Str = etValue3.text.toString()
            val value4Str = etValue4.text.toString()

            // Count how many values are provided
            val providedValues = listOf(value1Str, value2Str, value3Str, value4Str).count { it.isNotEmpty() }

            if (providedValues < 3) {
                clearResults()
                return
            }

            try {
                val value1 = if (value1Str.isNotEmpty()) value1Str.toDouble() else null
                val value2 = if (value2Str.isNotEmpty()) value2Str.toDouble() else null
                val value3 = if (value3Str.isNotEmpty()) value3Str.toDouble() else null
                val value4 = if (value4Str.isNotEmpty()) value4Str.toDouble() else null

                val result = solveMissingValue(value1, value2, value3, value4)

                if (result != null) {
                    displayResults(result, value1, value2, value3, value4)
                } else {
                    showError("Cannot solve proportion with current values")
                }

            } catch (e: NumberFormatException) {
                showError("Please enter valid numbers")
            }
        }
    }

    private fun solveMissingValue(v1: Double?, v2: Double?, v3: Double?, v4: Double?): ProportionResult? {
        // Proportion: a/b = c/d  =>  a*d = b*c

        return when {
            // Missing first value: a = (b * c) / d
            v1 == null && v2 != null && v3 != null && v4 != null -> {
                if (v4 == 0.0) {
                    showError("Cannot divide by zero")
                    return null
                }
                val result = (v2 * v3) / v4
                ProportionResult(result, 1, "a = (b × c) ÷ d = ($v2 × $v3) ÷ $v4 = $result")
            }

            // Missing second value: b = (a * d) / c
            v1 != null && v2 == null && v3 != null && v4 != null -> {
                if (v3 == 0.0) {
                    showError("Cannot divide by zero")
                    return null
                }
                val result = (v1 * v4) / v3
                ProportionResult(result, 2, "b = (a × d) ÷ c = ($v1 × $v4) ÷ $v3 = $result")
            }

            // Missing third value: c = (a * d) / b
            v1 != null && v2 != null && v3 == null && v4 != null -> {
                if (v2 == 0.0) {
                    showError("Cannot divide by zero")
                    return null
                }
                val result = (v1 * v4) / v2
                ProportionResult(result, 3, "c = (a × d) ÷ b = ($v1 × $v4) ÷ $v2 = $result")
            }

            // Missing fourth value: d = (b * c) / a
            v1 != null && v2 != null && v3 != null && v4 == null -> {
                if (v1 == 0.0) {
                    showError("Cannot divide by zero")
                    return null
                }
                val result = (v2 * v3) / v1
                ProportionResult(result, 4, "d = (b × c) ÷ a = ($v2 × $v3) ÷ $v1 = $result")
            }

            // All four values provided - check if proportion is valid
            v1 != null && v2 != null && v3 != null && v4 != null -> {
                val leftSide = v1 / v2
                val rightSide = v3 / v4
                val isValid = abs(leftSide - rightSide) < 0.0001
                ProportionResult(if (isValid) 1.0 else 0.0, 0,
                    "Verification: $v1/$v2 = ${String.format("%.4f", leftSide)}, $v3/$v4 = ${String.format("%.4f", rightSide)}\n" +
                            "Proportion is ${if (isValid) "VALID" else "INVALID"}")
            }

            else -> null
        }
    }

    private fun displayResults(result: ProportionResult, v1: Double?, v2: Double?, v3: Double?, v4: Double?) {
        binding.apply {
            // Update the missing value in the appropriate field
            when (result.position) {
                1 -> {
                    tvMissingValueLabel.text = "Missing Value (a):"
                    tvMissingValue.text = String.format("%.6f", result.value)
                }
                2 -> {
                    tvMissingValueLabel.text = "Missing Value (b):"
                    tvMissingValue.text = String.format("%.6f", result.value)
                }
                3 -> {
                    tvMissingValueLabel.text = "Missing Value (c):"
                    tvMissingValue.text = String.format("%.6f", result.value)
                }
                4 -> {
                    tvMissingValueLabel.text = "Missing Value (d):"
                    tvMissingValue.text = String.format("%.6f", result.value)
                }
                0 -> {
                    tvMissingValueLabel.text = "Validation Result:"
                    tvMissingValue.text = if (result.value == 1.0) "VALID" else "INVALID"
                }
            }

            // Show calculation steps
            tvCalculationSteps.text = result.steps

            // Calculate cross products for verification
            val finalV1 = v1 ?: result.value
            val finalV2 = v2 ?: result.value
            val finalV3 = v3 ?: result.value
            val finalV4 = v4 ?: result.value

            if (result.position != 0) {
                val crossProduct1 = finalV1 * finalV4  // a × d
                val crossProduct2 = finalV2 * finalV3  // b × c

                // Update separate cross product fields
                tvCrossProductLeftValue.text = String.format("%.4f", crossProduct1)
                tvCrossProductRightValue.text = String.format("%.4f", crossProduct2)

                // Calculate ratios
                val ratio1 = if (finalV2 != 0.0) finalV1 / finalV2 else 0.0
                val ratio2 = if (finalV4 != 0.0) finalV3 / finalV4 else 0.0

                tvRatiosValue.text = "a:b = ${String.format("%.4f", ratio1)}\nc:d = ${String.format("%.4f", ratio2)}"

                // Show percentage relationship
                val percentage = if (finalV2 != 0.0) (finalV1 / finalV2) * 100 else 0.0
                tvPercentageValue.text = "${String.format("%.2f", percentage)}%"
            }
        }
    }

    private fun clearAllFields() {
        binding.apply {
            etValue1.text?.clear()
            etValue2.text?.clear()
            etValue3.text?.clear()
            etValue4.text?.clear()
            clearResults()
        }
        Toast.makeText(requireContext(), "All fields cleared", Toast.LENGTH_SHORT).show()
    }

    private fun clearResults() {
        binding.apply {
            tvMissingValueLabel.text = "Missing Value:"
            tvMissingValue.text = "Enter 3 values"
            tvCalculationSteps.text = "Enter three known values and leave one empty to solve the proportion"
            tvCrossProductLeftValue.text = "0"      // Update this
            tvCrossProductRightValue.text = "0"     // Update this
            tvRatiosValue.text = "0"
            tvPercentageValue.text = "0%"
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

    // Data class to hold proportion calculation results
    data class ProportionResult(
        val value: Double,
        val position: Int, // 1=a, 2=b, 3=c, 4=d, 0=validation
        val steps: String
    )

    companion object {
        private const val TAG = "ProportionCalculatorFragment"
    }
}