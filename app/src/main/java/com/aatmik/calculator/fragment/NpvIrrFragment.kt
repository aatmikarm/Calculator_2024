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
import androidx.recyclerview.widget.LinearLayoutManager
import com.aatmik.calculator.adapter.CashFlowAdapter
import com.aatmik.calculator.databinding.FragmentNpvIrrBinding
import com.aatmik.calculator.model.CashFlow
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.pow

class NpvIrrFragment : Fragment() {

    private lateinit var binding: FragmentNpvIrrBinding
    private val cashFlows = mutableListOf<CashFlow>()
    private lateinit var cashFlowAdapter: CashFlowAdapter

    private val currencyFormat = DecimalFormat("#,##0.00")
    private val percentFormat = DecimalFormat("0.00")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentNpvIrrBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        setupRecyclerView()
        setupListeners()
        addDefaultCashFlows()
    }

    private fun setupRecyclerView() {
        cashFlowAdapter = CashFlowAdapter(
            cashFlows,
            onDelete = { position ->
                if (cashFlows.size > 1) {
                    cashFlows.removeAt(position)
                    cashFlowAdapter.notifyItemRemoved(position)
                    updateYearNumbers()
                } else {
                    Toast.makeText(context, "At least one cash flow required", Toast.LENGTH_SHORT).show()
                }
            },
            onUpdate = { position, amount ->
                cashFlows[position].amount = amount
            }
        )

        binding.cashFlowRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = cashFlowAdapter
        }
    }

    private fun setupListeners() {
        binding.apply {
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            btnAddCashFlow.setOnClickListener {
                addCashFlow()
            }

            btnCalculate.setOnClickListener {
                calculate()
            }

            btnReloadCalculator.setOnClickListener {
                reloadCalculator()
            }

            btnShare.setOnClickListener {
                if (resultsSection.visibility == View.VISIBLE) {
                    shareResults()
                } else {
                    Toast.makeText(context, "Please calculate first", Toast.LENGTH_SHORT).show()
                }
            }

            etDiscountRate.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
    }

    private fun addDefaultCashFlows() {
        // Add initial investment (Year 0)
        cashFlows.add(CashFlow(0, -100000.0))
        // Add 5 years of positive cash flows
        for (i in 1..5) {
            cashFlows.add(CashFlow(i, 30000.0))
        }
        cashFlowAdapter.notifyDataSetChanged()
    }

    private fun addCashFlow() {
        val nextYear = if (cashFlows.isEmpty()) 0 else cashFlows.last().year + 1
        cashFlows.add(CashFlow(nextYear, 0.0))
        cashFlowAdapter.notifyItemInserted(cashFlows.size - 1)
        binding.cashFlowRecyclerView.smoothScrollToPosition(cashFlows.size - 1)
    }

    private fun updateYearNumbers() {
        cashFlows.forEachIndexed { index, cashFlow ->
            cashFlow.year = index
        }
        cashFlowAdapter.notifyDataSetChanged()
    }

    private fun calculate() {
        try {
            val discountRateText = binding.etDiscountRate.text.toString()

            if (discountRateText.isEmpty()) {
                Toast.makeText(context, "Please enter discount rate", Toast.LENGTH_SHORT).show()
                return
            }

            val discountRate = discountRateText.toDouble() / 100.0

            if (discountRate < 0) {
                Toast.makeText(context, "Discount rate cannot be negative", Toast.LENGTH_SHORT).show()
                return
            }

            if (cashFlows.isEmpty()) {
                Toast.makeText(context, "Please add cash flows", Toast.LENGTH_SHORT).show()
                return
            }

            // Calculate NPV
            val npv = calculateNPV(cashFlows, discountRate)

            // Calculate IRR
            val irr = calculateIRR(cashFlows)

            // Calculate Payback Period
            val paybackPeriod = calculatePaybackPeriod(cashFlows)

            // Calculate Profitability Index
            val profitabilityIndex = calculateProfitabilityIndex(cashFlows, discountRate)

            // Display results
            displayResults(npv, irr, paybackPeriod, profitabilityIndex, discountRate)

            binding.resultsSection.visibility = View.VISIBLE

        } catch (e: NumberFormatException) {
            Toast.makeText(context, "Please enter valid numbers", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateNPV(cashFlows: List<CashFlow>, discountRate: Double): Double {
        var npv = 0.0
        cashFlows.forEach { cf ->
            npv += cf.amount / (1 + discountRate).pow(cf.year.toDouble())
        }
        return npv
    }

    private fun calculateIRR(cashFlows: List<CashFlow>): Double? {
        // Newton-Raphson method to find IRR
        var irr = 0.1 // Initial guess
        val maxIterations = 1000
        val tolerance = 0.00001

        for (iteration in 0 until maxIterations) {
            var npv = 0.0
            var derivative = 0.0

            cashFlows.forEach { cf ->
                val year = cf.year.toDouble()
                val discountFactor = (1 + irr).pow(year)
                npv += cf.amount / discountFactor
                derivative -= year * cf.amount / ((1 + irr) * discountFactor)
            }

            if (abs(npv) < tolerance) {
                return irr * 100 // Return as percentage
            }

            if (abs(derivative) < tolerance) {
                return null // Cannot converge
            }

            irr = irr - npv / derivative

            // Check for unreasonable values
            if (irr < -0.99 || irr > 10.0) {
                return null
            }
        }

        return null // Did not converge
    }

    private fun calculatePaybackPeriod(cashFlows: List<CashFlow>): Double? {
        if (cashFlows.isEmpty()) return null

        var cumulativeCashFlow = 0.0
        var previousCumulativeCashFlow = 0.0

        for (cf in cashFlows) {
            previousCumulativeCashFlow = cumulativeCashFlow
            cumulativeCashFlow += cf.amount

            if (cumulativeCashFlow >= 0 && previousCumulativeCashFlow < 0) {
                // Payback occurs in this period
                val fraction = abs(previousCumulativeCashFlow) / cf.amount
                return cf.year - 1 + fraction
            }
        }

        return null // Payback period not achieved
    }

    private fun calculateProfitabilityIndex(cashFlows: List<CashFlow>, discountRate: Double): Double? {
        if (cashFlows.isEmpty() || cashFlows[0].amount >= 0) return null

        val initialInvestment = abs(cashFlows[0].amount)
        val presentValueOfFutureCashFlows = cashFlows.drop(1).sumOf { cf ->
            cf.amount / (1 + discountRate).pow(cf.year.toDouble())
        }

        return presentValueOfFutureCashFlows / initialInvestment
    }

    private fun displayResults(
        npv: Double,
        irr: Double?,
        paybackPeriod: Double?,
        profitabilityIndex: Double?,
        discountRate: Double
    ) {
        binding.apply {
            // NPV
            tvNpvValue.text = "${currencyFormat.format(npv)}"
            tvNpvValue.setTextColor(
                resources.getColor(
                    if (npv >= 0) android.R.color.holo_green_dark
                    else android.R.color.holo_red_dark,
                    null
                )
            )

            // IRR
            if (irr != null) {
                tvIrrValue.text = "${percentFormat.format(irr)}%"
                tvIrrValue.setTextColor(
                    resources.getColor(
                        if (irr > discountRate * 100) android.R.color.holo_green_dark
                        else android.R.color.holo_red_dark,
                        null
                    )
                )
            } else {
                tvIrrValue.text = "N/A"
                tvIrrValue.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            }

            // Payback Period
            if (paybackPeriod != null) {
                tvPaybackValue.text = "${String.format("%.2f", paybackPeriod)} years"
            } else {
                tvPaybackValue.text = "Not achieved"
            }

            // Profitability Index
            if (profitabilityIndex != null) {
                tvProfitabilityIndexValue.text = String.format("%.2f", profitabilityIndex)
            } else {
                tvProfitabilityIndexValue.text = "N/A"
            }

            // Generate detailed analysis
            generateDetailedAnalysis(npv, irr, paybackPeriod, profitabilityIndex, discountRate)
        }
    }

    private fun generateDetailedAnalysis(
        npv: Double,
        irr: Double?,
        paybackPeriod: Double?,
        profitabilityIndex: Double?,
        discountRate: Double
    ) {
        val analysis = StringBuilder()

        analysis.append("📊 INVESTMENT ANALYSIS\n\n")

        // Project Summary
        analysis.append("💼 Project Summary:\n")
        val initialInvestment = cashFlows.firstOrNull()?.amount ?: 0.0
        analysis.append("• Initial Investment: ${currencyFormat.format(abs(initialInvestment))}\n")
        analysis.append("• Project Duration: ${cashFlows.size - 1} years\n")
        analysis.append("• Discount Rate: ${percentFormat.format(discountRate * 100)}%\n")

        val totalCashInflow = cashFlows.drop(1).sumOf { it.amount }
        analysis.append("• Total Cash Inflows: ${currencyFormat.format(totalCashInflow)}\n\n")

        // NPV Analysis
        analysis.append("💰 Net Present Value (NPV):\n")
        analysis.append("• NPV: ${currencyFormat.format(npv)}\n")
        when {
            npv > 0 -> {
                analysis.append("• Decision: ✅ ACCEPT PROJECT\n")
                analysis.append("• Reason: Project creates value of ${currencyFormat.format(npv)}\n")
            }
            npv < 0 -> {
                analysis.append("• Decision: ❌ REJECT PROJECT\n")
                analysis.append("• Reason: Project destroys value of ${currencyFormat.format(abs(npv))}\n")
            }
            else -> {
                analysis.append("• Decision: ⚠️ INDIFFERENT\n")
                analysis.append("• Reason: Project breaks even at current discount rate\n")
            }
        }
        analysis.append("\n")

        // IRR Analysis
        analysis.append("📈 Internal Rate of Return (IRR):\n")
        if (irr != null) {
            analysis.append("• IRR: ${percentFormat.format(irr)}%\n")
            analysis.append("• Required Rate: ${percentFormat.format(discountRate * 100)}%\n")

            if (irr > discountRate * 100) {
                analysis.append("• Decision: ✅ ACCEPT PROJECT\n")
                analysis.append("• Reason: IRR exceeds discount rate by ${percentFormat.format(irr - discountRate * 100)}%\n")
            } else if (irr < discountRate * 100) {
                analysis.append("• Decision: ❌ REJECT PROJECT\n")
                analysis.append("• Reason: IRR falls short of discount rate by ${percentFormat.format(discountRate * 100 - irr)}%\n")
            } else {
                analysis.append("• Decision: ⚠️ INDIFFERENT\n")
                analysis.append("• Reason: IRR equals discount rate\n")
            }
        } else {
            analysis.append("• IRR: Cannot be calculated\n")
            analysis.append("• Note: Unusual cash flow pattern\n")
        }
        analysis.append("\n")

        // Payback Period Analysis
        analysis.append("⏱️ Payback Period:\n")
        if (paybackPeriod != null) {
            analysis.append("• Payback: ${String.format("%.2f", paybackPeriod)} years\n")
            val years = paybackPeriod.toInt()
            val months = ((paybackPeriod - years) * 12).toInt()
            analysis.append("• Time: $years years and $months months\n")
            if (paybackPeriod <= 3) {
                analysis.append("• Assessment: ✅ Quick payback (< 3 years)\n")
            } else if (paybackPeriod <= 5) {
                analysis.append("• Assessment: ⚠️ Moderate payback (3-5 years)\n")
            } else {
                analysis.append("• Assessment: ❌ Slow payback (> 5 years)\n")
            }
        } else {
            analysis.append("• Payback: Not achieved within project life\n")
            analysis.append("• Assessment: ❌ Investment not recovered\n")
        }
        analysis.append("\n")

        // Profitability Index
        if (profitabilityIndex != null) {
            analysis.append("📊 Profitability Index:\n")
            analysis.append("• PI: ${String.format("%.2f", profitabilityIndex)}\n")
            when {
                profitabilityIndex > 1 -> {
                    analysis.append("• Decision: ✅ ACCEPT PROJECT\n")
                    analysis.append("• Reason: Creates ${String.format("%.2f", profitabilityIndex)} per 1 invested\n")
                }
                profitabilityIndex < 1 -> {
                    analysis.append("• Decision: ❌ REJECT PROJECT\n")
                    analysis.append("• Reason: Returns only ${String.format("%.2f", profitabilityIndex)} per 1 invested\n")
                }
                else -> {
                    analysis.append("• Decision: ⚠️ INDIFFERENT\n")
                    analysis.append("• Reason: Breaks even (1 per 1 invested)\n")
                }
            }
            analysis.append("\n")
        }

        // Year-by-year breakdown
        analysis.append("📅 Cash Flow Breakdown:\n")
        var cumulativeCashFlow = 0.0
        cashFlows.forEach { cf ->
            cumulativeCashFlow += cf.amount
            val pv = cf.amount / (1 + discountRate).pow(cf.year.toDouble())
            analysis.append("Year ${cf.year}: ${currencyFormat.format(cf.amount)} " +
                    "(PV: ${currencyFormat.format(pv)}, Cumulative: ${currencyFormat.format(cumulativeCashFlow)})\n")
        }
        analysis.append("\n")

        // Final Recommendation
        analysis.append("🎯 Final Recommendation:\n")
        val acceptCount = listOf(npv > 0, irr?.let { it > discountRate * 100 } ?: false, profitabilityIndex?.let { it > 1 } ?: false).count { it }

        when {
            acceptCount >= 2 -> {
                analysis.append("✅ STRONGLY ACCEPT\n")
                analysis.append("Multiple metrics indicate this is a profitable investment.\n")
            }
            acceptCount == 1 -> {
                analysis.append("⚠️ CAUTIOUSLY CONSIDER\n")
                analysis.append("Mixed signals - conduct further analysis.\n")
            }
            else -> {
                analysis.append("❌ REJECT\n")
                analysis.append("Metrics suggest this investment will not create value.\n")
            }
        }

        binding.tvDetailedAnalysis.text = analysis.toString()
    }

    private fun shareResults() {
        val options = arrayOf("Share as Text", "Share as Image")
        AlertDialog.Builder(requireContext())
            .setTitle("Share Investment Analysis")
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
        val npv = binding.tvNpvValue.text.toString()
        val irr = binding.tvIrrValue.text.toString()
        val payback = binding.tvPaybackValue.text.toString()
        val pi = binding.tvProfitabilityIndexValue.text.toString()

        val shareText = buildString {
            append("📊 NPV & IRR Analysis Results\n\n")
            append("Investment Metrics:\n")
            append("• NPV: $npv\n")
            append("• IRR: $irr\n")
            append("• Payback Period: $payback\n")
            append("• Profitability Index: $pi\n\n")
            append("Discount Rate: ${binding.etDiscountRate.text}%\n")
            append("Project Duration: ${cashFlows.size - 1} years\n\n")
            append("Calculated with All In One Calculator")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Investment Analysis")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    private fun captureAndShareScreenshot() {
        try {
            val scrollView = binding.scrollView
            val bitmap = Bitmap.createBitmap(scrollView.width, scrollView.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            scrollView.draw(canvas)

            val cachePath = File(requireContext().cacheDir, "images")
            cachePath.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(cachePath, "npv_irr_analysis_$timestamp.png")

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
            Toast.makeText(context, "Error sharing: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun reloadCalculator() {
        cashFlows.clear()
        addDefaultCashFlows()
        binding.etDiscountRate.text?.clear()
        binding.resultsSection.visibility = View.GONE
        Toast.makeText(context, "Calculator reloaded", Toast.LENGTH_SHORT).show()
    }
}