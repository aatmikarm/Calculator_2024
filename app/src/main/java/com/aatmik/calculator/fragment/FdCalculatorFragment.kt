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
import com.aatmik.calculator.databinding.FragmentFdCalculatorBinding
import kotlin.math.pow

class FdCalculatorFragment : Fragment() {

    private lateinit var binding: FragmentFdCalculatorBinding
    private var selectedCompoundingFrequency: Int = 4 // Quarterly by default

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        binding.apply {
            // Handle back button press
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            // Set initial button states
            updateCompoundingButtonStates()

            // Text Watcher for input fields
            etPrincipal.addTextChangedListener(fdTextWatcher)
            etRate.addTextChangedListener(fdTextWatcher)
            etTime.addTextChangedListener(fdTextWatcher)

            // Compounding frequency buttons
            btnMonthly.setOnClickListener {
                selectedCompoundingFrequency = 12
                updateCompoundingButtonStates()
                calculateAndDisplayFD()
            }

            btnQuarterly.setOnClickListener {
                selectedCompoundingFrequency = 4
                updateCompoundingButtonStates()
                calculateAndDisplayFD()
            }

            btnHalfYearly.setOnClickListener {
                selectedCompoundingFrequency = 2
                updateCompoundingButtonStates()
                calculateAndDisplayFD()
            }

            btnYearly.setOnClickListener {
                selectedCompoundingFrequency = 1
                updateCompoundingButtonStates()
                calculateAndDisplayFD()
            }

            // Calculate FD when the button is pressed
            btnCalculateFd.setOnClickListener {
                calculateAndDisplayFD()
            }
        }
    }

    // Method to update compounding frequency button states
    private fun updateCompoundingButtonStates() {
        binding.apply {
            btnMonthly.isSelected = selectedCompoundingFrequency == 12
            btnQuarterly.isSelected = selectedCompoundingFrequency == 4
            btnHalfYearly.isSelected = selectedCompoundingFrequency == 2
            btnYearly.isSelected = selectedCompoundingFrequency == 1
        }
    }

    // Method to calculate and display the FD returns
    private fun calculateAndDisplayFD() {
        binding.apply {
            val principalText = etPrincipal.text.toString()
            val rateText = etRate.text.toString()
            val timeText = etTime.text.toString()

            if (principalText.isEmpty() || rateText.isEmpty() || timeText.isEmpty()) {
                tvMaturityAmountResult.text = "Enter all values to calculate"
                tvInterestEarnedResult.text = ""
                tvCompoundingFrequencyResult.text = ""
                compoundingBadge.visibility = View.GONE
                return
            }

            try {
                val principal = principalText.toDouble()
                val rate = rateText.toDouble()
                val timeInYears = timeText.toDouble()

                if (principal <= 0 || rate <= 0 || timeInYears <= 0) {
                    tvMaturityAmountResult.text = "Please enter positive values"
                    tvInterestEarnedResult.text = ""
                    tvCompoundingFrequencyResult.text = ""
                    compoundingBadge.visibility = View.GONE
                    return
                }

                // FD Formula: A = P * (1 + r/n)^(n*t)
                // Where:
                // A = Maturity Amount
                // P = Principal Amount
                // r = Annual Interest Rate (in decimal)
                // n = Compounding Frequency (times per year)
                // t = Time Period (in years)

                val rateDecimal = rate / 100
                val maturityAmount = principal * (1 + rateDecimal / selectedCompoundingFrequency).pow(selectedCompoundingFrequency * timeInYears)
                val interestEarned = maturityAmount - principal

                // Get compounding frequency text with detailed info
                val compoundingText = when (selectedCompoundingFrequency) {
                    12 -> "Monthly (n=12)"
                    4 -> "Quarterly (n=4)"
                    2 -> "Half-Yearly (n=2)"
                    1 -> "Yearly (n=1)"
                    else -> "Quarterly (n=4)"
                }

                // Format and display the results
                tvMaturityAmountResult.text = "Maturity Amount: %.2f".format(maturityAmount)
                tvInterestEarnedResult.text = "Interest Earned: %.2f".format(interestEarned)
                tvCompoundingFrequencyResult.text = compoundingText
                compoundingBadge.visibility = View.VISIBLE

            } catch (e: NumberFormatException) {
                Log.e(TAG, "Invalid number format", e)
                tvMaturityAmountResult.text = "Please enter valid numerical values"
                tvInterestEarnedResult.text = ""
                tvCompoundingFrequencyResult.text = ""
                compoundingBadge.visibility = View.GONE
            }
        }
    }

    // TextWatcher to handle real-time input
    private val fdTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            // Automatically calculate FD when all fields are filled
            calculateAndDisplayFD()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentFdCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        private const val TAG = "FdCalculatorFragment"
    }
}