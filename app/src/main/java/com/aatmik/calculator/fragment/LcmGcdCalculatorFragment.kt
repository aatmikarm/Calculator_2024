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
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aatmik.calculator.databinding.FragmentLcmGcdCalculatorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import startGlowAnimation
import stopGlowAnimation
import kotlin.math.abs

class LcmGcdCalculatorFragment : Fragment() {

    private lateinit var binding: FragmentLcmGcdCalculatorBinding
    private val inputFields = mutableListOf<EditText>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentLcmGcdCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        animateView(binding.inputLayout)
        hideKeyboardFunctionality(view)
        setupInputFields()
        setupSpinner()

        binding.apply {
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            // Set a click listener on the Calculate button
            btnCalculate.setOnClickListener {
                hideKeyboard()
                calculateLcmGcd()
            }

            // Clear button functionality
            btnClear.setOnClickListener {
                clearAllFields()
            }

            // Add text watchers for real-time calculation
            inputFields.forEach { editText ->
                editText.addTextChangedListener(createTextWatcher())
            }
        }
    }

    private fun setupInputFields() {
        inputFields.clear()
        inputFields.add(binding.etNumber1)
        inputFields.add(binding.etNumber2)
        inputFields.add(binding.etNumber3)
        inputFields.add(binding.etNumber4)
        inputFields.add(binding.etNumber5)
        inputFields.add(binding.etNumber6)
    }

    private fun setupSpinner() {
        val numberCounts = arrayOf("2 Numbers", "3 Numbers", "4 Numbers", "5 Numbers", "6 Numbers")

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, numberCounts)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerNumberCount.adapter = adapter

        binding.spinnerNumberCount.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateInputFieldsVisibility(position + 2) // position 0 = 2 numbers, etc.
                calculateLcmGcd()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateInputFieldsVisibility(numberOfInputs: Int) {
        binding.apply {
            // Show/hide input field rows based on selection
            when {
                numberOfInputs >= 3 -> layoutNumbers34.visibility = View.VISIBLE
                else -> {
                    layoutNumbers34.visibility = View.GONE
                    etNumber3.text?.clear()
                    etNumber4.text?.clear()
                }
            }

            when {
                numberOfInputs >= 5 -> layoutNumbers56.visibility = View.VISIBLE
                else -> {
                    layoutNumbers56.visibility = View.GONE
                    etNumber5.text?.clear()
                    etNumber6.text?.clear()
                }
            }
        }
    }

    private fun createTextWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                calculateLcmGcd()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
    }

    private fun calculateLcmGcd() {
        val selectedCount = binding.spinnerNumberCount.selectedItemPosition + 2
        val numbers = mutableListOf<Long>()

        try {
            // Collect input numbers
            for (i in 0 until selectedCount) {
                val text = inputFields[i].text.toString()
                if (text.isNotEmpty()) {
                    val number = text.toLong()
                    if (number <= 0) {
                        showError("Please enter positive integers only")
                        return
                    }
                    if (number > 1000000) {
                        showError("Numbers must be less than 1,000,000")
                        return
                    }
                    numbers.add(number)
                }
            }

            if (numbers.size < 2) {
                clearResults()
                return
            }

            // Calculate LCM and GCD
            val lcm = calculateLCM(numbers)
            val gcd = calculateGCD(numbers)

            displayResults(numbers, lcm, gcd)

        } catch (e: NumberFormatException) {
            showError("Please enter valid integers")
        }
    }

    private fun calculateGCD(numbers: List<Long>): Long {
        return numbers.reduce { acc, number -> gcd(acc, number) }
    }

    private fun calculateLCM(numbers: List<Long>): Long {
        return numbers.reduce { acc, number -> lcm(acc, number) }
    }

    private fun gcd(a: Long, b: Long): Long {
        return if (b == 0L) a else gcd(b, a % b)
    }

    private fun lcm(a: Long, b: Long): Long {
        return abs(a * b) / gcd(a, b)
    }

    private fun getPrimeFactorization(n: Long): Map<Long, Int> {
        val factors = mutableMapOf<Long, Int>()
        var num = n
        var factor = 2L

        while (factor * factor <= num) {
            while (num % factor == 0L) {
                factors[factor] = factors.getOrDefault(factor, 0) + 1
                num /= factor
            }
            factor++
        }

        if (num > 1) {
            factors[num] = factors.getOrDefault(num, 0) + 1
        }

        return factors
    }

    private fun displayResults(numbers: List<Long>, lcm: Long, gcd: Long) {
        binding.apply {
            // Display main results
            tvLcmValue.text = lcm.toString()
            tvGcdValue.text = gcd.toString()

            // Show relationship (for two numbers: a × b = LCM × GCD)
            if (numbers.size == 2) {
                val product = numbers[0] * numbers[1]
                val lcmGcdProduct = lcm * gcd
                tvRelationshipValue.text = "Valid: ${product == lcmGcdProduct}"
            } else {
                tvRelationshipValue.text = "LCM × GCD = ${lcm * gcd}"
            }

            // Display prime factorizations
            displayPrimeFactorizations(numbers)

            // Display step-by-step calculation
            displayCalculationSteps(numbers, lcm, gcd)
        }
    }

    private fun displayPrimeFactorizations(numbers: List<Long>) {
        val factorizations = StringBuilder()

        numbers.forEach { number ->
            val factors = getPrimeFactorization(number)
            factorizations.append("$number = ")

            if (factors.isEmpty() || number == 1L) {
                factorizations.append("1")
            } else {
                val factorStrings = factors.map { (prime, count) ->
                    if (count == 1) prime.toString() else "$prime^$count"
                }
                factorizations.append(factorStrings.joinToString(" × "))
            }
            factorizations.append("\n")
        }

        binding.tvPrimeFactorizationValue.text = factorizations.toString().trim()
    }

    private fun displayCalculationSteps(numbers: List<Long>, lcm: Long, gcd: Long) {
        val steps = StringBuilder()

        steps.append("Numbers: ${numbers.joinToString(", ")}\n\n")

        // GCD calculation using Euclidean algorithm (for 2 numbers)
        if (numbers.size == 2) {
            steps.append("GCD Calculation using Euclidean Algorithm:\n")
            val gcdSteps = getGcdSteps(numbers[0], numbers[1])
            steps.append(gcdSteps)
            steps.append("\n")
        } else {
            steps.append("GCD Calculation:\n")
            steps.append("GCD(${numbers.joinToString(", ")}) = $gcd\n\n")
        }

        // LCM calculation
        steps.append("LCM Calculation:\n")
        if (numbers.size == 2) {
            steps.append("LCM = (${numbers[0]} × ${numbers[1]}) ÷ GCD\n")
            steps.append("LCM = ${numbers[0] * numbers[1]} ÷ $gcd = $lcm\n\n")
        } else {
            steps.append("LCM(${numbers.joinToString(", ")}) = $lcm\n\n")
        }

        // Verification
        steps.append("Verification:\n")
        numbers.forEach { number ->
            steps.append("$lcm ÷ $number = ${lcm / number} (remainder: ${lcm % number})\n")
        }

        binding.tvCalculationSteps.text = steps.toString()
    }

    private fun getGcdSteps(a: Long, b: Long): String {
        val steps = StringBuilder()
        var num1 = a
        var num2 = b

        while (num2 != 0L) {
            val quotient = num1 / num2
            val remainder = num1 % num2
            steps.append("$num1 = $num2 × $quotient + $remainder\n")
            num1 = num2
            num2 = remainder
        }

        steps.append("GCD = $num1\n")
        return steps.toString()
    }

    private fun clearAllFields() {
        inputFields.forEach { it.text?.clear() }
        binding.spinnerNumberCount.setSelection(0)
        updateInputFieldsVisibility(2)
        clearResults()
        Toast.makeText(requireContext(), "All fields cleared", Toast.LENGTH_SHORT).show()
    }

    private fun clearResults() {
        binding.apply {
            tvLcmValue.text = "0"
            tvGcdValue.text = "0"
            tvRelationshipValue.text = "0"
            tvPrimeFactorizationValue.text = "Enter numbers to see their prime factorization"
            tvCalculationSteps.text = "Enter positive integers to see calculation steps"
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
        private const val TAG = "LcmGcdCalculatorFragment"
    }
}