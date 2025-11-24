package com.aatmik.calculator.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.aatmik.calculator.R
import com.aatmik.calculator.databinding.FragmentCellDilutionBinding

class CellDilutionFragment : Fragment() {

    private lateinit var binding: FragmentCellDilutionBinding

    // Concentration units (cells/ml)
    private val concentrationUnits = arrayOf(
        "cells / ml",
        "cells / μl",
        "cells / l"
    )

    // Volume units
    private val volumeUnits = arrayOf(
        "ml",
        "μl",
        "l"
    )

    // Conversion factors to ml
    private val concentrationConversionFactors = mapOf(
        "cells / ml" to 1.0,
        "cells / μl" to 1000.0,  // 1 μl = 0.001 ml, so multiply by 1000
        "cells / l" to 0.001      // 1 l = 1000 ml, so divide by 1000
    )

    private val volumeConversionFactors = mapOf(
        "ml" to 1.0,
        "μl" to 0.001,  // 1 μl = 0.001 ml
        "l" to 1000.0    // 1 l = 1000 ml
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        binding.apply {
            // Handle back button
            backIv.setOnClickListener {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }

            // Setup spinners
            setupSpinners()

            // Text watchers for auto-calculation
            etInitialConcentration.addTextChangedListener(dilutionTextWatcher)
            etVolumeForSuspension.addTextChangedListener(dilutionTextWatcher)
            etFinalConcentration.addTextChangedListener(dilutionTextWatcher)
            etFinalVolume.addTextChangedListener(dilutionTextWatcher)

            // Button listeners
            btnReloadCalculator.setOnClickListener {
                reloadCalculator()
            }

            btnClearAll.setOnClickListener {
                clearAllInputs()
            }
        }
    }

    private fun setupSpinners() {
        binding.apply {
            // Initial concentration unit spinner
            val concentrationAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                concentrationUnits
            )
            concentrationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerInitialConcentration.adapter = concentrationAdapter

            // Volume for suspension unit spinner
            val volumeAdapter1 = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                volumeUnits
            )
            volumeAdapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerVolumeForSuspension.adapter = volumeAdapter1

            // Final concentration unit spinner
            val concentrationAdapter2 = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                concentrationUnits
            )
            concentrationAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerFinalConcentration.adapter = concentrationAdapter2

            // Final volume unit spinner
            val volumeAdapter2 = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                volumeUnits
            )
            volumeAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerFinalVolume.adapter = volumeAdapter2

            // Set default selection
            spinnerInitialConcentration.setSelection(0) // cells / ml
            spinnerVolumeForSuspension.setSelection(0)  // ml
            spinnerFinalConcentration.setSelection(0)   // cells / ml
            spinnerFinalVolume.setSelection(0)          // ml

            // Spinner listeners
            val spinnerListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    calculateDilution()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            spinnerInitialConcentration.onItemSelectedListener = spinnerListener
            spinnerVolumeForSuspension.onItemSelectedListener = spinnerListener
            spinnerFinalConcentration.onItemSelectedListener = spinnerListener
            spinnerFinalVolume.onItemSelectedListener = spinnerListener
        }
    }

    private fun calculateDilution() {
        binding.apply {
            val c1Text = etInitialConcentration.text.toString()
            val v1Text = etVolumeForSuspension.text.toString()
            val c2Text = etFinalConcentration.text.toString()
            val v2Text = etFinalVolume.text.toString()

            // Count how many fields are filled
            val filledFields = listOf(c1Text, v1Text, c2Text, v2Text).count { it.isNotEmpty() }

            // Need exactly 3 fields filled to calculate the 4th
            if (filledFields < 3) {
                tvResult.text = "Enter any 3 values to calculate the 4th"
                tvResult.visibility = View.VISIBLE
                return
            }

            try {
                // Get conversion factors
                val c1Unit = spinnerInitialConcentration.selectedItem.toString()
                val v1Unit = spinnerVolumeForSuspension.selectedItem.toString()
                val c2Unit = spinnerFinalConcentration.selectedItem.toString()
                val v2Unit = spinnerFinalVolume.selectedItem.toString()

                val c1Factor = concentrationConversionFactors[c1Unit] ?: 1.0
                val v1Factor = volumeConversionFactors[v1Unit] ?: 1.0
                val c2Factor = concentrationConversionFactors[c2Unit] ?: 1.0
                val v2Factor = volumeConversionFactors[v2Unit] ?: 1.0

                // Using C1V1 = C2V2
                when {
                    // Calculate V1 (Volume for suspension)
                    v1Text.isEmpty() && c1Text.isNotEmpty() && c2Text.isNotEmpty() && v2Text.isNotEmpty() -> {
                        val c1 = c1Text.toDouble() * c1Factor
                        val c2 = c2Text.toDouble() * c2Factor
                        val v2 = v2Text.toDouble() * v2Factor

                        if (c1 <= 0) {
                            tvResult.text = "Initial concentration must be greater than 0"
                            tvResult.visibility = View.VISIBLE
                            return
                        }

                        // V1 = (C2 * V2) / C1
                        val v1 = (c2 * v2) / c1
                        val v1Converted = v1 / v1Factor

                        etVolumeForSuspension.setText(String.format("%.4f", v1Converted))

                        tvResult.text = "Volume needed: %.4f %s\n\nAdd this to make final volume of %.4f %s".format(
                            v1Converted, v1Unit, v2Text.toDouble(), v2Unit
                        )
                        tvResult.visibility = View.VISIBLE
                    }

                    // Calculate C1 (Initial concentration)
                    c1Text.isEmpty() && v1Text.isNotEmpty() && c2Text.isNotEmpty() && v2Text.isNotEmpty() -> {
                        val v1 = v1Text.toDouble() * v1Factor
                        val c2 = c2Text.toDouble() * c2Factor
                        val v2 = v2Text.toDouble() * v2Factor

                        if (v1 <= 0) {
                            tvResult.text = "Volume for suspension must be greater than 0"
                            tvResult.visibility = View.VISIBLE
                            return
                        }

                        // C1 = (C2 * V2) / V1
                        val c1 = (c2 * v2) / v1
                        val c1Converted = c1 / c1Factor

                        etInitialConcentration.setText(String.format("%.2f", c1Converted))

                        tvResult.text = "Initial concentration needed: %.2f %s".format(c1Converted, c1Unit)
                        tvResult.visibility = View.VISIBLE
                    }

                    // Calculate C2 (Final concentration)
                    c2Text.isEmpty() && c1Text.isNotEmpty() && v1Text.isNotEmpty() && v2Text.isNotEmpty() -> {
                        val c1 = c1Text.toDouble() * c1Factor
                        val v1 = v1Text.toDouble() * v1Factor
                        val v2 = v2Text.toDouble() * v2Factor

                        if (v2 <= 0) {
                            tvResult.text = "Final volume must be greater than 0"
                            tvResult.visibility = View.VISIBLE
                            return
                        }

                        // C2 = (C1 * V1) / V2
                        val c2 = (c1 * v1) / v2
                        val c2Converted = c2 / c2Factor

                        etFinalConcentration.setText(String.format("%.2f", c2Converted))

                        tvResult.text = "Final concentration: %.2f %s".format(c2Converted, c2Unit)
                        tvResult.visibility = View.VISIBLE
                    }

                    // Calculate V2 (Final volume)
                    v2Text.isEmpty() && c1Text.isNotEmpty() && v1Text.isNotEmpty() && c2Text.isNotEmpty() -> {
                        val c1 = c1Text.toDouble() * c1Factor
                        val v1 = v1Text.toDouble() * v1Factor
                        val c2 = c2Text.toDouble() * c2Factor

                        if (c2 <= 0) {
                            tvResult.text = "Final concentration must be greater than 0"
                            tvResult.visibility = View.VISIBLE
                            return
                        }

                        // V2 = (C1 * V1) / C2
                        val v2 = (c1 * v1) / c2
                        val v2Converted = v2 / v2Factor

                        etFinalVolume.setText(String.format("%.4f", v2Converted))

                        tvResult.text = "Final volume will be: %.4f %s".format(v2Converted, v2Unit)
                        tvResult.visibility = View.VISIBLE
                    }

                    else -> {
                        tvResult.text = "Enter exactly 3 values to calculate the 4th"
                        tvResult.visibility = View.VISIBLE
                    }
                }

            } catch (e: NumberFormatException) {
                Log.e(TAG, "Invalid number format", e)
                tvResult.text = "Please enter valid numerical values"
                tvResult.visibility = View.VISIBLE
            }
        }
    }

    private fun reloadCalculator() {
        clearAllInputs()
        binding.tvResult.text = "Calculator reloaded. Enter 3 values to calculate."
    }

    private fun clearAllInputs() {
        binding.apply {
            etInitialConcentration.text?.clear()
            etVolumeForSuspension.text?.clear()
            etFinalConcentration.text?.clear()
            etFinalVolume.text?.clear()
            tvResult.visibility = View.GONE

            // Reset spinners to default
            spinnerInitialConcentration.setSelection(0)
            spinnerVolumeForSuspension.setSelection(0)
            spinnerFinalConcentration.setSelection(0)
            spinnerFinalVolume.setSelection(0)
        }
    }

    private val dilutionTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            // Auto-calculate on text change with slight delay
            binding.root.postDelayed({
                calculateDilution()
            }, 300)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCellDilutionBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        private const val TAG = "CellDilutionFragment"
    }
}