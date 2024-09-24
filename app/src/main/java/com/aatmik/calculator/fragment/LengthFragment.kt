package com.aatmik.calculator.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.aatmik.calculator.adapter.LengthAdapter
import com.aatmik.calculator.databinding.FragmentLengthBinding
import com.aatmik.calculator.model.LengthUnit

class LengthFragment : Fragment() {

    private lateinit var binding: FragmentLengthBinding
    private lateinit var adapter: LengthAdapter
    private val units = mutableListOf<LengthUnit>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backIv.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        initializeUnits()
        setupAddButton()
        adapterCode()
    }

    private fun adapterCode() {
        adapter = LengthAdapter(units) { position, newValue ->
            convertUnits(position, newValue)
        }

        binding.unitsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.unitsRecyclerView.adapter = adapter

    }

    private fun convertUnits(changedPosition: Int, newValue: String) {
        val changedUnit = units[changedPosition]
        val newValueDouble = newValue.toDoubleOrNull() ?: return

        // Convert to base unit (meters)
        val valueInMeters = newValueDouble * changedUnit.conversionFactor

        // Update all other units
        units.forEachIndexed { index, unit ->
            if (index != changedPosition) {
                val convertedValue = valueInMeters / unit.conversionFactor
                val formattedValue = "%.6f".format(convertedValue).trimEnd('0').trimEnd('.')
                unit.value = formattedValue
                adapter.updateUnit(index, formattedValue)
            }
        }
    }

    private fun setupAddButton() {
        binding.addButton.setOnClickListener {
            // Implement add unit functionality
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentLengthBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun initializeUnits() {
        units.apply {
            add(LengthUnit("Meter", "m", 1.0))
            add(LengthUnit("Kilometer", "km", 1000.0))
            add(LengthUnit("Centimeter", "cm", 0.01))
            add(LengthUnit("Millimeter", "mm", 0.001))
            add(LengthUnit("Inch", "in", 0.0254))
            add(LengthUnit("Foot", "ft", 0.3048))
            add(LengthUnit("Yard", "yd", 0.9144))
            add(LengthUnit("Mile", "mi", 1609.34))
        }
    }


    companion object {
        private const val TAG = "LengthFragment"
    }
}