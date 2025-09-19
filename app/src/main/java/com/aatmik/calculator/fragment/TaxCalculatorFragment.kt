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
import com.aatmik.calculator.databinding.FragmentTaxCalculatorBinding
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class TaxCalculatorFragment : Fragment() {

    private lateinit var binding: FragmentTaxCalculatorBinding
    private var isCalculating = false

    // Countries supported
    private val countries = arrayOf(
        "United States", "India", "United Kingdom", "Germany", "Canada",
        "Australia", "France", "Japan", "Singapore", "UAE"
    )

    // US States for state tax calculation
    private val usStates = arrayOf(
        "No State Tax", "Alabama", "Alaska", "Arizona", "Arkansas", "California",
        "Colorado", "Connecticut", "Delaware", "Florida", "Georgia", "Hawaii",
        "Idaho", "Illinois", "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana",
        "Maine", "Maryland", "Massachusetts", "Michigan", "Minnesota", "Mississippi",
        "Missouri", "Montana", "Nebraska", "Nevada", "New Hampshire", "New Jersey",
        "New Mexico", "New York", "North Carolina", "North Dakota", "Ohio",
        "Oklahoma", "Oregon", "Pennsylvania", "Rhode Island", "South Carolina",
        "South Dakota", "Tennessee", "Texas", "Utah", "Vermont", "Virginia",
        "Washington", "West Virginia", "Wisconsin", "Wyoming"
    )

    // State tax rates (simplified - top marginal rates for 2025)
    private val stateTaxRates = mapOf(
        "No State Tax" to 0.0,
        "Alabama" to 0.05,
        "Alaska" to 0.0,
        "Arizona" to 0.045,
        "Arkansas" to 0.055,
        "California" to 0.133,
        "Colorado" to 0.044,
        "Connecticut" to 0.0699,
        "Delaware" to 0.066,
        "Florida" to 0.0,
        "Georgia" to 0.0575,
        "Hawaii" to 0.11,
        "Idaho" to 0.058,
        "Illinois" to 0.0495,
        "Indiana" to 0.0323,
        "Iowa" to 0.0853,
        "Kansas" to 0.057,
        "Kentucky" to 0.05,
        "Louisiana" to 0.06,
        "Maine" to 0.0715,
        "Maryland" to 0.0575,
        "Massachusetts" to 0.05,
        "Michigan" to 0.0425,
        "Minnesota" to 0.0985,
        "Mississippi" to 0.05,
        "Missouri" to 0.054,
        "Montana" to 0.0675,
        "Nebraska" to 0.0684,
        "Nevada" to 0.0,
        "New Hampshire" to 0.0,
        "New Jersey" to 0.1075,
        "New Mexico" to 0.059,
        "New York" to 0.109,
        "North Carolina" to 0.0475,
        "North Dakota" to 0.029,
        "Ohio" to 0.0399,
        "Oklahoma" to 0.05,
        "Oregon" to 0.099,
        "Pennsylvania" to 0.0307,
        "Rhode Island" to 0.0599,
        "South Carolina" to 0.07,
        "South Dakota" to 0.0,
        "Tennessee" to 0.0,
        "Texas" to 0.0,
        "Utah" to 0.0495,
        "Vermont" to 0.0875,
        "Virginia" to 0.0575,
        "Washington" to 0.0,
        "West Virginia" to 0.065,
        "Wisconsin" to 0.0765,
        "Wyoming" to 0.0
    )

    // Currency symbols for each country
    private val currencySymbols = mapOf(
        "United States" to "$",
        "India" to "₹",
        "United Kingdom" to "£",
        "Germany" to "€",
        "Canada" to "C$",
        "Australia" to "A$",
        "France" to "€",
        "Japan" to "¥",
        "Singapore" to "S$",
        "UAE" to "AED"
    )

    // Updated Indian tax regimes for AY 2025-26
    private val indianTaxRegimes = arrayOf(
        "New Regime (Default)",
        "Old Regime (Optional)"
    )

    // Updated Tax brackets for different countries (2025 data)
    private val taxBrackets = mapOf(
        // United States - 2025 Tax Brackets (unchanged)
        "United States" to mapOf(
            "Single" to listOf(
                TaxBracket(0.0, 11600.0, 0.10),
                TaxBracket(11600.0, 47150.0, 0.12),
                TaxBracket(47150.0, 100525.0, 0.22),
                TaxBracket(100525.0, 191750.0, 0.24),
                TaxBracket(191750.0, 243725.0, 0.32),
                TaxBracket(243725.0, 609350.0, 0.35),
                TaxBracket(609350.0, Double.MAX_VALUE, 0.37)
            ),
            "Married Filing Jointly" to listOf(
                TaxBracket(0.0, 23200.0, 0.10),
                TaxBracket(23200.0, 94300.0, 0.12),
                TaxBracket(94300.0, 201050.0, 0.22),
                TaxBracket(201050.0, 383500.0, 0.24),
                TaxBracket(383500.0, 487450.0, 0.32),
                TaxBracket(487450.0, 731200.0, 0.35),
                TaxBracket(731200.0, Double.MAX_VALUE, 0.37)
            ),
            "Married Filing Separately" to listOf(
                TaxBracket(0.0, 11600.0, 0.10),
                TaxBracket(11600.0, 47150.0, 0.12),
                TaxBracket(47150.0, 100525.0, 0.22),
                TaxBracket(100525.0, 191750.0, 0.24),
                TaxBracket(191750.0, 243725.0, 0.32),
                TaxBracket(243725.0, 365600.0, 0.35),
                TaxBracket(365600.0, Double.MAX_VALUE, 0.37)
            ),
            "Head of Household" to listOf(
                TaxBracket(0.0, 16550.0, 0.10),
                TaxBracket(16550.0, 63100.0, 0.12),
                TaxBracket(63100.0, 100500.0, 0.22),
                TaxBracket(100500.0, 191750.0, 0.24),
                TaxBracket(191750.0, 243700.0, 0.32),
                TaxBracket(243700.0, 609350.0, 0.35),
                TaxBracket(609350.0, Double.MAX_VALUE, 0.37)
            )
        ),

        // Updated India AY 2025-26 Tax Brackets - New Regime (Default)
        "India" to mapOf(
            "Individual (<60 years) - New Regime" to listOf(
                TaxBracket(0.0, 300000.0, 0.0),
                TaxBracket(300000.0, 700000.0, 0.05),
                TaxBracket(700000.0, 1000000.0, 0.10),
                TaxBracket(1000000.0, 1200000.0, 0.15),
                TaxBracket(1200000.0, 1500000.0, 0.20),
                TaxBracket(1500000.0, Double.MAX_VALUE, 0.30)
            ),
            "Individual (<60 years) - Old Regime" to listOf(
                TaxBracket(0.0, 250000.0, 0.0),
                TaxBracket(250000.0, 500000.0, 0.05),
                TaxBracket(500000.0, 1000000.0, 0.20),
                TaxBracket(1000000.0, Double.MAX_VALUE, 0.30)
            ),
            "Senior Citizen (60-80 years) - New Regime" to listOf(
                TaxBracket(0.0, 300000.0, 0.0),
                TaxBracket(300000.0, 700000.0, 0.05),
                TaxBracket(700000.0, 1000000.0, 0.10),
                TaxBracket(1000000.0, 1200000.0, 0.15),
                TaxBracket(1200000.0, 1500000.0, 0.20),
                TaxBracket(1500000.0, Double.MAX_VALUE, 0.30)
            ),
            "Senior Citizen (60-80 years) - Old Regime" to listOf(
                TaxBracket(0.0, 300000.0, 0.0),
                TaxBracket(300000.0, 500000.0, 0.05),
                TaxBracket(500000.0, 1000000.0, 0.20),
                TaxBracket(1000000.0, Double.MAX_VALUE, 0.30)
            ),
            "Super Senior Citizen (80+ years) - New Regime" to listOf(
                TaxBracket(0.0, 300000.0, 0.0),
                TaxBracket(300000.0, 700000.0, 0.05),
                TaxBracket(700000.0, 1000000.0, 0.10),
                TaxBracket(1000000.0, 1200000.0, 0.15),
                TaxBracket(1200000.0, 1500000.0, 0.20),
                TaxBracket(1500000.0, Double.MAX_VALUE, 0.30)
            ),
            "Super Senior Citizen (80+ years) - Old Regime" to listOf(
                TaxBracket(0.0, 500000.0, 0.0),
                TaxBracket(500000.0, 1000000.0, 0.20),
                TaxBracket(1000000.0, Double.MAX_VALUE, 0.30)
            )
        ),

        // United Kingdom - 2025 Tax Brackets (unchanged)
        "United Kingdom" to mapOf(
            "Individual" to listOf(
                TaxBracket(0.0, 12570.0, 0.0),
                TaxBracket(12570.0, 50270.0, 0.20),
                TaxBracket(50270.0, 125140.0, 0.40),
                TaxBracket(125140.0, Double.MAX_VALUE, 0.45)
            ),
            "Scotland" to listOf(
                TaxBracket(0.0, 12570.0, 0.0),
                TaxBracket(12570.0, 14876.0, 0.19),
                TaxBracket(14876.0, 26561.0, 0.20),
                TaxBracket(26561.0, 43662.0, 0.21),
                TaxBracket(43662.0, 75000.0, 0.42),
                TaxBracket(75000.0, 125140.0, 0.45),
                TaxBracket(125140.0, Double.MAX_VALUE, 0.48)
            )
        ),

        // Other countries remain the same...
        "Germany" to mapOf(
            "Single" to listOf(
                TaxBracket(0.0, 11604.0, 0.0),
                TaxBracket(11604.0, 17005.0, 0.14),
                TaxBracket(17005.0, 66760.0, 0.24),
                TaxBracket(66760.0, 277825.0, 0.42),
                TaxBracket(277825.0, Double.MAX_VALUE, 0.45)
            ),
            "Married" to listOf(
                TaxBracket(0.0, 23208.0, 0.0),
                TaxBracket(23208.0, 34010.0, 0.14),
                TaxBracket(34010.0, 133520.0, 0.24),
                TaxBracket(133520.0, 555650.0, 0.42),
                TaxBracket(555650.0, Double.MAX_VALUE, 0.45)
            )
        ),

        "Canada" to mapOf(
            "Federal" to listOf(
                TaxBracket(0.0, 55867.0, 0.15),
                TaxBracket(55867.0, 111733.0, 0.205),
                TaxBracket(111733.0, 173205.0, 0.26),
                TaxBracket(173205.0, 246752.0, 0.29),
                TaxBracket(246752.0, Double.MAX_VALUE, 0.33)
            ),
            "Ontario Combined" to listOf(
                TaxBracket(0.0, 51446.0, 0.2005),
                TaxBracket(51446.0, 55867.0, 0.2415),
                TaxBracket(55867.0, 102135.0, 0.2915),
                TaxBracket(102135.0, 111733.0, 0.3148),
                TaxBracket(111733.0, 173205.0, 0.3748),
                TaxBracket(173205.0, 246752.0, 0.4048),
                TaxBracket(246752.0, Double.MAX_VALUE, 0.4448)
            )
        ),

        "Australia" to mapOf(
            "Resident" to listOf(
                TaxBracket(0.0, 18200.0, 0.0),
                TaxBracket(18200.0, 45000.0, 0.19),
                TaxBracket(45000.0, 120000.0, 0.325),
                TaxBracket(120000.0, 180000.0, 0.37),
                TaxBracket(180000.0, Double.MAX_VALUE, 0.45)
            ),
            "Non-Resident" to listOf(
                TaxBracket(0.0, 120000.0, 0.325),
                TaxBracket(120000.0, 180000.0, 0.37),
                TaxBracket(180000.0, Double.MAX_VALUE, 0.45)
            )
        ),

        "France" to mapOf(
            "Single" to listOf(
                TaxBracket(0.0, 11294.0, 0.0),
                TaxBracket(11294.0, 28797.0, 0.11),
                TaxBracket(28797.0, 82341.0, 0.30),
                TaxBracket(82341.0, 177106.0, 0.41),
                TaxBracket(177106.0, Double.MAX_VALUE, 0.45)
            ),
            "Married/PACS" to listOf(
                TaxBracket(0.0, 22588.0, 0.0),
                TaxBracket(22588.0, 57594.0, 0.11),
                TaxBracket(57594.0, 164682.0, 0.30),
                TaxBracket(164682.0, 354212.0, 0.41),
                TaxBracket(354212.0, Double.MAX_VALUE, 0.45)
            )
        ),

        "Japan" to mapOf(
            "Individual" to listOf(
                TaxBracket(0.0, 1950000.0, 0.05),
                TaxBracket(1950000.0, 3300000.0, 0.10),
                TaxBracket(3300000.0, 6950000.0, 0.20),
                TaxBracket(6950000.0, 9000000.0, 0.23),
                TaxBracket(9000000.0, 18000000.0, 0.33),
                TaxBracket(18000000.0, 40000000.0, 0.40),
                TaxBracket(40000000.0, Double.MAX_VALUE, 0.45)
            )
        ),

        "Singapore" to mapOf(
            "Resident" to listOf(
                TaxBracket(0.0, 20000.0, 0.0),
                TaxBracket(20000.0, 30000.0, 0.02),
                TaxBracket(30000.0, 40000.0, 0.035),
                TaxBracket(40000.0, 80000.0, 0.07),
                TaxBracket(80000.0, 120000.0, 0.115),
                TaxBracket(120000.0, 160000.0, 0.15),
                TaxBracket(160000.0, 200000.0, 0.18),
                TaxBracket(200000.0, 240000.0, 0.19),
                TaxBracket(240000.0, 280000.0, 0.195),
                TaxBracket(280000.0, 320000.0, 0.20),
                TaxBracket(320000.0, Double.MAX_VALUE, 0.24)
            ),
            "Non-Resident" to listOf(
                TaxBracket(0.0, Double.MAX_VALUE, 0.24)
            )
        ),

        "UAE" to mapOf(
            "Individual" to listOf(
                TaxBracket(0.0, Double.MAX_VALUE, 0.0)
            )
        )
    )

    // Updated Standard deductions for AY 2025-26
    private val standardDeductions = mapOf(
        "United States" to mapOf(
            "Single" to 15000.0,
            "Married Filing Jointly" to 30000.0,
            "Married Filing Separately" to 15000.0,
            "Head of Household" to 22500.0
        ),
        "India" to mapOf(
            // Updated for AY 2025-26 with correct standard deduction amounts
            "Individual (<60 years) - New Regime" to 75000.0,
            "Individual (<60 years) - Old Regime" to 0.0, // Old regime has no standard deduction
            "Senior Citizen (60-80 years) - New Regime" to 75000.0,
            "Senior Citizen (60-80 years) - Old Regime" to 0.0,
            "Super Senior Citizen (80+ years) - New Regime" to 75000.0,
            "Super Senior Citizen (80+ years) - Old Regime" to 0.0
        ),
        "United Kingdom" to mapOf(
            "Individual" to 12570.0,
            "Scotland" to 12570.0
        ),
        "Germany" to mapOf(
            "Single" to 1230.0,
            "Married" to 2460.0
        ),
        "Canada" to mapOf(
            "Federal" to 15705.0,
            "Ontario Combined" to 12399.0
        ),
        "Australia" to mapOf(
            "Resident" to 18200.0,
            "Non-Resident" to 0.0
        ),
        "France" to mapOf(
            "Single" to 4926.0,
            "Married/PACS" to 9852.0
        ),
        "Japan" to mapOf(
            "Individual" to 480000.0
        ),
        "Singapore" to mapOf(
            "Resident" to 20000.0,
            "Non-Resident" to 0.0
        ),
        "UAE" to mapOf(
            "Individual" to 0.0
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentTaxCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        setupSpinners()
        setupListeners()
        setupTextWatchers()
        calculateTax()
    }

    private fun setupSpinners() {
        binding.apply {
            // Setup Country Spinner
            val countryAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                countries
            )
            countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCountry.adapter = countryAdapter

            // Setup Tax Year Spinner
            val taxYears = arrayOf("2025-26", "2024-25")
            val yearAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                taxYears
            )
            yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerTaxYear.adapter = yearAdapter

            // Initially set up filing status and other spinners based on default country
            updateFilingStatusOptions()
            updateStateSpinnerVisibility()
            updateIndianRegimeSpinnerVisibility()
        }
    }

    private fun updateFilingStatusOptions() {
        val selectedCountry = binding.spinnerCountry.selectedItem?.toString() ?: "United States"
        val brackets = taxBrackets[selectedCountry] ?: return

        val filingStatuses = brackets.keys.toTypedArray()
        val filingAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            filingStatuses
        )
        filingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFilingStatus.adapter = filingAdapter
    }

    private fun updateStateSpinnerVisibility() {
        val selectedCountry = binding.spinnerCountry.selectedItem?.toString() ?: "United States"

        binding.apply {
            if (selectedCountry == "United States") {
                layoutState.visibility = View.VISIBLE
                val stateAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    usStates
                )
                stateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerState.adapter = stateAdapter
            } else {
                layoutState.visibility = View.GONE
            }
        }
    }

    // New method to handle Indian tax regime spinner visibility
    private fun updateIndianRegimeSpinnerVisibility() {
        val selectedCountry = binding.spinnerCountry.selectedItem?.toString() ?: "United States"

        binding.apply {
            if (selectedCountry == "India") {
                // Show Indian tax regime spinner
                layoutIndianRegime.visibility = View.VISIBLE
                val regimeAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    indianTaxRegimes
                )
                regimeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerIndianRegime.adapter = regimeAdapter
            } else {
                layoutIndianRegime.visibility = View.GONE
            }
        }
    }

    private fun updateCurrencySymbol() {
        val selectedCountry = binding.spinnerCountry.selectedItem?.toString() ?: "United States"
        val currencySymbol = currencySymbols[selectedCountry] ?: "$"

        binding.apply {
            tvCurrencySymbol.text = currencySymbol
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
                shareTaxCalculation()
            }

            // Country selection listener
            spinnerCountry.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    updateFilingStatusOptions()
                    updateStateSpinnerVisibility()
                    updateIndianRegimeSpinnerVisibility()
                    updateStandardDeduction()
                    updateCurrencySymbol()
                    calculateTax()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })

            // Filing status listener
            spinnerFilingStatus.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    updateStandardDeduction()
                    calculateTax()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })

            // State listener (for US)
            spinnerState.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    calculateTax()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })

            // Indian regime listener
            spinnerIndianRegime.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    updateFilingStatusOptionsForIndianRegime()
                    updateStandardDeduction()
                    calculateTax()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })

            // Tax year listener
            spinnerTaxYear.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    calculateTax()
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            })

            // Use standard deduction checkbox
            cbUseStandardDeduction.setOnCheckedChangeListener { _, isChecked ->
                etItemizedDeductions.isEnabled = !isChecked
                etItemizedDeductions.alpha = if (isChecked) 0.5f else 1.0f
                if (isChecked) {
                    updateStandardDeduction()
                }
                calculateTax()
            }
        }
    }

    // New method to update filing status options based on Indian regime selection
    private fun updateFilingStatusOptionsForIndianRegime() {
        val selectedCountry = binding.spinnerCountry.selectedItem?.toString() ?: "United States"
        if (selectedCountry != "India") return

        val selectedRegime = binding.spinnerIndianRegime.selectedItem?.toString() ?: "New Regime (Default)"
        val regimeSuffix = if (selectedRegime.contains("New")) "New Regime" else "Old Regime"

        val brackets = taxBrackets["India"] ?: return
        val filingStatuses = brackets.keys.filter { it.contains(regimeSuffix) }.toTypedArray()

        val filingAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            filingStatuses
        )
        filingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFilingStatus.adapter = filingAdapter
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isCalculating) {
                    calculateTax()
                }
            }
        }

        binding.apply {
            etGrossIncome.addTextChangedListener(textWatcher)
            etTaxableIncome.addTextChangedListener(textWatcher)
            etItemizedDeductions.addTextChangedListener(textWatcher)
            etWithholdings.addTextChangedListener(textWatcher)
            etEstimatedPayments.addTextChangedListener(textWatcher)
        }
    }

    private fun updateStandardDeduction() {
        val selectedCountry = binding.spinnerCountry.selectedItem?.toString() ?: "United States"
        val filingStatus = binding.spinnerFilingStatus.selectedItem?.toString() ?: ""

        val countryDeductions = standardDeductions[selectedCountry] ?: return
        val standardDeduction = countryDeductions[filingStatus] ?: 0.0

        if (binding.cbUseStandardDeduction.isChecked) {
            binding.etItemizedDeductions.setText(String.format("%.0f", standardDeduction))
        }
    }

    private fun calculateTax() {
        binding.apply {
            try {
                val selectedCountry = spinnerCountry.selectedItem?.toString() ?: "United States"
                val filingStatus = spinnerFilingStatus.selectedItem?.toString() ?: ""
                val selectedState = if (selectedCountry == "United States") {
                    spinnerState.selectedItem?.toString() ?: "No State Tax"
                } else ""

                val grossIncome = etGrossIncome.text.toString().toDoubleOrNull() ?: 0.0
                val deductions = etItemizedDeductions.text.toString().toDoubleOrNull() ?: 0.0
                val withholdings = etWithholdings.text.toString().toDoubleOrNull() ?: 0.0
                val estimatedPayments = etEstimatedPayments.text.toString().toDoubleOrNull() ?: 0.0

                // Get tax brackets for selected country and filing status
                val countryBrackets = taxBrackets[selectedCountry] ?: return
                val brackets = countryBrackets[filingStatus] ?: return

                // Calculate taxable income
                val taxableIncome = max(0.0, grossIncome - deductions)

                // Set taxable income (prevent recursive calculation)
                isCalculating = true
                etTaxableIncome.setText(String.format("%.0f", taxableIncome))
                isCalculating = false

                // Calculate income tax
                val incomeTax = calculateTaxFromBrackets(taxableIncome, brackets)

                // Calculate state tax for US
                val stateTax = if (selectedCountry == "United States" && selectedState.isNotEmpty()) {
                    val stateRate = stateTaxRates[selectedState] ?: 0.0
                    taxableIncome * stateRate
                } else 0.0

                // Calculate additional taxes based on country
                val additionalTax = calculateAdditionalTaxes(selectedCountry, grossIncome, taxableIncome, filingStatus)

                val totalTax = incomeTax + stateTax + additionalTax
                val totalPayments = withholdings + estimatedPayments
                val refundOrOwed = totalPayments - totalTax

                val effectiveRate = if (grossIncome > 0) (totalTax / grossIncome) * 100 else 0.0
                val marginalRate = getMarginalTaxRate(taxableIncome, brackets) * 100

                // Calculate rebate for India
                val rebate = calculateIndianRebate(selectedCountry, filingStatus, taxableIncome, incomeTax)
                val finalTotalTax = max(0.0, totalTax - rebate)

                // Update results
                updateResults(
                    selectedCountry, grossIncome, taxableIncome, deductions, incomeTax,
                    stateTax, additionalTax, finalTotalTax, totalPayments, refundOrOwed,
                    effectiveRate, marginalRate, filingStatus, selectedState, rebate
                )

            } catch (e: Exception) {
                // Handle calculation errors silently
            }
        }
    }

    private fun calculateIndianRebate(country: String, filingStatus: String, taxableIncome: Double, incomeTax: Double): Double {
        if (country != "India") return 0.0

        return when {
            // New Regime rebate under Section 87A
            filingStatus.contains("New Regime") && taxableIncome <= 700000.0 -> {
                min(25000.0, incomeTax) // Rebate up to ₹25,000 for income up to ₹7 lakh
            }
            // Old Regime rebate under Section 87A
            filingStatus.contains("Old Regime") && taxableIncome <= 500000.0 -> {
                min(12500.0, incomeTax) // Rebate up to ₹12,500 for income up to ₹5 lakh
            }
            else -> 0.0
        }
    }

    private fun calculateAdditionalTaxes(country: String, grossIncome: Double, taxableIncome: Double, filingStatus: String): Double {
        return when (country) {
            "United States" -> {
                // Social Security (6.2%) + Medicare (1.45%) up to certain limits
                val socialSecurity = min(grossIncome * 0.062, 168600 * 0.062) // 2025 SS wage base
                val medicare = grossIncome * 0.0145
                val additionalMedicare = if (grossIncome > 200000) (grossIncome - 200000) * 0.009 else 0.0
                socialSecurity + medicare + additionalMedicare
            }
            "India" -> {
                // Health and Education Cess @ 4% on income tax
                val brackets = taxBrackets["India"]?.get(filingStatus) ?: return 0.0
                val incomeTax = calculateTaxFromBrackets(taxableIncome, brackets)
                val rebate = calculateIndianRebate("India", filingStatus, taxableIncome, incomeTax)
                val taxAfterRebate = max(0.0, incomeTax - rebate)
                taxAfterRebate * 0.04 // 4% cess on tax after rebate
            }
            "United Kingdom" -> {
                // National Insurance
                val niThreshold = 12570.0
                val upperEarningsLimit = 50270.0
                if (grossIncome > niThreshold) {
                    val niableIncome = grossIncome - niThreshold
                    if (niableIncome <= upperEarningsLimit - niThreshold) {
                        niableIncome * 0.12
                    } else {
                        (upperEarningsLimit - niThreshold) * 0.12 + (niableIncome - (upperEarningsLimit - niThreshold)) * 0.02
                    }
                } else 0.0
            }
            "Germany" -> {
                // Social Insurance (approx 20% total)
                grossIncome * 0.20
            }
            "Canada" -> {
                // CPP + EI contributions
                val cpp = min(grossIncome * 0.0595, 3754.45) // 2025 max
                val ei = min(grossIncome * 0.0229, 1049.48) // 2025 max
                cpp + ei
            }
            "Australia" -> {
                // Medicare Levy (2%) + Medicare Levy Surcharge if applicable
                val medicareLevy = if (grossIncome > 29207) grossIncome * 0.02 else 0.0
                val medicareSurcharge = if (grossIncome > 97000) grossIncome * 0.01 else 0.0
                medicareLevy + medicareSurcharge
            }
            "France" -> {
                // Social contributions (approx 8% for employees)
                grossIncome * 0.08
            }
            "Japan" -> {
                // Social insurance (approx 15%)
                grossIncome * 0.15
            }
            else -> 0.0
        }
    }

    private fun calculateTaxFromBrackets(income: Double, brackets: List<TaxBracket>): Double {
        var tax = 0.0
        var remainingIncome = income

        for (bracket in brackets) {
            if (remainingIncome <= 0) break

            val taxableInThisBracket = min(remainingIncome, bracket.upperLimit - bracket.lowerLimit)
            if (taxableInThisBracket > 0) {
                tax += taxableInThisBracket * bracket.rate
                remainingIncome -= taxableInThisBracket
            }
        }

        return tax
    }

    private fun getMarginalTaxRate(income: Double, brackets: List<TaxBracket>): Double {
        for (bracket in brackets) {
            if (income <= bracket.upperLimit) {
                return bracket.rate
            }
        }
        return brackets.last().rate
    }

    private fun updateResults(
        country: String, grossIncome: Double, taxableIncome: Double, deductions: Double,
        incomeTax: Double, stateTax: Double, additionalTax: Double, totalTax: Double,
        totalPayments: Double, refundOrOwed: Double, effectiveRate: Double,
        marginalRate: Double, filingStatus: String, selectedState: String, rebate: Double
    ) {
        binding.apply {
            val currencySymbol = currencySymbols[country] ?: "$"

            // Update currency symbol in UI
            tvCurrencySymbol.text = currencySymbol

            // Main result
            tvTotalTax.text = String.format("%.0f", totalTax)
            tvEffectiveRate.text = String.format("%.1f%%", effectiveRate)

            // Breakdown
            tvGrossIncomeResult.text = String.format("%s%.0f", currencySymbol, grossIncome)
            tvDeductionsResult.text = String.format("%s%.0f", currencySymbol, deductions)
            tvTaxableIncomeResult.text = String.format("%s%.0f", currencySymbol, taxableIncome)
            tvIncomeTax.text = String.format("%s%.0f", currencySymbol, incomeTax)
            tvAdditionalTax.text = String.format("%s%.0f", currencySymbol, stateTax + additionalTax)
            tvTotalPayments.text = String.format("%s%.0f", currencySymbol, totalPayments)

            // Refund or owed
            val finalRefundOrOwed = totalPayments - totalTax
            if (finalRefundOrOwed >= 0) {
                tvRefundOwed.text = String.format("%s%.0f Refund", currencySymbol, finalRefundOrOwed)
                tvRefundOwed.setTextColor(android.graphics.Color.parseColor("#00AA00"))
            } else {
                tvRefundOwed.text = String.format("%s%.0f Owed", currencySymbol, kotlin.math.abs(finalRefundOrOwed))
                tvRefundOwed.setTextColor(android.graphics.Color.parseColor("#FF6B6B"))
            }

            // Update detailed analysis
            updateDetailedAnalysis(
                country, grossIncome, taxableIncome, deductions, incomeTax, stateTax,
                additionalTax, totalTax, totalPayments, finalRefundOrOwed, effectiveRate,
                marginalRate, filingStatus, selectedState, currencySymbol, rebate
            )
        }
    }

    private fun updateDetailedAnalysis(
        country: String, grossIncome: Double, taxableIncome: Double, deductions: Double,
        incomeTax: Double, stateTax: Double, additionalTax: Double, totalTax: Double,
        totalPayments: Double, refundOrOwed: Double, effectiveRate: Double,
        marginalRate: Double, filingStatus: String, selectedState: String,
        currencySymbol: String, rebate: Double
    ) {
        binding.apply {
            val analysis = StringBuilder()
            analysis.append("📊 International Tax Analysis (AY 2025-26)\n\n")

            analysis.append("🌍 Tax Jurisdiction:\n")
            analysis.append("• Country: $country\n")
            analysis.append("• Filing Status: $filingStatus\n")

            // Show Indian regime selection
            if (country == "India") {
                val selectedRegime = spinnerIndianRegime.selectedItem?.toString() ?: ""
                analysis.append("• Tax Regime: $selectedRegime\n")
                analysis.append("• Assessment Year: 2025-26\n")
            }

            if (country == "United States" && selectedState.isNotEmpty()) {
                analysis.append("• State: $selectedState\n")
            }
            analysis.append("• Tax Year: ${spinnerTaxYear.selectedItem}\n")
            analysis.append("• Currency: $currencySymbol\n\n")

            analysis.append("💰 Income Breakdown:\n")
            analysis.append("• Gross Income: $currencySymbol${String.format("%,.0f", grossIncome)}\n")
            analysis.append("• Deductions: $currencySymbol${String.format("%,.0f", deductions)}\n")
            analysis.append("• Taxable Income: $currencySymbol${String.format("%,.0f", taxableIncome)}\n\n")

            analysis.append("🏛️ Tax Breakdown:\n")
            analysis.append("• Income Tax: $currencySymbol${String.format("%,.0f", incomeTax)}\n")

            // Show rebate for India
            if (country == "India" && rebate > 0) {
                analysis.append("• Rebate u/s 87A: -$currencySymbol${String.format("%,.0f", rebate)}\n")
                analysis.append("• Tax after Rebate: $currencySymbol${String.format("%,.0f", max(0.0, incomeTax - rebate))}\n")
            }

            if (country == "United States" && stateTax > 0) {
                analysis.append("• State Tax ($selectedState): $currencySymbol${String.format("%,.0f", stateTax)}\n")
            }

            when (country) {
                "United States" -> analysis.append("• Social Security & Medicare: $currencySymbol${String.format("%,.0f", additionalTax)}\n")
                "India" -> analysis.append("• Health & Education Cess (4%): $currencySymbol${String.format("%,.0f", additionalTax)}\n")
                "United Kingdom" -> analysis.append("• National Insurance: $currencySymbol${String.format("%,.0f", additionalTax)}\n")
                "Germany" -> analysis.append("• Social Insurance: $currencySymbol${String.format("%,.0f", additionalTax)}\n")
                "Canada" -> analysis.append("• CPP & EI: $currencySymbol${String.format("%,.0f", additionalTax)}\n")
                "Australia" -> analysis.append("• Medicare Levy: $currencySymbol${String.format("%,.0f", additionalTax)}\n")
                "France" -> analysis.append("• Social Contributions: $currencySymbol${String.format("%,.0f", additionalTax)}\n")
                "Japan" -> analysis.append("• Social Insurance: $currencySymbol${String.format("%,.0f", additionalTax)}\n")
                else -> if (additionalTax > 0) analysis.append("• Additional Taxes: $currencySymbol${String.format("%,.0f", additionalTax)}\n")
            }

            analysis.append("• Total Tax: $currencySymbol${String.format("%,.0f", totalTax)}\n\n")

            analysis.append("📈 Tax Rates:\n")
            analysis.append("• Effective Rate: ${String.format("%.1f", effectiveRate)}%\n")
            analysis.append("• Marginal Rate: ${String.format("%.1f", marginalRate)}%\n\n")

            analysis.append("💳 Payments & Refund:\n")
            analysis.append("• Total Payments: $currencySymbol${String.format("%,.0f", totalPayments)}\n")
            if (refundOrOwed >= 0) {
                analysis.append("• Expected Refund: $currencySymbol${String.format("%,.0f", refundOrOwed)}\n")
            } else {
                analysis.append("• Amount Owed: $currencySymbol${String.format("%,.0f", kotlin.math.abs(refundOrOwed))}\n")
            }
            analysis.append("\n")

            // Country-specific tax planning advice
            analysis.append("💡 Tax Planning Tips for $country:\n")

            when (country) {
                "India" -> {
                    val regime = spinnerIndianRegime.selectedItem?.toString() ?: ""
                    if (regime.contains("New")) {
                        analysis.append("• New Regime Benefits: Higher exemption limit (₹3 lakh)\n")
                        analysis.append("• Rebate available up to ₹7 lakh income (₹25,000 max)\n")
                        analysis.append("• Standard deduction of ₹75,000 available\n")
                        analysis.append("• No major deductions like 80C, 80D available\n")
                        analysis.append("• Consider switching to old regime if you have large deductions\n")
                    } else {
                        analysis.append("• Old Regime: Lower exemption (₹2.5 lakh for <60 years)\n")
                        analysis.append("• Rebate available up to ₹5 lakh income (₹12,500 max)\n")
                        analysis.append("• Maximize Section 80C deductions (₹1.5 lakh limit)\n")
                        analysis.append("• Claim Section 80D for health insurance premiums\n")
                        analysis.append("• Consider HRA, LTA exemptions if applicable\n")
                        analysis.append("• House property interest deduction up to ₹2 lakh\n")
                    }
                    analysis.append("• File ITR-1 (SAHAJ) if eligible for simplified filing\n")
                    analysis.append("• Keep TDS certificates (Form 16, 16A) ready\n")

                    // Age-specific advice
                    if (filingStatus.contains("Senior Citizen")) {
                        if (filingStatus.contains("60-80")) {
                            analysis.append("• Senior Citizen: Higher exemption limit (₹3 lakh in old regime)\n")
                            analysis.append("• Section 80TTB deduction up to ₹50,000 on interest income\n")
                        } else if (filingStatus.contains("80+")) {
                            analysis.append("• Super Senior: Higher exemption (₹5 lakh in old regime)\n")
                            analysis.append("• Section 80TTB deduction up to ₹50,000 on interest income\n")
                        }
                    }
                }
                "United States" -> {
                    analysis.append("• Maximize 401(k) contributions ($23,000 limit for 2025)\n")
                    analysis.append("• Consider Roth IRA conversions if in lower tax bracket\n")
                    analysis.append("• Use HSA for triple tax advantage ($4,300/$8,550 limits)\n")
                    if (selectedState.isNotEmpty() && stateTax > 0) {
                        analysis.append("• Consider $selectedState state-specific deductions\n")
                        if (stateTaxRates[selectedState] ?: 0.0 > 0.07) {
                            analysis.append("• High state tax - consider tax-free municipal bonds\n")
                        }
                    }
                    if (effectiveRate > 22) {
                        analysis.append("• Consider municipal bonds for tax-free income\n")
                        analysis.append("• Look into tax-loss harvesting strategies\n")
                    }
                }
                else -> {
                    analysis.append("• Review tax situation quarterly\n")
                    analysis.append("• Keep detailed records of deductions\n")
                    analysis.append("• Stay updated on tax law changes\n")
                }
            }

            analysis.append("\n⚠️ Important Disclaimers:\n")
            analysis.append("• This is an estimate based on AY 2025-26 tax slabs\n")
            analysis.append("• Actual tax may vary based on specific circumstances\n")
            analysis.append("• For India: Surcharge and cess calculations included\n")
            analysis.append("• Consult a qualified tax professional for your situation\n")
            analysis.append("• Tax laws change frequently - verify current rates\n")

            tvTaxDetails.text = analysis.toString()
        }
    }

    private fun shareTaxCalculation() {
        val options = arrayOf("Share as Text", "Share as Image")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Share International Tax Calculation")
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
        val selectedCountry = binding.spinnerCountry.selectedItem.toString()
        val currencySymbol = currencySymbols[selectedCountry] ?: "$"
        val totalTax = binding.tvTotalTax.text.toString()
        val effectiveRate = binding.tvEffectiveRate.text.toString()
        val filingStatus = binding.spinnerFilingStatus.selectedItem.toString()
        val taxYear = binding.spinnerTaxYear.selectedItem.toString()

        val shareText = buildString {
            append("🌍 International Tax Calculator Results (AY 2025-26)\n\n")
            append("Country: $selectedCountry\n")
            append("Filing Status: $filingStatus\n")

            if (selectedCountry == "India") {
                append("Tax Regime: ${binding.spinnerIndianRegime.selectedItem}\n")
            }

            append("Tax Year: $taxYear\n")
            append("Total Tax: $currencySymbol$totalTax\n")
            append("Effective Rate: $effectiveRate\n\n")

            append("Breakdown:\n")
            append("• Gross Income: ${binding.tvGrossIncomeResult.text}\n")
            append("• Deductions: ${binding.tvDeductionsResult.text}\n")
            append("• Taxable Income: ${binding.tvTaxableIncomeResult.text}\n")
            append("• Income Tax: ${binding.tvIncomeTax.text}\n")
            append("• Additional Taxes: ${binding.tvAdditionalTax.text}\n")
            append("• Total Payments: ${binding.tvTotalPayments.text}\n")
            append("• ${binding.tvRefundOwed.text}\n\n")

            append("Calculated with International Tax Calculator (AY 2025-26)\n")
            append("⚠️ Estimate only - consult a qualified tax professional")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "International Tax Calculation - $selectedCountry (AY 2025-26)")
        }

        startActivity(Intent.createChooser(intent, "Share Tax Calculation"))
    }

    private fun captureAndShareScreenshot() {
        val screenshotView = binding.taxResultCard
        val bitmap = Bitmap.createBitmap(
            screenshotView.width,
            screenshotView.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        screenshotView.draw(canvas)

        val file = File(requireContext().cacheDir, "international_tax_calculation_ay2025_26.png")
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

        startActivity(Intent.createChooser(shareIntent, "Share Tax Calculation (AY 2025-26)"))
    }

    data class TaxBracket(
        val lowerLimit: Double,
        val upperLimit: Double,
        val rate: Double
    )

    companion object {
        private const val TAG = "TaxCalculatorFragment"
    }
}