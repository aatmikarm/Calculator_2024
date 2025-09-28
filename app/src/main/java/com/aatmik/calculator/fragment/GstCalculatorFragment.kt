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
import com.aatmik.calculator.databinding.FragmentGstCalculatorBinding
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

class GstCalculatorFragment : Fragment() {

    private lateinit var binding: FragmentGstCalculatorBinding
    private var isCalculating = false

    // GST Calculation Types
    private val calculationTypes = arrayOf(
        "Add GST to Price",
        "Remove GST from Price",
        "Find Price Before GST",
        "Compare Old vs New Rates"
    )

    // Updated GST Rates as per September 22, 2025 reforms
    private val gstRates = arrayOf(
        "0% (Exempt)",
        "0.25% (Gems & Jewelry)",
        "3% (Gold & Silver)",
        "5% (Essentials & Daily Use)",
        "18% (Standard Rate)",
        "40% (Luxury & Sin Goods)"
    )

    // Category-wise GST rates for easy selection
    private val gstCategories = mapOf(
        "Essential Items (0%)" to listOf(
            "Fresh fruits & vegetables", "Milk & dairy (fresh)", "Bread & cereals (unbranded)",
            "Salt", "Jaggery", "Honey", "Life & health insurance premiums",
            "Educational books", "Newspapers", "Handloom products"
        ),

        "Daily Essentials (5%)" to listOf(
            "Packaged food items", "Instant noodles & pasta", "Chocolates & confectionery",
            "Branded cereals", "Dairy products (packaged)", "Tea & coffee", "Spices & condiments",
            "Footwear under ₹1000", "Textiles & garments", "Medicines & healthcare",
            "Soaps & shampoos", "Toothpaste & oral care", "Bicycles", "Small tools",
            "Hotel stays up to ₹7500/day", "Gym & salon services", "Handicrafts",
            "Agricultural equipment", "Tractors", "Bio-pesticides", "Medical devices",
            "Spectacles", "Exercise books & stationery"
        ),

        "Standard Items (18%)" to listOf(
            "Electronics (TVs, ACs, Laptops)", "Mobile phones", "Home appliances",
            "Small cars (petrol <1200cc, diesel <1500cc)", "Two-wheelers ≤350cc",
            "Auto parts & accessories", "Buses & trucks", "Three-wheelers",
            "Cement", "Steel & construction materials", "Furniture", "Bags & luggage",
            "Restaurant services", "Telecom services", "Banking & financial services",
            "Professional services", "IT services", "Transportation services",
            "Footwear above ₹1000", "Branded garments above ₹1000", "Cosmetics",
            "Hair oil", "Books (non-educational)", "Computer software"
        ),

        "Luxury & Sin Goods (40%)" to listOf(
            "Premium cars & SUVs", "Motorcycles >350cc", "Luxury watches",
            "High-end electronics", "Yachts & boats", "Private aircraft",
            "Aerated drinks & sodas", "Energy drinks", "Caffeinated beverages",
            "Pan masala & gutkha", "Cigarettes & tobacco*", "Casino services",
            "Race club betting", "Online gaming", "Premium event tickets",
            "Luxury hotels", "Premium restaurant services"
        ),

        "Special Rates" to listOf(
            "Gold jewelry (3%)", "Silver jewelry (3%)", "Cut & polished diamonds (0.25%)",
            "Rough diamonds (0.25%)", "Precious stones (0.25%)"
        )
    )

    // Sample items with their GST rates for quick calculation
    private val quickCalculationItems = mapOf(
        "Mobile Phone" to 18.0,
        "Laptop" to 18.0,
        "Smart TV" to 18.0,
        "Air Conditioner" to 18.0,
        "Refrigerator" to 18.0,
        "Washing Machine" to 18.0,
        "Car (Small)" to 18.0,
        "Motorcycle (≤350cc)" to 18.0,
        "Motorcycle (>350cc)" to 40.0,
        "Premium Car" to 40.0,
        "Gold Jewelry" to 3.0,
        "Medicines" to 5.0,
        "Packaged Food" to 5.0,
        "Toothpaste" to 5.0,
        "Soap" to 5.0,
        "Chocolate" to 5.0,
        "Instant Noodles" to 5.0,
        "Restaurant Service" to 18.0,
        "Hotel Stay (Budget)" to 5.0,
        "Aerated Drinks" to 40.0,
        "Cigarettes" to 40.0,
        "Life Insurance" to 0.0,
        "Health Insurance" to 0.0,
        "Fresh Vegetables" to 0.0,
        "Fresh Milk" to 0.0
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentGstCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        setupSpinners()
        setupListeners()
        setupTextWatchers()
        setupQuickCalculation()
        calculateGst()
    }

    private fun setupSpinners() {
        binding.apply {
            // Setup Calculation Type Spinner
            val calculationAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                calculationTypes
            )
            calculationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCalculationType.adapter = calculationAdapter

            // Setup GST Rate Spinner
            val rateAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                gstRates
            )
            rateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerGstRate.adapter = rateAdapter

            // Set default to 18% (most common)
            spinnerGstRate.setSelection(4)

            // Setup Quick Calculation Spinner
            val quickItems = quickCalculationItems.keys.toTypedArray()
            val quickAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                quickItems
            )
            quickAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerQuickItems.adapter = quickAdapter
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
                shareGstCalculation()
            }

            // Calculation type listener
            spinnerCalculationType.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    updateInputLabels()
                    calculateGst()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })

            // GST rate listener
            spinnerGstRate.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    calculateGst()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })

            // Quick calculation item listener
            spinnerQuickItems.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    updateQuickCalculation()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })

            // Category info button
            btnCategoryInfo.setOnClickListener {
                showCategoryInfo()
            }

            // Clear button
            btnClear.setOnClickListener {
                clearAllFields()
            }
        }
    }

    private fun setupQuickCalculation() {
        binding.layoutQuickCalculation.visibility = View.VISIBLE
        updateQuickCalculation()
    }

    private fun updateQuickCalculation() {
        val selectedItem = binding.spinnerQuickItems.selectedItem.toString()
        val gstRate = quickCalculationItems[selectedItem] ?: 18.0

        // Update GST rate spinner to match
        val rateIndex = when {
            gstRate == 0.0 -> 0
            gstRate == 0.25 -> 1
            gstRate == 3.0 -> 2
            gstRate == 5.0 -> 3
            gstRate == 18.0 -> 4
            gstRate == 40.0 -> 5
            else -> 4
        }
        binding.spinnerGstRate.setSelection(rateIndex)

        // Show rate change info if applicable
        showRateChangeInfo(selectedItem, gstRate)
    }

    private fun showRateChangeInfo(itemName: String, newRate: Double) {
        val rateChangeInfo = when (itemName) {
            "Smart TV", "Air Conditioner", "Refrigerator", "Washing Machine" ->
                "Rate reduced from 28% to 18% (Sept 2025 reform)"
            "Car (Small)", "Motorcycle (≤350cc)" ->
                "Rate reduced from 28% to 18% (Sept 2025 reform)"
            "Medicines", "Packaged Food", "Toothpaste", "Soap" ->
                "Rate reduced from 12% to 5% (Sept 2025 reform)"
            "Hotel Stay (Budget)" ->
                "Rate reduced from 12% to 5% for stays up to ₹7500/day"
            "Life Insurance", "Health Insurance" ->
                "Made exempt (0%) from Sept 2025"
            "Aerated Drinks", "Cigarettes" ->
                "Rate increased from 28% to 40% (new luxury/sin rate)"
            else -> null
        }

        if (rateChangeInfo != null) {
            binding.tvRateChangeInfo.text = "📢 $rateChangeInfo"
            binding.tvRateChangeInfo.visibility = View.VISIBLE
        } else {
            binding.tvRateChangeInfo.visibility = View.GONE
        }
    }

    private fun updateInputLabels() {
        val calculationType = binding.spinnerCalculationType.selectedItem.toString()

        binding.apply {
            when (calculationType) {
                "Add GST to Price" -> {
                    tvBaseAmountLabel.text = "Base Price (Excluding GST)"
                    etBaseAmount.hint = "Enter price without GST"
                }
                "Remove GST from Price" -> {
                    tvBaseAmountLabel.text = "Total Price (Including GST)"
                    etBaseAmount.hint = "Enter price with GST included"
                }
                "Find Price Before GST" -> {
                    tvBaseAmountLabel.text = "Final Price (After GST)"
                    etBaseAmount.hint = "Enter final selling price"
                }
                "Compare Old vs New Rates" -> {
                    tvBaseAmountLabel.text = "Base Price"
                    etBaseAmount.hint = "Enter base price for comparison"
                }
            }
        }
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isCalculating) {
                    calculateGst()
                }
            }
        }

        binding.apply {
            etBaseAmount.addTextChangedListener(textWatcher)
            etQuantity.addTextChangedListener(textWatcher)
        }
    }

    private fun calculateGst() {
        binding.apply {
            try {
                val baseAmount = etBaseAmount.text.toString().toDoubleOrNull() ?: 0.0
                val quantity = etQuantity.text.toString().toDoubleOrNull() ?: 1.0
                val calculationType = spinnerCalculationType.selectedItem.toString()
                val gstRateStr = spinnerGstRate.selectedItem.toString()

                val gstRate = extractGstRateFromString(gstRateStr)

                val totalBaseAmount = baseAmount * quantity

                val result = when (calculationType) {
                    "Add GST to Price" -> addGstToPrice(totalBaseAmount, gstRate)
                    "Remove GST from Price" -> removeGstFromPrice(totalBaseAmount, gstRate)
                    "Find Price Before GST" -> findPriceBeforeGst(totalBaseAmount, gstRate)
                    "Compare Old vs New Rates" -> compareRates(totalBaseAmount, gstRate)
                    else -> GstCalculationResult()
                }

                updateResults(result, calculationType, gstRate)

            } catch (e: Exception) {
                // Handle calculation errors silently
            }
        }
    }

    private fun extractGstRateFromString(gstRateStr: String): Double {
        return when {
            gstRateStr.contains("0%") && gstRateStr.contains("Exempt") -> 0.0
            gstRateStr.contains("0.25%") -> 0.25
            gstRateStr.contains("3%") -> 3.0
            gstRateStr.contains("5%") -> 5.0
            gstRateStr.contains("18%") -> 18.0
            gstRateStr.contains("40%") -> 40.0
            else -> 18.0
        }
    }

    private fun addGstToPrice(basePrice: Double, gstRate: Double): GstCalculationResult {
        val gstAmount = (basePrice * gstRate) / 100
        val totalPrice = basePrice + gstAmount

        return GstCalculationResult(
            basePrice = basePrice,
            gstAmount = gstAmount,
            totalPrice = totalPrice,
            gstRate = gstRate
        )
    }

    private fun removeGstFromPrice(totalPrice: Double, gstRate: Double): GstCalculationResult {
        val basePrice = totalPrice / (1 + gstRate / 100)
        val gstAmount = totalPrice - basePrice

        return GstCalculationResult(
            basePrice = basePrice,
            gstAmount = gstAmount,
            totalPrice = totalPrice,
            gstRate = gstRate
        )
    }

    private fun findPriceBeforeGst(finalPrice: Double, gstRate: Double): GstCalculationResult {
        val basePrice = finalPrice / (1 + gstRate / 100)
        val gstAmount = finalPrice - basePrice

        return GstCalculationResult(
            basePrice = basePrice,
            gstAmount = gstAmount,
            totalPrice = finalPrice,
            gstRate = gstRate
        )
    }

    private fun compareRates(basePrice: Double, newGstRate: Double): GstCalculationResult {
        // Compare with old rates (assuming 28% for items now at 18%, 12% for items now at 5%)
        val oldRate = when {
            newGstRate == 18.0 -> 28.0
            newGstRate == 5.0 -> 12.0
            newGstRate == 0.0 -> 18.0 // For insurance
            else -> newGstRate
        }

        val oldGstAmount = (basePrice * oldRate) / 100
        val oldTotalPrice = basePrice + oldGstAmount

        val newGstAmount = (basePrice * newGstRate) / 100
        val newTotalPrice = basePrice + newGstAmount

        val savings = oldTotalPrice - newTotalPrice
        val savingsPercentage = if (oldTotalPrice > 0) (savings / oldTotalPrice) * 100 else 0.0

        return GstCalculationResult(
            basePrice = basePrice,
            gstAmount = newGstAmount,
            totalPrice = newTotalPrice,
            gstRate = newGstRate,
            oldGstAmount = oldGstAmount,
            oldTotalPrice = oldTotalPrice,
            savings = savings,
            savingsPercentage = savingsPercentage,
            oldGstRate = oldRate
        )
    }

    private fun updateResults(
        result: GstCalculationResult,
        calculationType: String,
        gstRate: Double
    ) {
        binding.apply {
            // Main results
            tvBasePrice.text = String.format("₹%.2f", result.basePrice)
            tvGstAmount.text = String.format("₹%.2f", result.gstAmount)
            tvTotalPrice.text = String.format("₹%.2f", result.totalPrice)
            tvGstRate.text = String.format("%.2f%%", result.gstRate)

            // Show/hide comparison results
            if (calculationType == "Compare Old vs New Rates" && result.oldGstAmount != null) {
                layoutComparison.visibility = View.VISIBLE
                tvOldGstAmount.text = String.format("₹%.2f", result.oldGstAmount)
                tvOldTotalPrice.text = String.format("₹%.2f", result.oldTotalPrice ?: 0.0)
                tvSavings.text = String.format("₹%.2f", result.savings ?: 0.0)
                tvSavingsPercentage.text = String.format("%.1f%%", result.savingsPercentage ?: 0.0)
                tvOldGstRate.text = String.format("%.1f%%", result.oldGstRate ?: 0.0)

                // Set color based on savings
                val savingsAmount = result.savings ?: 0.0
                if (savingsAmount > 0) {
                    tvSavings.setTextColor(android.graphics.Color.parseColor("#00AA00"))
                    tvSavingsPercentage.setTextColor(android.graphics.Color.parseColor("#00AA00"))
                } else if (savingsAmount < 0) {
                    tvSavings.setTextColor(android.graphics.Color.parseColor("#FF6B6B"))
                    tvSavingsPercentage.setTextColor(android.graphics.Color.parseColor("#FF6B6B"))
                } else {
                    tvSavings.setTextColor(android.graphics.Color.parseColor("#808080"))
                    tvSavingsPercentage.setTextColor(android.graphics.Color.parseColor("#808080"))
                }
            } else {
                layoutComparison.visibility = View.GONE
            }

            // Update detailed analysis
            updateDetailedAnalysis(result, calculationType, gstRate)
        }
    }

    private fun updateDetailedAnalysis(
        result: GstCalculationResult,
        calculationType: String,
        gstRate: Double
    ) {
        binding.apply {
            val analysis = StringBuilder()
            analysis.append("📊 GST Calculation Analysis (Sept 2025 Reforms)\n\n")

            analysis.append("🏛️ Current GST Structure:\n")
            analysis.append("• 0% - Essential items, insurance\n")
            analysis.append("• 5% - Daily essentials, medicines\n")
            analysis.append("• 18% - Standard rate for most goods\n")
            analysis.append("• 40% - Luxury & sin goods\n")
            analysis.append("• Special rates: 0.25%, 3% for jewelry\n\n")

            analysis.append("💰 Calculation Details:\n")
            analysis.append("• Calculation Type: $calculationType\n")
            analysis.append("• GST Rate: ${String.format("%.2f", gstRate)}%\n")
            analysis.append("• Base Amount: ₹${String.format("%.2f", result.basePrice)}\n")
            analysis.append("• GST Amount: ₹${String.format("%.2f", result.gstAmount)}\n")
            analysis.append("• Total Amount: ₹${String.format("%.2f", result.totalPrice)}\n\n")

            if (calculationType == "Compare Old vs New Rates" && result.oldGstAmount != null) {
                analysis.append("📈 Rate Comparison:\n")
                analysis.append("• Old GST Rate: ${String.format("%.1f", result.oldGstRate ?: 0.0)}%\n")
                analysis.append("• New GST Rate: ${String.format("%.1f", result.gstRate)}%\n")
                analysis.append("• Old Total: ₹${String.format("%.2f", result.oldTotalPrice ?: 0.0)}\n")
                analysis.append("• New Total: ₹${String.format("%.2f", result.totalPrice)}\n")

                val savings = result.savings ?: 0.0
                if (savings > 0) {
                    analysis.append("• Savings: ₹${String.format("%.2f", savings)} (${String.format("%.1f", result.savingsPercentage ?: 0.0)}%)\n")
                } else if (savings < 0) {
                    analysis.append("• Additional Cost: ₹${String.format("%.2f", kotlin.math.abs(savings))} (${String.format("%.1f", kotlin.math.abs(result.savingsPercentage ?: 0.0))}%)\n")
                } else {
                    analysis.append("• No change in price\n")
                }
                analysis.append("\n")
            }

            analysis.append("🎯 GST Rate Category:\n")
            val categoryInfo = when {
                gstRate == 0.0 -> "Essential Items - Tax-free essentials and insurance"
                gstRate == 0.25 -> "Gems & Jewelry - Special rate for precious stones"
                gstRate == 3.0 -> "Gold & Silver - Precious metals"
                gstRate == 5.0 -> "Daily Essentials - Reduced rate for common items"
                gstRate == 18.0 -> "Standard Rate - Most goods and services"
                gstRate == 40.0 -> "Luxury & Sin Goods - Higher rate for premium items"
                else -> "Custom Rate"
            }
            analysis.append("• $categoryInfo\n\n")

            analysis.append("📅 GST 2025 Reform Impact:\n")
            analysis.append("• Simplified structure: 0%, 5%, 18%, 40%\n")
            analysis.append("• Removed confusing 12% and 28% slabs\n")
            analysis.append("• Essential items made cheaper or exempt\n")
            analysis.append("• Electronics and vehicles became affordable\n")
            analysis.append("• Insurance premiums exempted\n")
            analysis.append("• Luxury items face higher taxation\n\n")

            when {
                gstRate == 5.0 -> {
                    analysis.append("💡 Rate Reduction Benefits:\n")
                    analysis.append("• Many items moved from 12% to 5%\n")
                    analysis.append("• Includes medicines, food, toiletries\n")
                    analysis.append("• Significant savings for consumers\n")
                    analysis.append("• Boost to FMCG sector expected\n")
                }
                gstRate == 18.0 -> {
                    analysis.append("💡 Standard Rate Benefits:\n")
                    analysis.append("• Electronics moved from 28% to 18%\n")
                    analysis.append("• Small cars and bikes reduced\n")
                    analysis.append("• Manufacturing boost expected\n")
                    analysis.append("• Middle-class relief on durables\n")
                }
                gstRate == 40.0 -> {
                    analysis.append("💡 Luxury Tax Policy:\n")
                    analysis.append("• Replaces old 28% + cess structure\n")
                    analysis.append("• Targets premium and sin goods\n")
                    analysis.append("• Revenue generation from luxury consumption\n")
                    analysis.append("• Encourages responsible consumption\n")
                }
                gstRate == 0.0 -> {
                    analysis.append("💡 Exemption Benefits:\n")
                    analysis.append("• Health and life insurance made free\n")
                    analysis.append("• Essential items remain tax-free\n")
                    analysis.append("• Supports financial inclusion\n")
                    analysis.append("• Reduces healthcare costs\n")
                }
            }

            analysis.append("\n⚠️ Important Notes:\n")
            analysis.append("• Rates effective from September 22, 2025\n")
            analysis.append("• Tobacco products transition pending\n")
            analysis.append("• Check HSN codes for specific items\n")
            analysis.append("• Consult tax professional for business compliance\n")

            tvGstDetails.text = analysis.toString()
        }
    }

    private fun showCategoryInfo() {
        val categories = gstCategories.keys.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("GST Categories (2025 Reforms)")
            .setItems(categories) { _, which ->
                val selectedCategory = categories[which]
                val items = gstCategories[selectedCategory] ?: emptyList()

                val itemsText = items.joinToString("\n• ", "• ")

                AlertDialog.Builder(requireContext())
                    .setTitle(selectedCategory)
                    .setMessage(itemsText)
                    .setPositiveButton("OK", null)
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearAllFields() {
        binding.apply {
            etBaseAmount.text?.clear()
            etQuantity.setText("1")
            spinnerCalculationType.setSelection(0)
            spinnerGstRate.setSelection(4) // 18%
            tvRateChangeInfo.visibility = View.GONE
        }
    }

    private fun shareGstCalculation() {
        val options = arrayOf("Share as Text", "Share as Image")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Share GST Calculation")
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
        val calculationType = binding.spinnerCalculationType.selectedItem.toString()
        val gstRate = binding.spinnerGstRate.selectedItem.toString()
        val basePrice = binding.tvBasePrice.text.toString()
        val gstAmount = binding.tvGstAmount.text.toString()
        val totalPrice = binding.tvTotalPrice.text.toString()

        val shareText = buildString {
            append("🧮 GST Calculator Results (2025 Reforms)\n\n")
            append("Calculation: $calculationType\n")
            append("GST Rate: $gstRate\n")
            append("Base Price: $basePrice\n")
            append("GST Amount: $gstAmount\n")
            append("Total Price: $totalPrice\n\n")

            if (binding.layoutComparison.visibility == View.VISIBLE) {
                append("📊 Old vs New Comparison:\n")
                append("Old GST: ${binding.tvOldGstAmount.text}\n")
                append("Old Total: ${binding.tvOldTotalPrice.text}\n")
                append("Savings: ${binding.tvSavings.text}\n")
                append("Savings %: ${binding.tvSavingsPercentage.text}\n\n")
            }

            append("Calculated with GST Calculator (Sept 2025 Reforms)\n")
            append("⚠️ For reference only - verify current rates")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "GST Calculation - 2025 Reforms")
        }

        startActivity(Intent.createChooser(intent, "Share GST Calculation"))
    }

    private fun captureAndShareScreenshot() {
        val screenshotView = binding.gstResultCard
        val bitmap = Bitmap.createBitmap(
            screenshotView.width,
            screenshotView.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        screenshotView.draw(canvas)

        val file = File(requireContext().cacheDir, "gst_calculation_2025.png")
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

        startActivity(Intent.createChooser(shareIntent, "Share GST Calculation"))
    }

    data class GstCalculationResult(
        val basePrice: Double = 0.0,
        val gstAmount: Double = 0.0,
        val totalPrice: Double = 0.0,
        val gstRate: Double = 0.0,
        val oldGstAmount: Double? = null,
        val oldTotalPrice: Double? = null,
        val oldGstRate: Double? = null,
        val savings: Double? = null,
        val savingsPercentage: Double? = null
    )

    companion object {
        private const val TAG = "GstCalculatorFragment"
    }
}