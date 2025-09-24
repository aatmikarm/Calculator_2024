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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aatmik.calculator.databinding.FragmentFractionCalculatorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import startGlowAnimation
import stopGlowAnimation
import kotlin.math.abs

class FractionCalculatorFragment : Fragment() {

    private lateinit var binding: FragmentFractionCalculatorBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentFractionCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        animateView(binding.fractionLayout)
        hideKeyboardFunctionality(view)
        setupSpinners()

        binding.apply {
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            // Set a click listener on the Calculate button
            btnCalculate.setOnClickListener {
                hideKeyboard()
                calculateFraction()
            }

            // Clear button functionality
            btnClear.setOnClickListener {
                clearAllFields()
            }

            // Text watchers for real-time calculation
            etNumerator1.addTextChangedListener(createTextWatcher())
            etDenominator1.addTextChangedListener(createTextWatcher())
            etNumerator2.addTextChangedListener(createTextWatcher())
            etDenominator2.addTextChangedListener(createTextWatcher())

            // Set up editor action listeners
            etNumerator1.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    etDenominator1.requestFocus()
                    true
                } else {
                    false
                }
            }

            etDenominator1.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    etNumerator2.requestFocus()
                    true
                } else {
                    false
                }
            }

            etNumerator2.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    etDenominator2.requestFocus()
                    true
                } else {
                    false
                }
            }

            etDenominator2.setOnEditorActionListener { _, actionId, event ->
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
        val operations = arrayOf("Add (+)", "Subtract (-)", "Multiply (×)", "Divide (÷)")

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, operations)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerOperation.adapter = adapter

        binding.spinnerOperation.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                calculateFraction()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun createTextWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                calculateFraction()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
    }

    private fun calculateFraction() {
        binding.apply {
            val num1Str = etNumerator1.text.toString()
            val den1Str = etDenominator1.text.toString()
            val num2Str = etNumerator2.text.toString()
            val den2Str = etDenominator2.text.toString()

            // Check if all inputs are valid
            if (num1Str.isNotEmpty() && den1Str.isNotEmpty() &&
                num2Str.isNotEmpty() && den2Str.isNotEmpty()) {
                try {
                    val num1 = num1Str.toInt()
                    val den1 = den1Str.toInt()
                    val num2 = num2Str.toInt()
                    val den2 = den2Str.toInt()

                    if (den1 == 0 || den2 == 0) {
                        showError("Denominator cannot be zero")
                        return
                    }

                    val operation = spinnerOperation.selectedItemPosition
                    val result = performOperation(num1, den1, num2, den2, operation)

                    if (result != null) {
                        displayResults(result, num1, den1, num2, den2, operation)
                    }

                } catch (e: NumberFormatException) {
                    showError("Please enter valid integers")
                } catch (e: ArithmeticException) {
                    showError("Cannot divide by zero")
                }
            } else {
                clearResults()
            }
        }
    }

    private fun performOperation(num1: Int, den1: Int, num2: Int, den2: Int, operation: Int): Pair<Int, Int>? {
        return when (operation) {
            0 -> { // Addition: a/b + c/d = (ad + bc) / bd
                val resultNum = num1 * den2 + num2 * den1
                val resultDen = den1 * den2
                Pair(resultNum, resultDen)
            }
            1 -> { // Subtraction: a/b - c/d = (ad - bc) / bd
                val resultNum = num1 * den2 - num2 * den1
                val resultDen = den1 * den2
                Pair(resultNum, resultDen)
            }
            2 -> { // Multiplication: a/b × c/d = ac / bd
                val resultNum = num1 * num2
                val resultDen = den1 * den2
                Pair(resultNum, resultDen)
            }
            3 -> { // Division: a/b ÷ c/d = a/b × d/c = ad / bc
                if (num2 == 0) {
                    showError("Cannot divide by zero")
                    return null
                }
                val resultNum = num1 * den2
                val resultDen = den1 * num2
                Pair(resultNum, resultDen)
            }
            else -> null
        }
    }

    private fun displayResults(result: Pair<Int, Int>, num1: Int, den1: Int, num2: Int, den2: Int, operation: Int) {
        binding.apply {
            val (resultNum, resultDen) = result

            // Display original result
            tvOriginalValue.text = "$resultNum/$resultDen"

            // Simplify the fraction
            val gcd = findGCD(abs(resultNum), abs(resultDen))
            val simplifiedNum = resultNum / gcd
            val simplifiedDen = resultDen / gcd

            // Handle sign properly
            val finalNum: Int
            val finalDen: Int

            if (simplifiedDen < 0) {
                finalNum = -simplifiedNum
                finalDen = -simplifiedDen
            } else {
                finalNum = simplifiedNum
                finalDen = simplifiedDen
            }

            tvSimplifiedValue.text = if (finalDen == 1) "$finalNum" else "$finalNum/$finalDen"

            // Calculate decimal value
            val decimal = finalNum.toDouble() / finalDen.toDouble()
            tvDecimalValue.text = String.format("%.6f", decimal)

            // Calculate percentage
            val percentage = decimal * 100
            tvPercentageValue.text = String.format("%.2f%%", percentage)

            // Mixed number (if improper fraction)
            if (abs(finalNum) > abs(finalDen) && finalDen != 1) {
                val wholePart = finalNum / finalDen
                val remainderNum = abs(finalNum % finalDen)
                if (remainderNum != 0) {
                    tvMixedNumberValue.text = "$wholePart ${remainderNum}/${finalDen}"
                    linearMixedNumber.visibility = View.VISIBLE
                } else {
                    tvMixedNumberValue.text = "$wholePart"
                    linearMixedNumber.visibility = View.VISIBLE
                }
            } else {
                linearMixedNumber.visibility = View.GONE
            }

            // Show operation step
            val operationSymbol = when (operation) {
                0 -> "+"
                1 -> "-"
                2 -> "×"
                3 -> "÷"
                else -> ""
            }
            tvStepByStepValue.text = buildOperationSteps(num1, den1, num2, den2, operationSymbol, resultNum, resultDen, finalNum, finalDen)
        }
    }

    private fun buildOperationSteps(num1: Int, den1: Int, num2: Int, den2: Int, op: String,
                                    resultNum: Int, resultDen: Int, finalNum: Int, finalDen: Int): String {
        val step = StringBuilder()
        step.append("$num1/$den1 $op $num2/$den2\n\n")

        when (op) {
            "+" -> {
                step.append("= ($num1×$den2 + $num2×$den1) / ($den1×$den2)\n")
                step.append("= (${num1*den2} + ${num2*den1}) / ${den1*den2}\n")
                step.append("= $resultNum/$resultDen")
            }
            "-" -> {
                step.append("= ($num1×$den2 - $num2×$den1) / ($den1×$den2)\n")
                step.append("= (${num1*den2} - ${num2*den1}) / ${den1*den2}\n")
                step.append("= $resultNum/$resultDen")
            }
            "×" -> {
                step.append("= ($num1×$num2) / ($den1×$den2)\n")
                step.append("= ${num1*num2} / ${den1*den2}\n")
                step.append("= $resultNum/$resultDen")
            }
            "÷" -> {
                step.append("= $num1/$den1 × $den2/$num2\n")
                step.append("= ($num1×$den2) / ($den1×$num2)\n")
                step.append("= ${num1*den2} / ${den1*num2}\n")
                step.append("= $resultNum/$resultDen")
            }
        }

        if (resultNum != finalNum || resultDen != finalDen) {
            step.append("\n\nSimplified: $finalNum/$finalDen")
        }

        return step.toString()
    }

    private fun findGCD(a: Int, b: Int): Int {
        return if (b == 0) a else findGCD(b, a % b)
    }

    private fun clearAllFields() {
        binding.apply {
            etNumerator1.text?.clear()
            etDenominator1.text?.clear()
            etNumerator2.text?.clear()
            etDenominator2.text?.clear()
            spinnerOperation.setSelection(0)
            clearResults()
        }
        Toast.makeText(requireContext(), "All fields cleared", Toast.LENGTH_SHORT).show()
    }

    private fun clearResults() {
        binding.apply {
            tvOriginalValue.text = "0"
            tvSimplifiedValue.text = "0"
            tvDecimalValue.text = "0"
            tvPercentageValue.text = "0%"
            tvMixedNumberValue.text = "0"
            tvStepByStepValue.text = "Enter fractions to see calculation steps"
            linearMixedNumber.visibility = View.GONE
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

    companion object {
        private const val TAG = "FractionCalculatorFragment"
    }
}