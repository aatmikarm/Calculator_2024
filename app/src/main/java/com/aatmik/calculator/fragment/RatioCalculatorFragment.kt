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
import com.aatmik.calculator.databinding.FragmentRatioCalculatorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import startGlowAnimation
import stopGlowAnimation
import kotlin.math.abs
import kotlin.math.round

class RatioCalculatorFragment : Fragment() {

    private lateinit var binding: FragmentRatioCalculatorBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentRatioCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        animateView(binding.ratioLayout)
        hideKeyboardFunctionality(view)

        binding.apply {
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            // Set a click listener on the Calculate button
            btnCalculate.setOnClickListener {
                hideKeyboard()
                calculateRatio()
            }

            // Set an OnClickListener on the Swap CardView
            swap.setOnClickListener {
                var valueA = etValueA.text.toString()
                var valueB = etValueB.text.toString()

                if (valueA.isEmpty() || valueB.isEmpty()) {
                    Toast.makeText(
                        requireContext(), "Both values must be non-empty!", Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Swap values
                    val temp = valueA
                    valueA = valueB
                    valueB = temp

                    etValueA.setText(valueA)
                    etValueB.setText(valueB)

                    calculateRatio()
                }
            }

            // Clear button functionality
            btnClear.setOnClickListener {
                clearAllFields()
            }

            // Text watchers for real-time calculation
            etValueA.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    calculateRatio()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    calculateRatio()
                }
            })

            etValueB.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    calculateRatio()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    calculateRatio()
                }
            })

            etScaleValue.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    calculateRatio()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    calculateRatio()
                }
            })

            // Set up editor action listeners for better UX
            etValueA.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    etValueB.requestFocus()
                    true
                } else {
                    false
                }
            }

            etValueB.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    etScaleValue.requestFocus()
                    true
                } else {
                    false
                }
            }

            etScaleValue.setOnEditorActionListener { _, actionId, event ->
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

    private fun calculateRatio() {
        binding.apply {
            val valueAStr = etValueA.text.toString()
            val valueBStr = etValueB.text.toString()
            val scaleValueStr = etScaleValue.text.toString()

            // Check if the basic ratio inputs are valid
            if (valueAStr.isNotEmpty() && valueBStr.isNotEmpty()) {
                try {
                    val valueA = valueAStr.toDouble()
                    val valueB = valueBStr.toDouble()

                    if (valueA <= 0 || valueB <= 0) {
                        showError("Values must be positive numbers")
                        return
                    }

                    // Calculate simplified ratio
                    val gcd = findGCD(valueA, valueB)
                    val simplifiedA = (valueA / gcd).let { if (it == it.toInt().toDouble()) it.toInt().toString() else String.format("%.2f", it) }
                    val simplifiedB = (valueB / gcd).let { if (it == it.toInt().toDouble()) it.toInt().toString() else String.format("%.2f", it) }

                    // Calculate decimal ratio
                    val decimalRatio = valueA / valueB
                    val reverseDecimalRatio = valueB / valueA

                    // Calculate percentage
                    val percentageA = (valueA / (valueA + valueB)) * 100
                    val percentageB = (valueB / (valueA + valueB)) * 100

                    // Display basic ratio results
                    tvSimplifiedValue.text = "$simplifiedA : $simplifiedB"
                    tvDecimalValue.text = String.format("%.4f", decimalRatio)
                    tvReverseValue.text = String.format("%.4f", reverseDecimalRatio)
                    tvPercentageAValue.text = String.format("%.2f%%", percentageA)
                    tvPercentageBValue.text = String.format("%.2f%%", percentageB)

                    // Calculate proportional scaling if scale value is provided
                    if (scaleValueStr.isNotEmpty()) {
                        try {
                            val scaleValue = scaleValueStr.toDouble()
                            if (scaleValue > 0) {
                                val scaledB = (scaleValue * valueB) / valueA
                                val scaledA = (scaleValue * valueA) / valueB

                                tvScaledAValue.text = String.format("%.4f", scaledB)
                                tvScaledBValue.text = String.format("%.4f", scaledA)

                                // Show proportional section
                                cardScaling.visibility = View.VISIBLE
                            }
                        } catch (e: NumberFormatException) {
                            tvScaledAValue.text = "Invalid"
                            tvScaledBValue.text = "Invalid"
                        }
                    } else {
                        cardScaling.visibility = View.GONE
                    }

                    // Calculate golden ratio comparison
                    val goldenRatio = 1.618033988749
                    val ratioComparisonToGolden = abs(decimalRatio - goldenRatio)
                    val isNearGolden = ratioComparisonToGolden < 0.01

                    if (isNearGolden) {
                        tvGoldenRatioValue.text = "Near Golden Ratio! (φ ≈ 1.618)"
                        tvGoldenRatioValue.visibility = View.VISIBLE
                    } else {
                        tvGoldenRatioValue.visibility = View.GONE
                    }

                    // Show common fraction equivalent
                    val fractionEquivalent = findCommonFraction(decimalRatio)
                    if (fractionEquivalent.isNotEmpty()) {
                        tvFractionValue.text = fractionEquivalent
                        tvFractionValue.visibility = View.VISIBLE
                    } else {
                        tvFractionValue.visibility = View.GONE
                    }

                } catch (e: NumberFormatException) {
                    showError("Please enter valid numbers")
                }
            } else {
                // Clear results if inputs are incomplete
                clearResults()
            }
        }
    }

    private fun findGCD(a: Double, b: Double): Double {
        // Convert to integers for GCD calculation, handling decimals
        val factor = 10000.0 // Handle up to 4 decimal places
        val intA = (a * factor).toLong()
        val intB = (b * factor).toLong()

        val gcdInt = findGCDLong(intA, intB)
        return gcdInt / factor
    }

    private fun findGCDLong(a: Long, b: Long): Long {
        return if (b == 0L) a else findGCDLong(b, a % b)
    }

    private fun findCommonFraction(decimal: Double): String {
        // Check for common fractions
        val commonFractions = mapOf(
            0.5 to "1/2",
            0.3333 to "1/3",
            0.6667 to "2/3",
            0.25 to "1/4",
            0.75 to "3/4",
            0.2 to "1/5",
            0.4 to "2/5",
            0.6 to "3/5",
            0.8 to "4/5",
            0.1667 to "1/6",
            0.8333 to "5/6",
            0.125 to "1/8",
            0.375 to "3/8",
            0.625 to "5/8",
            0.875 to "7/8"
        )

        for ((value, fraction) in commonFractions) {
            if (abs(decimal - value) < 0.01) {
                return fraction
            }
        }

        return ""
    }

    private fun clearAllFields() {
        binding.apply {
            etValueA.text?.clear()
            etValueB.text?.clear()
            etScaleValue.text?.clear()
            clearResults()
        }
        Toast.makeText(requireContext(), "All fields cleared", Toast.LENGTH_SHORT).show()
    }

    private fun clearResults() {
        binding.apply {
            tvSimplifiedValue.text = "0"
            tvDecimalValue.text = "0"
            tvReverseValue.text = "0"
            tvPercentageAValue.text = "0%"
            tvPercentageBValue.text = "0%"
            tvScaledAValue.text = "0"
            tvScaledBValue.text = "0"
            tvGoldenRatioValue.visibility = View.GONE
            tvFractionValue.visibility = View.GONE
            cardScaling.visibility = View.GONE
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
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
        private const val TAG = "RatioCalculatorFragment"
    }
}