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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentBreakEvenAnalysisBinding
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class BreakEvenAnalysisFragment : Fragment() {

    private lateinit var binding: FragmentBreakEvenAnalysisBinding

    // Formatting
    private val currencyFormat = DecimalFormat("#,##0.00")
    private val numberFormat = DecimalFormat("#,##0")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentBreakEvenAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Adjust keyboard behavior
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        setupInputWatchers()
        setupListeners()
    }

    private fun setupInputWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Auto-calculate when all fields are filled
                if (areAllFieldsFilled()) {
                    calculate()
                }
            }
        }

        binding.apply {
            etFixedCosts.addTextChangedListener(textWatcher)
            etVariableCostPerUnit.addTextChangedListener(textWatcher)
            etSellingPricePerUnit.addTextChangedListener(textWatcher)
            etTargetProfit.addTextChangedListener(textWatcher)
        }
    }

    private fun areAllFieldsFilled(): Boolean {
        return binding.etFixedCosts.text.isNotEmpty() &&
                binding.etVariableCostPerUnit.text.isNotEmpty() &&
                binding.etSellingPricePerUnit.text.isNotEmpty()
    }

    private fun setupListeners() {
        binding.apply {
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            btnCalculate.setOnClickListener {
                calculate()
            }

            btnReloadCalculator.setOnClickListener {
                clearAll()
                Toast.makeText(context, "Calculator reloaded", Toast.LENGTH_SHORT).show()
            }

            btnShare.setOnClickListener {
                if (resultsSection.visibility == View.VISIBLE) {
                    shareResults()
                } else {
                    Toast.makeText(context, "Please calculate first", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun calculate() {
        try {
            // Get input values
            val fixedCosts = binding.etFixedCosts.text.toString().toDoubleOrNull()
            val variableCostPerUnit = binding.etVariableCostPerUnit.text.toString().toDoubleOrNull()
            val sellingPricePerUnit = binding.etSellingPricePerUnit.text.toString().toDoubleOrNull()
            val targetProfit = binding.etTargetProfit.text.toString().toDoubleOrNull() ?: 0.0

            // Validate inputs
            if (fixedCosts == null || variableCostPerUnit == null || sellingPricePerUnit == null) {
                Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return
            }

            if (fixedCosts < 0 || variableCostPerUnit < 0 || sellingPricePerUnit < 0) {
                Toast.makeText(context, "Values cannot be negative", Toast.LENGTH_SHORT).show()
                return
            }

            if (sellingPricePerUnit <= variableCostPerUnit) {
                Toast.makeText(
                    context,
                    "Selling price must be greater than variable cost per unit",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            // Calculate contribution margin
            val contributionMargin = sellingPricePerUnit - variableCostPerUnit
            val contributionMarginRatio = (contributionMargin / sellingPricePerUnit) * 100

            // Calculate break-even point in units
            val breakEvenUnits = (fixedCosts + targetProfit) / contributionMargin

            // Calculate break-even point in revenue
            val breakEvenRevenue = breakEvenUnits * sellingPricePerUnit

            // Calculate margin of safety (example: at 1000 units)
            val actualSalesUnits = if (breakEvenUnits > 0) breakEvenUnits * 1.2 else 1000.0
            val marginOfSafety = actualSalesUnits - breakEvenUnits
            val marginOfSafetyPercentage = (marginOfSafety / actualSalesUnits) * 100

            // Display results
            displayResults(
                breakEvenUnits,
                breakEvenRevenue,
                contributionMargin,
                contributionMarginRatio,
                marginOfSafety,
                marginOfSafetyPercentage
            )

            // Show results section
            binding.resultsSection.visibility = View.VISIBLE

        } catch (e: Exception) {
            Toast.makeText(context, "Error in calculation: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayResults(
        breakEvenUnits: Double,
        breakEvenRevenue: Double,
        contributionMargin: Double,
        contributionMarginRatio: Double,
        marginOfSafety: Double,
        marginOfSafetyPercentage: Double
    ) {
        binding.apply {
            // Break-even results
            tvBreakEvenUnits.text = numberFormat.format(breakEvenUnits)
            tvBreakEvenRevenue.text = "${currencyFormat.format(breakEvenRevenue)}"

            // Contribution margin
            tvContributionMargin.text = "${currencyFormat.format(contributionMargin)}"
            tvContributionMarginRatio.text = "${String.format("%.2f", contributionMarginRatio)}%"

            // Generate detailed analysis
            generateDetailedAnalysis(
                breakEvenUnits,
                breakEvenRevenue,
                contributionMargin,
                contributionMarginRatio
            )
        }
    }

    private fun generateDetailedAnalysis(
        breakEvenUnits: Double,
        breakEvenRevenue: Double,
        contributionMargin: Double,
        contributionMarginRatio: Double
    ) {
        val fixedCosts = binding.etFixedCosts.text.toString().toDouble()
        val variableCostPerUnit = binding.etVariableCostPerUnit.text.toString().toDouble()
        val sellingPricePerUnit = binding.etSellingPricePerUnit.text.toString().toDouble()
        val targetProfit = binding.etTargetProfit.text.toString().toDoubleOrNull() ?: 0.0

        val analysis = StringBuilder()

        analysis.append("📊 BREAK-EVEN ANALYSIS\n\n")

        // Basic Information
        analysis.append("💼 Business Parameters:\n")
        analysis.append("• Fixed Costs: ${currencyFormat.format(fixedCosts)}\n")
        analysis.append("• Variable Cost/Unit: ${currencyFormat.format(variableCostPerUnit)}\n")
        analysis.append("• Selling Price/Unit: ${currencyFormat.format(sellingPricePerUnit)}\n")
        if (targetProfit > 0) {
            analysis.append("• Target Profit: ${currencyFormat.format(targetProfit)}\n")
        }
        analysis.append("\n")

        // Break-Even Point
        analysis.append("🎯 Break-Even Point:\n")
        analysis.append("• Units to Sell: ${numberFormat.format(breakEvenUnits)} units\n")
        analysis.append("• Revenue Needed: ${currencyFormat.format(breakEvenRevenue)}\n")
        analysis.append("• Total Costs at BEP: ${currencyFormat.format(breakEvenRevenue)}\n")
        analysis.append("\n")

        // Contribution Analysis
        analysis.append("💰 Contribution Analysis:\n")
        analysis.append("• Contribution/Unit: ${currencyFormat.format(contributionMargin)}\n")
        analysis.append("• Contribution Margin Ratio: ${String.format("%.2f", contributionMarginRatio)}%\n")
        analysis.append("• Meaning: Each unit sold contributes ${currencyFormat.format(contributionMargin)} towards fixed costs and profit\n")
        analysis.append("\n")

        // Profitability at Different Volumes
        analysis.append("📈 Profitability at Different Sales Volumes:\n\n")

        val volumes = listOf(
            breakEvenUnits * 0.5,
            breakEvenUnits * 0.75,
            breakEvenUnits,
            breakEvenUnits * 1.25,
            breakEvenUnits * 1.5,
            breakEvenUnits * 2.0
        )

        for (volume in volumes) {
            val revenue = volume * sellingPricePerUnit
            val totalVariableCost = volume * variableCostPerUnit
            val totalCost = fixedCosts + totalVariableCost
            val profit = revenue - totalCost
            val profitMargin = (profit / revenue) * 100

            analysis.append("At ${numberFormat.format(volume)} units:\n")
            analysis.append("  Revenue: ${currencyFormat.format(revenue)}\n")
            analysis.append("  Profit/Loss: ${currencyFormat.format(profit)}\n")
            analysis.append("  Profit Margin: ${String.format("%.2f", profitMargin)}%\n\n")
        }

        // Key Insights
        analysis.append("💡 Key Insights:\n")

        if (contributionMarginRatio >= 40) {
            analysis.append("• Excellent contribution margin - Strong pricing power\n")
        } else if (contributionMarginRatio >= 25) {
            analysis.append("• Good contribution margin - Healthy business model\n")
        } else {
            analysis.append("• Low contribution margin - Consider cost reduction or price increase\n")
        }

        val unitsFor100kProfit = (fixedCosts + 100000) / contributionMargin
        analysis.append("• To earn 1,00,000 profit: Sell ${numberFormat.format(unitsFor100kProfit)} units\n")

        val unitsFor20PercentMargin = fixedCosts / (contributionMargin * 0.8)
        analysis.append("• For 20% profit margin: Sell ${numberFormat.format(unitsFor20PercentMargin)} units\n")
        analysis.append("\n")

        // Business Recommendations
        analysis.append("🎓 Business Recommendations:\n")

        if (breakEvenUnits > 10000) {
            analysis.append("• High break-even point - Consider:\n")
            analysis.append("  - Reducing fixed costs\n")
            analysis.append("  - Increasing selling price\n")
            analysis.append("  - Improving operational efficiency\n")
        } else {
            analysis.append("• Manageable break-even point\n")
            analysis.append("• Focus on scaling sales volume\n")
        }

        analysis.append("• Monitor contribution margin regularly\n")
        analysis.append("• Track actual vs break-even performance\n")
        analysis.append("• Review pricing strategy periodically\n")
        analysis.append("\n")

        // Cost Structure Analysis
        val totalCostAtBEP = fixedCosts + (breakEvenUnits * variableCostPerUnit)
        val fixedCostPercentage = (fixedCosts / totalCostAtBEP) * 100
        val variableCostPercentage = 100 - fixedCostPercentage

        analysis.append("📊 Cost Structure at Break-Even:\n")
        analysis.append("• Fixed Costs: ${String.format("%.1f", fixedCostPercentage)}%\n")
        analysis.append("• Variable Costs: ${String.format("%.1f", variableCostPercentage)}%\n")

        if (fixedCostPercentage > 60) {
            analysis.append("• High fixed cost structure - High operating leverage\n")
            analysis.append("• Small sales increases lead to large profit increases\n")
        } else {
            analysis.append("• Balanced cost structure\n")
        }

        binding.tvDetailedAnalysis.text = analysis.toString()
    }

    private fun shareResults() {
        val options = arrayOf("Share as Text", "Share as Image")
        AlertDialog.Builder(requireContext())
            .setTitle("Share Break-Even Analysis")
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
        val breakEvenUnits = binding.tvBreakEvenUnits.text.toString()
        val breakEvenRevenue = binding.tvBreakEvenRevenue.text.toString()
        val contributionMargin = binding.tvContributionMargin.text.toString()
        val contributionMarginRatio = binding.tvContributionMarginRatio.text.toString()

        val shareText = buildString {
            append("📊 Break-Even Analysis Results\n\n")

            append("🎯 Break-Even Point:\n")
            append("Units: $breakEvenUnits\n")
            append("Revenue: $breakEvenRevenue\n\n")

            append("💰 Contribution Analysis:\n")
            append("Per Unit: $contributionMargin\n")
            append("Margin Ratio: $contributionMarginRatio\n\n")

            append("Input Parameters:\n")
            append("Fixed Costs: ${binding.etFixedCosts.text}\n")
            append("Variable Cost/Unit: ${binding.etVariableCostPerUnit.text}\n")
            append("Selling Price/Unit: ${binding.etSellingPricePerUnit.text}\n")

            val targetProfit = binding.etTargetProfit.text.toString()
            if (targetProfit.isNotEmpty() && targetProfit.toDoubleOrNull() != null && targetProfit.toDouble() > 0) {
                append("Target Profit: $targetProfit\n")
            }

            append("\nCalculated with All In One Calculator")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Break-Even Analysis")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    private fun captureAndShareScreenshot() {
        try {
            val scrollView = binding.scrollView
            val bitmap = Bitmap.createBitmap(
                scrollView.width,
                scrollView.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            scrollView.draw(canvas)

            val cachePath = File(requireContext().cacheDir, "images")
            cachePath.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(cachePath, "break_even_analysis_$timestamp.png")

            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

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

            startActivity(Intent.createChooser(shareIntent, "Share via"))

        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing screenshot: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearAll() {
        binding.apply {
            etFixedCosts.text?.clear()
            etVariableCostPerUnit.text?.clear()
            etSellingPricePerUnit.text?.clear()
            etTargetProfit.text?.clear()

            tvBreakEvenUnits.text = "0"
            tvBreakEvenRevenue.text = "0.00"
            tvContributionMargin.text = "0.00"
            tvContributionMarginRatio.text = "0%"
            tvDetailedAnalysis.text = ""

            resultsSection.visibility = View.GONE
        }
    }
}