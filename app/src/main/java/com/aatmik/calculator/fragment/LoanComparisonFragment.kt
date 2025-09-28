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
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentLoanComparisonBinding
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.*
import kotlin.math.pow

class LoanComparisonFragment : Fragment() {

    private lateinit var binding: FragmentLoanComparisonBinding
    private var isCalculating = false
    private var selectedCurrency = "USD"
    private var selectedCountry = "United States"

    // Country and Currency data
    private val countryData = mapOf(
        "United States" to CountryInfo("USD", "$", "US"),
        "India" to CountryInfo("INR", "₹", "IN"),
        "United Kingdom" to CountryInfo("GBP", "£", "UK"),
        "Canada" to CountryInfo("CAD", "C$", "CA"),
        "Australia" to CountryInfo("AUD", "A$", "AU"),
        "Germany" to CountryInfo("EUR", "€", "DE"),
        "France" to CountryInfo("EUR", "€", "FR"),
        "Japan" to CountryInfo("JPY", "¥", "JP"),
        "China" to CountryInfo("CNY", "¥", "CN"),
        "Brazil" to CountryInfo("BRL", "R$", "BR"),
        "Mexico" to CountryInfo("MXN", "$", "MX"),
        "South Africa" to CountryInfo("ZAR", "R", "ZA"),
        "Singapore" to CountryInfo("SGD", "S$", "SG"),
        "Hong Kong" to CountryInfo("HKD", "HK$", "HK"),
        "Switzerland" to CountryInfo("CHF", "CHF", "CH"),
        "Sweden" to CountryInfo("SEK", "kr", "SE"),
        "Norway" to CountryInfo("NOK", "kr", "NO"),
        "South Korea" to CountryInfo("KRW", "₩", "KR"),
        "Thailand" to CountryInfo("THB", "฿", "TH"),
        "UAE" to CountryInfo("AED", "د.إ", "AE")
    )

    // Universal loan types
    private val loanTypes = arrayOf(
        "Personal Loan",
        "Mortgage/Home Loan",
        "Auto/Car Loan",
        "Student/Education Loan",
        "Business Loan",
        "Credit Card",
        "Payday Loan",
        "Secured Loan",
        "Unsecured Loan",
        "Line of Credit",
        "Custom Loan"
    )

    // Interest rate ranges by country and loan type
    private val interestRates = mapOf(
        "US" to mapOf(
            "Personal Loan" to "6% - 36%",
            "Mortgage/Home Loan" to "3% - 8%",
            "Auto/Car Loan" to "3% - 15%",
            "Student/Education Loan" to "3% - 12%",
            "Business Loan" to "4% - 25%",
            "Credit Card" to "15% - 29%",
            "Payday Loan" to "200% - 600%",
            "Secured Loan" to "3% - 15%",
            "Unsecured Loan" to "8% - 36%",
            "Line of Credit" to "5% - 25%",
            "Custom Loan" to "Varies"
        ),
        "IN" to mapOf(
            "Personal Loan" to "10.5% - 24%",
            "Mortgage/Home Loan" to "8.5% - 12%",
            "Auto/Car Loan" to "7.5% - 15%",
            "Student/Education Loan" to "8.5% - 16%",
            "Business Loan" to "11% - 20%",
            "Credit Card" to "12% - 45%",
            "Payday Loan" to "24% - 36%",
            "Secured Loan" to "7% - 16%",
            "Unsecured Loan" to "10% - 24%",
            "Line of Credit" to "9% - 18%",
            "Custom Loan" to "Varies"
        ),
        "UK" to mapOf(
            "Personal Loan" to "3% - 35%",
            "Mortgage/Home Loan" to "2% - 6%",
            "Auto/Car Loan" to "3% - 15%",
            "Student/Education Loan" to "1% - 6%",
            "Business Loan" to "3% - 20%",
            "Credit Card" to "18% - 35%",
            "Payday Loan" to "100% - 1500%",
            "Secured Loan" to "3% - 12%",
            "Unsecured Loan" to "5% - 35%",
            "Line of Credit" to "4% - 20%",
            "Custom Loan" to "Varies"
        ),
        "DEFAULT" to mapOf(
            "Personal Loan" to "5% - 30%",
            "Mortgage/Home Loan" to "3% - 10%",
            "Auto/Car Loan" to "4% - 18%",
            "Student/Education Loan" to "3% - 15%",
            "Business Loan" to "5% - 25%",
            "Credit Card" to "15% - 35%",
            "Payday Loan" to "100% - 400%",
            "Secured Loan" to "3% - 15%",
            "Unsecured Loan" to "8% - 30%",
            "Line of Credit" to "5% - 25%",
            "Custom Loan" to "Varies"
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentLoanComparisonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        setupCountrySpinner()
        setupSpinners()
        setupListeners()
        setupTextWatchers()
        calculateLoans()
    }

    private fun setupCountrySpinner() {
        val countries = countryData.keys.sorted()
        val countryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            countries
        )
        countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCountry.adapter = countryAdapter

        // Set default to United States
        binding.spinnerCountry.setSelection(countries.indexOf("United States"))
    }

    private fun setupSpinners() {
        binding.apply {
            // Setup Loan Type Spinners
            val loanAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                loanTypes
            )
            loanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spinnerLoan1Type.adapter = loanAdapter
            spinnerLoan2Type.adapter = loanAdapter
            spinnerLoan3Type.adapter = loanAdapter

            // Set different defaults for comparison
            spinnerLoan1Type.setSelection(0) // Personal Loan
            spinnerLoan2Type.setSelection(1) // Mortgage/Home Loan
            spinnerLoan3Type.setSelection(2) // Auto/Car Loan
        }
    }

    private fun setupListeners() {
        binding.apply {
            // Back button
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            // Share button
            shareBt.setOnClickListener {
                shareLoanComparison()
            }

            // Clear button
            btnClear.setOnClickListener {
                clearAllFields()
            }

            // Country selection listener
            spinnerCountry.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val countries = countryData.keys.sorted()
                    selectedCountry = countries[position]
                    val countryInfo = countryData[selectedCountry]!!
                    selectedCurrency = countryInfo.currency

                    // Update all rate hints and currency displays
                    updateAllCurrencyDisplays()
                    updateAllRateHints()
                    calculateLoans()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })

            // Loan type listeners
            spinnerLoan1Type.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    updateTypicalRate(1, loanTypes[position])
                    calculateLoans()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })

            spinnerLoan2Type.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    updateTypicalRate(2, loanTypes[position])
                    calculateLoans()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })

            spinnerLoan3Type.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    updateTypicalRate(3, loanTypes[position])
                    calculateLoans()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })

            // Toggle buttons for showing/hiding loans
            btnToggleLoan2.setOnClickListener {
                toggleLoanVisibility(2)
            }

            btnToggleLoan3.setOnClickListener {
                toggleLoanVisibility(3)
            }
        }
    }

    private fun updateAllCurrencyDisplays() {
        val currencySymbol = countryData[selectedCountry]?.symbol ?: "$"
        binding.apply {
            // Update input field hints
            etLoan1Amount.hint = "Loan amount ($currencySymbol)"
            etLoan2Amount.hint = "Loan amount ($currencySymbol)"
            etLoan3Amount.hint = "Loan amount ($currencySymbol)"

            etLoan1ProcessingFee.hint = "Processing fee ($currencySymbol)"
            etLoan2ProcessingFee.hint = "Processing fee ($currencySymbol)"
            etLoan3ProcessingFee.hint = "Processing fee ($currencySymbol)"

            // Update labels
            tvLoan1AmountLabel.text = "Amount ($currencySymbol)"
            tvLoan2AmountLabel.text = "Amount ($currencySymbol)"
            tvLoan3AmountLabel.text = "Amount ($currencySymbol)"

            tvLoan1ProcessingFeeLabel.text = "Proc. Fee ($currencySymbol)"
            tvLoan2ProcessingFeeLabel.text = "Proc. Fee ($currencySymbol)"
            tvLoan3ProcessingFeeLabel.text = "Proc. Fee ($currencySymbol)"
        }
    }

    private fun updateAllRateHints() {
        updateTypicalRate(1, binding.spinnerLoan1Type.selectedItem.toString())
        updateTypicalRate(2, binding.spinnerLoan2Type.selectedItem.toString())
        updateTypicalRate(3, binding.spinnerLoan3Type.selectedItem.toString())
    }

    private fun updateTypicalRate(loanNumber: Int, loanType: String) {
        val countryCode = countryData[selectedCountry]?.code ?: "DEFAULT"
        val rateRange = interestRates[countryCode]?.get(loanType)
            ?: interestRates["DEFAULT"]?.get(loanType)
            ?: "Varies"

        binding.apply {
            when (loanNumber) {
                1 -> tvLoan1RateHint.text = "Typical in $selectedCountry: $rateRange"
                2 -> tvLoan2RateHint.text = "Typical in $selectedCountry: $rateRange"
                3 -> tvLoan3RateHint.text = "Typical in $selectedCountry: $rateRange"
            }
        }
    }

    private fun toggleLoanVisibility(loanNumber: Int) {
        binding.apply {
            when (loanNumber) {
                2 -> {
                    if (layoutLoan2.visibility == View.VISIBLE) {
                        layoutLoan2.visibility = View.GONE
                        btnToggleLoan2.text = "Add Loan 2"
                    } else {
                        layoutLoan2.visibility = View.VISIBLE
                        btnToggleLoan2.text = "Remove Loan 2"
                    }
                }
                3 -> {
                    if (layoutLoan3.visibility == View.VISIBLE) {
                        layoutLoan3.visibility = View.GONE
                        btnToggleLoan3.text = "Add Loan 3"
                    } else {
                        layoutLoan3.visibility = View.VISIBLE
                        btnToggleLoan3.text = "Remove Loan 3"
                    }
                }
            }
            calculateLoans()
        }
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isCalculating) {
                    calculateLoans()
                }
            }
        }

        binding.apply {
            // Loan 1 watchers
            etLoan1Amount.addTextChangedListener(textWatcher)
            etLoan1Rate.addTextChangedListener(textWatcher)
            etLoan1Tenure.addTextChangedListener(textWatcher)
            etLoan1ProcessingFee.addTextChangedListener(textWatcher)

            // Loan 2 watchers
            etLoan2Amount.addTextChangedListener(textWatcher)
            etLoan2Rate.addTextChangedListener(textWatcher)
            etLoan2Tenure.addTextChangedListener(textWatcher)
            etLoan2ProcessingFee.addTextChangedListener(textWatcher)

            // Loan 3 watchers
            etLoan3Amount.addTextChangedListener(textWatcher)
            etLoan3Rate.addTextChangedListener(textWatcher)
            etLoan3Tenure.addTextChangedListener(textWatcher)
            etLoan3ProcessingFee.addTextChangedListener(textWatcher)
        }
    }

    private fun formatCurrency(amount: Double): String {
        val currencyInfo = countryData[selectedCountry]!!
        return if (selectedCurrency == "JPY" || selectedCurrency == "KRW") {
            // No decimal places for Japanese Yen and Korean Won
            "${currencyInfo.symbol}${String.format("%.0f", amount)}"
        } else {
            "${currencyInfo.symbol}${String.format("%.2f", amount)}"
        }
    }

    private fun calculateLoans() {
        binding.apply {
            try {
                // Calculate Loan 1
                val loan1 = calculateLoanDetails(
                    etLoan1Amount.text.toString().toDoubleOrNull() ?: 0.0,
                    etLoan1Rate.text.toString().toDoubleOrNull() ?: 0.0,
                    etLoan1Tenure.text.toString().toDoubleOrNull() ?: 0.0,
                    etLoan1ProcessingFee.text.toString().toDoubleOrNull() ?: 0.0
                )
                updateLoanResults(1, loan1)

                // Calculate Loan 2 if visible
                if (layoutLoan2.visibility == View.VISIBLE) {
                    val loan2 = calculateLoanDetails(
                        etLoan2Amount.text.toString().toDoubleOrNull() ?: 0.0,
                        etLoan2Rate.text.toString().toDoubleOrNull() ?: 0.0,
                        etLoan2Tenure.text.toString().toDoubleOrNull() ?: 0.0,
                        etLoan2ProcessingFee.text.toString().toDoubleOrNull() ?: 0.0
                    )
                    updateLoanResults(2, loan2)
                }

                // Calculate Loan 3 if visible
                if (layoutLoan3.visibility == View.VISIBLE) {
                    val loan3 = calculateLoanDetails(
                        etLoan3Amount.text.toString().toDoubleOrNull() ?: 0.0,
                        etLoan3Rate.text.toString().toDoubleOrNull() ?: 0.0,
                        etLoan3Tenure.text.toString().toDoubleOrNull() ?: 0.0,
                        etLoan3ProcessingFee.text.toString().toDoubleOrNull() ?: 0.0
                    )
                    updateLoanResults(3, loan3)
                }

                // Update comparison
                updateComparison(loan1,
                    if (layoutLoan2.visibility == View.VISIBLE) calculateLoanDetails(
                        etLoan2Amount.text.toString().toDoubleOrNull() ?: 0.0,
                        etLoan2Rate.text.toString().toDoubleOrNull() ?: 0.0,
                        etLoan2Tenure.text.toString().toDoubleOrNull() ?: 0.0,
                        etLoan2ProcessingFee.text.toString().toDoubleOrNull() ?: 0.0
                    ) else null,
                    if (layoutLoan3.visibility == View.VISIBLE) calculateLoanDetails(
                        etLoan3Amount.text.toString().toDoubleOrNull() ?: 0.0,
                        etLoan3Rate.text.toString().toDoubleOrNull() ?: 0.0,
                        etLoan3Tenure.text.toString().toDoubleOrNull() ?: 0.0,
                        etLoan3ProcessingFee.text.toString().toDoubleOrNull() ?: 0.0
                    ) else null
                )

            } catch (e: Exception) {
                // Handle calculation errors silently
            }
        }
    }

    private fun calculateLoanDetails(
        principal: Double,
        annualRate: Double,
        tenureYears: Double,
        processingFee: Double
    ): LoanDetails {
        if (principal <= 0 || annualRate <= 0 || tenureYears <= 0) {
            return LoanDetails()
        }

        val monthlyRate = annualRate / (12 * 100)
        val tenureMonths = (tenureYears * 12).toInt()

        // EMI calculation using standard formula
        val emi = if (monthlyRate == 0.0) {
            principal / tenureMonths
        } else {
            principal * monthlyRate * (1 + monthlyRate).pow(tenureMonths) /
                    ((1 + monthlyRate).pow(tenureMonths) - 1)
        }

        val totalPayment = emi * tenureMonths
        val totalInterest = totalPayment - principal
        val totalCost = totalPayment + processingFee

        return LoanDetails(
            principal = principal,
            emi = emi,
            totalInterest = totalInterest,
            totalPayment = totalPayment,
            processingFee = processingFee,
            totalCost = totalCost,
            annualRate = annualRate,
            tenureMonths = tenureMonths
        )
    }

    private fun updateLoanResults(loanNumber: Int, loan: LoanDetails) {
        binding.apply {
            when (loanNumber) {
                1 -> {
                    tvLoan1Emi.text = formatCurrency(loan.emi)
                    tvLoan1TotalInterest.text = formatCurrency(loan.totalInterest)
                    tvLoan1TotalPayment.text = formatCurrency(loan.totalPayment)
                    tvLoan1TotalCost.text = formatCurrency(loan.totalCost)
                }
                2 -> {
                    tvLoan2Emi.text = formatCurrency(loan.emi)
                    tvLoan2TotalInterest.text = formatCurrency(loan.totalInterest)
                    tvLoan2TotalPayment.text = formatCurrency(loan.totalPayment)
                    tvLoan2TotalCost.text = formatCurrency(loan.totalCost)
                }
                3 -> {
                    tvLoan3Emi.text = formatCurrency(loan.emi)
                    tvLoan3TotalInterest.text = formatCurrency(loan.totalInterest)
                    tvLoan3TotalPayment.text = formatCurrency(loan.totalPayment)
                    tvLoan3TotalCost.text = formatCurrency(loan.totalCost)
                }
            }
        }
    }

    private fun updateComparison(loan1: LoanDetails, loan2: LoanDetails?, loan3: LoanDetails?) {
        binding.apply {
            val loans = listOfNotNull(loan1, loan2, loan3).filter { it.emi > 0 }

            if (loans.size < 2) {
                layoutComparison.visibility = View.GONE
                return
            }

            layoutComparison.visibility = View.VISIBLE

            // Find best and worst options
            val lowestEmi = loans.minByOrNull { it.emi }
            val lowestTotalCost = loans.minByOrNull { it.totalCost }
            val highestTotalCost = loans.maxByOrNull { it.totalCost }

            val analysis = StringBuilder()
            analysis.append("Loan Comparison Analysis ($selectedCountry)\n\n")

            analysis.append("Best EMI Option:\n")
            analysis.append("• Lowest EMI: ${formatCurrency(lowestEmi?.emi ?: 0.0)}\n")
            analysis.append("• ${getLoanName(loans.indexOf(lowestEmi) + 1)}\n\n")

            analysis.append("Best Overall Option:\n")
            analysis.append("• Lowest Total Cost: ${formatCurrency(lowestTotalCost?.totalCost ?: 0.0)}\n")
            analysis.append("• ${getLoanName(loans.indexOf(lowestTotalCost) + 1)}\n\n")

            if (lowestTotalCost != null && highestTotalCost != null && loans.size > 1) {
                val savings = highestTotalCost.totalCost - lowestTotalCost.totalCost
                analysis.append("Potential Savings:\n")
                analysis.append("• You could save ${formatCurrency(savings)} by choosing the best option\n")
                analysis.append("• That's ${String.format("%.1f", (savings / highestTotalCost.totalCost) * 100)}% less cost\n\n")
            }

            analysis.append("Key Metrics Comparison:\n")
            loans.forEachIndexed { index, loan ->
                val loanName = getLoanName(index + 1)
                analysis.append("• $loanName:\n")
                analysis.append("  EMI: ${formatCurrency(loan.emi)}\n")
                analysis.append("  Total Interest: ${formatCurrency(loan.totalInterest)}\n")
                analysis.append("  Total Cost: ${formatCurrency(loan.totalCost)}\n\n")
            }

            analysis.append("Quick Tips:\n")
            analysis.append("• Lower EMI doesn't always mean lower total cost\n")
            analysis.append("• Consider processing fees in total cost calculation\n")
            analysis.append("• Shorter tenure = higher EMI but lower total interest\n")
            analysis.append("• Compare APR (Annual Percentage Rate) for true cost\n")
            analysis.append("• Interest rates vary by country and creditworthiness\n")

            tvComparisonAnalysis.text = analysis.toString()
        }
    }

    private fun getLoanName(loanNumber: Int): String {
        return binding.run {
            when (loanNumber) {
                1 -> spinnerLoan1Type.selectedItem.toString()
                2 -> if (layoutLoan2.visibility == View.VISIBLE) spinnerLoan2Type.selectedItem.toString() else "Loan 2"
                3 -> if (layoutLoan3.visibility == View.VISIBLE) spinnerLoan3Type.selectedItem.toString() else "Loan 3"
                else -> "Loan $loanNumber"
            }
        }
    }

    private fun clearAllFields() {
        binding.apply {
            // Clear Loan 1
            etLoan1Amount.text?.clear()
            etLoan1Rate.text?.clear()
            etLoan1Tenure.text?.clear()
            etLoan1ProcessingFee.text?.clear()

            // Clear Loan 2
            etLoan2Amount.text?.clear()
            etLoan2Rate.text?.clear()
            etLoan2Tenure.text?.clear()
            etLoan2ProcessingFee.text?.clear()

            // Clear Loan 3
            etLoan3Amount.text?.clear()
            etLoan3Rate.text?.clear()
            etLoan3Tenure.text?.clear()
            etLoan3ProcessingFee.text?.clear()

            // Reset spinners
            spinnerLoan1Type.setSelection(0)
            spinnerLoan2Type.setSelection(1)
            spinnerLoan3Type.setSelection(2)
        }
    }

    private fun shareLoanComparison() {
        val options = arrayOf("Share as Text", "Share as Image")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Share Loan Comparison")
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
        val currencyInfo = countryData[selectedCountry]!!
        val loan1Type = binding.spinnerLoan1Type.selectedItem.toString()
        val loan1Emi = binding.tvLoan1Emi.text.toString()
        val loan1TotalCost = binding.tvLoan1TotalCost.text.toString()

        val shareText = buildString {
            append("Loan Comparison Results ($selectedCountry - ${currencyInfo.currency})\n\n")
            append("Loan 1 ($loan1Type):\n")
            append("• EMI: $loan1Emi\n")
            append("• Total Cost: $loan1TotalCost\n\n")

            if (binding.layoutLoan2.visibility == View.VISIBLE) {
                val loan2Type = binding.spinnerLoan2Type.selectedItem.toString()
                append("Loan 2 ($loan2Type):\n")
                append("• EMI: ${binding.tvLoan2Emi.text}\n")
                append("• Total Cost: ${binding.tvLoan2TotalCost.text}\n\n")
            }

            if (binding.layoutLoan3.visibility == View.VISIBLE) {
                val loan3Type = binding.spinnerLoan3Type.selectedItem.toString()
                append("Loan 3 ($loan3Type):\n")
                append("• EMI: ${binding.tvLoan3Emi.text}\n")
                append("• Total Cost: ${binding.tvLoan3TotalCost.text}\n\n")
            }

            append("Calculated with International Loan Comparison Calculator\n")
            append("Always verify with local lenders before making decisions")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Loan Comparison Analysis - $selectedCountry")
        }

        startActivity(Intent.createChooser(intent, "Share Loan Comparison"))
    }

    private fun captureAndShareScreenshot() {
        val screenshotView = binding.loanComparisonCard
        val bitmap = Bitmap.createBitmap(
            screenshotView.width,
            screenshotView.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        screenshotView.draw(canvas)

        val file = File(requireContext().cacheDir, "loan_comparison_${selectedCountry.lowercase().replace(" ", "_")}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val contentUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share Loan Comparison"))
    }

    data class CountryInfo(
        val currency: String,
        val symbol: String,
        val code: String
    )

    data class LoanDetails(
        val principal: Double = 0.0,
        val emi: Double = 0.0,
        val totalInterest: Double = 0.0,
        val totalPayment: Double = 0.0,
        val processingFee: Double = 0.0,
        val totalCost: Double = 0.0,
        val annualRate: Double = 0.0,
        val tenureMonths: Int = 0
    )

    companion object {
        private const val TAG = "LoanComparisonFragment"
    }
}