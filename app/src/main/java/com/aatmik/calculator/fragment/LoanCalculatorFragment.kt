package com.aatmik.calculator.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentLoanCalculatorBinding
import kotlin.math.pow

class LoanCalculatorFragment : Fragment() {

    private lateinit var binding: FragmentLoanCalculatorBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            // Handle back button press
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            // Text Watcher for input fields
            etLoanAmount.addTextChangedListener(loanTextWatcher)
            etInterestRate.addTextChangedListener(loanTextWatcher)
            etLoanTenure.addTextChangedListener(loanTextWatcher)

            // Calculate EMI when the button is pressed
            btnCalculateEmi.setOnClickListener {
                calculateAndDisplayEmi()
            }
        }
    }

    // Method to calculate and display the EMI
    private fun calculateAndDisplayEmi() {
        binding.apply {
            val loanAmountText = etLoanAmount.text.toString()
            val interestRateText = etInterestRate.text.toString()
            val loanTenureText = etLoanTenure.text.toString()

            if (loanAmountText.isEmpty() || interestRateText.isEmpty() || loanTenureText.isEmpty()) {
                tvEmiResult.text = "Enter all loan details to calculate"
                tvTotalInterestResult.text = ""
                tvTotalPaymentResult.text = ""
                tvLoanSummary.text = "Complete the form above"
                return
            }

            try {
                val loanAmount = loanAmountText.toDouble()
                val annualInterestRate = interestRateText.toDouble()
                val loanTenureYears = loanTenureText.toDouble()

                if (loanAmount <= 0 || annualInterestRate <= 0 || loanTenureYears <= 0) {
                    tvEmiResult.text = "Please enter positive values"
                    tvTotalInterestResult.text = ""
                    tvTotalPaymentResult.text = ""
                    tvLoanSummary.text = "All values must be greater than 0"
                    return
                }

                // EMI Calculation
                // Convert annual rate to monthly rate
                val monthlyInterestRate = annualInterestRate / (12 * 100)

                // Convert years to months
                val numberOfMonths = (loanTenureYears * 12).toInt()

                val emi = if (monthlyInterestRate == 0.0) {
                    // If interest rate is 0, EMI is simply loan amount divided by number of months
                    loanAmount / numberOfMonths
                } else {
                    // EMI Formula: P * r * (1+r)^n / ((1+r)^n - 1)
                    // Where P = Principal, r = monthly interest rate, n = number of months
                    val numerator = loanAmount * monthlyInterestRate * (1 + monthlyInterestRate).pow(numberOfMonths)
                    val denominator = (1 + monthlyInterestRate).pow(numberOfMonths) - 1
                    numerator / denominator
                }

                // Calculate total payment and total interest
                val totalPayment = emi * numberOfMonths
                val totalInterest = totalPayment - loanAmount

                // Calculate interest percentage of total payment
                val interestPercentage = (totalInterest / totalPayment) * 100

                // Format and display the results
                tvEmiResult.text = "Monthly EMI: ₹%.2f".format(emi)
                tvTotalInterestResult.text = "Total Interest: ₹%.2f".format(totalInterest)
                tvTotalPaymentResult.text = "Total Payment: ₹%.2f".format(totalPayment)
                tvLoanSummary.text = "Interest is %.1f%% of total payment over %d months".format(interestPercentage, numberOfMonths)

            } catch (e: NumberFormatException) {
                Log.e(TAG, "Invalid number format", e)
                tvEmiResult.text = "Please enter valid numerical values"
                tvTotalInterestResult.text = ""
                tvTotalPaymentResult.text = ""
                tvLoanSummary.text = "Invalid input detected"
            } catch (e: Exception) {
                Log.e(TAG, "Calculation error", e)
                tvEmiResult.text = "Calculation error occurred"
                tvTotalInterestResult.text = ""
                tvTotalPaymentResult.text = ""
                tvLoanSummary.text = "Please check your input values"
            }
        }
    }

    // TextWatcher to handle real-time input
    private val loanTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            // Automatically calculate EMI when all fields are filled
            calculateAndDisplayEmi()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentLoanCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        private const val TAG = "LoanCalculatorFragment"
    }
}