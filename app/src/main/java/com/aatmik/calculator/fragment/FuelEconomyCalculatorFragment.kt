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
    private var isMetricSystem: Boolean = true // Default to metric (liters)

    // Currency data class with both metric and imperial prices
    data class Currency(
        val code: String,
        val symbol: String,
        val name: String,
        val petrolPricePerLiter: Double,
        val dieselPricePerLiter: Double,
        val cngPricePerLiter: Double,
        val petrolPricePerGallon: Double,
        val dieselPricePerGallon: Double,
        val cngPricePerGallon: Double,
        val defaultUnit: String // "metric" or "imperial"
    ) {
        companion object {
            val INR = Currency("INR", "₹", "Indian Rupee", 102.50, 89.75, 75.00, 388.0, 340.0, 284.0, "metric")
            val USD = Currency("USD", "$", "US Dollar", 0.85, 0.75, 0.60, 3.22, 2.84, 2.27, "imperial")
            val EUR = Currency("EUR", "€", "Euro", 1.45, 1.35, 1.10, 5.49, 5.11, 4.16, "metric")
            val GBP = Currency("GBP", "£", "British Pound", 1.55, 1.45, 1.20, 7.05, 6.59, 5.46, "imperial")
            val AED = Currency("AED", "د.إ", "UAE Dirham", 3.20, 3.00, 2.50, 12.11, 11.36, 9.46, "metric")
            val SAR = Currency("SAR", "﷼", "Saudi Riyal", 2.35, 2.15, 1.80, 8.90, 8.14, 6.81, "metric")
            val QAR = Currency("QAR", "ر.ق", "Qatari Riyal", 3.65, 3.45, 2.90, 13.82, 13.06, 10.98, "metric")
            val CAD = Currency("CAD", "C$", "Canadian Dollar", 1.45, 1.35, 1.10, 5.49, 5.11, 4.16, "imperial")
            val AUD = Currency("AUD", "A$", "Australian Dollar", 1.65, 1.55, 1.25, 6.25, 5.87, 4.73, "metric")
            val SGD = Currency("SGD", "S$", "Singapore Dollar", 2.15, 2.00, 1.65, 8.14, 7.57, 6.25, "metric")
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
            updateUnitSystemButtons()
            updateFuelTypeButtons()

            // Text Watcher for input fields
            etDistance.addTextChangedListener(fuelTextWatcher)
            etFuelUsed.addTextChangedListener(fuelTextWatcher)
            etFuelPrice.addTextChangedListener(fuelTextWatcher)

            // Unit system toggle buttons
            btnMetric.setOnClickListener {
                isMetricSystem = true
                updateUnitSystemButtons()
                updateFuelPrice()
                updateInputHints()
                updateEfficiencyGuide()
                calculateAndDisplayFuelEconomy()
            }

            btnImperial.setOnClickListener {
                isMetricSystem = false
                updateUnitSystemButtons()
                updateFuelPrice()
                updateInputHints()
                updateEfficiencyGuide()
                calculateAndDisplayFuelEconomy()
            }

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

            // Set initial state
            updateInputHints()
            updateEfficiencyGuide()
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

                // Auto-set unit system based on currency's default
                isMetricSystem = selectedCurrency.defaultUnit == "metric"
                updateUnitSystemButtons()
                updateInputHints()
                updateEfficiencyGuide()
                updateFuelPrice()
                calculateAndDisplayFuelEconomy()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun updateUnitSystemButtons() {
        binding.apply {
            btnMetric.isSelected = isMetricSystem
            btnImperial.isSelected = !isMetricSystem
        }
    }

    private fun updateInputHints() {
        val fuelUnit = if (isMetricSystem) "liters" else "gallons"
        val priceUnit = if (isMetricSystem) "liter" else "gallon"

        binding.apply {
            fuelUsedInputLayout.hint = "Fuel Used ($fuelUnit)"
            fuelPriceInputLayout.hint = "Fuel Price (${selectedCurrency.symbol} per $priceUnit)"
        }
    }

    private fun updateEfficiencyGuide() {
        val guide = if (isMetricSystem) {
            "Excellent: >20 km/l • Good: 15-20 km/l • Average: 10-15 km/l • Poor: <10 km/l"
        } else {
            "Excellent: >47 mpg • Good: 35-47 mpg • Average: 24-35 mpg • Poor: <24 mpg"
        }
        binding.tvEfficiencyGuide.text = guide
    }

    private fun updateFuelPrice() {
        val price = when (selectedFuelType) {
            "Petrol" -> if (isMetricSystem) selectedCurrency.petrolPricePerLiter else selectedCurrency.petrolPricePerGallon
            "Diesel" -> if (isMetricSystem) selectedCurrency.dieselPricePerLiter else selectedCurrency.dieselPricePerGallon
            "CNG" -> if (isMetricSystem) selectedCurrency.cngPricePerLiter else selectedCurrency.cngPricePerGallon
            else -> if (isMetricSystem) selectedCurrency.petrolPricePerLiter else selectedCurrency.petrolPricePerGallon
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

            val unitSystem = if (isMetricSystem) "Metric" else "Imperial"
            val fuelUnit = if (isMetricSystem) "liters" else "gallons"

            if (distanceText.isEmpty() || fuelUsedText.isEmpty() || fuelPriceText.isEmpty()) {
                tvFuelEfficiencyResult.text = "Enter trip details to calculate"
                tvFuelCostResult.text = ""
                tvCostPerKmResult.text = ""
                tvFuelSummary.text = "Complete the form above for $selectedFuelType ($unitSystem - ${selectedCurrency.code})"
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

                // Calculate fuel efficiency
                val fuelEfficiency = distance / fuelUsed
                val efficiencyUnit = if (isMetricSystem) "km/L" else "mpg"

                // Convert to consistent efficiency rating (using metric standards)
                val metricEfficiency = if (isMetricSystem) fuelEfficiency else fuelEfficiency * 0.425144 // mpg to km/L

                // Calculate total fuel cost
                val totalFuelCost = fuelUsed * fuelPrice

                // Calculate cost per kilometer
                val costPerKm = totalFuelCost / distance

                // Determine efficiency category (based on metric standards)
                val efficiencyCategory = when {
                    metricEfficiency >= 20 -> "Excellent ⭐⭐⭐"
                    metricEfficiency >= 15 -> "Good ⭐⭐"
                    metricEfficiency >= 10 -> "Average ⭐"
                    else -> "Needs Improvement"
                }

                // Calculate monthly cost estimate (assuming 1000 km/month)
                val monthlyCost = costPerKm * 1000

                // Format and display the results with currency symbol
                tvFuelEfficiencyResult.text = "%.2f %s (%s)".format(fuelEfficiency, efficiencyUnit, selectedFuelType)
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