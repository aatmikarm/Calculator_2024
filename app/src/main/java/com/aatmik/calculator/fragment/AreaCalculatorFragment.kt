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
import com.aatmik.calculator.databinding.FragmentAreaCalculatorBinding
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt

class AreaCalculatorFragment : Fragment() {

    private lateinit var binding: FragmentAreaCalculatorBinding
    private var selectedShape: String = "Rectangle" // Default shape
    private var selectedUnit: String = "Meters" // Default unit
    private var selectedUsage: String = "Flooring" // Default usage
    private var selectedCountry: Country = Country.INDIA // Default country

    // Unit conversion factors to square meters
    private val unitFactors = mapOf(
        "Meters" to 1.0,
        "Feet" to 0.092903,  // 1 sq ft = 0.092903 sq m
        "Inches" to 0.00064516 // 1 sq in = 0.00064516 sq m
    )

    // Country data class with pricing for different materials
    data class Country(
        val name: String,
        val currency: String,
        val symbol: String,
        val flooringPrice: Double,
        val tilingPrice: Double,
        val paintPrice: Double, // per liter
        val carpetPrice: Double,
        val defaultUnit: String
    ) {
        companion object {
            val INDIA = Country("India", "INR", "₹", 50.0, 75.0, 300.0, 120.0, "Meters")
            val USA = Country("United States", "USD", "$", 4.5, 6.8, 25.0, 12.5, "Feet")
            val UK = Country("United Kingdom", "GBP", "£", 35.0, 55.0, 45.0, 25.0, "Feet")
            val CANADA = Country("Canada", "CAD", "C$", 5.2, 7.8, 32.0, 15.0, "Feet")
            val AUSTRALIA = Country("Australia", "AUD", "A$", 6.0, 9.0, 38.0, 18.0, "Meters")
            val GERMANY = Country("Germany", "EUR", "€", 45.0, 65.0, 35.0, 28.0, "Meters")
            val UAE = Country("UAE", "AED", "د.إ", 25.0, 35.0, 85.0, 45.0, "Meters")
            val SINGAPORE = Country("Singapore", "SGD", "S$", 35.0, 50.0, 45.0, 25.0, "Meters")
            val SOUTH_AFRICA = Country("South Africa", "ZAR", "R", 180.0, 250.0, 320.0, 150.0, "Meters")
            val BRAZIL = Country("Brazil", "BRL", "R$", 25.0, 35.0, 45.0, 20.0, "Meters")
        }
    }

    private val countries = listOf(
        Country.INDIA, Country.USA, Country.UK, Country.CANADA, Country.AUSTRALIA,
        Country.GERMANY, Country.UAE, Country.SINGAPORE, Country.SOUTH_AFRICA, Country.BRAZIL
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            // Handle back button press
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            // Set up country spinner
            setupCountrySpinner()

            // Set initial button states
            updateShapeButtons()
            updateUnitButtons()
            updateUsageButtons()
            updateInputFields()

            // Text Watcher for input fields
            etDimension1.addTextChangedListener(areaTextWatcher)
            etDimension2.addTextChangedListener(areaTextWatcher)
            etDimension3.addTextChangedListener(areaTextWatcher)

            // Shape selection buttons
            btnRectangle.setOnClickListener {
                selectedShape = "Rectangle"
                updateShapeButtons()
                updateInputFields()
                calculateAndDisplayArea()
            }

            btnSquare.setOnClickListener {
                selectedShape = "Square"
                updateShapeButtons()
                updateInputFields()
                calculateAndDisplayArea()
            }

            btnCircle.setOnClickListener {
                selectedShape = "Circle"
                updateShapeButtons()
                updateInputFields()
                calculateAndDisplayArea()
            }

            btnTriangle.setOnClickListener {
                selectedShape = "Triangle"
                updateShapeButtons()
                updateInputFields()
                calculateAndDisplayArea()
            }

            btnLShaped.setOnClickListener {
                selectedShape = "L-Shape"
                updateShapeButtons()
                updateInputFields()
                calculateAndDisplayArea()
            }

            btnCustom.setOnClickListener {
                selectedShape = "Custom"
                updateShapeButtons()
                updateInputFields()
                calculateAndDisplayArea()
            }

            // Unit selection buttons
            btnMeters.setOnClickListener {
                selectedUnit = "Meters"
                updateUnitButtons()
                updateInputHints()
                calculateAndDisplayArea()
            }

            btnFeet.setOnClickListener {
                selectedUnit = "Feet"
                updateUnitButtons()
                updateInputHints()
                calculateAndDisplayArea()
            }

            btnInches.setOnClickListener {
                selectedUnit = "Inches"
                updateUnitButtons()
                updateInputHints()
                calculateAndDisplayArea()
            }

            // Usage selection buttons
            btnFlooring.setOnClickListener {
                selectedUsage = "Flooring"
                updateUsageButtons()
                calculateAndDisplayArea()
            }

            btnTiling.setOnClickListener {
                selectedUsage = "Tiling"
                updateUsageButtons()
                calculateAndDisplayArea()
            }

            btnPainting.setOnClickListener {
                selectedUsage = "Painting"
                updateUsageButtons()
                calculateAndDisplayArea()
            }

            btnCarpet.setOnClickListener {
                selectedUsage = "Carpet"
                updateUsageButtons()
                calculateAndDisplayArea()
            }

            // Calculate button
            btnCalculateArea.setOnClickListener {
                calculateAndDisplayArea()
            }

            // Set initial state
            updateInputHints()
        }
    }

    private fun setupCountrySpinner() {
        val countryNames = countries.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, countryNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerCountry.adapter = adapter
        binding.spinnerCountry.setSelection(0) // Default to India

        binding.spinnerCountry.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedCountry = countries[position]

                // Auto-set unit system based on country's default
                selectedUnit = selectedCountry.defaultUnit
                updateUnitButtons()
                updateInputHints()
                calculateAndDisplayArea()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun updateShapeButtons() {
        binding.apply {
            btnRectangle.isSelected = selectedShape == "Rectangle"
            btnSquare.isSelected = selectedShape == "Square"
            btnCircle.isSelected = selectedShape == "Circle"
            btnTriangle.isSelected = selectedShape == "Triangle"
            btnLShaped.isSelected = selectedShape == "L-Shape"
            btnCustom.isSelected = selectedShape == "Custom"
        }
    }

    private fun updateUnitButtons() {
        binding.apply {
            btnMeters.isSelected = selectedUnit == "Meters"
            btnFeet.isSelected = selectedUnit == "Feet"
            btnInches.isSelected = selectedUnit == "Inches"
        }
    }

    private fun updateUsageButtons() {
        binding.apply {
            btnFlooring.isSelected = selectedUsage == "Flooring"
            btnTiling.isSelected = selectedUsage == "Tiling"
            btnPainting.isSelected = selectedUsage == "Painting"
            btnCarpet.isSelected = selectedUsage == "Carpet"
        }
    }

    private fun updateInputFields() {
        binding.apply {
            // Hide all fields first
            dimension1InputLayout.visibility = View.GONE
            dimension2InputLayout.visibility = View.GONE
            dimension3InputLayout.visibility = View.GONE

            // Show required fields based on shape
            when (selectedShape) {
                "Rectangle" -> {
                    dimension1InputLayout.visibility = View.VISIBLE
                    dimension2InputLayout.visibility = View.VISIBLE
                    dimension1InputLayout.hint = "Length"
                    dimension2InputLayout.hint = "Width"
                }
                "Square" -> {
                    dimension1InputLayout.visibility = View.VISIBLE
                    dimension1InputLayout.hint = "Side Length"
                }
                "Circle" -> {
                    dimension1InputLayout.visibility = View.VISIBLE
                    dimension1InputLayout.hint = "Radius"
                }
                "Triangle" -> {
                    dimension1InputLayout.visibility = View.VISIBLE
                    dimension2InputLayout.visibility = View.VISIBLE
                    dimension1InputLayout.hint = "Base"
                    dimension2InputLayout.hint = "Height"
                }
                "L-Shape" -> {
                    dimension1InputLayout.visibility = View.VISIBLE
                    dimension2InputLayout.visibility = View.VISIBLE
                    dimension3InputLayout.visibility = View.VISIBLE
                    dimension1InputLayout.hint = "Large Length"
                    dimension2InputLayout.hint = "Large Width"
                    dimension3InputLayout.hint = "Cut-out Size"
                }
                "Custom" -> {
                    dimension1InputLayout.visibility = View.VISIBLE
                    dimension1InputLayout.hint = "Total Area"
                }
            }
        }
    }

    private fun updateInputHints() {
        val unitText = selectedUnit.lowercase()
        binding.apply {
            when (selectedShape) {
                "Rectangle" -> {
                    dimension1InputLayout.hint = "Length ($unitText)"
                    dimension2InputLayout.hint = "Width ($unitText)"
                }
                "Square" -> {
                    dimension1InputLayout.hint = "Side Length ($unitText)"
                }
                "Circle" -> {
                    dimension1InputLayout.hint = "Radius ($unitText)"
                }
                "Triangle" -> {
                    dimension1InputLayout.hint = "Base ($unitText)"
                    dimension2InputLayout.hint = "Height ($unitText)"
                }
                "L-Shape" -> {
                    dimension1InputLayout.hint = "Large Length ($unitText)"
                    dimension2InputLayout.hint = "Large Width ($unitText)"
                    dimension3InputLayout.hint = "Cut-out Size ($unitText)"
                }
                "Custom" -> {
                    dimension1InputLayout.hint = "Total Area (sq $unitText)"
                }
            }
        }
    }

    private fun calculateAndDisplayArea() {
        binding.apply {
            val dim1Text = etDimension1.text.toString()
            val dim2Text = etDimension2.text.toString()
            val dim3Text = etDimension3.text.toString()

            // Check if required fields are filled
            val requiredFieldsFilled = when (selectedShape) {
                "Rectangle", "Triangle" -> dim1Text.isNotEmpty() && dim2Text.isNotEmpty()
                "Square", "Circle", "Custom" -> dim1Text.isNotEmpty()
                "L-Shape" -> dim1Text.isNotEmpty() && dim2Text.isNotEmpty() && dim3Text.isNotEmpty()
                else -> false
            }

            if (!requiredFieldsFilled) {
                tvAreaResult.text = "Enter dimensions for $selectedShape"
                tvPerimeterResult.text = ""
                tvMaterialResult.text = ""
                tvUsageInfo.text = "Complete the form above for $selectedUsage calculation (${selectedCountry.name})"
                return
            }

            try {
                val dim1 = dim1Text.toDouble()
                val dim2 = if (dim2Text.isNotEmpty()) dim2Text.toDouble() else 0.0
                val dim3 = if (dim3Text.isNotEmpty()) dim3Text.toDouble() else 0.0

                if (dim1 <= 0 || (dim2 <= 0 && dim2Text.isNotEmpty()) || (dim3 <= 0 && dim3Text.isNotEmpty())) {
                    tvAreaResult.text = "Please enter positive values"
                    tvPerimeterResult.text = ""
                    tvMaterialResult.text = ""
                    tvUsageInfo.text = "All dimensions must be greater than 0"
                    return
                }

                // Calculate area based on shape
                val area = calculateShapeArea(dim1, dim2, dim3)
                val perimeter = calculateShapePerimeter(dim1, dim2, dim3)

                // Convert to square meters for consistent calculations
                val areaInSqMeters = area * (unitFactors[selectedUnit] ?: 1.0)

                // Calculate material estimates
                val materialInfo = calculateMaterialEstimate(areaInSqMeters)

                // Format and display results
                val unitSymbol = when (selectedUnit) {
                    "Meters" -> "m²"
                    "Feet" -> "ft²"
                    "Inches" -> "in²"
                    else -> "units²"
                }

                tvAreaResult.text = "Area: %.2f %s (%s)".format(area, unitSymbol, selectedShape)
                tvPerimeterResult.text = if (perimeter > 0) "Perimeter: %.2f %s".format(perimeter, selectedUnit.lowercase()) else ""
                tvMaterialResult.text = materialInfo.first
                tvUsageInfo.text = materialInfo.second

            } catch (e: NumberFormatException) {
                Log.e(TAG, "Invalid number format", e)
                tvAreaResult.text = "Please enter valid numerical values"
                tvPerimeterResult.text = ""
                tvMaterialResult.text = ""
                tvUsageInfo.text = "Invalid input detected"
            } catch (e: Exception) {
                Log.e(TAG, "Calculation error", e)
                tvAreaResult.text = "Calculation error occurred"
                tvPerimeterResult.text = ""
                tvMaterialResult.text = ""
                tvUsageInfo.text = "Please check your input values"
            }
        }
    }

    private fun calculateShapeArea(dim1: Double, dim2: Double, dim3: Double): Double {
        return when (selectedShape) {
            "Rectangle" -> dim1 * dim2
            "Square" -> dim1 * dim1
            "Circle" -> PI * dim1 * dim1
            "Triangle" -> 0.5 * dim1 * dim2
            "L-Shape" -> {
                // Large rectangle minus cut-out square
                val largeArea = dim1 * dim2
                val cutoutArea = dim3 * dim3
                largeArea - cutoutArea
            }
            "Custom" -> dim1 // User enters area directly
            else -> 0.0
        }
    }

    private fun calculateShapePerimeter(dim1: Double, dim2: Double, dim3: Double): Double {
        return when (selectedShape) {
            "Rectangle" -> 2 * (dim1 + dim2)
            "Square" -> 4 * dim1
            "Circle" -> 2 * PI * dim1
            "Triangle" -> {
                // Assuming right triangle, calculate third side
                val hypotenuse = sqrt(dim1 * dim1 + dim2 * dim2)
                dim1 + dim2 + hypotenuse
            }
            "L-Shape" -> {
                // Perimeter of L-shape (outer perimeter)
                2 * dim1 + 2 * dim2 + 2 * dim3
            }
            "Custom" -> 0.0 // Cannot calculate perimeter for custom area
            else -> 0.0
        }
    }

    private fun calculateMaterialEstimate(areaInSqMeters: Double): Pair<String, String> {
        return when (selectedUsage) {
            "Flooring" -> {
                val tiles = (areaInSqMeters / 0.36).toInt() + 1 // Assuming 60cm x 60cm tiles
                val cost = areaInSqMeters * selectedCountry.flooringPrice
                Pair(
                    "Tiles needed: ~${tiles} pieces",
                    "Est. cost: ${selectedCountry.symbol}${cost.toInt()} • Add 10% extra for wastage"
                )
            }
            "Tiling" -> {
                val tiles = (areaInSqMeters / 0.25).toInt() + 1 // Assuming 50cm x 50cm tiles
                val cost = areaInSqMeters * selectedCountry.tilingPrice
                Pair(
                    "Tiles needed: ~${tiles} pieces",
                    "Est. cost: ${selectedCountry.symbol}${cost.toInt()} • Include grout and adhesive"
                )
            }
            "Painting" -> {
                val paint = (areaInSqMeters / 12).toInt() + 1 // 1 liter covers ~12 sq meters
                val cost = paint * selectedCountry.paintPrice
                Pair(
                    "Paint needed: ~${paint} liters",
                    "Est. cost: ${selectedCountry.symbol}${cost.toInt()} • 2 coats recommended"
                )
            }
            "Carpet" -> {
                val cost = areaInSqMeters * selectedCountry.carpetPrice
                Pair(
                    "Carpet area: ${String.format("%.2f", areaInSqMeters)} sq meters",
                    "Est. cost: ${selectedCountry.symbol}${cost.toInt()} • Professional installation recommended"
                )
            }
            else -> Pair("", "")
        }
    }

    // TextWatcher to handle real-time input
    private val areaTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            // Automatically calculate area when all required fields are filled
            calculateAndDisplayArea()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAreaCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        private const val TAG = "AreaCalculatorFragment"
    }
}