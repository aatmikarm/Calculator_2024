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
    private val temperatureUnits = arrayOf("Celsius", "Fahrenheit", "Kelvin")

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

        val result = when {
            fromUnit == toUnit -> inputTemp
            fromUnit == "Celsius" && toUnit == "Fahrenheit" -> celsiusToFahrenheit(inputTemp)
            fromUnit == "Celsius" && toUnit == "Kelvin" -> celsiusToKelvin(inputTemp)
            fromUnit == "Fahrenheit" && toUnit == "Celsius" -> fahrenheitToCelsius(inputTemp)
            fromUnit == "Fahrenheit" && toUnit == "Kelvin" -> fahrenheitToKelvin(inputTemp)
            fromUnit == "Kelvin" && toUnit == "Celsius" -> kelvinToCelsius(inputTemp)
            fromUnit == "Kelvin" && toUnit == "Fahrenheit" -> kelvinToFahrenheit(inputTemp)
            else -> throw IllegalArgumentException("Invalid conversion")
        }

        binding.resultText.text = "Result: ${result.roundToInt()} $toUnit"
    }

    private fun celsiusToFahrenheit(celsius: Double): Double = celsius * 9 / 5 + 32
    private fun celsiusToKelvin(celsius: Double): Double = celsius + 273.15
    private fun fahrenheitToCelsius(fahrenheit: Double): Double = (fahrenheit - 32) * 5 / 9
    private fun fahrenheitToKelvin(fahrenheit: Double): Double = (fahrenheit + 459.67) * 5 / 9
    private fun kelvinToCelsius(kelvin: Double): Double = kelvin - 273.15
    private fun kelvinToFahrenheit(kelvin: Double): Double = kelvin * 9 / 5 - 459.67

    companion object {
        private const val TAG = "TemperatureFragment"
    }
}