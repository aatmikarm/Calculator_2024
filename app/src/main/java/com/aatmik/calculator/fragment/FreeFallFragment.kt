package com.aatmik.calculator.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentFreeFallBinding
import kotlin.math.pow

class FreeFallFragment : Fragment() {

    private lateinit var binding: FragmentFreeFallBinding
    private var isMetric = true // true = metric (m/s²), false = imperial (ft/s²)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFreeFallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        updateUnits()
    }

    private fun setupClickListeners() {
        // Back button
        binding.backIv.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        // Calculate button
        binding.btnCalculateHeight.setOnClickListener {
            calculateHeight()
        }

        // Unit toggle button
        binding.btnUnitToggle.setOnClickListener {
            toggleUnits()
        }

        // Clear button (optional)
        binding.btnClear?.setOnClickListener {
            clearFields()
        }
    }

    private fun calculateHeight() {
        val timeText = binding.etTime.text.toString()

        if (timeText.isEmpty()) {
            Toast.makeText(context, "Please enter fall time", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val time = timeText.toDouble()

            if (time <= 0) {
                Toast.makeText(context, "Time must be positive", Toast.LENGTH_SHORT).show()
                return
            }

            // Calculate height using h = 0.5 * g * t²
            val gravity = if (isMetric) 9.81 else 32.174 // m/s² or ft/s²
            val height = 0.5 * gravity * time.pow(2)

            // Display result
            val unit = if (isMetric) "meters" else "feet"
            val resultText = "Height: ${String.format("%.2f", height)} $unit"

            binding.tvHeightResult.text = resultText
            binding.tvHeightResult.visibility = View.VISIBLE

            // Show additional info
            showAdditionalInfo(height, time)

        } catch (e: NumberFormatException) {
            Toast.makeText(context, "Please enter a valid number", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAdditionalInfo(height: Double, time: Double) {
        val velocity = if (isMetric) 9.81 * time else 32.174 * time
        val velocityUnit = if (isMetric) "m/s" else "ft/s"

        val conversions = if (isMetric) {
            "≈ ${String.format("%.2f", height * 3.281)} feet\n" +
                    "≈ ${String.format("%.0f", height * 100)} centimeters\n" +
                    "Final velocity: ${String.format("%.2f", velocity)} $velocityUnit"
        } else {
            "≈ ${String.format("%.2f", height / 3.281)} meters\n" +
                    "≈ ${String.format("%.0f", height * 12)} inches\n" +
                    "Final velocity: ${String.format("%.2f", velocity)} $velocityUnit"
        }

        binding.tvAdditionalInfo.text = conversions
        binding.tvAdditionalInfo.visibility = View.VISIBLE
    }

    private fun toggleUnits() {
        isMetric = !isMetric
        updateUnits()
        clearFields()
    }

    private fun updateUnits() {
        if (isMetric) {
            binding.tvTimeLabel.text = "Time (seconds)"
            binding.btnUnitToggle.text = "Imperial"
            binding.etTime.hint = "Enter fall time in seconds"
        } else {
            binding.tvTimeLabel.text = "Time (seconds)"
            binding.btnUnitToggle.text = "Metric"
            binding.etTime.hint = "Enter fall time in seconds"
        }
    }

    private fun clearFields() {
        binding.etTime.setText("")
        binding.tvHeightResult.visibility = View.GONE
        binding.tvAdditionalInfo.visibility = View.GONE
    }
}