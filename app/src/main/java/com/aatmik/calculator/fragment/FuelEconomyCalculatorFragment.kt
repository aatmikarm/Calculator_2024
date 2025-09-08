package com.aatmik.calculator.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentFuelEconomyCalculatorBinding

class FuelEconomyCalculatorFragment : Fragment() {

    private lateinit var binding: FragmentFuelEconomyCalculatorBinding
    private var selectedFuelType: String = "Petrol" // Default fuel type
    private var selectedCurrency: Currency = Currency.INR // Default currency

    // Currency data class
    data class Currency(
        val code: String,
        val symbol: String,
        val name: String,
        val petrolPrice: Double,
        val dieselPrice: Double,
        val cngPrice: Double
    ) {
        companion object {
            val INR = Currency("INR", "₹", "Indian Rupee", 102.50, 89.75, 75.00)
            val USD = Currency("USD", "$", "US Dollar", 0.85, 0.75, 0.60)
            val EUR = Currency("EUR", "€", "Euro", 1.45, 1.35, 1.10)
            val GBP = Currency("GBP", "£", "British Pound", 1.55, 1.45, 1.20)
            val AED = Currency("AED", "د.إ", "UAE Dirham", 3.20, 3.00, 2.50)
            val SAR = Currency("SAR", "﷼", "Saudi Riyal", 2.35, 2.15, 1.80)
            val QAR = Currency("QAR", "ر.ق", "Qatari Riyal", 3.65, 3.45, 2.90)
            val CAD = Currency("CAD", "C$", "Canadian Dollar", 1.45, 1.35, 1.10)
            val AUD = Currency("AUD", "A$", "Australian Dollar", 1.65, 1.55, 1.25)
            val SGD = Currency("SGD", "S$", "Singapore Dollar", 2.15, 2.00, 1.65)
        }
    }

    private val currencies = listOf(
        Currency.INR, Currency.USD, Currency.EUR, Currency.GBP, Currency.AED,
        Currency.SAR, Currency.QAR, Currency.CAD, Currency.AUD, Currency.SGD
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            // Handle back button press
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            // Set up currency spinner
            setupCurrencySpinner()

            // Set initial button states
            updateFuelTypeButtons()

            // Text Watcher for input fields
            etDistance.addTextChangedListener(fuelTextWatcher)
            etFuelUsed.addTextChangedListener(fuelTextWatcher)
            etFuelPrice.addTextChangedListener(fuelTextWatcher)

            // Fuel type selection buttons
            btnPetrol.setOnClickListener {
                selectedFuelType = "Petrol"
                updateFuelPrice()
                updateFuelTypeButtons()
                calculateAndDisplayFuelEconomy()
            }

            btnDiesel.setOnClickListener {
                selectedFuelType = "Diesel"
                updateFuelPrice()
                updateFuelTypeButtons()
                calculateAndDisplayFuelEconomy()
            }

            btnCng.setOnClickListener {
                selectedFuelType = "CNG"
                updateFuelPrice()
                updateFuelTypeButtons()
                calculateAndDisplayFuelEconomy()
            }

            // Calculate button
            btnCalculateFuelEconomy.setOnClickListener {
                calculateAndDisplayFuelEconomy()
            }

            // Set default fuel price
            updateFuelPrice()
        }
    }

    private fun setupCurrencySpinner() {
        val currencyNames = currencies.map { "${it.code} - ${it.name}" }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, currencyNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerCurrency.adapter = adapter
        binding.spinnerCurrency.setSelection(0) // Default to INR

        binding.spinnerCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedCurrency = currencies[position]
                updateFuelPrice()
                updateFuelPriceHint()
                calculateAndDisplayFuelEconomy()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun updateFuelPriceHint() {
        binding.fuelPriceInputLayout.hint = "Fuel Price (${selectedCurrency.symbol} per liter)"
    }

    private fun updateFuelPrice() {
        val price = when (selectedFuelType) {
            "Petrol" -> selectedCurrency.petrolPrice
            "Diesel" -> selectedCurrency.dieselPrice
            "CNG" -> selectedCurrency.cngPrice
            else -> selectedCurrency.petrolPrice
        }
        binding.etFuelPrice.setText(String.format("%.2f", price))
    }

    // Method to update fuel type button states
    private fun updateFuelTypeButtons() {
        binding.apply {
            btnPetrol.isSelected = selectedFuelType == "Petrol"
            btnDiesel.isSelected = selectedFuelType == "Diesel"
            btnCng.isSelected = selectedFuelType == "CNG"
        }
    }

    // Method to calculate and display fuel economy
    private fun calculateAndDisplayFuelEconomy() {
        binding.apply {
            val distanceText = etDistance.text.toString()
            val fuelUsedText = etFuelUsed.text.toString()
            val fuelPriceText = etFuelPrice.text.toString()

            if (distanceText.isEmpty() || fuelUsedText.isEmpty() || fuelPriceText.isEmpty()) {
                tvFuelEfficiencyResult.text = "Enter trip details to calculate"
                tvFuelCostResult.text = ""
                tvCostPerKmResult.text = ""
                tvFuelSummary.text = "Complete the form above for $selectedFuelType (${selectedCurrency.code})"
                return
            }

            try {
                val distance = distanceText.toDouble()
                val fuelUsed = fuelUsedText.toDouble()
                val fuelPrice = fuelPriceText.toDouble()

                if (distance <= 0 || fuelUsed <= 0 || fuelPrice <= 0) {
                    tvFuelEfficiencyResult.text = "Please enter positive values"
                    tvFuelCostResult.text = ""
                    tvCostPerKmResult.text = ""
                    tvFuelSummary.text = "All values must be greater than 0"
                    return
                }

                // Calculate fuel efficiency (km per liter)
                val fuelEfficiency = distance / fuelUsed

                // Calculate total fuel cost
                val totalFuelCost = fuelUsed * fuelPrice

                // Calculate cost per kilometer
                val costPerKm = totalFuelCost / distance

                // Determine efficiency category
                val efficiencyCategory = when {
                    fuelEfficiency >= 20 -> "Excellent ⭐⭐⭐"
                    fuelEfficiency >= 15 -> "Good ⭐⭐"
                    fuelEfficiency >= 10 -> "Average ⭐"
                    else -> "Needs Improvement"
                }

                // Calculate monthly cost estimate (assuming 1000 km/month)
                val monthlyCost = costPerKm * 1000

                // Format and display the results with currency symbol
                tvFuelEfficiencyResult.text = "%.2f km/liter (%s)".format(fuelEfficiency, selectedFuelType)
                tvFuelCostResult.text = "Trip Cost: %s%.2f".format(selectedCurrency.symbol, totalFuelCost)
                tvCostPerKmResult.text = "Cost per km: %s%.2f".format(selectedCurrency.symbol, costPerKm)
                tvFuelSummary.text = "%s • Monthly est: %s%.0f (1000km)".format(efficiencyCategory, selectedCurrency.symbol, monthlyCost)

            } catch (e: NumberFormatException) {
                Log.e(TAG, "Invalid number format", e)
                tvFuelEfficiencyResult.text = "Please enter valid numerical values"
                tvFuelCostResult.text = ""
                tvCostPerKmResult.text = ""
                tvFuelSummary.text = "Invalid input detected"
            } catch (e: Exception) {
                Log.e(TAG, "Calculation error", e)
                tvFuelEfficiencyResult.text = "Calculation error occurred"
                tvFuelCostResult.text = ""
                tvCostPerKmResult.text = ""
                tvFuelSummary.text = "Please check your input values"
            }
        }
    }

    // TextWatcher to handle real-time input
    private val fuelTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            // Automatically calculate fuel economy when all fields are filled
            calculateAndDisplayFuelEconomy()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentFuelEconomyCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        private const val TAG = "FuelEconomyCalculatorFragment"
    }
}