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
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentTripEstimateBinding
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil
import kotlin.math.roundToInt

class TripEstimateFragment : Fragment() {

    private lateinit var binding: FragmentTripEstimateBinding
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentTripEstimateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        setupSpinners()
        setupListeners()
        setupTextWatchers()
        calculateTrip()
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

            // Setup Vehicle Type Spinner
            val vehicleTypes = arrayOf("Car", "Motorcycle", "SUV", "Truck", "Bus")
            val vehicleAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                vehicleTypes
            )
            vehicleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerVehicleType.adapter = vehicleAdapter

            // Setup Accommodation Type Spinner
            val accommodationTypes = arrayOf("Budget Hotel", "Mid-range Hotel", "Luxury Hotel", "Hostel", "Airbnb", "Camping")
            val accommodationAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                accommodationTypes
            )
            accommodationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerAccommodationType.adapter = accommodationAdapter
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
                captureAndShareScreenshot()
            }

            // Spinner listeners for auto-calculation
            spinnerCurrency.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    calculateTrip()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })

            spinnerVehicleType.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    updateDefaultMileage()
                    calculateTrip()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })

            spinnerAccommodationType.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    updateDefaultAccommodationCost()
                    calculateTrip()
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
                    calculateTrip()
                }
            }
        }

        binding.apply {
            etDistance.addTextChangedListener(textWatcher)
            etFuelPrice.addTextChangedListener(textWatcher)
            etMileage.addTextChangedListener(textWatcher)
            etDays.addTextChangedListener(textWatcher)
            etAccommodationCost.addTextChangedListener(textWatcher)
            etFoodCost.addTextChangedListener(textWatcher)
            etMiscCost.addTextChangedListener(textWatcher)
        }
    }

    private fun updateDefaultMileage() {
        binding.apply {
            val vehicleType = spinnerVehicleType.selectedItem.toString()
            val defaultMileage = when (vehicleType) {
                "Car" -> "15.0"
                "Motorcycle" -> "35.0"
                "SUV" -> "12.0"
                "Truck" -> "8.0"
                "Bus" -> "6.0"
                else -> "15.0"
            }

            if (etMileage.text.isEmpty()) {
                isCalculating = true
                etMileage.setText(defaultMileage)
                isCalculating = false
            }
        }
    }

    private fun updateDefaultAccommodationCost() {
        binding.apply {
            val accommodationType = spinnerAccommodationType.selectedItem.toString()
            val defaultCost = when (accommodationType) {
                "Budget Hotel" -> "50"
                "Mid-range Hotel" -> "100"
                "Luxury Hotel" -> "250"
                "Hostel" -> "25"
                "Airbnb" -> "75"
                "Camping" -> "15"
                else -> "75"
            }

            if (etAccommodationCost.text.isEmpty()) {
                isCalculating = true
                etAccommodationCost.setText(defaultCost)
                isCalculating = false
            }
        }
    }

    private fun calculateTrip() {
        binding.apply {
            try {
                val distance = etDistance.text.toString().toDoubleOrNull() ?: 0.0
                val fuelPrice = etFuelPrice.text.toString().toDoubleOrNull() ?: 0.0
                val mileage = etMileage.text.toString().toDoubleOrNull() ?: 15.0
                val days = etDays.text.toString().toIntOrNull() ?: 1
                val accommodationCost = etAccommodationCost.text.toString().toDoubleOrNull() ?: 0.0
                val foodCost = etFoodCost.text.toString().toDoubleOrNull() ?: 0.0
                val miscCost = etMiscCost.text.toString().toDoubleOrNull() ?: 0.0

                // Calculate fuel costs
                val totalDistance = distance * 2 // Round trip
                val fuelNeeded = totalDistance / mileage
                val fuelCost = fuelNeeded * fuelPrice

                // Calculate accommodation costs (days - 1 for overnight stays)
                val nightsNeeded = maxOf(0, days - 1)
                val totalAccommodationCost = nightsNeeded * accommodationCost

                // Calculate food and misc costs for all days
                val totalFoodCost = days * foodCost
                val totalMiscCost = days * miscCost

                // Calculate totals
                val totalCost = fuelCost + totalAccommodationCost + totalFoodCost + totalMiscCost

                // Get currency symbol
                val selectedCurrency = spinnerCurrency.selectedItem.toString()
                val currencySymbol = currencySymbols[selectedCurrency] ?: "$"

                // Update main result displays
                tvTotalCost.text = String.format("%.0f", totalCost)
                tvCurrencySymbol.text = currencySymbol

                // Update breakdown
                tvFuelCost.text = String.format("%.2f", fuelCost)
                tvAccommodationTotal.text = String.format("%.2f", totalAccommodationCost)
                tvFoodTotal.text = String.format("%.2f", totalFoodCost)
                tvMiscTotal.text = String.format("%.2f", totalMiscCost)

                // Update fuel details
                tvFuelNeeded.text = String.format("%.1f L", fuelNeeded)
                tvTotalDistance.text = String.format("%.0f km", totalDistance)

                // Update accommodation details
                tvNightsNeeded.text = if (nightsNeeded > 0) "$nightsNeeded nights" else "Day trip"

                // Calculate cost per person if multiple people
                // Assuming 1 person for now, can be enhanced later
                val costPerPerson = totalCost
                tvCostPerPerson.text = String.format("%.0f", costPerPerson)

                // Calculate cost per day
                val costPerDay = totalCost / days
                tvCostPerDay.text = String.format("%.0f", costPerDay)

                // Update additional info
                updateAdditionalInfo(totalDistance, fuelNeeded, fuelCost, days, totalCost, currencySymbol)

            } catch (e: Exception) {
                // Handle calculation errors silently
            }
        }
    }

    private fun updateAdditionalInfo(totalDistance: Double, fuelNeeded: Double, fuelCost: Double, days: Int, totalCost: Double, currencySymbol: String) {
        binding.apply {
            val result = StringBuilder()
            result.append("🚗 Trip Summary\n\n")

            result.append("📍 Distance: ${String.format("%.0f", totalDistance)} km (round trip)\n")
            result.append("⛽ Fuel needed: ${String.format("%.1f", fuelNeeded)} L\n")
            result.append("🏨 Duration: $days day(s)\n")
            result.append("💰 Total cost: $currencySymbol${String.format("%.0f", totalCost)}\n\n")

            // Cost breakdown as percentage
            if (totalCost > 0) {
                val fuelPercentage = (fuelCost / totalCost) * 100
                val accommodationTotal = etAccommodationCost.text.toString().toDoubleOrNull()?.let { it * maxOf(0, days - 1) } ?: 0.0
                val accommodationPercentage = (accommodationTotal / totalCost) * 100
                val foodTotal = etFoodCost.text.toString().toDoubleOrNull()?.let { it * days } ?: 0.0
                val foodPercentage = (foodTotal / totalCost) * 100
                val miscTotal = etMiscCost.text.toString().toDoubleOrNull()?.let { it * days } ?: 0.0
                val miscPercentage = (miscTotal / totalCost) * 100

                result.append("📊 Cost Breakdown:\n")
                result.append("• Fuel: ${String.format("%.0f", fuelPercentage)}%\n")
                if (accommodationTotal > 0) result.append("• Accommodation: ${String.format("%.0f", accommodationPercentage)}%\n")
                if (foodTotal > 0) result.append("• Food: ${String.format("%.0f", foodPercentage)}%\n")
                if (miscTotal > 0) result.append("• Miscellaneous: ${String.format("%.0f", miscPercentage)}%\n")
                result.append("\n")
            }

            // Travel tips based on trip characteristics
            result.append("💡 Travel Tips:\n")

            if (totalDistance > 1000) {
                result.append("• Long trip - consider breaking journey into multiple days\n")
                result.append("• Plan rest stops every 2-3 hours\n")
            }

            if (fuelCost > totalCost * 0.4) {
                result.append("• Fuel is a major expense - consider carpooling\n")
                result.append("• Check for cheaper fuel stations along the route\n")
            }

            val accommodationTotal = etAccommodationCost.text.toString().toDoubleOrNull()?.let { it * maxOf(0, days - 1) } ?: 0.0
            if (accommodationTotal > totalCost * 0.5) {
                result.append("• Accommodation is expensive - consider budget options\n")
                result.append("• Look for deals or group bookings\n")
            }

            result.append("• Pack snacks to reduce food costs\n")
            result.append("• Check vehicle before departure\n")
            result.append("• Have emergency fund (10-20% extra)\n")

            tvTripDetails.text = result.toString()
        }
    }

    private fun shareTrip() {
        val selectedCurrency = binding.spinnerCurrency.selectedItem.toString()
        val currencySymbol = currencySymbols[selectedCurrency] ?: "$"
        val totalCost = binding.tvTotalCost.text.toString()
        val distance = binding.etDistance.text.toString()
        val days = binding.etDays.text.toString()

        val shareText = """
            🚗 Trip Estimate
            
            Distance: ${distance} km (one way)
            Duration: ${days} day(s)
            Total Cost: $currencySymbol$totalCost
            
            Breakdown:
            • Fuel: $currencySymbol${binding.tvFuelCost.text}
            • Accommodation: $currencySymbol${binding.tvAccommodationTotal.text}
            • Food: $currencySymbol${binding.tvFoodTotal.text}
            • Miscellaneous: $currencySymbol${binding.tvMiscTotal.text}
            
            Calculated with Trip Estimate Calculator
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Trip Estimate")
        }

        startActivity(Intent.createChooser(intent, "Share Trip Estimate"))
    }

    private fun captureAndShareScreenshot() {
        val screenshotView = binding.tripResultCard
        val bitmap = Bitmap.createBitmap(
            screenshotView.width,
            screenshotView.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        screenshotView.draw(canvas)

        // Save the bitmap to a file
        val file = File(requireContext().cacheDir, "trip_result_screenshot.png")
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
        startActivity(Intent.createChooser(shareIntent, "Share Trip Result"))
    }

    companion object {
        private const val TAG = "TripEstimateFragment"
    }
}