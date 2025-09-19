package com.aatmik.calculator.fragment

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentContributionCalculatorBinding
import java.io.File
import java.io.FileOutputStream
import kotlin.math.pow
import kotlin.math.roundToInt

class ContributionCalculatorFragment : Fragment() {

    private lateinit var binding: FragmentContributionCalculatorBinding
    private var isCalculating = false

    // Currency symbols for different regions
    private val currencySymbols = mapOf(
        "USD" to "$",
        "EUR" to "€",
        "GBP" to "£",
        "INR" to "₹",
        "JPY" to "¥",
        "CAD" to "C$",
        "AUD" to "A$"
    )

    // Contribution types with default rates
    private val contributionTypes = mapOf(
        "Retirement Savings (401k/IRA)" to 7.0,
        "Emergency Fund" to 2.5,
        "Investment Portfolio" to 8.0,
        "Education Fund" to 6.0,
        "House Down Payment" to 3.0,
        "Vacation Fund" to 2.0,
        "Debt Repayment" to 0.0,
        "General Savings" to 3.5
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentContributionCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        setupSpinners()
        setupListeners()
        setupTextWatchers()
        calculateContribution()
    }

    private fun setupSpinners() {
        binding.apply {
            // Setup Currency Spinner
            val currencyAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                currencySymbols.keys.toList()
            )
            currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCurrency.adapter = currencyAdapter

            // Set default to USD
            spinnerCurrency.setSelection(0)

            // Setup Contribution Type Spinner
            val contributionAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                contributionTypes.keys.toList()
            )
            contributionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerContributionType.adapter = contributionAdapter

            // Setup Frequency Spinner
            val frequencies = arrayOf("Monthly", "Quarterly", "Semi-Annually", "Annually")
            val frequencyAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                frequencies
            )
            frequencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerFrequency.adapter = frequencyAdapter

            // Setup Calculation Mode Spinner
            val modes = arrayOf("Calculate Future Value", "Calculate Required Contribution", "Calculate Time to Goal")
            val modeAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                modes
            )
            modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCalculationMode.adapter = modeAdapter
        }
    }

    private fun setupListeners() {
        binding.apply {
            // Handle back button press
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            // Share button
            shareBt.setOnClickListener {
                shareContribution()
            }

            // Spinner listeners for auto-calculation
            spinnerCurrency.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    calculateContribution()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })

            spinnerContributionType.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    updateDefaultInterestRate()
                    calculateContribution()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })

            spinnerFrequency.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    calculateContribution()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })

            spinnerCalculationMode.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    updateFieldVisibility()
                    calculateContribution()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })
        }
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isCalculating) {
                    calculateContribution()
                }
            }
        }

        binding.apply {
            etGoalName.addTextChangedListener(textWatcher)
            etCurrentAmount.addTextChangedListener(textWatcher)
            etMonthlyContribution.addTextChangedListener(textWatcher)
            etTargetAmount.addTextChangedListener(textWatcher)
            etYears.addTextChangedListener(textWatcher)
            etInterestRate.addTextChangedListener(textWatcher)
        }
    }

    private fun updateDefaultInterestRate() {
        binding.apply {
            val contributionType = spinnerContributionType.selectedItem.toString()
            val defaultRate = contributionTypes[contributionType] ?: 5.0

            if (etInterestRate.text.isEmpty()) {
                isCalculating = true
                etInterestRate.setText(defaultRate.toString())
                isCalculating = false
            }
        }
    }

    private fun updateFieldVisibility() {
        binding.apply {
            val mode = spinnerCalculationMode.selectedItem.toString()

            when (mode) {
                "Calculate Future Value" -> {
                    etTargetAmount.isEnabled = false
                    etMonthlyContribution.isEnabled = true
                    etYears.isEnabled = true
                    etTargetAmount.alpha = 0.5f
                    etMonthlyContribution.alpha = 1.0f
                    etYears.alpha = 1.0f
                }
                "Calculate Required Contribution" -> {
                    etTargetAmount.isEnabled = true
                    etMonthlyContribution.isEnabled = false
                    etYears.isEnabled = true
                    etTargetAmount.alpha = 1.0f
                    etMonthlyContribution.alpha = 0.5f
                    etYears.alpha = 1.0f
                }
                "Calculate Time to Goal" -> {
                    etTargetAmount.isEnabled = true
                    etMonthlyContribution.isEnabled = true
                    etYears.isEnabled = false
                    etTargetAmount.alpha = 1.0f
                    etMonthlyContribution.alpha = 1.0f
                    etYears.alpha = 0.5f
                }
            }
        }
    }

    private fun calculateContribution() {
        binding.apply {
            try {
                val currentAmount = etCurrentAmount.text.toString().toDoubleOrNull() ?: 0.0
                val monthlyContribution = etMonthlyContribution.text.toString().toDoubleOrNull() ?: 0.0
                val targetAmount = etTargetAmount.text.toString().toDoubleOrNull() ?: 0.0
                val years = etYears.text.toString().toDoubleOrNull() ?: 1.0
                val annualRate = etInterestRate.text.toString().toDoubleOrNull() ?: 5.0
                val frequency = getFrequencyMultiplier()
                val mode = spinnerCalculationMode.selectedItem.toString()

                // Convert annual rate to period rate
                val periodRate = annualRate / 100.0 / frequency
                val totalPeriods = years * frequency

                val result = when (mode) {
                    "Calculate Future Value" -> calculateFutureValue(currentAmount, monthlyContribution, periodRate, totalPeriods)
                    "Calculate Required Contribution" -> calculateRequiredContribution(currentAmount, targetAmount, periodRate, totalPeriods)
                    "Calculate Time to Goal" -> calculateTimeToGoal(currentAmount, monthlyContribution, targetAmount, periodRate, frequency)
                    else -> 0.0
                }

                // Get currency symbol
                val selectedCurrency = spinnerCurrency.selectedItem.toString()
                val currencySymbol = currencySymbols[selectedCurrency] ?: "$"

                // Update result displays
                updateResults(result, mode, currencySymbol, currentAmount, monthlyContribution, targetAmount, years, annualRate, frequency)

            } catch (e: Exception) {
                // Handle calculation errors silently
            }
        }
    }

    private fun getFrequencyMultiplier(): Double {
        return when (binding.spinnerFrequency.selectedItem.toString()) {
            "Monthly" -> 12.0
            "Quarterly" -> 4.0
            "Semi-Annually" -> 2.0
            "Annually" -> 1.0
            else -> 12.0
        }
    }

    private fun calculateFutureValue(present: Double, payment: Double, rate: Double, periods: Double): Double {
        if (rate == 0.0) {
            return present + (payment * periods)
        }

        val futureValuePresent = present * (1 + rate).pow(periods)
        val futureValueAnnuity = payment * (((1 + rate).pow(periods) - 1) / rate)
        return futureValuePresent + futureValueAnnuity
    }

    private fun calculateRequiredContribution(present: Double, future: Double, rate: Double, periods: Double): Double {
        if (rate == 0.0) {
            return (future - present) / periods
        }

        val futureValuePresent = present * (1 + rate).pow(periods)
        val requiredFromPayments = future - futureValuePresent
        return requiredFromPayments * rate / ((1 + rate).pow(periods) - 1)
    }

    private fun calculateTimeToGoal(present: Double, payment: Double, future: Double, rate: Double, frequency: Double): Double {
        if (rate == 0.0) {
            return (future - present) / payment / frequency
        }

        if (payment <= 0 || future <= present) return 0.0

        // Using iterative approach for complex calculation
        var years = 1.0
        while (years <= 100) {
            val periods = years * frequency
            val fv = calculateFutureValue(present, payment, rate, periods)
            if (fv >= future) {
                return years
            }
            years += 0.1
        }
        return 100.0 // Max reasonable time
    }

    private fun updateResults(result: Double, mode: String, currencySymbol: String,
                              currentAmount: Double, monthlyContribution: Double,
                              targetAmount: Double, years: Double, annualRate: Double, frequency: Double) {
        binding.apply {
            // Set flag to prevent TextWatcher from triggering calculations
            isCalculating = true

            // Update main result based on mode
            when (mode) {
                "Calculate Future Value" -> {
                    tvMainResult.text = String.format("%.0f", result)
                    tvMainResultLabel.text = "Future Value"
                    etTargetAmount.setText(String.format("%.0f", result))
                }
                "Calculate Required Contribution" -> {
                    tvMainResult.text = String.format("%.0f", result)
                    tvMainResultLabel.text = "Required Contribution"
                    etMonthlyContribution.setText(String.format("%.0f", result))
                }
                "Calculate Time to Goal" -> {
                    tvMainResult.text = String.format("%.1f", result)
                    tvMainResultLabel.text = "Years to Goal"
                    etYears.setText(String.format("%.1f", result))
                }
            }

            // Reset flag after all setText calls
            isCalculating = false

            // Update all currency symbols
            tvCurrencySymbol.text = currencySymbol
            tvCurrencySymbol2.text = currencySymbol
            tvCurrencySymbol3.text = currencySymbol

            // Calculate additional metrics
            val totalContributions = monthlyContribution * years * frequency
            val totalGrowth = when (mode) {
                "Calculate Future Value" -> result - currentAmount - totalContributions
                else -> calculateFutureValue(currentAmount, monthlyContribution, annualRate/100.0/frequency, years * frequency) - currentAmount - totalContributions
            }

            // Update breakdown
            tvCurrentAmount.text = String.format("%s%.0f", currencySymbol, currentAmount)
            tvTotalContributions.text = String.format("%s%.0f", currencySymbol, totalContributions)
            tvTotalGrowth.text = String.format("%s%.0f", currencySymbol, totalGrowth)
            tvFinalAmount.text = String.format("%s%.0f", currencySymbol, currentAmount + totalContributions + totalGrowth)

            // Update additional info
            updateAdditionalInfo(currentAmount, monthlyContribution, targetAmount, years, annualRate, frequency, mode, currencySymbol)
        }
    }

    private fun updateAdditionalInfo(currentAmount: Double, monthlyContribution: Double,
                                     targetAmount: Double, years: Double, annualRate: Double,
                                     frequency: Double, mode: String, currencySymbol: String) {
        binding.apply {
            val goalName = etGoalName.text.toString().trim().ifEmpty { "Financial Goal" }
            val contributionType = spinnerContributionType.selectedItem?.toString() ?: "General Savings"

            val result = StringBuilder()
            result.append("📊 $goalName Analysis\n\n")

            result.append("🎯 Goal Type: $contributionType\n")
            result.append("📅 Contribution Frequency: ${spinnerFrequency.selectedItem}\n")
            result.append("📈 Expected Annual Return: ${String.format("%.1f", annualRate)}%\n")
            result.append("⏱️ Time Period: ${String.format("%.1f", years)} years\n\n")

            // Calculate monthly breakdown
            val monthlyFromContributions = monthlyContribution
            val monthlyFromGrowth = (annualRate / 100.0 / 12.0) * (currentAmount + (monthlyContribution * 6)) // Approximate mid-year balance

            result.append("💰 Monthly Breakdown:\n")
            result.append("• Your contribution: $currencySymbol${String.format("%.0f", monthlyFromContributions)}\n")
            result.append("• Expected growth: $currencySymbol${String.format("%.0f", monthlyFromGrowth)}\n")
            result.append("• Total monthly increase: $currencySymbol${String.format("%.0f", monthlyFromContributions + monthlyFromGrowth)}\n\n")

            // Goal progress
            val targetProgress = if (targetAmount > 0) {
                val finalAmount = currentAmount + (monthlyContribution * years * frequency) +
                        calculateFutureValue(currentAmount, monthlyContribution, annualRate/100.0/frequency, years * frequency) - currentAmount - (monthlyContribution * years * frequency)
                (finalAmount / targetAmount * 100).coerceAtMost(100.0)
            } else 100.0

            result.append("🎯 Goal Achievement:\n")
            result.append("• Progress toward target: ${String.format("%.1f", targetProgress)}%\n")

            if (targetProgress >= 100) {
                result.append("• Status: ✅ Goal will be achieved!\n")
            } else {
                val shortfall = targetAmount - (currentAmount + (monthlyContribution * years * frequency))
                result.append("• Status: ⚠️ May fall short by $currencySymbol${String.format("%.0f", shortfall)}\n")
            }
            result.append("\n")

            // Tips based on contribution type and results
            result.append("💡 Recommendations:\n")

            when (contributionType) {
                "Retirement Savings (401k/IRA)" -> {
                    result.append("• Maximize employer matching if available\n")
                    result.append("• Consider tax advantages of retirement accounts\n")
                    result.append("• Review and rebalance portfolio annually\n")
                }
                "Emergency Fund" -> {
                    result.append("• Aim for 3-6 months of expenses\n")
                    result.append("• Keep in high-yield savings account\n")
                    result.append("• Prioritize accessibility over high returns\n")
                }
                "Investment Portfolio" -> {
                    result.append("• Diversify across asset classes\n")
                    result.append("• Consider dollar-cost averaging\n")
                    result.append("• Rebalance portfolio periodically\n")
                }
                else -> {
                    result.append("• Review progress quarterly\n")
                    result.append("• Adjust contributions as income changes\n")
                    result.append("• Consider automatic transfers\n")
                }
            }

            if (annualRate > 8.0) {
                result.append("• High return assumption - consider conservative estimates\n")
            }

            if (monthlyContribution > 0) {
                val incomePercentage = (monthlyContribution * 12) / (monthlyContribution * 12 / 0.2) // Assuming 20% savings rate is good
                if (incomePercentage < 0.1) {
                    result.append("• Consider increasing contribution rate if possible\n")
                }
            }

            tvContributionDetails.text = result.toString()
        }
    }

    private fun shareContribution() {
        // Show options: Share as Text or Share as Image
        val options = arrayOf("Share as Text", "Share as Image")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Share Contribution Plan")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> shareAsText()
                    1 -> captureAndShareScreenshot()
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun shareAsText() {
        val selectedCurrency = binding.spinnerCurrency.selectedItem.toString()
        val currencySymbol = currencySymbols[selectedCurrency] ?: "$"
        val goalName = binding.etGoalName.text.toString().trim().ifEmpty { "Financial Goal" }
        val contributionType = binding.spinnerContributionType.selectedItem.toString()
        val mode = binding.spinnerCalculationMode.selectedItem.toString()
        val mainResult = binding.tvMainResult.text.toString()

        val shareText = buildString {
            append("📊 $goalName Plan\n\n")
            append("Goal Type: $contributionType\n")
            append("Calculation: $mode\n")
            append("Result: $currencySymbol$mainResult\n\n")

            append("Breakdown:\n")
            append("• Current Amount: ${binding.tvCurrentAmount.text}\n")
            append("• Total Contributions: ${binding.tvTotalContributions.text}\n")
            append("• Expected Growth: ${binding.tvTotalGrowth.text}\n")
            append("• Final Amount: ${binding.tvFinalAmount.text}\n\n")

            append("Calculated with Contribution Calculator")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "$goalName Plan")
        }

        startActivity(Intent.createChooser(intent, "Share Contribution Plan"))
    }

    private fun captureAndShareScreenshot() {
        val screenshotView = binding.contributionResultCard
        val bitmap = Bitmap.createBitmap(
            screenshotView.width,
            screenshotView.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        screenshotView.draw(canvas)

        // Save the bitmap to a file
        val file = File(requireContext().cacheDir, "contribution_result_screenshot.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        // Get a content URI for the file using FileProvider
        val contentUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        // Create a share intent
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Start the share activity
        startActivity(Intent.createChooser(shareIntent, "Share Contribution Plan"))
    }

    companion object {
        private const val TAG = "ContributionCalculatorFragment"
    }
}