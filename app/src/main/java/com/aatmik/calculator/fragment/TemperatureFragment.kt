package com.aatmik.calculator.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentTemperatureBinding
import kotlin.math.roundToInt

class TemperatureFragment : Fragment() {

    private lateinit var binding: FragmentTemperatureBinding
    private val temperatureUnits = arrayOf(
        "Celsius",
        "Fahrenheit",
        "Kelvin",
        "Rankine",
        "Réaumur",
        "Delisle",
        "Newton",
        "Rømer",
        "Leiden",
        "Gas mark"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentTemperatureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()
        setupListeners()
    }

    private fun setupSpinners() {
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, temperatureUnits)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.fromUnitSpinner.adapter = adapter
        binding.toUnitSpinner.adapter = adapter
    }

    private fun setupListeners() {
        binding.backIv.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        binding.convertButton.setOnClickListener {
            convertTemperature()
        }
    }

    private fun convertTemperature() {
        val inputTemp = binding.inputTemperature.text.toString().toDoubleOrNull()
        if (inputTemp == null) {
            binding.resultText.text = "Please enter a valid temperature"
            return
        }

        val fromUnit = binding.fromUnitSpinner.selectedItem.toString()
        val toUnit = binding.toUnitSpinner.selectedItem.toString()

        val celsiusTemp = toCelsius(inputTemp, fromUnit)
        val result = fromCelsius(celsiusTemp, toUnit)

        binding.resultText.text = "Result: ${result.roundToInt()} $toUnit"
    }

    private fun toCelsius(temp: Double, unit: String): Double = when (unit) {
        "Celsius" -> temp
        "Fahrenheit" -> (temp - 32) * 5 / 9
        "Kelvin" -> temp - 273.15
        "Rankine" -> (temp - 491.67) * 5 / 9
        "Réaumur" -> temp * 5 / 4
        "Delisle" -> 100 - temp * 2 / 3
        "Newton" -> temp * 100 / 33
        "Rømer" -> (temp - 7.5) * 40 / 21
        "Leiden" -> temp + 253.15
        "Gas mark" -> (temp * 125 + 130) * 5 / 9
        else -> throw IllegalArgumentException("Invalid unit: $unit")
    }

    private fun fromCelsius(celsius: Double, unit: String): Double = when (unit) {
        "Celsius" -> celsius
        "Fahrenheit" -> celsius * 9 / 5 + 32
        "Kelvin" -> celsius + 273.15
        "Rankine" -> (celsius + 273.15) * 9 / 5
        "Réaumur" -> celsius * 4 / 5
        "Delisle" -> (100 - celsius) * 3 / 2
        "Newton" -> celsius * 33 / 100
        "Rømer" -> celsius * 21 / 40 + 7.5
        "Leiden" -> celsius - 253.15
        "Gas mark" -> (celsius * 9 / 5 - 130) / 125
        else -> throw IllegalArgumentException("Invalid unit: $unit")
    }

    companion object {
        private const val TAG = "TemperatureFragment"
    }
}