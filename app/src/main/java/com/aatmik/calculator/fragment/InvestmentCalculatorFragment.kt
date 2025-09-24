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
import com.aatmik.calculator.databinding.FragmentInvestmentCalculatorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import startGlowAnimation
import stopGlowAnimation
import kotlin.math.pow

class InvestmentCalculatorFragment : Fragment() {

    private lateinit var binding: FragmentInvestmentCalculatorBinding

    // Investment types
    private val investmentTypes = arrayOf("SIP", "Fixed Deposit (FD)", "Recurring Deposit (RD)", "PPF")
    private val timeUnits = arrayOf("Months", "Years")
    private val compoundingFrequencies = arrayOf("Yearly", "Half-Yearly", "Quarterly", "Monthly")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentInvestmentCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        animateView(binding.investmentLayout)
        hideKeyboardFunctionality(view)
        setupSpinners()

        binding.apply {
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            // Set a click listener on the Calculate button
            btnCalculate.setOnClickListener {
                hideKeyboard()
                calculateInvestment()
            }

            // Clear button functionality
            btnClear.setOnClickListener {
                clearAllFields()
            }

            // Text watchers for real-time calculation
            etPrincipalAmount.addTextChangedListener(createTextWatcher())
            etInterestRate.addTextChangedListener(createTextWatcher())
            etTimePeriod.addTextChangedListener(createTextWatcher())

            // Set up editor action listeners
            etPrincipalAmount.setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {
                    etInterestRate.requestFocus()
                    true
                } else {
                    false
                }
            }

            etInterestRate.setOnEditorActionListener { _, actionId, event ->
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
        // Setup investment type spinner
        val investmentAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, investmentTypes)
        investmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerInvestmentType.adapter = investmentAdapter

        binding.spinnerInvestmentType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateUIForInvestmentType(position)
                updateInstructions(position)
                calculateInvestment()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Setup time unit spinner
        val timeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, timeUnits)
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerTimeUnit.adapter = timeAdapter

        binding.spinnerTimeUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                calculateInvestment()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Setup compounding frequency spinner
        val compoundingAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, compoundingFrequencies)
        compoundingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerCompounding.adapter = compoundingAdapter

        binding.spinnerCompounding.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                calculateInvestment()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateUIForInvestmentType(investmentType: Int) {
        binding.apply {
            when (investmentType) {
                0 -> { // SIP
                    etPrincipalAmount.hint = "Monthly SIP Amount (₹)"
                    cardCompoundingFrequency.visibility = View.GONE
                    cardPpfWarning.visibility = View.GONE
                    layoutEffectiveRate.visibility = View.GONE
                    layoutMonthlyReturns.visibility = View.VISIBLE
                }
                1 -> { // Fixed Deposit
                    etPrincipalAmount.hint = "Principal Amount (₹)"
                    cardCompoundingFrequency.visibility = View.VISIBLE
                    cardPpfWarning.visibility = View.GONE
                    layoutEffectiveRate.visibility = View.VISIBLE
                    layoutMonthlyReturns.visibility = View.GONE
                }
                2 -> { // Recurring Deposit
                    etPrincipalAmount.hint = "Monthly Deposit (₹)"
                    cardCompoundingFrequency.visibility = View.VISIBLE
                    cardPpfWarning.visibility = View.GONE
                    layoutEffectiveRate.visibility = View.VISIBLE
                    layoutMonthlyReturns.visibility = View.GONE
                }
                3 -> { // PPF
                    etPrincipalAmount.hint = "Annual Contribution (₹)"
                    etInterestRate.setText("7.1") // Current PPF rate
                    cardCompoundingFrequency.visibility = View.GONE
                    cardPpfWarning.visibility = View.VISIBLE
                    layoutEffectiveRate.visibility = View.GONE
                    layoutMonthlyReturns.visibility = View.GONE
                }
            }
        }
    }

    private fun updateInstructions(investmentType: Int) {
        val instructions = when (investmentType) {
            0 -> "SIP: Enter monthly investment amount, expected annual return rate, and investment period"
            1 -> "Fixed Deposit: Enter principal amount, annual interest rate, tenure, and compounding frequency"
            2 -> "Recurring Deposit: Enter monthly deposit, annual interest rate, and tenure"
            3 -> "PPF: Enter annual contribution (₹500 - ₹1,50,000), minimum 15 years tenure"
            else -> "Select investment type and enter details to calculate returns"
        }
        binding.tvInstructions.text = instructions
    }

    private fun createTextWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                calculateInvestment()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
    }

    private fun calculateInvestment() {
        binding.apply {
            val principalStr = etPrincipalAmount.text.toString()
            val interestRateStr = etInterestRate.text.toString()
            val timePeriodStr = etTimePeriod.text.toString()

            if (principalStr.isEmpty() || interestRateStr.isEmpty() || timePeriodStr.isEmpty()) {
                clearResults()
                return
            }

            try {
                val principalAmount = principalStr.toDouble()
                val annualInterestRate = interestRateStr.toDouble() / 100
                val timePeriod = timePeriodStr.toDouble()

                if (principalAmount <= 0 || annualInterestRate < 0 || timePeriod <= 0) {
                    showError("Please enter positive values")
                    return
                }

                val investmentType = spinnerInvestmentType.selectedItemPosition
                val timeUnit = spinnerTimeUnit.selectedItemPosition
                val compoundingFreq = spinnerCompounding.selectedItemPosition

                // Validate PPF constraints
                if (investmentType == 3) { // PPF
                    if (principalAmount < 500 || principalAmount > 150000) {
                        showError("PPF contribution must be between ₹500 and ₹1,50,000")
                        return
                    }
                    if (timeUnit == 1 && timePeriod < 15) { // Years
                        showError("PPF has a minimum tenure of 15 years")
                        return
                    }
                }

                val result = calculateInvestmentResult(
                    principalAmount, annualInterestRate, timePeriod,
                    investmentType, timeUnit, compoundingFreq
                )

                displayResults(result, principalAmount, annualInterestRate, timePeriod, investmentType)

            } catch (e: NumberFormatException) {
                showError("Please enter valid numbers")
            }
        }
    }

    private fun calculateInvestmentResult(
        amount: Double,
        annualRate: Double,
        period: Double,
        investmentType: Int,
        timeUnit: Int,
        compoundingFreq: Int
    ): InvestmentResult {

        val periodInMonths = if (timeUnit == 1) period * 12 else period // Convert to months
        val periodInYears = periodInMonths / 12

        return when (investmentType) {
            0 -> calculateSIP(amount, annualRate, periodInMonths)
            1 -> calculateFD(amount, annualRate, periodInYears, compoundingFreq)
            2 -> calculateRD(amount, annualRate, periodInMonths)
            3 -> calculatePPF(amount, annualRate, periodInYears)
            else -> InvestmentResult(0.0, 0.0, 0.0, 0.0)
        }
    }

    private fun calculateSIP(monthlyAmount: Double, annualRate: Double, months: Double): InvestmentResult {
        val monthlyRate = annualRate / 12
        val totalMonths = months.toInt()

        if (monthlyRate == 0.0) {
            val maturityAmount = monthlyAmount * totalMonths
            return InvestmentResult(
                maturityAmount = maturityAmount,
                totalInvestment = maturityAmount,
                interestEarned = 0.0,
                effectiveRate = 0.0
            )
        }

        val maturityAmount = monthlyAmount * (((1 + monthlyRate).pow(totalMonths) - 1) / monthlyRate) * (1 + monthlyRate)
        val totalInvestment = monthlyAmount * totalMonths
        val interestEarned = maturityAmount - totalInvestment

        return InvestmentResult(
            maturityAmount = maturityAmount,
            totalInvestment = totalInvestment,
            interestEarned = interestEarned,
            effectiveRate = annualRate * 100
        )
    }

    private fun calculateFD(principal: Double, annualRate: Double, years: Double, compoundingFreq: Int): InvestmentResult {
        val compoundingPerYear = when (compoundingFreq) {
            0 -> 1.0  // Yearly
            1 -> 2.0  // Half-yearly
            2 -> 4.0  // Quarterly
            3 -> 12.0 // Monthly
            else -> 1.0
        }

        val maturityAmount = principal * (1 + annualRate / compoundingPerYear).pow(compoundingPerYear * years)
        val interestEarned = maturityAmount - principal
        val effectiveRate = ((maturityAmount / principal).pow(1.0 / years) - 1) * 100

        return InvestmentResult(
            maturityAmount = maturityAmount,
            totalInvestment = principal,
            interestEarned = interestEarned,
            effectiveRate = effectiveRate
        )
    }

    private fun calculateRD(monthlyAmount: Double, annualRate: Double, months: Double): InvestmentResult {
        val monthlyRate = annualRate / 12
        val totalMonths = months.toInt()

        if (monthlyRate == 0.0) {
            val maturityAmount = monthlyAmount * totalMonths
            return InvestmentResult(
                maturityAmount = maturityAmount,
                totalInvestment = maturityAmount,
                interestEarned = 0.0,
                effectiveRate = 0.0
            )
        }

        val maturityAmount = monthlyAmount * (((1 + monthlyRate).pow(totalMonths) - 1) / monthlyRate)
        val totalInvestment = monthlyAmount * totalMonths
        val interestEarned = maturityAmount - totalInvestment
        val effectiveRate = ((maturityAmount / totalInvestment).pow(1.0 / (months / 12)) - 1) * 100

        return InvestmentResult(
            maturityAmount = maturityAmount,
            totalInvestment = totalInvestment,
            interestEarned = interestEarned,
            effectiveRate = effectiveRate
        )
    }

    private fun calculatePPF(annualAmount: Double, annualRate: Double, years: Double): InvestmentResult {
        val maturityAmount = annualAmount * (((1 + annualRate).pow(years) - 1) / annualRate) * (1 + annualRate)
        val totalInvestment = annualAmount * years
        val interestEarned = maturityAmount - totalInvestment

        return InvestmentResult(
            maturityAmount = maturityAmount,
            totalInvestment = totalInvestment,
            interestEarned = interestEarned,
            effectiveRate = annualRate * 100
        )
    }

    private fun displayResults(
        result: InvestmentResult,
        amount: Double,
        annualRate: Double,
        period: Double,
        investmentType: Int
    ) {
        binding.apply {
            // Display main results
            tvMaturityAmount.text = String.format("₹%.0f", result.maturityAmount)
            tvTotalInvestment.text = String.format("₹%.0f", result.totalInvestment)
            tvInterestEarned.text = String.format("₹%.0f", result.interestEarned)

            // Color coding for gains
            val color = ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
            tvMaturityAmount.setTextColor(color)
            tvInterestEarned.setTextColor(color)

            // Show effective rate for applicable investments
            if (layoutEffectiveRate.visibility == View.VISIBLE) {
                tvEffectiveRate.text = String.format("%.2f%%", result.effectiveRate)
            }

            // Show monthly SIP amount for SIP
            if (layoutMonthlyReturns.visibility == View.VISIBLE && investmentType == 0) {
                tvMonthlySip.text = String.format("₹%.0f", amount)
            }

            // Display calculation details
            displayCalculationDetails(result, amount, annualRate, period, investmentType)
        }
    }

    private fun displayCalculationDetails(
        result: InvestmentResult,
        amount: Double,
        annualRate: Double,
        period: Double,
        investmentType: Int
    ) {
        val details = StringBuilder()
        val investmentName = investmentTypes[investmentType]
        val timeUnit = if (binding.spinnerTimeUnit.selectedItemPosition == 1) "years" else "months"

        details.append("$investmentName Calculation:\n\n")

        when (investmentType) {
            0 -> { // SIP
                details.append("Monthly SIP Amount: ₹${String.format("%.0f", amount)}\n")
                details.append("Expected Annual Return: ${String.format("%.2f", annualRate * 100)}%\n")
                details.append("Investment Period: ${String.format("%.0f", period)} $timeUnit\n\n")

                details.append("Formula: FV = PMT × [((1 + r)^n - 1) / r] × (1 + r)\n")
                details.append("Where:\n")
                details.append("PMT = Monthly investment (₹${String.format("%.0f", amount)})\n")
                details.append("r = Monthly interest rate (${String.format("%.4f", annualRate / 12)})\n")
                val months = if (binding.spinnerTimeUnit.selectedItemPosition == 1) period * 12 else period
                details.append("n = Number of months (${String.format("%.0f", months)})\n\n")

                details.append("Total Amount Invested: ₹${String.format("%.0f", result.totalInvestment)}\n")
                details.append("Wealth Gained: ₹${String.format("%.0f", result.interestEarned)}\n")
                details.append("Maturity Value: ₹${String.format("%.0f", result.maturityAmount)}")
            }

            1 -> { // Fixed Deposit
                val compounding = compoundingFrequencies[binding.spinnerCompounding.selectedItemPosition]
                details.append("Principal Amount: ₹${String.format("%.0f", amount)}\n")
                details.append("Annual Interest Rate: ${String.format("%.2f", annualRate * 100)}%\n")
                details.append("Tenure: ${String.format("%.1f", period)} $timeUnit\n")
                details.append("Compounding: $compounding\n\n")

                details.append("Formula: A = P(1 + r/n)^(nt)\n")
                details.append("Where:\n")
                details.append("P = Principal (₹${String.format("%.0f", amount)})\n")
                details.append("r = Annual interest rate (${String.format("%.4f", annualRate)})\n")
                val n = when (binding.spinnerCompounding.selectedItemPosition) {
                    0 -> 1; 1 -> 2; 2 -> 4; 3 -> 12; else -> 1
                }
                details.append("n = Compounding frequency ($n times per year)\n")
                val years = if (binding.spinnerTimeUnit.selectedItemPosition == 1) period else period / 12
                details.append("t = Time period (${String.format("%.2f", years)} years)\n\n")

                details.append("Maturity Amount: ₹${String.format("%.0f", result.maturityAmount)}\n")
                details.append("Interest Earned: ₹${String.format("%.0f", result.interestEarned)}\n")
                details.append("Effective Annual Rate: ${String.format("%.2f", result.effectiveRate)}%")
            }

            2 -> { // Recurring Deposit
                details.append("Monthly Deposit: ₹${String.format("%.0f", amount)}\n")
                details.append("Annual Interest Rate: ${String.format("%.2f", annualRate * 100)}%\n")
                details.append("Tenure: ${String.format("%.0f", period)} $timeUnit\n\n")

                details.append("Formula: A = PMT × [((1 + r)^n - 1) / r]\n")
                details.append("Where:\n")
                details.append("PMT = Monthly deposit (₹${String.format("%.0f", amount)})\n")
                details.append("r = Monthly interest rate (${String.format("%.4f", annualRate / 12)})\n")
                val months = if (binding.spinnerTimeUnit.selectedItemPosition == 1) period * 12 else period
                details.append("n = Number of months (${String.format("%.0f", months)})\n\n")

                details.append("Total Deposits: ₹${String.format("%.0f", result.totalInvestment)}\n")
                details.append("Interest Earned: ₹${String.format("%.0f", result.interestEarned)}\n")
                details.append("Maturity Amount: ₹${String.format("%.0f", result.maturityAmount)}")
            }

            3 -> { // PPF
                details.append("Annual Contribution: ₹${String.format("%.0f", amount)}\n")
                details.append("PPF Interest Rate: ${String.format("%.2f", annualRate * 100)}%\n")
                details.append("Investment Period: ${String.format("%.0f", period)} years\n\n")

                details.append("PPF Benefits:\n")
                details.append("• Tax deduction under Section 80C\n")
                details.append("• Tax-free maturity amount\n")
                details.append("• Government-backed scheme\n")
                details.append("• 15-year lock-in period\n\n")

                details.append("Formula: FV = PMT × [((1 + r)^n - 1) / r] × (1 + r)\n")
                details.append("Where:\n")
                details.append("PMT = Annual contribution (₹${String.format("%.0f", amount)})\n")
                details.append("r = Annual interest rate (${String.format("%.4f", annualRate)})\n")
                details.append("n = Number of years (${String.format("%.0f", period)})\n\n")

                details.append("Total Contribution: ₹${String.format("%.0f", result.totalInvestment)}\n")
                details.append("Interest Earned: ₹${String.format("%.0f", result.interestEarned)}\n")
                details.append("Maturity Value: ₹${String.format("%.0f", result.maturityAmount)}")
            }
        }

        binding.tvCalculationDetails.text = details.toString()
    }

    private fun clearAllFields() {
        binding.apply {
            etPrincipalAmount.text?.clear()
            etInterestRate.text?.clear()
            etTimePeriod.text?.clear()
            spinnerInvestmentType.setSelection(0)
            spinnerTimeUnit.setSelection(1) // Default to years
            spinnerCompounding.setSelection(0) // Default to yearly
            updateUIForInvestmentType(0)
            clearResults()
        }
        Toast.makeText(requireContext(), "All fields cleared", Toast.LENGTH_SHORT).show()
    }

    private fun clearResults() {
        binding.apply {
            tvMaturityAmount.text = "₹0"
            tvTotalInvestment.text = "₹0"
            tvInterestEarned.text = "₹0"
            tvEffectiveRate.text = "0%"
            tvMonthlySip.text = "₹0"
            tvCalculationDetails.text = "Select investment type and enter details to see calculation breakdown"

            // Reset colors
            val defaultColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
            tvMaturityAmount.setTextColor(defaultColor)
            tvInterestEarned.setTextColor(defaultColor)
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

    // Data class to hold investment calculation results
    data class InvestmentResult(
        val maturityAmount: Double,
        val totalInvestment: Double,
        val interestEarned: Double,
        val effectiveRate: Double
    )

    companion object {
        private const val TAG = "InvestmentCalculatorFragment"
    }
}