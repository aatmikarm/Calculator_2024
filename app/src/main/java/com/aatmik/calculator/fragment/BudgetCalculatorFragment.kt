package com.aatmik.calculator.fragment

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aatmik.calculator.databinding.FragmentBudgetCalculatorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import startGlowAnimation
import stopGlowAnimation
import java.io.File
import java.io.FileOutputStream

class BudgetCalculatorFragment : Fragment() {

    private lateinit var binding: FragmentBudgetCalculatorBinding
    private var isCalculating = false

    // Currency symbols for different regions
    private val currencySymbols = mapOf(
        "USD" to "$",
        "EUR" to "€",
        "GBP" to "£",
        "INR" to "₹",
        "JPY" to "¥",
        "CAD" to "C$",
        "AUD" to "A$",
        "CNY" to "¥",
        "KRW" to "₩"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentBudgetCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        animateView(binding.budgetLayout)
        hideKeyboardFunctionality(view)
        setupSpinners()
        setupListeners()
        setupTextWatchers()
        calculateBudget()
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
            spinnerCurrency.setSelection(0) // Default to USD

            // Setup Budget Period Spinner
            val budgetPeriods = arrayOf("Monthly", "Weekly", "Yearly")
            val periodAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                budgetPeriods
            )
            periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerBudgetPeriod.adapter = periodAdapter
        }
    }

    private fun setupListeners() {
        binding.apply {
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            btnShare.setOnClickListener {
                captureAndShareScreenshot()
            }

            btnReset.setOnClickListener {
                resetBudget()
            }

            btnSavingsGoal.setOnClickListener {
                calculateSavingsGoal()
            }

            // Spinner listeners for auto-calculation
            spinnerCurrency.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    calculateBudget()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })

            spinnerBudgetPeriod.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    calculateBudget()
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
                    calculateBudget()
                }
            }
        }

        binding.apply {
            // Income fields
            etSalary.addTextChangedListener(textWatcher)
            etFreelance.addTextChangedListener(textWatcher)
            etInvestments.addTextChangedListener(textWatcher)
            etOtherIncome.addTextChangedListener(textWatcher)

            // Fixed expenses
            etRent.addTextChangedListener(textWatcher)
            etUtilities.addTextChangedListener(textWatcher)
            etInsurance.addTextChangedListener(textWatcher)
            etSubscriptions.addTextChangedListener(textWatcher)

            // Variable expenses
            etFood.addTextChangedListener(textWatcher)
            etTransportation.addTextChangedListener(textWatcher)
            etEntertainment.addTextChangedListener(textWatcher)
            etShopping.addTextChangedListener(textWatcher)
            etHealthcare.addTextChangedListener(textWatcher)
            etOtherExpenses.addTextChangedListener(textWatcher)

            // Savings and debt
            etSavingsGoal.addTextChangedListener(textWatcher)
            etEmergencyFund.addTextChangedListener(textWatcher)
            etDebtPayments.addTextChangedListener(textWatcher)
        }
    }

    private fun calculateBudget() {
        binding.apply {
            try {
                // Get currency symbol
                val selectedCurrency = spinnerCurrency.selectedItem.toString()
                val currencySymbol = currencySymbols[selectedCurrency] ?: "$"
                val period = spinnerBudgetPeriod.selectedItem.toString()

                // Calculate total income
                val salary = etSalary.text.toString().toDoubleOrNull() ?: 0.0
                val freelance = etFreelance.text.toString().toDoubleOrNull() ?: 0.0
                val investments = etInvestments.text.toString().toDoubleOrNull() ?: 0.0
                val otherIncome = etOtherIncome.text.toString().toDoubleOrNull() ?: 0.0
                val totalIncome = salary + freelance + investments + otherIncome

                // Calculate fixed expenses
                val rent = etRent.text.toString().toDoubleOrNull() ?: 0.0
                val utilities = etUtilities.text.toString().toDoubleOrNull() ?: 0.0
                val insurance = etInsurance.text.toString().toDoubleOrNull() ?: 0.0
                val subscriptions = etSubscriptions.text.toString().toDoubleOrNull() ?: 0.0
                val totalFixedExpenses = rent + utilities + insurance + subscriptions

                // Calculate variable expenses
                val food = etFood.text.toString().toDoubleOrNull() ?: 0.0
                val transportation = etTransportation.text.toString().toDoubleOrNull() ?: 0.0
                val entertainment = etEntertainment.text.toString().toDoubleOrNull() ?: 0.0
                val shopping = etShopping.text.toString().toDoubleOrNull() ?: 0.0
                val healthcare = etHealthcare.text.toString().toDoubleOrNull() ?: 0.0
                val otherExpenses = etOtherExpenses.text.toString().toDoubleOrNull() ?: 0.0
                val totalVariableExpenses = food + transportation + entertainment + shopping + healthcare + otherExpenses

                // Calculate savings and debt
                val savingsGoal = etSavingsGoal.text.toString().toDoubleOrNull() ?: 0.0
                val emergencyFund = etEmergencyFund.text.toString().toDoubleOrNull() ?: 0.0
                val debtPayments = etDebtPayments.text.toString().toDoubleOrNull() ?: 0.0

                // Calculate totals
                val totalExpenses = totalFixedExpenses + totalVariableExpenses
                val totalSavingsAndDebt = savingsGoal + emergencyFund + debtPayments
                val totalOutgoing = totalExpenses + totalSavingsAndDebt
                val remainingMoney = totalIncome - totalOutgoing

                // Update main displays
                tvTotalIncome.text = String.format("%.0f", totalIncome)
                tvTotalExpenses.text = String.format("%.0f", totalExpenses)
                tvRemainingMoney.text = String.format("%.0f", remainingMoney)

                // Update currency symbols
                tvCurrencyIncome.text = currencySymbol
                tvCurrencyExpenses.text = currencySymbol
                tvCurrencyRemaining.text = currencySymbol

                // Update breakdown values
                tvIncomeBreakdown.text = String.format("%.0f", totalIncome)
                tvFixedBreakdown.text = String.format("%.0f", totalFixedExpenses)
                tvVariableBreakdown.text = String.format("%.0f", totalVariableExpenses)
                tvSavingsBreakdown.text = String.format("%.0f", totalSavingsAndDebt)

                // Set color for remaining money (green if positive, red if negative)
                if (remainingMoney >= 0) {
                    tvRemainingMoney.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                    tvBudgetStatus.text = "Budget is healthy!"
                    tvBudgetStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                } else {
                    tvRemainingMoney.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                    tvBudgetStatus.text = "Over budget - review expenses"
                    tvBudgetStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                }

                // Calculate and display percentages
                if (totalIncome > 0) {
                    val expensePercentage = (totalExpenses / totalIncome) * 100
                    val savingsPercentage = (totalSavingsAndDebt / totalIncome) * 100

                    tvExpensePercentage.text = String.format("%.1f%%", expensePercentage)
                    tvSavingsPercentage.text = String.format("%.1f%%", savingsPercentage)
                } else {
                    tvExpensePercentage.text = "0%"
                    tvSavingsPercentage.text = "0%"
                }

                // Update detailed analysis
                updateDetailedAnalysis(totalIncome, totalExpenses, totalSavingsAndDebt, remainingMoney, currencySymbol, period)

            } catch (e: Exception) {
                // Handle calculation errors silently
            }
        }
    }

    private fun updateDetailedAnalysis(
        totalIncome: Double,
        totalExpenses: Double,
        totalSavingsAndDebt: Double,
        remainingMoney: Double,
        currencySymbol: String,
        period: String
    ) {
        binding.apply {
            val result = StringBuilder()
            result.append("Budget Analysis ($period)\n\n")

            result.append("Income Summary:\n")
            result.append("Total Income: $currencySymbol${String.format("%.0f", totalIncome)}\n")

            if (totalIncome > 0) {
                val salary = etSalary.text.toString().toDoubleOrNull() ?: 0.0
                val freelance = etFreelance.text.toString().toDoubleOrNull() ?: 0.0
                val investments = etInvestments.text.toString().toDoubleOrNull() ?: 0.0
                val otherIncome = etOtherIncome.text.toString().toDoubleOrNull() ?: 0.0

                if (salary > 0) result.append("• Salary: ${String.format("%.1f", (salary/totalIncome)*100)}%\n")
                if (freelance > 0) result.append("• Freelance: ${String.format("%.1f", (freelance/totalIncome)*100)}%\n")
                if (investments > 0) result.append("• Investments: ${String.format("%.1f", (investments/totalIncome)*100)}%\n")
                if (otherIncome > 0) result.append("• Other: ${String.format("%.1f", (otherIncome/totalIncome)*100)}%\n")
            }

            result.append("\nExpense Summary:\n")
            result.append("Total Expenses: $currencySymbol${String.format("%.0f", totalExpenses)}\n")

            if (totalIncome > 0) {
                val expenseRatio = (totalExpenses / totalIncome) * 100
                result.append("Expense Ratio: ${String.format("%.1f", expenseRatio)}%\n")

                if (expenseRatio > 80) {
                    result.append("⚠ High expense ratio - consider reducing costs\n")
                } else if (expenseRatio < 50) {
                    result.append("✓ Good expense control\n")
                }
            }

            result.append("\nSavings & Debt:\n")
            result.append("Total: $currencySymbol${String.format("%.0f", totalSavingsAndDebt)}\n")

            if (totalIncome > 0) {
                val savingsRatio = (totalSavingsAndDebt / totalIncome) * 100
                result.append("Savings Rate: ${String.format("%.1f", savingsRatio)}%\n")

                if (savingsRatio >= 20) {
                    result.append("✓ Excellent savings rate!\n")
                } else if (savingsRatio >= 10) {
                    result.append("✓ Good savings rate\n")
                } else if (savingsRatio > 0) {
                    result.append("⚠ Consider increasing savings\n")
                } else {
                    result.append("⚠ No savings allocated\n")
                }
            }

            result.append("\nRecommendations:\n")

            // 50/30/20 rule analysis
            if (totalIncome > 0) {
                val needsTarget = totalIncome * 0.5
                val wantsTarget = totalIncome * 0.3
                val savingsTarget = totalIncome * 0.2

                val fixedExpenses = (etRent.text.toString().toDoubleOrNull() ?: 0.0) +
                        (etUtilities.text.toString().toDoubleOrNull() ?: 0.0) +
                        (etInsurance.text.toString().toDoubleOrNull() ?: 0.0) +
                        (etFood.text.toString().toDoubleOrNull() ?: 0.0) +
                        (etTransportation.text.toString().toDoubleOrNull() ?: 0.0)

                val discretionaryExpenses = (etEntertainment.text.toString().toDoubleOrNull() ?: 0.0) +
                        (etShopping.text.toString().toDoubleOrNull() ?: 0.0)

                result.append("50/30/20 Rule Analysis:\n")
                result.append("• Needs (50%): Target $currencySymbol${String.format("%.0f", needsTarget)}, Actual $currencySymbol${String.format("%.0f", fixedExpenses)}\n")
                result.append("• Wants (30%): Target $currencySymbol${String.format("%.0f", wantsTarget)}, Actual $currencySymbol${String.format("%.0f", discretionaryExpenses)}\n")
                result.append("• Savings (20%): Target $currencySymbol${String.format("%.0f", savingsTarget)}, Actual $currencySymbol${String.format("%.0f", totalSavingsAndDebt)}\n")
            }

            if (remainingMoney < 0) {
                result.append("\nBudget Deficit Actions:\n")
                result.append("• Review and reduce variable expenses\n")
                result.append("• Consider additional income sources\n")
                result.append("• Prioritize essential expenses only\n")
            } else if (remainingMoney > totalIncome * 0.1) {
                result.append("\nSurplus Suggestions:\n")
                result.append("• Increase emergency fund contribution\n")
                result.append("• Consider additional investments\n")
                result.append("• Plan for future large expenses\n")
            }

            // Emergency fund recommendation
            val monthlyExpenses = when (period) {
                "Weekly" -> totalExpenses * 4.33
                "Yearly" -> totalExpenses / 12
                else -> totalExpenses
            }
            val recommendedEmergencyFund = monthlyExpenses * 6
            val currentEmergencyFund = etEmergencyFund.text.toString().toDoubleOrNull() ?: 0.0

            result.append("\nEmergency Fund:\n")
            result.append("Recommended: $currencySymbol${String.format("%.0f", recommendedEmergencyFund)} (6 months expenses)\n")
            result.append("Current: $currencySymbol${String.format("%.0f", currentEmergencyFund)}\n")

            if (currentEmergencyFund < recommendedEmergencyFund) {
                val shortfall = recommendedEmergencyFund - currentEmergencyFund
                result.append("Shortfall: $currencySymbol${String.format("%.0f", shortfall)}\n")
            }

            tvBudgetDetails.text = result.toString()
        }
    }

    private fun calculateSavingsGoal() {
        // This could open a dialog or navigate to a savings goal calculator
        Toast.makeText(requireContext(), "Savings goal calculator - coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun resetBudget() {
        binding.apply {
            // Clear all input fields
            etSalary.text?.clear()
            etFreelance.text?.clear()
            etInvestments.text?.clear()
            etOtherIncome.text?.clear()

            etRent.text?.clear()
            etUtilities.text?.clear()
            etInsurance.text?.clear()
            etSubscriptions.text?.clear()

            etFood.text?.clear()
            etTransportation.text?.clear()
            etEntertainment.text?.clear()
            etShopping.text?.clear()
            etHealthcare.text?.clear()
            etOtherExpenses.text?.clear()

            etSavingsGoal.text?.clear()
            etEmergencyFund.text?.clear()
            etDebtPayments.text?.clear()

            // Reset displays
            tvTotalIncome.text = "0"
            tvTotalExpenses.text = "0"
            tvRemainingMoney.text = "0"
            tvBudgetStatus.text = "Enter your budget details"
            tvBudgetDetails.text = "Complete your income and expense information to see detailed analysis"
        }
        Toast.makeText(requireContext(), "Budget reset", Toast.LENGTH_SHORT).show()
    }

    private fun captureAndShareScreenshot() {
        val screenshotView = binding.budgetResultCard
        val bitmap = Bitmap.createBitmap(
            screenshotView.width,
            screenshotView.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        screenshotView.draw(canvas)

        // Save the bitmap to a file
        val file = File(requireContext().cacheDir, "budget_screenshot.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        // Get a content URI for the file using FileProvider
        val contentUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        // Create share intent
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share Budget Analysis"))
    }

    // Function to hide the keyboard
    private fun hideKeyboard() {
        val inputMethodManager =
            activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    private fun hideKeyboardFunctionality(view: View) {
        view.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val imm =
                    activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                val currentFocusView = activity?.currentFocus
                if (currentFocusView != null) {
                    imm.hideSoftInputFromWindow(currentFocusView.windowToken, 0)
                    currentFocusView.clearFocus()
                }
            }
            false
        }
    }

    private fun animateView(view: View) {
        lifecycleScope.launch(Dispatchers.Main) {
            var glowAnimator: ObjectAnimator? = null
            glowAnimator = view.startGlowAnimation()
            delay(3000)
            view.stopGlowAnimation(glowAnimator)
        }
    }

    companion object {
        private const val TAG = "BudgetCalculatorFragment"
    }
}