package com.aatmik.calculator.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentInterestCalculatorBinding
import kotlin.math.pow

class InterestCalculatorFragment : Fragment() {

    private lateinit var binding: FragmentInterestCalculatorBinding
    private var isSimpleInterest: Boolean = true // By default, set to Simple Interest

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        binding.apply {
            // Handle back button press
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            // Set initial button states
            updateButtonStates()

            // Text Watcher for input fields
            etPrincipal.addTextChangedListener(interestTextWatcher)
            etRate.addTextChangedListener(interestTextWatcher)
            etTime.addTextChangedListener(interestTextWatcher)

            // Toggle between Simple and Compound Interest
            btnSimpleInterest.setOnClickListener {
                isSimpleInterest = true
                updateButtonStates()
                calculateAndDisplayInterest()
            }

            btnCompoundInterest.setOnClickListener {
                isSimpleInterest = false
                updateButtonStates()
                calculateAndDisplayInterest()
            }

            // Calculate Interest when the button is pressed
            btnCalculateInterest.setOnClickListener {
                calculateAndDisplayInterest()
            }
        }
    }

    // Method to update button states based on selected interest type
    private fun updateButtonStates() {
        binding.apply {
            if (isSimpleInterest) {
                btnSimpleInterest.isSelected = true
                btnCompoundInterest.isSelected = false
                // You can also change background colors here if needed
            } else {
                btnSimpleInterest.isSelected = false
                btnCompoundInterest.isSelected = true
            }
        }
    }

    // Method to calculate and display the interest
    private fun calculateAndDisplayInterest() {
        binding.apply {
            val principalText = etPrincipal.text.toString()
            val rateText = etRate.text.toString()
            val timeText = etTime.text.toString()

            if (principalText.isEmpty() || rateText.isEmpty() || timeText.isEmpty()) {
                tvInterestResult.text = "Enter all values to calculate"
                tvTotalAmountResult.text = ""
                tvInterestTypeResult.text = ""
                return
            }

            try {
                val principal = principalText.toDouble()
                val rate = rateText.toDouble()
                val time = timeText.toDouble()

                if (principal <= 0 || rate <= 0 || time <= 0) {
                    tvInterestResult.text = "Please enter positive values"
                    tvTotalAmountResult.text = ""
                    tvInterestTypeResult.text = ""
                    return
                }

                var interest: Double
                var totalAmount: Double

                if (isSimpleInterest) {
                    // Simple Interest Formula: SI = (P * R * T) / 100
                    interest = (principal * rate * time) / 100
                    totalAmount = principal + interest
                    tvInterestTypeResult.text = "Simple Interest Calculation"
                } else {
                    // Compound Interest Formula: CI = P * (1 + R/100)^T - P
                    totalAmount = principal * (1 + rate / 100).pow(time)
                    interest = totalAmount - principal
                    tvInterestTypeResult.text = "Compound Interest Calculation"
                }

                // Format and display the results
                tvInterestResult.text = "Interest Earned: %.2f".format(interest)
                tvTotalAmountResult.text = "Total Amount: %.2f".format(totalAmount)

            } catch (e: NumberFormatException) {
                Log.e(TAG, "Invalid number format", e)
                tvInterestResult.text = "Please enter valid numerical values"
                tvTotalAmountResult.text = ""
                tvInterestTypeResult.text = ""
            }
        }
    }

    // TextWatcher to handle real-time input
    private val interestTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            // Automatically calculate interest when all fields are filled
            calculateAndDisplayInterest()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentInterestCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        private const val TAG = "InterestCalculatorFragment"
    }
}